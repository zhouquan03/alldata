/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.presto.common.block;

import io.airlift.slice.SliceOutput;
import org.openjdk.jol.info.ClassLayout;

import javax.annotation.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

import static com.facebook.presto.common.block.BlockUtil.appendNullToIsNullArray;
import static com.facebook.presto.common.block.BlockUtil.appendNullToOffsetsArray;
import static com.facebook.presto.common.block.BlockUtil.checkArrayRange;
import static com.facebook.presto.common.block.BlockUtil.checkValidPositions;
import static com.facebook.presto.common.block.BlockUtil.checkValidRegion;
import static com.facebook.presto.common.block.BlockUtil.compactArray;
import static com.facebook.presto.common.block.BlockUtil.compactOffsets;
import static com.facebook.presto.common.block.BlockUtil.countAndMarkSelectedPositionsFromOffsets;
import static com.facebook.presto.common.block.BlockUtil.countSelectedPositionsFromOffsets;
import static com.facebook.presto.common.block.BlockUtil.internalPositionInRange;
import static com.facebook.presto.common.block.MapBlock.createMapBlockInternal;
import static com.facebook.presto.common.block.MapBlockBuilder.buildHashTable;
import static com.facebook.presto.common.block.MapBlockBuilder.verify;
import static io.airlift.slice.SizeOf.sizeOf;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class AbstractMapBlock
        implements Block
{
    // inverse of hash fill ratio, must be integer
    static final int HASH_MULTIPLIER = 2;

    protected volatile long logicalSizeInBytes;

    protected abstract Block getRawKeyBlock();

    protected abstract Block getRawValueBlock();

    protected abstract HashTables getHashTables();

    /**
     * offset is entry-based, not position-based. In other words,
     * if offset[1] is 6, it means the first map has 6 key-value pairs,
     * not 6 key/values (which would be 3 pairs).
     */
    protected abstract int[] getOffsets();

    /**
     * offset is entry-based, not position-based. (see getOffsets)
     */
    public abstract int getOffsetBase();

    @Nullable
    protected abstract boolean[] getMapIsNull();

    protected abstract void ensureHashTableLoaded(MethodHandle keyBlockHashCode);

    int getOffset(int position)
    {
        return getOffsets()[position + getOffsetBase()];
    }

    @Override
    public String getEncodingName()
    {
        return MapBlockEncoding.NAME;
    }

    @Override
    public Block copyPositions(int[] positions, int offset, int length)
    {
        checkArrayRange(positions, offset, length);

        int[] newOffsets = new int[length + 1];
        boolean[] newMapIsNull = new boolean[length];

        IntArrayList entriesPositions = new IntArrayList();
        int newPosition = 0;
        for (int i = offset; i < offset + length; ++i) {
            int position = positions[i];
            if (isNull(position)) {
                newMapIsNull[newPosition] = true;
                newOffsets[newPosition + 1] = newOffsets[newPosition];
            }
            else {
                int entriesStartOffset = getOffset(position);
                int entriesEndOffset = getOffset(position + 1);
                int entryCount = entriesEndOffset - entriesStartOffset;

                newOffsets[newPosition + 1] = newOffsets[newPosition] + entryCount;

                for (int elementIndex = entriesStartOffset; elementIndex < entriesEndOffset; elementIndex++) {
                    entriesPositions.add(elementIndex);
                }
            }
            newPosition++;
        }

        int[] rawHashTables = getHashTables().get();
        int[] newRawHashTables = null;
        int newHashTableEntries = newOffsets[newOffsets.length - 1] * HASH_MULTIPLIER;
        if (rawHashTables != null) {
            newRawHashTables = new int[newHashTableEntries];
            int newHashIndex = 0;
            for (int i = offset; i < offset + length; ++i) {
                int position = positions[i];
                int entriesStartOffset = getOffset(position);
                int entriesEndOffset = getOffset(position + 1);
                for (int hashIndex = entriesStartOffset * HASH_MULTIPLIER; hashIndex < entriesEndOffset * HASH_MULTIPLIER; hashIndex++) {
                    newRawHashTables[newHashIndex] = rawHashTables[hashIndex];
                    newHashIndex++;
                }
            }
        }

        Block newKeys = getRawKeyBlock().copyPositions(entriesPositions.elements(), 0, entriesPositions.size());
        Block newValues = getRawValueBlock().copyPositions(entriesPositions.elements(), 0, entriesPositions.size());
        return createMapBlockInternal(
                0,
                length,
                Optional.of(newMapIsNull),
                newOffsets,
                newKeys,
                newValues,
                new HashTables(Optional.ofNullable(newRawHashTables), length));
    }

    @Override
    public Block getRegion(int position, int length)
    {
        int positionCount = getPositionCount();
        checkValidRegion(positionCount, position, length);

        return createMapBlockInternal(
                position + getOffsetBase(),
                length,
                Optional.ofNullable(getMapIsNull()),
                getOffsets(),
                getRawKeyBlock(),
                getRawValueBlock(),
                getHashTables());
    }

    @Override
    public OptionalInt fixedSizeInBytesPerPosition()
    {
        return OptionalInt.empty(); // size per row is variable on the number of entries in each row
    }

    private OptionalInt keyAndValueFixedSizeInBytesPerRow()
    {
        OptionalInt keyFixedSizePerRow = getRawKeyBlock().fixedSizeInBytesPerPosition();
        if (!keyFixedSizePerRow.isPresent()) {
            return OptionalInt.empty();
        }
        OptionalInt valueFixedSizePerRow = getRawValueBlock().fixedSizeInBytesPerPosition();
        if (!valueFixedSizePerRow.isPresent()) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(keyFixedSizePerRow.getAsInt() + valueFixedSizePerRow.getAsInt());
    }

    @Override
    public long getRegionSizeInBytes(int position, int length)
    {
        int positionCount = getPositionCount();
        checkValidRegion(positionCount, position, length);

        int entriesStart = getOffsets()[getOffsetBase() + position];
        int entriesEnd = getOffsets()[getOffsetBase() + position + length];
        int entryCount = entriesEnd - entriesStart;

        return getRawKeyBlock().getRegionSizeInBytes(entriesStart, entryCount) +
                getRawValueBlock().getRegionSizeInBytes(entriesStart, entryCount) +
                (Integer.BYTES + Byte.BYTES) * (long) length +
                Integer.BYTES * HASH_MULTIPLIER * (long) entryCount;
    }

    @Override
    public long getLogicalSizeInBytes()
    {
        if (logicalSizeInBytes < 0) {
            calculateLogicalSize();
        }
        return logicalSizeInBytes;
    }

    @Override
    public long getRegionLogicalSizeInBytes(int position, int length)
    {
        int positionCount = getPositionCount();
        checkValidRegion(positionCount, position, length);

        int entriesStart = getOffsets()[getOffsetBase() + position];
        int entriesEnd = getOffsets()[getOffsetBase() + position + length];
        int entryCount = entriesEnd - entriesStart;

        return getRawKeyBlock().getRegionLogicalSizeInBytes(entriesStart, entryCount) +
                getRawValueBlock().getRegionLogicalSizeInBytes(entriesStart, entryCount) +
                (Integer.BYTES + Byte.BYTES) * (long) length +
                Integer.BYTES * HASH_MULTIPLIER * (long) entryCount;
    }

    @Override
    public long getApproximateRegionLogicalSizeInBytes(int position, int length)
    {
        int positionCount = getPositionCount();
        checkValidRegion(positionCount, position, length);

        int entriesStart = getOffset(position);
        int entriesEnd = getOffset(position + length);
        int entryCount = entriesEnd - entriesStart;

        return getRawKeyBlock().getApproximateRegionLogicalSizeInBytes(entriesStart, entryCount) +
                getRawValueBlock().getApproximateRegionLogicalSizeInBytes(entriesStart, entryCount) +
                (Integer.BYTES + Byte.BYTES) * (long) length +         // offsets and mapIsNull
                Integer.BYTES * HASH_MULTIPLIER * (long) entryCount;   // hash tables
    }

    @Override
    public final long getPositionsSizeInBytes(boolean[] positions, int selectedMapPositions)
    {
        int positionCount = getPositionCount();
        checkValidPositions(positions, positionCount);
        if (selectedMapPositions == 0) {
            return 0;
        }
        if (selectedMapPositions == positionCount) {
            return getSizeInBytes();
        }
        int[] offsets = getOffsets();
        int offsetBase = getOffsetBase();
        OptionalInt fixedKeyAndValueSizePerRow = keyAndValueFixedSizeInBytesPerRow();

        int selectedEntryCount;
        long keyAndValuesSizeInBytes;
        if (fixedKeyAndValueSizePerRow.isPresent()) {
            // no new positions array need be created, we can just count the number of elements
            selectedEntryCount = countSelectedPositionsFromOffsets(positions, offsets, offsetBase);
            keyAndValuesSizeInBytes = fixedKeyAndValueSizePerRow.getAsInt() * (long) selectedEntryCount;
        }
        else {
            // We can use either the getRegionSizeInBytes or getPositionsSizeInBytes
            // from the underlying raw blocks to implement this function. We chose
            // getPositionsSizeInBytes with the assumption that constructing a
            // positions array is cheaper than calling getRegionSizeInBytes for each
            // used position.
            boolean[] entryPositions = new boolean[getRawKeyBlock().getPositionCount()];
            selectedEntryCount = countAndMarkSelectedPositionsFromOffsets(positions, offsets, offsetBase, entryPositions);
            keyAndValuesSizeInBytes = getRawKeyBlock().getPositionsSizeInBytes(entryPositions, selectedEntryCount) +
                    getRawValueBlock().getPositionsSizeInBytes(entryPositions, selectedEntryCount);
        }
        return keyAndValuesSizeInBytes +
                (Integer.BYTES + Byte.BYTES) * (long) selectedMapPositions +
                Integer.BYTES * HASH_MULTIPLIER * (long) selectedEntryCount;
    }

    @Override
    public Block copyRegion(int position, int length)
    {
        int positionCount = getPositionCount();
        checkValidRegion(positionCount, position, length);

        int startValueOffset = getOffset(position);
        int endValueOffset = getOffset(position + length);
        Block newKeys = getRawKeyBlock().copyRegion(startValueOffset, endValueOffset - startValueOffset);
        Block newValues = getRawValueBlock().copyRegion(startValueOffset, endValueOffset - startValueOffset);

        int[] newOffsets = compactOffsets(getOffsets(), position + getOffsetBase(), length);
        boolean[] mapIsNull = getMapIsNull();
        boolean[] newMapIsNull = mapIsNull == null ? null : compactArray(mapIsNull, position + getOffsetBase(), length);

        int[] rawHashTables = getHashTables().get();
        int[] newRawHashTables = null;
        int expectedNewHashTableEntries = (endValueOffset - startValueOffset) * HASH_MULTIPLIER;
        if (rawHashTables != null) {
            newRawHashTables = compactArray(rawHashTables, startValueOffset * HASH_MULTIPLIER, expectedNewHashTableEntries);
        }

        if (newKeys == getRawKeyBlock() && newValues == getRawValueBlock() && newOffsets == getOffsets() && newMapIsNull == mapIsNull && newRawHashTables == rawHashTables) {
            return this;
        }
        return createMapBlockInternal(
                0,
                length,
                Optional.ofNullable(newMapIsNull),
                newOffsets,
                newKeys,
                newValues,
                new HashTables(Optional.ofNullable(newRawHashTables), length));
    }

    @Override
    public Block getBlock(int position)
    {
        checkReadablePosition(position);

        int startEntryOffset = getOffset(position);
        int endEntryOffset = getOffset(position + 1);
        return new SingleMapBlock(
                position,
                startEntryOffset * 2,
                (endEntryOffset - startEntryOffset) * 2,
                this);
    }

    @Override
    public void writePositionTo(int position, BlockBuilder blockBuilder)
    {
        checkReadablePosition(position);
        blockBuilder.appendStructureInternal(this, position);
    }

    @Override
    public void writePositionTo(int position, SliceOutput output)
    {
        if (isNull(position)) {
            output.writeByte(0);
        }
        else {
            int startValueOffset = getOffset(position);
            int endValueOffset = getOffset(position + 1);
            int numberOfElements = endValueOffset - startValueOffset;

            output.writeByte(1);
            output.writeInt(numberOfElements);
            Block rawKeyBlock = getRawKeyBlock();
            Block rawValueBlock = getRawValueBlock();
            for (int i = startValueOffset; i < endValueOffset; i++) {
                rawKeyBlock.writePositionTo(i, output);
                rawValueBlock.writePositionTo(i, output);
            }
        }
    }

    protected Block getSingleValueBlockInternal(int position)
    {
        checkReadablePosition(position);

        int startValueOffset = getOffset(position);
        int endValueOffset = getOffset(position + 1);
        int valueLength = endValueOffset - startValueOffset;
        Block newKeys = getRawKeyBlock().copyRegion(startValueOffset, valueLength);
        Block newValues = getRawValueBlock().copyRegion(startValueOffset, valueLength);

        int[] rawHashTables = getHashTables().get();
        int[] newRawHashTables = null;
        int expectedNewHashTableEntries = (endValueOffset - startValueOffset) * HASH_MULTIPLIER;
        if (rawHashTables != null) {
            newRawHashTables = Arrays.copyOfRange(rawHashTables, startValueOffset * HASH_MULTIPLIER, endValueOffset * HASH_MULTIPLIER);
        }

        return createMapBlockInternal(
                0,
                1,
                Optional.of(new boolean[] {isNull(position)}),
                new int[] {0, valueLength},
                newKeys,
                newValues,
                new HashTables(Optional.ofNullable(newRawHashTables), 1));
    }

    @Override
    public long getEstimatedDataSizeForStats(int position)
    {
        checkReadablePosition(position);

        if (isNull(position)) {
            return 0;
        }

        int startValueOffset = getOffset(position);
        int endValueOffset = getOffset(position + 1);

        long size = 0;
        Block rawKeyBlock = getRawKeyBlock();
        Block rawValueBlock = getRawValueBlock();
        for (int i = startValueOffset; i < endValueOffset; i++) {
            size += rawKeyBlock.getEstimatedDataSizeForStats(i);
            size += rawValueBlock.getEstimatedDataSizeForStats(i);
        }
        return size;
    }

    @Override
    public boolean mayHaveNull()
    {
        return getMapIsNull() != null;
    }

    @Override
    public boolean isNull(int position)
    {
        checkReadablePosition(position);
        boolean[] mapIsNull = getMapIsNull();
        return mapIsNull != null && mapIsNull[position + getOffsetBase()];
    }

    public boolean isHashTablesPresent()
    {
        return getHashTables().get() != null;
    }

    private void checkReadablePosition(int position)
    {
        if (position < 0 || position >= getPositionCount()) {
            throw new IllegalArgumentException("position is not valid");
        }
    }

    public static class HashTables
    {
        private static final int INSTANCE_SIZE = ClassLayout.parseClass(HashTables.class).instanceSize();

        // Hash to location in map. Writes to the field by MapBlock is protected by "HashTables" monitor in MapBlock.
        // MapBlockBuilder instances have their dedicated hashTables instances, so the write accesses to the hashTables
        // fields do not need to be synchronized in that class.
        @Nullable
        private volatile int[] hashTables;

        // The number of hash tables. Each map row corresponds to one hash table if it's built.
        private int expectedHashTableCount;

        HashTables(Optional<int[]> hashTables, int expectedHashTableCount)
        {
            this.hashTables = hashTables.orElse(null);
            this.expectedHashTableCount = expectedHashTableCount;
        }

        @Nullable
        int[] get()
        {
            return hashTables;
        }

        void set(int[] hashTables)
        {
            requireNonNull(hashTables, "hashTables is null");
            this.hashTables = hashTables;
        }

        void setExpectedHashTableCount(int count)
        {
            expectedHashTableCount = count;
        }

        int getExpectedHashTableCount()
        {
            return expectedHashTableCount;
        }

        public long getRetainedSizeInBytes()
        {
            return INSTANCE_SIZE + sizeOf(hashTables);
        }

        public void loadHashTables(int positionCount, int[] offsets, boolean[] mapIsNull, Block keyBlock, MethodHandle keyBlockHashCode)
        {
            int[] hashTables = new int[keyBlock.getPositionCount() * HASH_MULTIPLIER];
            Arrays.fill(hashTables, -1);

            verify(positionCount < offsets.length, "incorrect offsets size");

            for (int i = 0; i < positionCount; i++) {
                int keyOffset = offsets[i];
                int keyCount = offsets[i + 1] - keyOffset;
                if (keyCount < 0) {
                    throw new IllegalArgumentException(format("Offset is not monotonically ascending. offsets[%s]=%s, offsets[%s]=%s", i, offsets[i], i + 1, offsets[i + 1]));
                }
                if (mapIsNull != null && mapIsNull[i] && keyCount != 0) {
                    throw new IllegalArgumentException("A null map must have zero entries");
                }
                buildHashTable(
                        keyBlock,
                        keyOffset,
                        keyCount,
                        keyBlockHashCode,
                        hashTables,
                        keyOffset * HASH_MULTIPLIER,
                        keyCount * HASH_MULTIPLIER);
            }
            set(hashTables);
        }

        // This class intentionally does not implement hashcode and equals.
        // Any class using Hashtables as a field (MapBlock, MapBlockBuilder) should not include this class's implementation as this is
        // derived data. Only using KeyBlock hashcode/equals should suffice.
        // This class has no immutable fields, which makes hashcode/equals error-prone.
    }

    @Override
    public Block getBlockUnchecked(int internalPosition)
    {
        assert internalPositionInRange(internalPosition, this.getOffsetBase(), getPositionCount());

        int startEntryOffset = getOffsets()[internalPosition];
        int endEntryOffset = getOffsets()[internalPosition + 1];
        return new SingleMapBlock(internalPosition - getOffsetBase(), startEntryOffset * 2, (endEntryOffset - startEntryOffset) * 2, this);
    }

    @Override
    public boolean isNullUnchecked(int internalPosition)
    {
        assert mayHaveNull() : "no nulls present";
        assert internalPositionInRange(internalPosition, this.getOffsetBase(), getPositionCount());
        return getMapIsNull()[internalPosition];
    }

    @Override
    public Block appendNull()
    {
        boolean[] mapIsNull = appendNullToIsNullArray(getMapIsNull(), getOffsetBase(), getPositionCount());
        int[] offsets = appendNullToOffsetsArray(getOffsets(), getOffsetBase(), getPositionCount());

        return createMapBlockInternal(
                getOffsetBase(),
                getPositionCount() + 1,
                Optional.of(mapIsNull),
                offsets,
                getRawKeyBlock(),
                getRawValueBlock(),
                getHashTables());
    }

    private void calculateLogicalSize()
    {
        int entriesStart = getOffset(0);
        int entriesEnd = getOffset(getPositionCount());
        int entryCount = entriesEnd - entriesStart;
        logicalSizeInBytes = getRawKeyBlock().getRegionLogicalSizeInBytes(entriesStart, entryCount) +
                getRawValueBlock().getRegionLogicalSizeInBytes(entriesStart, entryCount) +
                (Integer.BYTES + Byte.BYTES) * (long) this.getPositionCount() +
                Integer.BYTES * HASH_MULTIPLIER * (long) entryCount;
    }
}

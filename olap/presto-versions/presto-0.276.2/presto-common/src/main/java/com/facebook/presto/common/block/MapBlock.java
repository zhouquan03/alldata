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

import org.openjdk.jol.info.ClassLayout;

import javax.annotation.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ObjLongConsumer;

import static io.airlift.slice.SizeOf.sizeOf;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class MapBlock
        extends AbstractMapBlock
        implements Block
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(MapBlock.class).instanceSize();

    private final int startOffset;
    private final int positionCount;

    @Nullable
    private final boolean[] mapIsNull;
    private final int[] offsets;
    private final Block keyBlock;
    private final Block valueBlock;
    private final HashTables hashTables;
    private final long retainedSizeInBytesExceptHashtable;

    private volatile long sizeInBytes;

    /**
     * Create a map block directly from columnar nulls, keys, values, and offsets into the keys and values.
     * A null map must have no entries.
     */
    public static MapBlock fromKeyValueBlock(
            int positionCount,
            Optional<boolean[]> mapIsNull,
            int[] offsets,
            Block keyBlock,
            Block valueBlock)
    {
        validateConstructorArguments(0, positionCount, mapIsNull.orElse(null), offsets, keyBlock, valueBlock);

        return createMapBlockInternal(
                0,
                positionCount,
                mapIsNull,
                offsets,
                keyBlock,
                valueBlock,
                new HashTables(Optional.empty(), positionCount));
    }

    /**
     * Create a map block directly without per element validations.
     * <p>
     * Internal use by this package and com.facebook.presto.spi.Type only.
     */
    public static MapBlock createMapBlockInternal(
            int startOffset,
            int positionCount,
            Optional<boolean[]> mapIsNull,
            int[] offsets,
            Block keyBlock,
            Block valueBlock,
            HashTables hashTables)
    {
        validateConstructorArguments(startOffset, positionCount, mapIsNull.orElse(null), offsets, keyBlock, valueBlock);
        requireNonNull(hashTables, "hashTables is null");
        return new MapBlock(
                startOffset,
                positionCount,
                mapIsNull.orElse(null),
                offsets,
                keyBlock,
                valueBlock,
                hashTables);
    }

    private static void validateConstructorArguments(
            int startOffset,
            int positionCount,
            @Nullable boolean[] mapIsNull,
            int[] offsets,
            Block keyBlock,
            Block valueBlock)
    {
        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset is negative");
        }

        if (positionCount < 0) {
            throw new IllegalArgumentException("positionCount is negative");
        }

        if (mapIsNull != null && mapIsNull.length - startOffset < positionCount) {
            throw new IllegalArgumentException("isNull length is less than positionCount");
        }

        requireNonNull(offsets, "offsets is null");
        if (offsets.length - startOffset < positionCount + 1) {
            throw new IllegalArgumentException("offsets length is less than positionCount");
        }

        requireNonNull(keyBlock, "keyBlock is null");
        requireNonNull(valueBlock, "valueBlock is null");
        if (keyBlock.getPositionCount() != valueBlock.getPositionCount()) {
            throw new IllegalArgumentException(format("keyBlock and valueBlock has different size: %s %s", keyBlock.getPositionCount(), valueBlock.getPositionCount()));
        }
    }

    /**
     * Use createRowBlockInternal or fromKeyValueBlock instead of this method.  The caller of this method is assumed to have
     * validated the arguments with validateConstructorArguments.
     */
    private MapBlock(
            int startOffset,
            int positionCount,
            @Nullable boolean[] mapIsNull,
            int[] offsets,
            Block keyBlock,
            Block valueBlock,
            HashTables hashTables)
    {
        int[] rawHashTables = hashTables.get();
        if (rawHashTables != null && rawHashTables.length < keyBlock.getPositionCount() * HASH_MULTIPLIER) {
            throw new IllegalArgumentException(format("keyBlock/valueBlock size does not match hash table size: %s %s", keyBlock.getPositionCount(), rawHashTables.length));
        }

        this.startOffset = startOffset;
        this.positionCount = positionCount;
        this.mapIsNull = mapIsNull;
        this.offsets = offsets;
        this.keyBlock = keyBlock;
        this.valueBlock = valueBlock;
        this.hashTables = hashTables;
        this.sizeInBytes = -1;
        this.logicalSizeInBytes = -1;

        // We will add the hashtable size to the retained size even if it's not built yet. This could be overestimating
        // but is necessary to avoid reliability issues. Currently the memory counting framework only pull the retained
        // size once for each operator so updating in the middle of the processing would not work.
        this.retainedSizeInBytesExceptHashtable = INSTANCE_SIZE
                + keyBlock.getRetainedSizeInBytes()
                + valueBlock.getRetainedSizeInBytes()
                + sizeOf(offsets)
                + sizeOf(mapIsNull);
    }

    @Override
    protected Block getRawKeyBlock()
    {
        return keyBlock;
    }

    @Override
    protected Block getRawValueBlock()
    {
        return valueBlock;
    }

    @Override
    protected HashTables getHashTables()
    {
        return hashTables;
    }

    @Override
    protected int[] getOffsets()
    {
        return offsets;
    }

    @Override
    public int getOffsetBase()
    {
        return startOffset;
    }

    @Override
    @Nullable
    protected boolean[] getMapIsNull()
    {
        return mapIsNull;
    }

    @Override
    public int getPositionCount()
    {
        return positionCount;
    }

    @Override
    public long getSizeInBytes()
    {
        if (sizeInBytes < 0) {
            calculateSize();
        }
        return sizeInBytes;
    }

    private boolean isSinglePositionBlock(int position)
    {
        return position == 0 && positionCount == 1 && offsets.length == 2;
    }

    @Override
    public Block getSingleValueBlock(int position)
    {
        if (isSinglePositionBlock(position)) {
            return this;
        }

        return getSingleValueBlockInternal(position);
    }

    private void calculateSize()
    {
        int entriesStart = offsets[startOffset];
        int entriesEnd = offsets[startOffset + positionCount];
        int entryCount = entriesEnd - entriesStart;
        sizeInBytes = keyBlock.getRegionSizeInBytes(entriesStart, entryCount) +
                valueBlock.getRegionSizeInBytes(entriesStart, entryCount) +
                (Integer.BYTES + Byte.BYTES) * (long) this.positionCount +
                Integer.BYTES * HASH_MULTIPLIER * (long) entryCount;
    }

    @Override
    public long getRetainedSizeInBytes()
    {
        return retainedSizeInBytesExceptHashtable + hashTables.getRetainedSizeInBytes();
    }

    @Override
    public void retainedBytesForEachPart(ObjLongConsumer<Object> consumer)
    {
        consumer.accept(keyBlock, keyBlock.getRetainedSizeInBytes());
        consumer.accept(valueBlock, valueBlock.getRetainedSizeInBytes());
        consumer.accept(offsets, sizeOf(offsets));
        if (mapIsNull != null) {
            consumer.accept(mapIsNull, sizeOf(mapIsNull));
        }
        consumer.accept(hashTables, hashTables.getRetainedSizeInBytes());
        consumer.accept(this, INSTANCE_SIZE);
    }

    @Override
    public String toString()
    {
        return format("MapBlock(%d){positionCount=%d}", hashCode(), getPositionCount());
    }

    @Override
    public Block getLoadedBlock()
    {
        if (keyBlock != keyBlock.getLoadedBlock()) {
            // keyBlock has to be loaded since MapBlock constructs hash table eagerly.
            throw new IllegalStateException();
        }

        Block loadedValueBlock = valueBlock.getLoadedBlock();
        if (loadedValueBlock == valueBlock) {
            return this;
        }
        return createMapBlockInternal(
                startOffset,
                positionCount,
                Optional.ofNullable(mapIsNull),
                offsets,
                keyBlock,
                loadedValueBlock,
                hashTables);
    }

    @Override
    protected void ensureHashTableLoaded(MethodHandle keyBlockHashCode)
    {
        if (isHashTablesPresent()) {
            return;
        }

        // We need to synchronize access to the hashTables field as it may be shared by multiple MapBlock instances.
        synchronized (hashTables) {
            if (!isHashTablesPresent()) {
                hashTables.loadHashTables(hashTables.getExpectedHashTableCount(), offsets, mapIsNull, getRawKeyBlock(), keyBlockHashCode);
            }
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MapBlock other = (MapBlock) obj;
        // Do not use mutable fields sizeInBytes, hashtables as it makes the implementation non-deterministic.
        return this.startOffset == other.startOffset &&
                this.positionCount == other.positionCount &&
                Arrays.equals(this.mapIsNull, other.mapIsNull) &&
                Arrays.equals(this.offsets, other.offsets) &&
                Objects.equals(this.keyBlock, other.keyBlock) &&
                Objects.equals(this.valueBlock, other.valueBlock) &&
                this.retainedSizeInBytesExceptHashtable == other.retainedSizeInBytesExceptHashtable;
    }

    @Override
    public int hashCode()
    {
        // Do not use mutable fields sizeInBytes, hashtables as it makes the implementation non-deterministic.
        return Objects.hash(startOffset,
                positionCount,
                Arrays.hashCode(mapIsNull),
                Arrays.hashCode(offsets),
                keyBlock,
                valueBlock,
                retainedSizeInBytesExceptHashtable);
    }
}

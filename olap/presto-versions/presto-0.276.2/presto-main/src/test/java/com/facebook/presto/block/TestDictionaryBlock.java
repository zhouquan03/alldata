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
package com.facebook.presto.block;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.block.DictionaryBlock;
import com.facebook.presto.common.block.DictionaryId;
import com.facebook.presto.common.block.IntArrayBlock;
import com.facebook.presto.common.block.VariableWidthBlockBuilder;
import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.facebook.presto.block.BlockAssertions.createRLEBlock;
import static com.facebook.presto.block.BlockAssertions.createRandomDictionaryBlock;
import static com.facebook.presto.block.BlockAssertions.createRandomLongsBlock;
import static com.facebook.presto.block.BlockAssertions.createSlicesBlock;
import static com.facebook.presto.common.type.VarcharType.VARCHAR;
import static io.airlift.slice.SizeOf.SIZE_OF_INT;
import static io.airlift.slice.Slices.utf8Slice;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestDictionaryBlock
        extends AbstractTestBlock
{
    @Test
    public void testSizeInBytes()
    {
        Slice[] expectedValues = createExpectedValues(10);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);
        assertEquals(dictionaryBlock.getSizeInBytes(), dictionaryBlock.getDictionary().getSizeInBytes() + (100 * SIZE_OF_INT));
    }

    @Test
    public void testNonCachedLogicalBytes()
    {
        int numEntries = 10;
        BlockBuilder blockBuilder = VARCHAR.createBlockBuilder(null, numEntries);

        // Over allocate dictionary indexes but only use the required limit.
        int[] dictionaryIndexes = new int[numEntries + 10];
        Arrays.fill(dictionaryIndexes, 1);
        blockBuilder.appendNull();
        dictionaryIndexes[0] = 0;

        String string = "";
        for (int i = 1; i < numEntries; i++) {
            string += "a";
            VARCHAR.writeSlice(blockBuilder, utf8Slice(string));
            dictionaryIndexes[i] = numEntries - i;
        }

        // A dictionary block of size 10, 1st element -> null, 2nd element size -> 9....9th element size -> 1
        // Pass different maxChunkSize and different offset and verify if it computes the chunk lengths correctly.
        Block elementBlock = blockBuilder.build();
        DictionaryBlock block = new DictionaryBlock(numEntries, elementBlock, dictionaryIndexes);
        int elementSize = Integer.BYTES + Byte.BYTES;

        long size = block.getRegionLogicalSizeInBytes(0, 1);
        assertEquals(size, 0 + 1 * elementSize);

        size = block.getRegionLogicalSizeInBytes(0, numEntries);
        assertEquals(size, 45 + numEntries * elementSize);

        size = block.getRegionLogicalSizeInBytes(1, 2);
        assertEquals(size, 9 + 8 + 2 * elementSize);

        size = block.getRegionLogicalSizeInBytes(9, 1);
        assertEquals(size, 1 + 1 * elementSize);
    }

    @Test
    public void testLogicalSizeInBytes()
    {
        // The 10 Slices in the array will be of lengths 0 to 9.
        Slice[] expectedValues = createExpectedValues(10);

        // The dictionary within the dictionary block is expected to be a VariableWidthBlock of size 95 bytes.
        // 45 bytes for the expectedValues Slices (sum of seq(0,9)) and 50 bytes for the position and isNull array (total 10 positions).
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);
        assertEquals(dictionaryBlock.getDictionary().getLogicalSizeInBytes(), 95);

        // The 100 positions in the dictionary block index to 10 positions in the underlying dictionary (10 each).
        // Logical size calculation accounts for 4 bytes of offset and 1 byte of isNull. Therefore the expected unoptimized
        // size is 10 times the size of the underlying dictionary (VariableWidthBlock).
        assertEquals(dictionaryBlock.getLogicalSizeInBytes(), 95 * 10);

        // With alternating nulls, we have 21 positions, with the same size calculation as above.
        dictionaryBlock = createDictionaryBlock(alternatingNullValues(expectedValues), 210);
        assertEquals(dictionaryBlock.getDictionary().getPositionCount(), 21);
        assertEquals(dictionaryBlock.getDictionary().getLogicalSizeInBytes(), 150);

        // The null positions should be included in the logical size.
        assertEquals(dictionaryBlock.getLogicalSizeInBytes(), 150 * 10);

        Block longArrayBlock = createRandomLongsBlock(100, 0.5f);
        DictionaryBlock dictionaryDictionaryBlock = createRandomDictionaryBlock(createRandomDictionaryBlock(longArrayBlock, 50), 10);
        assertEquals(dictionaryDictionaryBlock.getDictionary().getLogicalSizeInBytes(), 450);
        assertEquals(dictionaryDictionaryBlock.getLogicalSizeInBytes(), 90);

        DictionaryBlock dictionaryRleBlock = createRandomDictionaryBlock(createRLEBlock(1, 50), 10);
        assertEquals(dictionaryRleBlock.getDictionary().getLogicalSizeInBytes(), 450);
        assertEquals(dictionaryRleBlock.getLogicalSizeInBytes(), 90);
    }

    @Test
    public void testCopyRegionCreatesCompactBlock()
    {
        Slice[] expectedValues = createExpectedValues(10);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);

        DictionaryBlock copyRegionDictionaryBlock = (DictionaryBlock) dictionaryBlock.copyRegion(1, 3);
        assertTrue(copyRegionDictionaryBlock.isCompact());
    }

    @Test
    public void testCopyPositionsWithCompaction()
    {
        Slice[] expectedValues = createExpectedValues(10);
        Slice firstExpectedValue = expectedValues[0];
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);

        int[] positionsToCopy = new int[] {0, 10, 20, 30, 40};
        DictionaryBlock copiedBlock = (DictionaryBlock) dictionaryBlock.copyPositions(positionsToCopy, 0, positionsToCopy.length);

        assertEquals(copiedBlock.getDictionary().getPositionCount(), 1);
        assertEquals(copiedBlock.getPositionCount(), positionsToCopy.length);
        assertBlock(copiedBlock.getDictionary(), TestDictionaryBlock::createBlockBuilder, new Slice[] {firstExpectedValue});
        assertBlock(copiedBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {firstExpectedValue, firstExpectedValue, firstExpectedValue, firstExpectedValue,
                firstExpectedValue});
    }

    @Test
    public void testCopyPositionsWithCompactionsAndReorder()
    {
        Slice[] expectedValues = createExpectedValues(10);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);
        int[] positionsToCopy = new int[] {50, 55, 40, 45, 60};

        DictionaryBlock copiedBlock = (DictionaryBlock) dictionaryBlock.copyPositions(positionsToCopy, 0, positionsToCopy.length);

        assertEquals(copiedBlock.getDictionary().getPositionCount(), 2);
        assertEquals(copiedBlock.getPositionCount(), positionsToCopy.length);

        assertBlock(copiedBlock.getDictionary(), TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[0], expectedValues[5]});
        assertDictionaryIds(copiedBlock, 0, 1, 0, 1, 0);
    }

    @Test
    public void testCopyPositionsSamePosition()
    {
        Slice[] expectedValues = createExpectedValues(10);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);
        int[] positionsToCopy = new int[] {52, 52, 52};

        DictionaryBlock copiedBlock = (DictionaryBlock) dictionaryBlock.copyPositions(positionsToCopy, 0, positionsToCopy.length);

        assertEquals(copiedBlock.getDictionary().getPositionCount(), 1);
        assertEquals(copiedBlock.getPositionCount(), positionsToCopy.length);

        assertBlock(copiedBlock.getDictionary(), TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[2]});
        assertDictionaryIds(copiedBlock, 0, 0, 0);
    }

    @Test
    public void testCopyPositionsNoCompaction()
    {
        Slice[] expectedValues = createExpectedValues(1);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 100);

        int[] positionsToCopy = new int[] {0, 2, 4, 5};
        DictionaryBlock copiedBlock = (DictionaryBlock) dictionaryBlock.copyPositions(positionsToCopy, 0, positionsToCopy.length);

        assertEquals(copiedBlock.getPositionCount(), positionsToCopy.length);
        assertBlock(copiedBlock.getDictionary(), TestDictionaryBlock::createBlockBuilder, expectedValues);
    }

    @Test
    public void testCompact()
    {
        Slice[] expectedValues = createExpectedValues(5);
        DictionaryBlock dictionaryBlock = createDictionaryBlockWithUnreferencedKeys(expectedValues, 10);

        assertEquals(dictionaryBlock.isCompact(), false);
        DictionaryBlock compactBlock = dictionaryBlock.compact();
        assertNotEquals(dictionaryBlock.getDictionarySourceId(), compactBlock.getDictionarySourceId());

        assertEquals(compactBlock.getDictionary().getPositionCount(), (expectedValues.length / 2) + 1);
        assertBlock(compactBlock.getDictionary(), TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[0], expectedValues[1], expectedValues[3]});
        assertDictionaryIds(compactBlock, 0, 1, 1, 2, 2, 0, 1, 1, 2, 2);
        assertEquals(compactBlock.isCompact(), true);

        DictionaryBlock reCompactedBlock = compactBlock.compact();
        assertEquals(reCompactedBlock.getDictionarySourceId(), compactBlock.getDictionarySourceId());
    }

    @Test
    public void testCompactAllKeysReferenced()
    {
        Slice[] expectedValues = createExpectedValues(5);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, 10);
        DictionaryBlock compactBlock = dictionaryBlock.compact();

        // When there is nothing to compact, we return the same block
        assertEquals(compactBlock.getDictionary(), dictionaryBlock.getDictionary());
        assertEquals(compactBlock.getPositionCount(), dictionaryBlock.getPositionCount());
        for (int position = 0; position < compactBlock.getPositionCount(); position++) {
            assertEquals(compactBlock.getId(position), dictionaryBlock.getId(position));
        }
        assertEquals(compactBlock.isCompact(), true);
    }

    @Test
    public void testBasicGetPositions()
    {
        Slice[] expectedValues = createExpectedValues(10);
        Block dictionaryBlock = new DictionaryBlock(createSlicesBlock(expectedValues), new int[] {0, 1, 2, 3, 4, 5});
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[0], expectedValues[1], expectedValues[2], expectedValues[3],
                expectedValues[4], expectedValues[5]});
        DictionaryId dictionaryId = ((DictionaryBlock) dictionaryBlock).getDictionarySourceId();

        // first getPositions
        dictionaryBlock = dictionaryBlock.getPositions(new int[] {0, 8, 1, 2, 4, 5, 7, 9}, 2, 4);
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[1], expectedValues[2], expectedValues[4], expectedValues[5]});
        assertEquals(((DictionaryBlock) dictionaryBlock).getDictionarySourceId(), dictionaryId);

        // second getPositions
        dictionaryBlock = dictionaryBlock.getPositions(new int[] {0, 1, 3, 0, 0}, 0, 3);
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[1], expectedValues[2], expectedValues[5]});
        assertEquals(((DictionaryBlock) dictionaryBlock).getDictionarySourceId(), dictionaryId);

        // third getPositions; we do not validate if -1 is an invalid position
        dictionaryBlock = dictionaryBlock.getPositions(new int[] {-1, -1, 0, 1, 2}, 2, 3);
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[1], expectedValues[2], expectedValues[5]});
        assertEquals(((DictionaryBlock) dictionaryBlock).getDictionarySourceId(), dictionaryId);

        // mixed getPositions
        dictionaryBlock = dictionaryBlock.getPositions(new int[] {0, 2, 2}, 0, 3);
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[1], expectedValues[5], expectedValues[5]});
        assertEquals(((DictionaryBlock) dictionaryBlock).getDictionarySourceId(), dictionaryId);

        // duplicated getPositions
        dictionaryBlock = dictionaryBlock.getPositions(new int[] {1, 1, 1, 1, 1}, 0, 5);
        assertBlock(dictionaryBlock, TestDictionaryBlock::createBlockBuilder, new Slice[] {expectedValues[5], expectedValues[5], expectedValues[5], expectedValues[5],
                expectedValues[5]});
        assertEquals(((DictionaryBlock) dictionaryBlock).getDictionarySourceId(), dictionaryId);

        // out of range
        for (int position : ImmutableList.of(-1, 6)) {
            try {
                dictionaryBlock.getPositions(new int[] {position}, 0, 1);
                fail("Expected to fail");
            }
            catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().startsWith("Invalid position"));
            }
        }

        for (int offset : ImmutableList.of(-1, 6)) {
            try {
                dictionaryBlock.getPositions(new int[] {0}, offset, 1);
                fail("Expected to fail");
            }
            catch (IndexOutOfBoundsException e) {
                assertTrue(e.getMessage().startsWith("Invalid offset"));
            }
        }

        for (int length : ImmutableList.of(-1, 6)) {
            try {
                dictionaryBlock.getPositions(new int[] {0}, 0, length);
                fail("Expected to fail");
            }
            catch (IndexOutOfBoundsException e) {
                assertTrue(e.getMessage().startsWith("Invalid offset"));
            }
        }
    }

    @Test
    public void testCompactGetPositions()
    {
        DictionaryBlock block = new DictionaryBlock(createSlicesBlock(createExpectedValues(10)), new int[] {0, 1, 2, 3, 4, 5}).compact();

        // 3, 3, 4, 5, 2, 0, 1, 1
        block = (DictionaryBlock) block.getPositions(new int[] {3, 3, 4, 5, 2, 0, 1, 1}, 0, 7);
        assertTrue(block.isCompact());

        // 3, 3, 4, 5, 2, 0, 1, 1, 0, 2, 5, 4, 3
        block = (DictionaryBlock) block.getPositions(new int[] {0, 1, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2, 1}, 0, 12);
        assertTrue(block.isCompact());

        // 3, 4, 3, 4, 3
        block = (DictionaryBlock) block.getPositions(new int[] {0, 2, 0, 2, 0}, 0, 5);
        assertFalse(block.isCompact());

        block = block.compact();
        // 3, 4, 4, 4
        block = (DictionaryBlock) block.getPositions(new int[] {0, 1, 1, 1}, 0, 4);
        assertTrue(block.isCompact());

        // 4, 4, 4, 4
        block = (DictionaryBlock) block.getPositions(new int[] {1, 1, 1, 1}, 0, 4);
        assertFalse(block.isCompact());

        block = block.compact();
        // 4
        block = (DictionaryBlock) block.getPositions(new int[] {0}, 0, 1);
        assertTrue(block.isCompact());

        // empty
        block = (DictionaryBlock) block.getPositions(new int[] {}, 0, 0);
        assertFalse(block.isCompact());

        block = block.compact();
        // empty
        block = (DictionaryBlock) block.getPositions(new int[] {}, 0, 0);
        assertTrue(block.isCompact());
    }

    @Test
    public void testEstimatedDataSizeForStats()
    {
        int positionCount = 10;
        int dictionaryPositionCount = 100;
        Slice[] expectedValues = createExpectedValues(positionCount);
        DictionaryBlock dictionaryBlock = createDictionaryBlock(expectedValues, dictionaryPositionCount);
        for (int position = 0; position < dictionaryPositionCount; position++) {
            assertEquals(dictionaryBlock.getEstimatedDataSizeForStats(position), expectedValues[position % positionCount].length());
        }
    }

    @Test
    public void testDictionarySizeMethods()
    {
        // fixed width block
        Block fixedWidthBlock = new IntArrayBlock(100, Optional.empty(), IntStream.range(0, 100).toArray());
        assertDictionarySizeMethods(fixedWidthBlock);
        // variable width block
        Block variableWidthBlock = createSlicesBlock(createExpectedValues(fixedWidthBlock.getPositionCount()));
        assertDictionarySizeMethods(variableWidthBlock);

        // sparse dictionary block from getPositions
        assertDictionarySizeMethods(fixedWidthBlock.getPositions(IntStream.range(0, 50).toArray(), 0, 50));
        assertDictionarySizeMethods(variableWidthBlock.getPositions(IntStream.range(0, 50).toArray(), 0, 50));

        // nested sparse dictionary block via constructor
        assertDictionarySizeMethods(new DictionaryBlock(fixedWidthBlock, IntStream.range(0, 50).toArray()));
        assertDictionarySizeMethods(new DictionaryBlock(variableWidthBlock, IntStream.range(0, 50).toArray()));
        // compact dictionary block via getPositions
        int[] positions = createCompactRepeatingIdsRange(fixedWidthBlock.getPositionCount());
        assertDictionarySizeMethods(fixedWidthBlock.getPositions(positions, 0, positions.length));
        assertDictionarySizeMethods(variableWidthBlock.getPositions(positions, 0, positions.length));
        // nested compact dictionary block via constructor
        assertDictionarySizeMethods(new DictionaryBlock(fixedWidthBlock, createCompactRepeatingIdsRange(fixedWidthBlock.getPositionCount())));
        assertDictionarySizeMethods(new DictionaryBlock(variableWidthBlock, createCompactRepeatingIdsRange(variableWidthBlock.getPositionCount())));
    }

    private static int[] createCompactRepeatingIdsRange(int positions)
    {
        int[] ids = new int[positions * 2];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i % positions;
        }
        return ids;
    }

    private static void assertDictionarySizeMethods(Block block)
    {
        int positions = block.getPositionCount();

        int[] allIds = IntStream.range(0, positions).toArray();
        assertEquals(new DictionaryBlock(block, allIds).getSizeInBytes(), block.getSizeInBytes() + (Integer.BYTES * (long) positions));

        if (positions > 0) {
            int firstHalfLength = positions / 2;
            int secondHalfLength = positions - firstHalfLength;
            int[] firstHalfIds = IntStream.range(0, firstHalfLength).toArray();
            int[] secondHalfIds = IntStream.range(firstHalfLength, positions).toArray();

            // No positions getPositionSizeInBytes
            boolean[] selectedPositions = new boolean[positions];
            assertEquals(new DictionaryBlock(block, allIds).getPositionsSizeInBytes(selectedPositions, 0), 0);
            // Single position getPositionSizeInBytes
            selectedPositions[0] = true;
            assertEquals(
                    new DictionaryBlock(block, allIds).getPositionsSizeInBytes(selectedPositions, 1),
                    block.getPositionsSizeInBytes(selectedPositions, 1) + Integer.BYTES);
            // Single position getSizeInBytes
            assertEquals(
                    new DictionaryBlock(block, new int[]{0}).getSizeInBytes(),
                    block.getPositionsSizeInBytes(selectedPositions, 1) + Integer.BYTES);

            // All positions getPositionSizeInBytes
            Arrays.fill(selectedPositions, true);
            assertEquals(
                    new DictionaryBlock(block, allIds).getPositionsSizeInBytes(selectedPositions, positions),
                    block.getSizeInBytes() + (Integer.BYTES * (long) positions));

            // Half selected getSizeInBytes
            assertEquals(
                    new DictionaryBlock(block, firstHalfIds).getSizeInBytes(),
                    block.getRegionSizeInBytes(0, firstHalfLength) + (Integer.BYTES * (long) firstHalfLength));
            assertEquals(
                    new DictionaryBlock(block, secondHalfIds).getSizeInBytes(),
                    block.getRegionSizeInBytes(firstHalfLength, secondHalfLength) + (Integer.BYTES * (long) secondHalfLength));

            // Half selected getRegionSizeInBytes
            assertEquals(
                    new DictionaryBlock(block, allIds).getRegionSizeInBytes(0, firstHalfLength),
                    block.getRegionSizeInBytes(0, firstHalfLength) + (Integer.BYTES * (long) firstHalfLength));
            assertEquals(
                    new DictionaryBlock(block, allIds).getRegionSizeInBytes(firstHalfLength, secondHalfLength),
                    block.getRegionSizeInBytes(firstHalfLength, secondHalfLength) + (Integer.BYTES * (long) secondHalfLength));
        }
    }

    private static DictionaryBlock createDictionaryBlockWithUnreferencedKeys(Slice[] expectedValues, int positionCount)
    {
        // adds references to 0 and all odd indexes
        int dictionarySize = expectedValues.length;
        int[] ids = new int[positionCount];

        for (int i = 0; i < positionCount; i++) {
            int index = i % dictionarySize;
            if (index % 2 == 0 && index != 0) {
                index--;
            }
            ids[i] = index;
        }
        return new DictionaryBlock(createSlicesBlock(expectedValues), ids);
    }

    private static DictionaryBlock createDictionaryBlock(Slice[] expectedValues, int positionCount)
    {
        int dictionarySize = expectedValues.length;
        int[] ids = new int[positionCount];

        for (int i = 0; i < positionCount; i++) {
            ids[i] = i % dictionarySize;
        }
        return new DictionaryBlock(createSlicesBlock(expectedValues), ids);
    }

    private static BlockBuilder createBlockBuilder()
    {
        return new VariableWidthBlockBuilder(null, 100, 1);
    }

    private static void assertDictionaryIds(DictionaryBlock dictionaryBlock, int... expected)
    {
        assertEquals(dictionaryBlock.getPositionCount(), expected.length);
        for (int position = 0; position < dictionaryBlock.getPositionCount(); position++) {
            assertEquals(dictionaryBlock.getId(position), expected[position]);
        }
    }
}

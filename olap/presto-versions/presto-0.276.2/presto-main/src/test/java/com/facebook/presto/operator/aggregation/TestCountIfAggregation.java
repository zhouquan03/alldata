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
package com.facebook.presto.operator.aggregation;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.StandardTypes;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.facebook.presto.common.type.BooleanType.BOOLEAN;

public class TestCountIfAggregation
        extends AbstractTestAggregationFunction
{
    @Override
    public Block[] getSequenceBlocks(int start, int length)
    {
        BlockBuilder blockBuilder = BOOLEAN.createBlockBuilder(null, length);
        for (int i = start; i < start + length; i++) {
            BOOLEAN.writeBoolean(blockBuilder, i % 2 == 0);
        }
        return new Block[] {blockBuilder.build()};
    }

    @Override
    public Number getExpectedValue(int start, int length)
    {
        long count = 0;
        for (int i = start; i < start + length; i++) {
            if (i % 2 == 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected String getFunctionName()
    {
        return "count_if";
    }

    @Override
    protected List<String> getFunctionParameterTypes()
    {
        return ImmutableList.of(StandardTypes.BOOLEAN);
    }
}

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
import com.facebook.presto.type.SqlIntervalYearMonth;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.facebook.presto.type.IntervalYearMonthType.INTERVAL_YEAR_MONTH;
import static java.lang.Math.round;
import static java.lang.Math.toIntExact;

public class TestIntervalYearToMonthAverageAggregation
        extends AbstractTestAggregationFunction
{
    @Override
    public Block[] getSequenceBlocks(int start, int length)
    {
        BlockBuilder blockBuilder = INTERVAL_YEAR_MONTH.createBlockBuilder(null, length);
        for (int i = start; i < start + length; i++) {
            INTERVAL_YEAR_MONTH.writeLong(blockBuilder, i);
        }
        return new Block[] {blockBuilder.build()};
    }

    @Override
    public SqlIntervalYearMonth getExpectedValue(int start, int length)
    {
        if (length == 0) {
            return null;
        }

        double sum = 0;
        for (int i = start; i < start + length; i++) {
            sum += i;
        }
        return new SqlIntervalYearMonth(toIntExact(round(sum / length)));
    }

    @Override
    protected String getFunctionName()
    {
        return "avg";
    }

    @Override
    protected List<String> getFunctionParameterTypes()
    {
        return ImmutableList.of(StandardTypes.INTERVAL_YEAR_TO_MONTH);
    }
}

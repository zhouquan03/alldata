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

import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.operator.aggregation.state.NullableLongState;
import com.facebook.presto.spi.function.AggregationFunction;
import com.facebook.presto.spi.function.AggregationState;
import com.facebook.presto.spi.function.CombineFunction;
import com.facebook.presto.spi.function.InputFunction;
import com.facebook.presto.spi.function.OutputFunction;
import com.facebook.presto.spi.function.SqlType;

@AggregationFunction("bitwise_and_agg")
public final class BitwiseAndAggregation
{
    private BitwiseAndAggregation() {}

    @InputFunction
    public static void bitAnd(@AggregationState NullableLongState state, @SqlType(StandardTypes.BIGINT) long value)
    {
        if (state.isNull()) {
            state.setLong(value);
        }
        else {
            state.setLong(state.getLong() & value);
        }
        state.setNull(false);
    }

    @CombineFunction
    public static void combine(@AggregationState NullableLongState state, @AggregationState NullableLongState otherState)
    {
        if (state.isNull()) {
            state.setNull(otherState.isNull());
            state.setLong(otherState.getLong());
        }
        else if (!otherState.isNull()) {
            state.setLong(state.getLong() & otherState.getLong());
        }
    }

    @OutputFunction(StandardTypes.BIGINT)
    public static void output(@AggregationState NullableLongState state, BlockBuilder out)
    {
        NullableLongState.write(BigintType.BIGINT, state, out);
    }
}

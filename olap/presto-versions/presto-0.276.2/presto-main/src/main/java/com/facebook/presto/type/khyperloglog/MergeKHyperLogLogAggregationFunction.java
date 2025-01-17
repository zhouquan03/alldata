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

package com.facebook.presto.type.khyperloglog;

import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.spi.function.AggregationFunction;
import com.facebook.presto.spi.function.AggregationState;
import com.facebook.presto.spi.function.CombineFunction;
import com.facebook.presto.spi.function.InputFunction;
import com.facebook.presto.spi.function.OutputFunction;
import com.facebook.presto.spi.function.SqlType;
import io.airlift.slice.Slice;

import static com.facebook.presto.type.khyperloglog.KHyperLogLogType.K_HYPER_LOG_LOG;

@AggregationFunction("merge")
public final class MergeKHyperLogLogAggregationFunction
{
    private MergeKHyperLogLogAggregationFunction() {}

    @InputFunction
    public static void input(@AggregationState KHyperLogLogState state, @SqlType(KHyperLogLogType.NAME) Slice value)
    {
        KHyperLogLog instance = KHyperLogLog.newInstance(value);
        merge(state, instance);
    }

    @CombineFunction
    public static void combine(@AggregationState KHyperLogLogState state, @AggregationState KHyperLogLogState otherState)
    {
        merge(state, otherState.getKHLL());
    }

    private static void merge(@AggregationState KHyperLogLogState state, KHyperLogLog instance)
    {
        if (state.getKHLL() == null) {
            state.setKHLL(instance);
        }
        else {
            state.getKHLL().mergeWith(instance);
        }
    }

    @OutputFunction(KHyperLogLogType.NAME)
    public static void output(@AggregationState KHyperLogLogState state, BlockBuilder out)
    {
        if (state.getKHLL() == null) {
            out.appendNull();
        }
        else {
            K_HYPER_LOG_LOG.writeSlice(out, state.getKHLL().serialize());
        }
    }
}

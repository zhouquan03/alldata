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

import com.facebook.presto.operator.UpdateMemory;
import com.facebook.presto.spi.function.aggregation.Accumulator;
import com.facebook.presto.spi.function.aggregation.GroupedAccumulator;

import java.util.List;

public interface AccumulatorFactory
{
    List<Integer> getInputChannels();

    Accumulator createAccumulator(UpdateMemory updateMemory);

    Accumulator createIntermediateAccumulator();

    GroupedAccumulator createGroupedAccumulator(UpdateMemory updateMemory);

    GroupedAccumulator createGroupedIntermediateAccumulator(UpdateMemory updateMemory);

    boolean hasOrderBy();

    boolean hasDistinct();
}

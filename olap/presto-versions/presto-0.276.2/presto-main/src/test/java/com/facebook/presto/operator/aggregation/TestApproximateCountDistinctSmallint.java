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

import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.function.JavaAggregationFunctionImplementation;

import java.util.concurrent.ThreadLocalRandom;

import static com.facebook.presto.common.type.DoubleType.DOUBLE;
import static com.facebook.presto.common.type.SmallintType.SMALLINT;
import static com.facebook.presto.sql.analyzer.TypeSignatureProvider.fromTypes;

public class TestApproximateCountDistinctSmallint
        extends AbstractTestApproximateCountDistinct
{
    @Override
    public JavaAggregationFunctionImplementation getAggregationFunction()
    {
        return FUNCTION_AND_TYPE_MANAGER.getJavaAggregateFunctionImplementation(
                FUNCTION_AND_TYPE_MANAGER.lookupFunction("approx_distinct", fromTypes(SMALLINT, DOUBLE)));
    }

    @Override
    public Type getValueType()
    {
        return SMALLINT;
    }

    @Override
    public Object randomValue()
    {
        return ThreadLocalRandom.current().nextLong(Short.MIN_VALUE, Short.MAX_VALUE + 1);
    }
}

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
package com.facebook.presto.type;

import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.function.ScalarOperator;
import com.facebook.presto.spi.function.SqlType;
import io.airlift.slice.Slice;

import static com.facebook.presto.common.function.OperatorType.CAST;

public final class TDigestOperators
{
    private TDigestOperators() {}

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.VARBINARY)
    public static Slice castToBinaryDouble(@SqlType("tdigest(double)") Slice slice)
    {
        return slice;
    }

    @ScalarOperator(CAST)
    @SqlType("tdigest(double)")
    public static Slice castFromVarbinaryDouble(@SqlType(StandardTypes.VARBINARY) Slice slice)
    {
        return slice;
    }
}

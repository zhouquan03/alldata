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
package com.facebook.presto.operator.scalar;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.function.Description;
import com.facebook.presto.spi.function.ScalarFunction;
import com.facebook.presto.spi.function.SqlType;
import com.facebook.presto.spi.function.TypeParameter;

@ScalarFunction("reverse")
@Description("Returns an array which has the reversed order of the given array.")
public final class ArrayReverseFunction
{
    @TypeParameter("E")
    public ArrayReverseFunction(@TypeParameter("E") Type elementType) {}

    @TypeParameter("E")
    @SqlType("array(E)")
    public Block reverse(
            @TypeParameter("E") Type type,
            @SqlType("array(E)") Block block)
    {
        int arrayLength = block.getPositionCount();

        if (arrayLength < 2) {
            return block;
        }

        BlockBuilder blockBuilder = type.createBlockBuilder(null, block.getPositionCount());
        for (int i = arrayLength - 1; i >= 0; i--) {
            type.appendTo(block, i, blockBuilder);
        }
        return blockBuilder.build();
    }
}

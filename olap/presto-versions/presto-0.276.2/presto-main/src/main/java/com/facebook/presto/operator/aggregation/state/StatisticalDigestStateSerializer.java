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

package com.facebook.presto.operator.aggregation.state;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.function.AccumulatorStateSerializer;

import static com.facebook.presto.common.type.VarbinaryType.VARBINARY;

public class StatisticalDigestStateSerializer
        implements AccumulatorStateSerializer<StatisticalDigestState>
{
    @Override
    public Type getSerializedType()
    {
        return VARBINARY;
    }

    @Override
    public void serialize(StatisticalDigestState state, BlockBuilder out)
    {
        if (state.getStatisticalDigest() == null) {
            out.appendNull();
        }
        else {
            VARBINARY.writeSlice(out, state.getStatisticalDigest().serialize());
        }
    }

    @Override
    public void deserialize(Block block, int index, StatisticalDigestState state)
    {
        state.setStatisticalDigest(state.deserialize(VARBINARY.getSlice(block, index)));
    }
}

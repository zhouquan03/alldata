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
package com.facebook.presto.thrift.api.datatypes;

import com.facebook.drift.annotations.ThriftConstructor;
import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftStruct;
import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.block.ByteArrayBlock;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static com.facebook.drift.annotations.ThriftField.Requiredness.OPTIONAL;
import static com.facebook.presto.common.type.BooleanType.BOOLEAN;
import static com.facebook.presto.thrift.api.datatypes.PrestoThriftBlock.booleanData;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Elements of {@code nulls} array determine if a value for a corresponding row is null.
 * Elements of {@code booleans} array are values for each row. If row is null then value is ignored.
 */
@ThriftStruct
public final class PrestoThriftBoolean
        implements PrestoThriftColumnData
{
    private final boolean[] nulls;
    private final boolean[] booleans;

    @ThriftConstructor
    public PrestoThriftBoolean(@Nullable boolean[] nulls, @Nullable boolean[] booleans)
    {
        checkArgument(sameSizeIfPresent(nulls, booleans), "nulls and values must be of the same size");
        this.nulls = nulls;
        this.booleans = booleans;
    }

    @Nullable
    @ThriftField(value = 1, requiredness = OPTIONAL)
    public boolean[] getNulls()
    {
        return nulls;
    }

    @Nullable
    @ThriftField(value = 2, requiredness = OPTIONAL)
    public boolean[] getBooleans()
    {
        return booleans;
    }

    @Override
    public Block toBlock(Type desiredType)
    {
        checkArgument(BOOLEAN.equals(desiredType), "type doesn't match: %s", desiredType);
        int numberOfRecords = numberOfRecords();
        return new ByteArrayBlock(
                numberOfRecords,
                Optional.ofNullable(nulls),
                booleans == null ? new byte[numberOfRecords] : toByteArray(booleans));
    }

    @Override
    public int numberOfRecords()
    {
        if (nulls != null) {
            return nulls.length;
        }
        if (booleans != null) {
            return booleans.length;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrestoThriftBoolean other = (PrestoThriftBoolean) obj;
        return Arrays.equals(this.nulls, other.nulls) &&
                Arrays.equals(this.booleans, other.booleans);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(Arrays.hashCode(nulls), Arrays.hashCode(booleans));
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("numberOfRecords", numberOfRecords())
                .toString();
    }

    public static PrestoThriftBlock fromBlock(Block block)
    {
        int positions = block.getPositionCount();
        if (positions == 0) {
            return booleanData(new PrestoThriftBoolean(null, null));
        }
        boolean[] nulls = null;
        boolean[] booleans = null;
        for (int position = 0; position < positions; position++) {
            if (block.isNull(position)) {
                if (nulls == null) {
                    nulls = new boolean[positions];
                }
                nulls[position] = true;
            }
            else {
                if (booleans == null) {
                    booleans = new boolean[positions];
                }
                booleans[position] = BOOLEAN.getBoolean(block, position);
            }
        }
        return booleanData(new PrestoThriftBoolean(nulls, booleans));
    }

    private static boolean sameSizeIfPresent(boolean[] nulls, boolean[] booleans)
    {
        return nulls == null || booleans == null || nulls.length == booleans.length;
    }

    private static byte[] toByteArray(boolean[] booleans)
    {
        byte[] bytes = new byte[booleans.length];
        for (int i = 0; i < booleans.length; i++) {
            bytes[i] = booleans[i] ? (byte) 1 : (byte) 0;
        }
        return bytes;
    }
}

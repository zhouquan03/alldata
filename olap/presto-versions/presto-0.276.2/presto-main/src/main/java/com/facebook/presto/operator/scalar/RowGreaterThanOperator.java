package com.facebook.presto.operator.scalar;
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

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.type.RowType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.metadata.BoundVariables;
import com.facebook.presto.metadata.FunctionAndTypeManager;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.util.List;

import static com.facebook.presto.common.function.OperatorType.GREATER_THAN;
import static com.facebook.presto.operator.scalar.ScalarFunctionImplementationChoice.ArgumentProperty.valueTypeArgumentProperty;
import static com.facebook.presto.operator.scalar.ScalarFunctionImplementationChoice.NullConvention.RETURN_NULL_ON_NULL;
import static com.facebook.presto.util.Reflection.methodHandle;

public final class RowGreaterThanOperator
        extends RowComparisonOperator
{
    public static final RowGreaterThanOperator ROW_GREATER_THAN = new RowGreaterThanOperator();
    private static final MethodHandle METHOD_HANDLE = methodHandle(RowGreaterThanOperator.class, "greater", RowType.class, List.class, Block.class, Block.class);

    private RowGreaterThanOperator()
    {
        super(GREATER_THAN);
    }

    @Override
    public BuiltInScalarFunctionImplementation specialize(BoundVariables boundVariables, int arity, FunctionAndTypeManager functionAndTypeManager)
    {
        Type type = boundVariables.getTypeVariable("T");
        return new BuiltInScalarFunctionImplementation(
                false,
                ImmutableList.of(
                        valueTypeArgumentProperty(RETURN_NULL_ON_NULL),
                        valueTypeArgumentProperty(RETURN_NULL_ON_NULL)),
                METHOD_HANDLE.bindTo(type).bindTo(getMethodHandles((RowType) type, functionAndTypeManager, GREATER_THAN)));
    }

    public static boolean greater(
            RowType rowType,
            List<MethodHandle> lessThanFunctions,
            Block leftRow, Block rightRow)
    {
        int compareResult = compare(rowType, lessThanFunctions, leftRow, rightRow);
        return compareResult > 0;
    }
}

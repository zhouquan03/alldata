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
package com.facebook.presto.sql.tree;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Parameter
        extends Expression
{
    private final int position;

    public Parameter(int id)
    {
        this(Optional.empty(), id);
    }

    public Parameter(NodeLocation location, int id)
    {
        this(Optional.of(location), id);
    }

    private Parameter(Optional<NodeLocation> location, int position)
    {
        super(location);
        this.position = position;
    }

    public int getPosition()
    {
        return position;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitParameter(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        return ImmutableList.of();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameter that = (Parameter) o;
        return Objects.equals(position, that.position);
    }

    @Override
    public int hashCode()
    {
        return position;
    }
}

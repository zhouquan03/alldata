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
package com.facebook.presto.spi.relation;

import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.SourceLocation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CallExpression.class, name = "call"),
        @JsonSubTypes.Type(value = SpecialFormExpression.class, name = "special"),
        @JsonSubTypes.Type(value = LambdaDefinitionExpression.class, name = "lambda"),
        @JsonSubTypes.Type(value = InputReferenceExpression.class, name = "input"),
        @JsonSubTypes.Type(value = VariableReferenceExpression.class, name = "variable"),
        @JsonSubTypes.Type(value = ConstantExpression.class, name = "constant")})
public abstract class RowExpression
{
    private final Optional<SourceLocation> sourceLocation;

    @JsonCreator
    public RowExpression(@JsonProperty("sourceLocation") Optional<SourceLocation> sourceLocation)
    {
        this.sourceLocation = sourceLocation;
    }

    public RowExpression()
    {
        this(Optional.empty());
    }

    @JsonProperty
    public Optional<SourceLocation> getSourceLocation()
    {
        return sourceLocation;
    }

    public abstract Type getType();

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract <R, C> R accept(RowExpressionVisitor<R, C> visitor, C context);

    /**
     * @return Canonical form of RowExpression by removing non-critical information
     * from the node, like source location. Does NOT canonicalize the child expressions.
     */
    public abstract RowExpression canonicalize();
}

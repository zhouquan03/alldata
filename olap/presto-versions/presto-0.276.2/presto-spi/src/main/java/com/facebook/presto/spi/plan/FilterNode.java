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
package com.facebook.presto.spi.plan;

import com.facebook.presto.spi.SourceLocation;
import com.facebook.presto.spi.relation.RowExpression;
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Immutable
public final class FilterNode
        extends PlanNode
{
    private final PlanNode source;
    private final RowExpression predicate;

    @JsonCreator
    public FilterNode(
            Optional<SourceLocation> sourceLocation,
            @JsonProperty("id") PlanNodeId id,
            @JsonProperty("source") PlanNode source,
            @JsonProperty("predicate") RowExpression predicate)
    {
        this(sourceLocation, id, Optional.empty(), source, predicate);
    }

    public FilterNode(
            Optional<SourceLocation> sourceLocation,
            PlanNodeId id,
            Optional<PlanNode> statsEquivalentPlanNode,
            PlanNode source,
            RowExpression predicate)
    {
        super(sourceLocation, id, statsEquivalentPlanNode);

        this.source = source;
        this.predicate = predicate;
    }

    /**
     * Get the predicate (a RowExpression of boolean type) of the FilterNode.
     * It serves as the criteria to determine whether the incoming rows should be filtered out or not.
     */
    @JsonProperty
    public RowExpression getPredicate()
    {
        return predicate;
    }

    /**
     * FilterNode only expects a single upstream PlanNode.
     */
    @JsonProperty("source")
    public PlanNode getSource()
    {
        return source;
    }

    @Override
    public List<VariableReferenceExpression> getOutputVariables()
    {
        return source.getOutputVariables();
    }

    @Override
    public List<PlanNode> getSources()
    {
        return unmodifiableList(singletonList(source));
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context)
    {
        return visitor.visitFilter(this, context);
    }

    @Override
    public PlanNode assignStatsEquivalentPlanNode(Optional<PlanNode> statsEquivalentPlanNode)
    {
        return new FilterNode(getSourceLocation(), getId(), statsEquivalentPlanNode, source, predicate);
    }

    @Override
    public PlanNode replaceChildren(List<PlanNode> newChildren)
    {
        // FilterNode only expects a single upstream PlanNode
        if (newChildren == null || newChildren.size() != 1) {
            throw new IllegalArgumentException("Expect exactly one child to replace");
        }
        return new FilterNode(getSourceLocation(), getId(), getStatsEquivalentPlanNode(), newChildren.get(0), predicate);
    }

    @Override
    public LogicalProperties computeLogicalProperties(LogicalPropertiesProvider logicalPropertiesProvider)
    {
        requireNonNull(logicalPropertiesProvider, "logicalPropertiesProvider cannot be null.");
        return logicalPropertiesProvider.getFilterProperties(this);
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
        FilterNode that = (FilterNode) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(predicate, that.predicate);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(source, predicate);
    }
}

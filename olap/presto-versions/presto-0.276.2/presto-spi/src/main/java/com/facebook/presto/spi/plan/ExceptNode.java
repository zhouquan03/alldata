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
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Immutable
public final class ExceptNode
        extends SetOperationNode
{
    public ExceptNode(
            Optional<SourceLocation> sourceLocation,
            @JsonProperty("id") PlanNodeId id,
            @JsonProperty("sources") List<PlanNode> sources,
            @JsonProperty("outputVariables") List<VariableReferenceExpression> outputVariables,
            @JsonProperty("outputToInputs") Map<VariableReferenceExpression, List<VariableReferenceExpression>> outputToInputs)
    {
        this(sourceLocation, id, Optional.empty(), sources, outputVariables, outputToInputs);
    }

    public ExceptNode(
            Optional<SourceLocation> sourceLocation,
            PlanNodeId id,
            Optional<PlanNode> statsEquivalentPlanNode,
            List<PlanNode> sources,
            List<VariableReferenceExpression> outputVariables,
            Map<VariableReferenceExpression, List<VariableReferenceExpression>> outputToInputs)
    {
        super(sourceLocation, id, statsEquivalentPlanNode, sources, outputVariables, outputToInputs);
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context)
    {
        return visitor.visitExcept(this, context);
    }

    @Override
    public PlanNode assignStatsEquivalentPlanNode(Optional<PlanNode> statsEquivalentPlanNode)
    {
        return new ExceptNode(getSourceLocation(), getId(), statsEquivalentPlanNode, getSources(), getOutputVariables(), getVariableMapping());
    }

    @Override
    public PlanNode replaceChildren(List<PlanNode> newChildren)
    {
        return new ExceptNode(getSourceLocation(), getId(), getStatsEquivalentPlanNode(), newChildren, getOutputVariables(), getVariableMapping());
    }
}

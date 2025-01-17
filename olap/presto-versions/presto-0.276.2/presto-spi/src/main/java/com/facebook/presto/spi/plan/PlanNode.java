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
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic component of a Presto IR (logic plan).
 * An IR is a tree structure with each PlanNode performing a specific operation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@type")
public abstract class PlanNode
{
    private final Optional<SourceLocation> sourceLocation;
    private final PlanNodeId id;
    /**
     * A statistically equivalent version of plan node, i.e. number of output rows/size remains similar.
     * This is assigned by Presto optimizer.
     * Once assigned by the planner, further optimizer rules should respect this id when changing the plan.
     *
     * For example, when doing pushdown: Filter(TableScan()) -> TableScan(),
     * output TableScan should have same statsEquivalentPlanNode as input Filter node
     */
    private final Optional<PlanNode> statsEquivalentPlanNode;

    protected PlanNode(Optional<SourceLocation> sourceLocation, PlanNodeId id, Optional<PlanNode> statsEquivalentPlanNode)
    {
        this.sourceLocation = sourceLocation;
        this.id = requireNonNull(id, "id is null");
        this.statsEquivalentPlanNode = requireNonNull(statsEquivalentPlanNode, "statsEquivalentPlanNode is null");
    }

    @JsonProperty("id")
    public PlanNodeId getId()
    {
        return id;
    }

    @JsonProperty("sourceLocation")
    public Optional<SourceLocation> getSourceLocation()
    {
        return sourceLocation;
    }

    public Optional<PlanNode> getStatsEquivalentPlanNode()
    {
        return statsEquivalentPlanNode;
    }

    /**
     * Get the upstream PlanNodes (i.e., children) of the current PlanNode.
     */
    public abstract List<PlanNode> getSources();

    /**
     * Logical properties are a function of source properties and the operation performed by the plan node
     */
    public LogicalProperties computeLogicalProperties(LogicalPropertiesProvider logicalPropertiesProvider)
    {
        requireNonNull(logicalPropertiesProvider, "logicalPropertiesProvider cannot be null.");
        return logicalPropertiesProvider.getDefaultProperties();
    }

    /**
     * The output from the upstream PlanNodes.
     * It should serve as the input for the current PlanNode.
     */
    public abstract List<VariableReferenceExpression> getOutputVariables();

    /**
     * Alter the upstream PlanNodes of the current PlanNode.
     */
    public abstract PlanNode replaceChildren(List<PlanNode> newChildren);

    /**
     * A visitor pattern interface to operate on IR.
     */
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context)
    {
        return visitor.visitPlan(this, context);
    }

    /**
     * Assigns statsEquivalentPlanNode to the plan node
     */
    public abstract PlanNode assignStatsEquivalentPlanNode(Optional<PlanNode> statsEquivalentPlanNode);
}

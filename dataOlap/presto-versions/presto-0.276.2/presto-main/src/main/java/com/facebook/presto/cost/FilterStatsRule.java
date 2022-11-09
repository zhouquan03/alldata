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
package com.facebook.presto.cost;

import com.facebook.presto.Session;
import com.facebook.presto.matching.Pattern;
import com.facebook.presto.spi.plan.FilterNode;
import com.facebook.presto.sql.planner.TypeProvider;
import com.facebook.presto.sql.planner.iterative.Lookup;

import java.util.Optional;

import static com.facebook.presto.SystemSessionProperties.isDefaultFilterFactorEnabled;
import static com.facebook.presto.cost.FilterStatsCalculator.UNKNOWN_FILTER_COEFFICIENT;
import static com.facebook.presto.sql.planner.plan.Patterns.filter;
import static com.facebook.presto.sql.relational.OriginalExpressionUtils.castToExpression;
import static com.facebook.presto.sql.relational.OriginalExpressionUtils.isExpression;

public class FilterStatsRule
        extends SimpleStatsRule<FilterNode>
{
    private static final Pattern<FilterNode> PATTERN = filter();

    private final FilterStatsCalculator filterStatsCalculator;

    public FilterStatsRule(StatsNormalizer normalizer, FilterStatsCalculator filterStatsCalculator)
    {
        super(normalizer);
        this.filterStatsCalculator = filterStatsCalculator;
    }

    @Override
    public Pattern<FilterNode> getPattern()
    {
        return PATTERN;
    }

    @Override
    public Optional<PlanNodeStatsEstimate> doCalculate(FilterNode node, StatsProvider statsProvider, Lookup lookup, Session session, TypeProvider types)
    {
        PlanNodeStatsEstimate sourceStats = statsProvider.getStats(node.getSource());
        PlanNodeStatsEstimate estimate;
        if (isExpression(node.getPredicate())) {
            estimate = filterStatsCalculator.filterStats(sourceStats, castToExpression(node.getPredicate()), session, types);
        }
        else {
            estimate = filterStatsCalculator.filterStats(sourceStats, node.getPredicate(), session);
        }
        if (isDefaultFilterFactorEnabled(session) && estimate.isOutputRowCountUnknown()) {
            estimate = sourceStats.mapOutputRowCount(sourceRowCount -> sourceStats.getOutputRowCount() * UNKNOWN_FILTER_COEFFICIENT);
        }
        return Optional.of(estimate);
    }
}
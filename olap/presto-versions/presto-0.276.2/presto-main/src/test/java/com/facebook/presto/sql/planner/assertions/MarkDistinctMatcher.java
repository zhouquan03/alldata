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
package com.facebook.presto.sql.planner.assertions;

import com.facebook.presto.Session;
import com.facebook.presto.cost.StatsProvider;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.spi.plan.MarkDistinctNode;
import com.facebook.presto.spi.plan.PlanNode;
import com.facebook.presto.spi.relation.VariableReferenceExpression;
import com.facebook.presto.sql.planner.Symbol;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import static com.facebook.presto.sql.analyzer.ExpressionTreeUtils.createSymbolReference;
import static com.facebook.presto.sql.planner.assertions.MatchResult.NO_MATCH;
import static com.facebook.presto.sql.planner.assertions.MatchResult.match;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;

public class MarkDistinctMatcher
        implements Matcher
{
    private final PlanTestSymbol markerSymbol;
    private final List<PlanTestSymbol> distinctSymbols;
    private final Optional<PlanTestSymbol> hashSymbol;

    public MarkDistinctMatcher(PlanTestSymbol markerSymbol, List<PlanTestSymbol> distinctSymbols, Optional<PlanTestSymbol> hashSymbol)
    {
        this.markerSymbol = requireNonNull(markerSymbol, "markerSymbol is null");
        this.distinctSymbols = ImmutableList.copyOf(distinctSymbols);
        this.hashSymbol = requireNonNull(hashSymbol, "hashVariable is null");
    }

    @Override
    public boolean shapeMatches(PlanNode node)
    {
        return node instanceof MarkDistinctNode;
    }

    @Override
    public MatchResult detailMatches(PlanNode node, StatsProvider stats, Session session, Metadata metadata, SymbolAliases symbolAliases)
    {
        checkState(shapeMatches(node), "Plan testing framework error: shapeMatches returned false in detailMatches in %s", this.getClass().getName());
        MarkDistinctNode markDistinctNode = (MarkDistinctNode) node;

        if (!markDistinctNode.getHashVariable().map(variable -> new Symbol(variable.getName())).equals(hashSymbol.map(alias -> alias.toSymbol(symbolAliases)))) {
            return NO_MATCH;
        }

        if (!markDistinctNode.getDistinctVariables()
                .stream()
                .map(VariableReferenceExpression::getName)
                .map(Symbol::new)
                .collect(toImmutableSet())
                .equals(distinctSymbols.stream().map(alias -> alias.toSymbol(symbolAliases)).collect(toImmutableSet()))) {
            return NO_MATCH;
        }

        return match(markerSymbol.toString(), createSymbolReference(markDistinctNode.getMarkerVariable()));
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("markerSymbol", markerSymbol)
                .add("distinctSymbols", distinctSymbols)
                .add("hashVariable", hashSymbol)
                .toString();
    }
}

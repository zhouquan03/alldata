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
package com.facebook.presto.spi.statistics;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

public class TableStatisticsMetadata
{
    private static final TableStatisticsMetadata EMPTY_STATISTICS_METADATA = new TableStatisticsMetadata(emptySet(), emptySet(), emptyList());

    private final Set<ColumnStatisticMetadata> columnStatistics;
    private final Set<TableStatisticType> tableStatistics;
    private final List<String> groupingColumns;

    public static TableStatisticsMetadata empty()
    {
        return EMPTY_STATISTICS_METADATA;
    }

    public TableStatisticsMetadata(
            Set<ColumnStatisticMetadata> columnStatistics,
            Set<TableStatisticType> tableStatistics,
            List<String> groupingColumns)
    {
        this.columnStatistics = unmodifiableSet(new LinkedHashSet<>(requireNonNull(columnStatistics, "columnStatistics is null")));
        this.tableStatistics = unmodifiableSet(new LinkedHashSet<>(requireNonNull(tableStatistics, "tableStatistics is null")));
        this.groupingColumns = unmodifiableList(new ArrayList<>(requireNonNull(groupingColumns, "groupingColumns is null")));
    }

    public Set<ColumnStatisticMetadata> getColumnStatistics()
    {
        return columnStatistics;
    }

    public Set<TableStatisticType> getTableStatistics()
    {
        return tableStatistics;
    }

    public List<String> getGroupingColumns()
    {
        return groupingColumns;
    }

    public boolean isEmpty()
    {
        return tableStatistics.isEmpty() && columnStatistics.isEmpty();
    }
}

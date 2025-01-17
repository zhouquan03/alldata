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
package com.facebook.presto.tpch.statistics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class TableStatisticsData
{
    private final long rowCount;
    private final Map<String, ColumnStatisticsData> columns;

    @JsonCreator
    public TableStatisticsData(
            @JsonProperty("rowCount") long rowCount,
            @JsonProperty("columns") Map<String, ColumnStatisticsData> columns)
    {
        this.rowCount = rowCount;
        this.columns = ImmutableMap.copyOf(columns);
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public Map<String, ColumnStatisticsData> getColumns()
    {
        return columns;
    }
}

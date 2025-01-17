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
package com.facebook.presto.resourceGroups;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class VariableMap
{
    private static final VariableMap EMPTY_VARIABLE_MAP = new VariableMap(ImmutableMap.of());

    private final Map<String, String> variables;

    public VariableMap(Map<String, String> variables)
    {
        this.variables = ImmutableMap.copyOf(requireNonNull(variables, "variables is null"));
    }

    public String getValue(String key)
    {
        return variables.get(key);
    }

    @Override
    public String toString()
    {
        return variables.toString();
    }

    public static VariableMap emptyVariableMap()
    {
        return EMPTY_VARIABLE_MAP;
    }
}

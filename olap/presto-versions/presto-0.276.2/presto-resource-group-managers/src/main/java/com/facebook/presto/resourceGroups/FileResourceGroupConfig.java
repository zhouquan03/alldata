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

import com.facebook.airlift.configuration.Config;

import javax.validation.constraints.NotNull;

public class FileResourceGroupConfig
{
    private String configFile;

    @NotNull
    public String getConfigFile()
    {
        return configFile;
    }

    @Config("resource-groups.config-file")
    public FileResourceGroupConfig setConfigFile(String configFile)
    {
        this.configFile = configFile;
        return this;
    }
}

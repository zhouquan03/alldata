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
package com.facebook.presto.server;

import com.facebook.drift.annotations.ThriftConstructor;
import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftStruct;
import com.facebook.presto.spi.resourceGroups.ResourceGroupId;
import com.facebook.presto.spi.resourceGroups.ResourceGroupState;
import com.facebook.presto.spi.resourceGroups.SchedulingPolicy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.airlift.units.DataSize;

import javax.annotation.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/*
 * This class is exposed to external systems via ResourceGroupStateInfoResource and QueryStateInfoResource.
 * Be careful while changing it.
 */
@ThriftStruct
public class ResourceGroupInfo
{
    private final ResourceGroupId id;
    private final ResourceGroupState state;

    private final SchedulingPolicy schedulingPolicy;
    private final int schedulingWeight;

    private final DataSize softMemoryLimit;
    private final int softConcurrencyLimit;
    private final int hardConcurrencyLimit;
    private final int maxQueuedQueries;

    private final DataSize memoryUsage;
    private final int numQueuedQueries;
    private final int numRunningQueries;
    private final int numEligibleSubGroups;

    // Summaries do not include the following fields
    private final List<ResourceGroupInfo> subGroups;
    private final List<QueryStateInfo> runningQueries;

    @ThriftConstructor
    @JsonCreator
    public ResourceGroupInfo(
            @JsonProperty("id") ResourceGroupId id,
            @JsonProperty("state") ResourceGroupState state,
            @JsonProperty("schedulingPolicy") SchedulingPolicy schedulingPolicy,
            @JsonProperty("schedulingWeight") int schedulingWeight,
            @JsonProperty("softMemoryLimit") DataSize softMemoryLimit,
            @JsonProperty("softConcurrencyLimit") int softConcurrencyLimit,
            @JsonProperty("hardConcurrencyLimit") int hardConcurrencyLimit,
            @JsonProperty("maxQueuedQueries") int maxQueuedQueries,
            @JsonProperty("memoryUsage") DataSize memoryUsage,
            @JsonProperty("numQueuedQueries") int numQueuedQueries,
            @JsonProperty("numRunningQueries") int numRunningQueries,
            @JsonProperty("numEligibleSubGroups") int numEligibleSubGroups,
            @JsonProperty("subGroups") List<ResourceGroupInfo> subGroups,
            @JsonProperty("runningQueries") List<QueryStateInfo> runningQueries)
    {
        this.id = requireNonNull(id, "id is null");
        this.state = requireNonNull(state, "state is null");

        this.schedulingPolicy = requireNonNull(schedulingPolicy, "schedulingPolicy is null");
        this.schedulingWeight = schedulingWeight;

        this.softMemoryLimit = requireNonNull(softMemoryLimit, "softMemoryLimit is null");

        this.softConcurrencyLimit = softConcurrencyLimit;
        this.hardConcurrencyLimit = hardConcurrencyLimit;
        this.maxQueuedQueries = maxQueuedQueries;

        this.memoryUsage = requireNonNull(memoryUsage, "memoryUsage is null");
        this.numQueuedQueries = numQueuedQueries;
        this.numRunningQueries = numRunningQueries;
        this.numEligibleSubGroups = numEligibleSubGroups;

        this.runningQueries = runningQueries;

        this.subGroups = subGroups;
    }

    @JsonProperty
    @ThriftField(1)
    public ResourceGroupId getId()
    {
        return id;
    }

    @JsonProperty
    @ThriftField(2)
    public ResourceGroupState getState()
    {
        return state;
    }

    @JsonProperty
    @ThriftField(3)
    public SchedulingPolicy getSchedulingPolicy()
    {
        return schedulingPolicy;
    }

    @JsonProperty
    @ThriftField(4)
    public int getSchedulingWeight()
    {
        return schedulingWeight;
    }

    @JsonProperty
    @ThriftField(5)
    public DataSize getSoftMemoryLimit()
    {
        return softMemoryLimit;
    }

    @JsonProperty
    @ThriftField(6)
    public DataSize getMemoryUsage()
    {
        return memoryUsage;
    }

    @JsonProperty
    @ThriftField(7)
    public int getSoftConcurrencyLimit()
    {
        return softConcurrencyLimit;
    }

    @JsonProperty
    @ThriftField(8)
    public int getHardConcurrencyLimit()
    {
        return hardConcurrencyLimit;
    }

    @JsonProperty
    @ThriftField(9)
    public int getMaxQueuedQueries()
    {
        return maxQueuedQueries;
    }

    @JsonProperty
    @ThriftField(10)
    public int getNumQueuedQueries()
    {
        return numQueuedQueries;
    }

    @JsonProperty
    @ThriftField(11)
    public int getNumRunningQueries()
    {
        return numRunningQueries;
    }

    @JsonProperty
    @Deprecated
    @ThriftField(12)
    public int numAggregatedQueuedQueries()
    {
        return numQueuedQueries;
    }

    @JsonProperty
    @Deprecated
    @ThriftField(13)
    public int numAggregatedRunningQueries()
    {
        return numRunningQueries;
    }

    /**
     * @deprecated This field is not very useful to expose as part of resource endpoint.
     * In case of multi coordinator set up, it requires adding additional complexity and
     * overhead to existing system to expose this field  with accurate value.
     */
    @Deprecated
    @JsonProperty
    @ThriftField(14)
    public int getNumEligibleSubGroups()
    {
        return numEligibleSubGroups;
    }

    @JsonProperty
    @Nullable
    @ThriftField(15)
    public List<QueryStateInfo> getRunningQueries()
    {
        return runningQueries;
    }

    @JsonProperty
    @Nullable
    @ThriftField(16)
    public List<ResourceGroupInfo> getSubGroups()
    {
        return subGroups;
    }
}

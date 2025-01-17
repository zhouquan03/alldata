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
package com.facebook.presto.execution;

import com.facebook.airlift.log.Logger;
import com.facebook.presto.metadata.InternalNode;
import com.facebook.presto.util.FinalizerService;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.Math.toIntExact;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class NodeTaskMap
{
    private static final Logger log = Logger.get(NodeTaskMap.class);
    private final ConcurrentHashMap<InternalNode, NodeTasks> nodeTasksMap = new ConcurrentHashMap<>();
    private final FinalizerService finalizerService;

    @Inject
    public NodeTaskMap(FinalizerService finalizerService)
    {
        this.finalizerService = requireNonNull(finalizerService, "finalizerService is null");
    }

    public void addTask(InternalNode node, RemoteTask task)
    {
        createOrGetNodeTasks(node).addTask(task);
    }

    public PartitionedSplitsInfo getPartitionedSplitsOnNode(InternalNode node)
    {
        return createOrGetNodeTasks(node).getPartitionedSplitsInfo();
    }

    public long getNodeTotalMemoryUsageInBytes(InternalNode node)
    {
        return createOrGetNodeTasks(node).getTotalMemoryUsageInBytes();
    }

    public double getNodeCpuUtilizationPercentage(InternalNode node)
    {
        return createOrGetNodeTasks(node).getTotalCpuTimePerMillis();
    }

    public NodeStatsTracker createTaskStatsTracker(InternalNode node, TaskId taskId)
    {
        return createOrGetNodeTasks(node).createTaskStatsTrackers(taskId);
    }

    private NodeTasks createOrGetNodeTasks(InternalNode node)
    {
        return nodeTasksMap.computeIfAbsent(node, key -> new NodeTasks(finalizerService));
    }

    private static class NodeTasks
    {
        private final Set<RemoteTask> remoteTasks = Sets.newConcurrentHashSet();
        private final AtomicLong nodeTotalPartitionedSplitCount = new AtomicLong();
        private final AtomicLong nodeTotalPartitionedSplitsWeight = new AtomicLong();
        private final AtomicLong nodeTotalMemoryUsageInBytes = new AtomicLong();
        private final AtomicDouble nodeTotalCpuTimePerMillis = new AtomicDouble();
        private final FinalizerService finalizerService;

        public NodeTasks(FinalizerService finalizerService)
        {
            this.finalizerService = requireNonNull(finalizerService, "finalizerService is null");
        }

        private PartitionedSplitsInfo getPartitionedSplitsInfo()
        {
            return PartitionedSplitsInfo.forSplitCountAndWeightSum(toIntExact(nodeTotalPartitionedSplitCount.get()), nodeTotalPartitionedSplitsWeight.get());
        }

        private long getTotalMemoryUsageInBytes()
        {
            return nodeTotalMemoryUsageInBytes.get();
        }

        private double getTotalCpuTimePerMillis()
        {
            return nodeTotalCpuTimePerMillis.get();
        }

        private void addTask(RemoteTask task)
        {
            if (remoteTasks.add(task)) {
                task.addStateChangeListener(taskStatus -> {
                    if (taskStatus.getState().isDone()) {
                        remoteTasks.remove(task);
                    }
                });

                // Check if task state is already done before adding the listener
                if (task.getTaskStatus().getState().isDone()) {
                    remoteTasks.remove(task);
                }
            }
        }

        public NodeStatsTracker createTaskStatsTrackers(TaskId taskId)
        {
            requireNonNull(taskId, "taskId is null");

            PartitionedSplitsTracker splitTracker = new PartitionedSplitsTracker(taskId, nodeTotalPartitionedSplitCount, nodeTotalPartitionedSplitsWeight);
            TaskStatsTracker memoryUsageTracker = new TaskStatsTracker("MemoryTracker", taskId, nodeTotalMemoryUsageInBytes);
            AccumulatedTaskStatsTracker cpuUtilizationPercentageTracker = new AccumulatedTaskStatsTracker("CpuTracker", taskId, nodeTotalCpuTimePerMillis);
            NodeStatsTracker nodeStatsTracker = new NodeStatsTracker(splitTracker, memoryUsageTracker::setValue, cpuUtilizationPercentageTracker::setValue);

            // when nodeStatsTracker is garbage collected, run the cleanup method on the tracker
            // Note: tracker can not have a reference to nodeStatsTracker
            // instances of TaskStatsTracker and AccumulatedTaskStatsTracker should not be passed for GC to
            // help ensure that GC is actually invoked for nodeStatsTracker
            finalizerService.addFinalizer(nodeStatsTracker, () -> {
                splitTracker.cleanup();
                memoryUsageTracker.cleanup();
                cpuUtilizationPercentageTracker.cleanup();
            });

            return nodeStatsTracker;
        }

        private static class PartitionedSplitsTracker
                implements Consumer<PartitionedSplitsInfo>
        {
            private final TaskId taskId;
            private final AtomicLong totalSplitCount;
            private final AtomicLong totalSplitWeight;
            private final AtomicLong localSplitCount = new AtomicLong();
            private final AtomicLong localSplitWeight = new AtomicLong();

            public PartitionedSplitsTracker(TaskId taskId, AtomicLong totalSplitCount, AtomicLong totalSplitWeight)
            {
                this.taskId = requireNonNull(taskId, "taskId is null");
                this.totalSplitCount = requireNonNull(totalSplitCount, "totalSplitCount is null");
                this.totalSplitWeight = requireNonNull(totalSplitWeight, "totalSplitWeight is null");
            }

            @Override
            public synchronized void accept(PartitionedSplitsInfo partitionedSplits)
            {
                if (partitionedSplits == null || partitionedSplits.getCount() < 0 || partitionedSplits.getWeightSum() < 0) {
                    clearLocalSplitInfo(false);
                    requireNonNull(partitionedSplits, "partitionedSplits is null"); // throw NPE if null, otherwise negative value
                    throw new IllegalArgumentException("Invalid negative value: " + partitionedSplits);
                }

                long newCount = partitionedSplits.getCount();
                long newWeight = partitionedSplits.getWeightSum();
                long countDelta = newCount - localSplitCount.getAndSet(newCount);
                long weightDelta = newWeight - localSplitWeight.getAndSet(newWeight);
                if (countDelta != 0) {
                    totalSplitCount.addAndGet(countDelta);
                }
                if (weightDelta != 0) {
                    totalSplitWeight.addAndGet(weightDelta);
                }
            }

            private void clearLocalSplitInfo(boolean reportAsLeaked)
            {
                long leakedCount = localSplitCount.getAndSet(0);
                long leakedWeight = localSplitWeight.getAndSet(0);
                if (leakedCount == 0 && leakedWeight == 0) {
                    return;
                }

                if (reportAsLeaked) {
                    log.error("BUG! %s for %s leaked with %s partitioned splits (weight: %s). Cleaning up so server can continue to function.",
                            getClass().getName(),
                            taskId,
                            leakedCount,
                            leakedWeight);
                }

                totalSplitCount.addAndGet(-leakedCount);
                totalSplitWeight.addAndGet(-leakedWeight);
            }

            public void cleanup()
            {
                clearLocalSplitInfo(true);
            }

            @Override
            public String toString()
            {
                return toStringHelper(this)
                        .add("taskId", taskId)
                        .add("splits", localSplitCount)
                        .add("weight", localSplitWeight)
                        .toString();
            }
        }

        @ThreadSafe
        private static class TaskStatsTracker
        {
            private final String counterName;
            private final TaskId taskId;
            private final AtomicLong totalValue;
            private final AtomicLong localValue = new AtomicLong();

            public TaskStatsTracker(String counterName, TaskId taskId, AtomicLong totalValue)
            {
                this.counterName = requireNonNull(counterName, "counterName is null");
                this.taskId = requireNonNull(taskId, "taskId is null");
                this.totalValue = requireNonNull(totalValue, "totalValue is null");
            }

            public synchronized void setValue(long value)
            {
                if (value < 0) {
                    long oldValue = this.localValue.getAndSet(0L);
                    totalValue.addAndGet(-oldValue);
                    throw new IllegalArgumentException(counterName + " is negative");
                }

                long oldValue = this.localValue.getAndSet(value);
                totalValue.addAndGet(value - oldValue);
            }

            public void cleanup()
            {
                long leakedValues = localValue.getAndSet(0);
                if (leakedValues == 0) {
                    return;
                }

                log.error("BUG! %s for %s leaked with %s %s.  Cleaning up so server can continue to function.",
                        getClass().getName(),
                        taskId,
                        leakedValues,
                        counterName);

                totalValue.addAndGet(-leakedValues);
            }

            @Override
            public String toString()
            {
                return toStringHelper(this)
                        .add("taskId", taskId)
                        .add(counterName, localValue)
                        .toString();
            }
        }

        // tracks stats which are passed as accumulated (cpu time) by calculating delta / duration.
        @ThreadSafe
        private static class AccumulatedTaskStatsTracker
        {
            private final String counterName;
            private final TaskId taskId;
            private final AtomicDouble totalValue;
            private final AtomicDouble localValue = new AtomicDouble();
            private long previousTaskAge;
            private long previousValue;

            AccumulatedTaskStatsTracker(String counterName, TaskId taskId, AtomicDouble totalValue)
            {
                this.counterName = requireNonNull(counterName, "counterName is null");
                this.taskId = requireNonNull(taskId, "taskId is null");
                this.totalValue = requireNonNull(totalValue, "totalValue is null");
            }

            private double getDeltaPerSecond(long taskAgeInMillis, long value)
            {
                if (previousTaskAge == 0 && value > 0) {
                    previousTaskAge = taskAgeInMillis;
                    previousValue = value;
                    return 0;
                }

                if (taskAgeInMillis <= previousTaskAge) {
                    return 0;
                }

                if (value > 0) {
                    double deltaValue = (value - previousValue) * 100;
                    long deltaDuration = taskAgeInMillis - previousTaskAge;
                    previousTaskAge = taskAgeInMillis;
                    previousValue = value;
                    return deltaValue > 0 ? deltaValue / deltaDuration : 0;
                }
                return 0;
            }

            public synchronized void setValue(long taskAgeInMillis, long value)
            {
                double delta = getDeltaPerSecond(taskAgeInMillis, value);

                if (delta < 0) {
                    double oldValue = this.localValue.getAndSet(0D);
                    totalValue.addAndGet(-oldValue);
                    throw new IllegalArgumentException(counterName + " is negative");
                }

                double oldValue = this.localValue.getAndSet(delta);
                totalValue.addAndGet(delta - oldValue);
            }

            public void cleanup()
            {
                double leakedValues = localValue.getAndSet(0D);
                if (leakedValues == 0) {
                    return;
                }

                log.error("BUG! %s for %s leaked with %s %s.  Cleaning up so server can continue to function.",
                        getClass().getName(),
                        taskId,
                        leakedValues,
                        counterName);

                totalValue.addAndGet(-leakedValues);
            }
        }
    }

    public static class NodeStatsTracker
    {
        private final Consumer<PartitionedSplitsInfo> splitSetter;
        private final LongConsumer memoryUsageSetter;
        private final CumulativeStatsConsumer cpuUsageSetter;

        public NodeStatsTracker(Consumer<PartitionedSplitsInfo> splitSetter, LongConsumer memoryUsageSetter, CumulativeStatsConsumer cpuUsageSetter)
        {
            this.splitSetter = requireNonNull(splitSetter, "splitSetter is null");
            this.memoryUsageSetter = requireNonNull(memoryUsageSetter, "memoryUsageSetter is null");
            this.cpuUsageSetter = requireNonNull(cpuUsageSetter, "cpuUsageSetter is null");
        }

        public void setPartitionedSplits(PartitionedSplitsInfo partitionedSplits)
        {
            splitSetter.accept(partitionedSplits);
        }

        public void setMemoryUsage(long memoryUsage)
        {
            memoryUsageSetter.accept(memoryUsage);
        }

        public void setCpuUsage(long age, long cpuUsage)
        {
            cpuUsageSetter.accept(age, cpuUsage);
        }

        @Override
        public String toString()
        {
            return toStringHelper(this)
                    .add("splitSetter", splitSetter.toString())
                    .add("memoryUsageSetter", memoryUsageSetter.toString())
                    .add("cpuUsageSetter", cpuUsageSetter.toString())
                    .toString();
        }
    }

    public interface CumulativeStatsConsumer
    {
        void accept(long age, long value);
    }
}

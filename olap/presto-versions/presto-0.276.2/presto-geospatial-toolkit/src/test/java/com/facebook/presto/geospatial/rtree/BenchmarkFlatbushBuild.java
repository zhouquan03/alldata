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
package com.facebook.presto.geospatial.rtree;

import com.facebook.presto.geospatial.Rectangle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.facebook.presto.geospatial.rtree.RtreeTestUtils.makeRectangles;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
public class BenchmarkFlatbushBuild
{
    private static final int SEED = 613;

    @Benchmark
    @OperationsPerInvocation(1)
    public void buildRtree(BenchmarkData data, Blackhole blackhole)
    {
        blackhole.consume(new Flatbush<>(
                data.getBuildRectangles().toArray(new Rectangle[] {}),
                data.getRtreeDegree()));
    }

    @State(Scope.Thread)
    public static class BenchmarkData
    {
        @Param({"8", "16", "32"})
        private int rtreeDegree;
        @Param({"1000", "3000", "10000", "30000", "100000", "300000", "1000000"})
        private int numBuildRectangles;

        private List<Rectangle> buildRectangles;

        @Setup
        public void setup()
        {
            Random random = new Random(SEED);
            buildRectangles = makeRectangles(random, numBuildRectangles);
        }

        public int getRtreeDegree()
        {
            return rtreeDegree;
        }

        public List<Rectangle> getBuildRectangles()
        {
            return buildRectangles;
        }
    }

    public static void main(String[] args)
            throws Throwable
    {
        Options options = new OptionsBuilder()
                .verbosity(VerboseMode.NORMAL)
                .include(".*" + BenchmarkFlatbushBuild.class.getSimpleName() + ".*")
                .build();
        new Runner(options).run();
    }
}

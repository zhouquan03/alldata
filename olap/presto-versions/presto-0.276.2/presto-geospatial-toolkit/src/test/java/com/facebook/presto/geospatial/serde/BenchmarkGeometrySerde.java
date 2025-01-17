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
package com.facebook.presto.geospatial.serde;

import com.esri.core.geometry.ogc.OGCGeometry;
import com.google.common.base.Joiner;
import io.airlift.slice.Slice;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import static com.esri.core.geometry.ogc.OGCGeometry.fromText;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.GEOMETRYCOLLECTION;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.LINESTRING;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.MULTILINESTRING;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.MULTIPOINT;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.MULTIPOLYGON;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.POINT;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.POLYGON;
import static com.facebook.presto.geospatial.serde.BenchmarkGeometrySerializationData.readResource;
import static com.facebook.presto.geospatial.serde.EsriGeometrySerde.deserialize;
import static com.facebook.presto.geospatial.serde.EsriGeometrySerde.deserializeEnvelope;
import static com.facebook.presto.geospatial.serde.EsriGeometrySerde.serialize;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 3, time = 3, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 4, timeUnit = SECONDS)
@OutputTimeUnit(SECONDS)
@BenchmarkMode(Throughput)
public class BenchmarkGeometrySerde
{
    // POINT
    @Benchmark
    public Object serializePoint(BenchmarkData data)
    {
        return serialize(data.point);
    }

    @Benchmark
    public Object deserializePoint(BenchmarkData data)
    {
        return deserialize(data.pointSerialized);
    }

    @Benchmark
    public Object deserializePointEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.pointSerialized);
    }

    // MULTI POINT
    @Benchmark
    public Object serializeSimpleMultipoint(BenchmarkData data)
    {
        return serialize(data.simpleMultipoint);
    }

    @Benchmark
    public Object deserializeSimpleMultipoint(BenchmarkData data)
    {
        return deserialize(data.simpleMultipointSerialized);
    }

    @Benchmark
    public Object deserializeSimpleMultipointEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simpleMultipointSerialized);
    }

    @Benchmark
    public Object serializeComplexMultipoint(BenchmarkData data)
    {
        return serialize(data.complexMultipoint);
    }

    @Benchmark
    public Object deserializeComplexMultipoint(BenchmarkData data)
    {
        return deserialize(data.complexMultipointSerialized);
    }

    @Benchmark
    public Object deserializeComplexMultipointEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexMultipointSerialized);
    }

    // LINE STRING
    @Benchmark
    public Object serializeSimpleLineString(BenchmarkData data)
    {
        return serialize(data.simpleLineString);
    }

    @Benchmark
    public Object deserializeSimpleLineString(BenchmarkData data)
    {
        return deserialize(data.simpleLineStringSerialized);
    }

    @Benchmark
    public Object deserializeSimpleLineStringEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simpleLineStringSerialized);
    }

    @Benchmark
    public Object serializeComplexLineString(BenchmarkData data)
    {
        return serialize(data.complexLineString);
    }

    @Benchmark
    public Object deserializeComplexLineString(BenchmarkData data)
    {
        return deserialize(data.complexLineStringSerialized);
    }

    @Benchmark
    public Object deserializeComplexLineStringEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexLineStringSerialized);
    }

    // MULTILINE STRING
    @Benchmark
    public Object serializeSimpleMultiLineString(BenchmarkData data)
    {
        return serialize(data.simpleMultiLineString);
    }

    @Benchmark
    public Object deserializeSimpleMultiLineString(BenchmarkData data)
    {
        return deserialize(data.simpleMultiLineStringSerialized);
    }

    @Benchmark
    public Object deserializeSimpleMultiLineStringEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simpleMultiLineStringSerialized);
    }

    @Benchmark
    public Object serializeComplexMultiLineString(BenchmarkData data)
    {
        return serialize(data.complexMultiLineString);
    }

    @Benchmark
    public Object deserializeComplexMultiLineString(BenchmarkData data)
    {
        return deserialize(data.complexMultiLineStringSerialized);
    }

    @Benchmark
    public Object deserializeComplexMultiLineStringEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexMultiLineStringSerialized);
    }

    // POLYGON
    @Benchmark
    public Object serializeSimplePolygon(BenchmarkData data)
    {
        return serialize(data.simplePolygon);
    }

    @Benchmark
    public Object deserializeSimplePolygon(BenchmarkData data)
    {
        return deserialize(data.simplePolygonSerialized);
    }

    @Benchmark
    public Object deserializeSimplePolygonEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simplePolygonSerialized);
    }

    @Benchmark
    public Object serializeComplexPolygon(BenchmarkData data)
    {
        return serialize(data.complexPolygon);
    }

    @Benchmark
    public Object deserializeComplexPolygon(BenchmarkData data)
    {
        return deserialize(data.complexPolygonSerialized);
    }

    @Benchmark
    public Object deserializeComplexPolygonEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexPolygonSerialized);
    }

    // MULTI POLYGON
    @Benchmark
    public Object serializeSimpleMultiPolygon(BenchmarkData data)
    {
        return serialize(data.simpleMultiPolygon);
    }

    @Benchmark
    public Object deserializeSimpleMultiPolygon(BenchmarkData data)
    {
        return deserialize(data.simpleMultiPolygonSerialized);
    }

    @Benchmark
    public Object deserializeSimpleMultiPolygonEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simpleMultiPolygonSerialized);
    }

    @Benchmark
    public Object serializeComplexMultiPolygon(BenchmarkData data)
    {
        return serialize(data.complexMultiPolygon);
    }

    @Benchmark
    public Object deserializeComplexMultiPolygon(BenchmarkData data)
    {
        return deserialize(data.complexMultiPolygonSerialized);
    }

    @Benchmark
    public Object deserializeComplexMultiPolygonEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexMultiPolygonSerialized);
    }

    // GEOMETRY COLLECTION
    @Benchmark
    public Object serializeSimpleGeometryCollection(BenchmarkData data)
    {
        return serialize(data.simpleGeometryCollection);
    }

    @Benchmark
    public Object deserializeSimpleGeometryCollection(BenchmarkData data)
    {
        return deserialize(data.simpleGeometryCollectionSerialized);
    }

    @Benchmark
    public Object deserializeSimpleGeometryCollectionEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.simpleGeometryCollectionSerialized);
    }

    @Benchmark
    public Object serializeComplexGeometryCollection(BenchmarkData data)
    {
        return serialize(data.complexGeometryCollection);
    }

    @Benchmark
    public Object deserializeComplexGeometryCollection(BenchmarkData data)
    {
        return deserialize(data.complexGeometryCollectionSerialized);
    }

    @Benchmark
    public Object deserializeComplexGeometryCollectionEnvelope(BenchmarkData data)
    {
        return deserializeEnvelope(data.complexGeometryCollectionSerialized);
    }

    @State(Scope.Thread)
    public static class BenchmarkData
    {
        // POINT
        private OGCGeometry point;
        private Slice pointSerialized;

        // MULTI POINT
        private OGCGeometry simpleMultipoint;
        private Slice simpleMultipointSerialized;
        private OGCGeometry complexMultipoint;
        private Slice complexMultipointSerialized;

        // LINE STRING
        private OGCGeometry simpleLineString;
        private Slice simpleLineStringSerialized;
        private OGCGeometry complexLineString;
        private Slice complexLineStringSerialized;

        // MULTILINE STRING
        private OGCGeometry simpleMultiLineString;
        private Slice simpleMultiLineStringSerialized;
        private OGCGeometry complexMultiLineString;
        private Slice complexMultiLineStringSerialized;

        // POLYGON
        private OGCGeometry simplePolygon;
        private Slice simplePolygonSerialized;
        private OGCGeometry complexPolygon;
        private Slice complexPolygonSerialized;

        // MULTI POLYGON
        private OGCGeometry simpleMultiPolygon;
        private Slice simpleMultiPolygonSerialized;
        private OGCGeometry complexMultiPolygon;
        private Slice complexMultiPolygonSerialized;

        // COLLECTION
        private OGCGeometry simpleGeometryCollection;
        private Slice simpleGeometryCollectionSerialized;
        private OGCGeometry complexGeometryCollection;
        private Slice complexGeometryCollectionSerialized;

        @Setup
        public void setup()
        {
            point = fromText(POINT);
            pointSerialized = serialize(point);

            simpleMultipoint = fromText(MULTIPOINT);
            simpleMultipointSerialized = serialize(simpleMultipoint);
            complexMultipoint = fromText(readResource("complex-multipoint.txt"));
            complexMultipointSerialized = serialize(complexMultipoint);

            simpleLineString = fromText(LINESTRING);
            simpleLineStringSerialized = serialize(simpleLineString);
            complexLineString = fromText(readResource("complex-linestring.txt"));
            complexLineStringSerialized = serialize(complexLineString);

            simpleMultiLineString = fromText(MULTILINESTRING);
            simpleMultiLineStringSerialized = serialize(simpleMultiLineString);
            complexMultiLineString = fromText(readResource("complex-multilinestring.txt"));
            complexMultiLineStringSerialized = serialize(complexMultiLineString);

            simplePolygon = fromText(POLYGON);
            simplePolygonSerialized = serialize(simplePolygon);
            complexPolygon = fromText(readResource("complex-polygon.txt"));
            complexPolygonSerialized = serialize(complexPolygon);

            simpleMultiPolygon = fromText(MULTIPOLYGON);
            simpleMultiPolygonSerialized = serialize(simpleMultiPolygon);
            complexMultiPolygon = fromText(readResource("complex-multipolygon.txt"));
            complexMultiPolygonSerialized = serialize(complexMultiPolygon);

            simpleGeometryCollection = fromText(GEOMETRYCOLLECTION);
            simpleGeometryCollectionSerialized = serialize(simpleGeometryCollection);
            complexGeometryCollection = fromText("GEOMETRYCOLLECTION (" + Joiner.on(", ").join(
                    readResource("complex-multipoint.txt"),
                    readResource("complex-linestring.txt"),
                    readResource("complex-multilinestring.txt"),
                    readResource("complex-polygon.txt"),
                    readResource("complex-multipolygon.txt")) + ")");
            complexGeometryCollectionSerialized = serialize(complexGeometryCollection);
        }
    }

    public static void main(String[] args)
            throws RunnerException
    {
        Options options = new OptionsBuilder()
                .verbosity(VerboseMode.NORMAL)
                .include(".*" + BenchmarkGeometrySerde.class.getSimpleName() + ".*")
                .build();
        new Runner(options).run();
    }
}

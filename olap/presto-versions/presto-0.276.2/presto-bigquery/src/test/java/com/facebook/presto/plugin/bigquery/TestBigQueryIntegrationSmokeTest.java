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
package com.facebook.presto.plugin.bigquery;

import com.facebook.presto.testing.MaterializedResult;
import com.facebook.presto.testing.QueryRunner;
import com.facebook.presto.tests.AbstractTestIntegrationSmokeTest;
import com.google.common.collect.ImmutableMap;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static com.facebook.presto.common.type.VarcharType.VARCHAR;
import static com.facebook.presto.testing.MaterializedResult.resultBuilder;
import static com.facebook.presto.testing.assertions.Assert.assertEquals;
import static java.lang.String.format;

@Test
public class TestBigQueryIntegrationSmokeTest
        extends AbstractTestIntegrationSmokeTest
{
    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        return BigQueryQueryRunner.createQueryRunner(ImmutableMap.of());
    }

    @Override
    public void testDescribeTable()
    {
        MaterializedResult expectedColumns = resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR, VARCHAR, VARCHAR, VARCHAR)
                .row("orderkey", "bigint", "", "")
                .row("custkey", "bigint", "", "")
                .row("orderstatus", "varchar", "", "")
                .row("totalprice", "double", "", "")
                .row("orderdate", "date", "", "")
                .row("orderpriority", "varchar", "", "")
                .row("clerk", "varchar", "", "")
                .row("shippriority", "bigint", "", "")
                .row("comment", "varchar", "", "")
                .build();
        MaterializedResult actualColumns = computeActual("DESCRIBE orders");
        assertEquals(actualColumns, expectedColumns);
    }

    @Test
    public void testDuplicatedRowCreateTable()
    {
        throw new SkipException("CREATE TABLE is not supported");
    }

    @Test
    public void testColumnPositionMismatch()
    {
        String tableName = "test.test_column_position_mismatch";

        assertUpdate("DROP TABLE IF EXISTS " + tableName);
        assertUpdate(format("CREATE TABLE %s (c_varchar VARCHAR, c_int INT, c_date DATE)", tableName));
        getQueryRunner().execute(getSession(), format("INSERT INTO %s VALUES ('a', 1, '2021-01-01')", tableName));

        // Adding a CAST makes BigQuery return columns in a different order
        assertQuery(
                "SELECT c_varchar, CAST(c_int AS SMALLINT), c_date FROM " + tableName,
                "VALUES ('a', 1, '2021-01-01')");

        assertUpdate("DROP TABLE " + tableName);
    }
}

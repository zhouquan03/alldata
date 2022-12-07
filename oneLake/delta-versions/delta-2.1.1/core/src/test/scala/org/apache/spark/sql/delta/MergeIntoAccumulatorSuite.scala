/*
 * Copyright (2021) The Delta Lake Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.delta

import java.util.concurrent.atomic.AtomicReference

import scala.collection.JavaConverters._

import org.apache.spark.sql.delta.commands.MergeIntoCommand
import org.apache.spark.sql.delta.test.DeltaSQLCommandTest

import org.apache.spark.scheduler.{SparkListener, SparkListenerTaskEnd}
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.status.TaskDataWrapper
import org.apache.spark.util.JsonProtocol

/**
 * Tests how the accumulator used by the MERGE command reacts with other Spark components such as
 * Spark UI. These tests stay in a separated file so that we can use the package name
 * `org.apache.spark.sql.delta` to access `private[spark]` APIs.
 */
class MergeIntoAccumulatorSuite
  extends SharedSparkSession
  with DeltaSQLCommandTest {

  import testImplicits._

  private def runTestMergeCommand(): Unit = {
    // Run a simple merge command
    withTempView("source") {
      withTempDir { tempDir =>
        val tempPath = tempDir.getCanonicalPath
        Seq((1, 1), (0, 3)).toDF("key", "value").createOrReplaceTempView("source")
        Seq((2, 2), (1, 4)).toDF("key", "value").write.format("delta").save(tempPath)
        spark.sql(s"""
          |MERGE INTO delta.`$tempPath` target
          |USING source src
          |ON src.key = target.key
          |WHEN MATCHED THEN UPDATE SET *
          |WHEN NOT MATCHED THEN INSERT *
          |""".stripMargin)
      }
    }
  }

  test("accumulators used by MERGE should not be tracked by Spark UI") {
    runTestMergeCommand()

    // Make sure all Spark events generated by the above command have been processed
    spark.sparkContext.listenerBus.waitUntilEmpty(30000)

    val store = spark.sparkContext.statusStore.store
    val iter = store.view(classOf[TaskDataWrapper]).closeableIterator()
    try {
      // Collect all accumulator names tracked by Spark UI.
      val accumNames = iter.asScala.toVector.flatMap { task =>
        task.accumulatorUpdates.map(_.name)
      }.toSet
      // Verify accumulators used by MergeIntoCommand are not tracked.
      assert(!accumNames.contains(MergeIntoCommand.TOUCHED_FILES_ACCUM_NAME))
    } finally {
      iter.close()
    }
  }

  test("accumulators used by MERGE should not fail Spark event log generation") {
    // Register a listener to convert `SparkListenerTaskEnd` to json and catch failures.
    val failure = new AtomicReference[Throwable]()
    val listener = new SparkListener {
      override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
        try JsonProtocol.sparkEventToJson(taskEnd) catch {
          case t: Throwable => failure.compareAndSet(null, t)
        }
      }
    }
    spark.sparkContext.listenerBus.addToSharedQueue(listener)
    try {
      runTestMergeCommand()

      // Make sure all Spark events generated by the above command have been processed
      spark.sparkContext.listenerBus.waitUntilEmpty(30000)
      // Converting `SparkListenerEvent` to json should not fail
      assert(failure.get == null)
    } finally {
      spark.sparkContext.listenerBus.removeListener(listener)
    }
  }
}
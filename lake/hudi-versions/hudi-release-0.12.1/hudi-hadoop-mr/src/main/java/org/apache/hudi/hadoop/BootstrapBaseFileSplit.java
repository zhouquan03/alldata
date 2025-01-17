/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.hadoop;

import org.apache.hadoop.mapred.FileSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Sub-type of File Split which encapsulates both skeleton and bootstrap base file splits.
 */
public class BootstrapBaseFileSplit extends FileSplit {

  private FileSplit bootstrapFileSplit;

  /**
   * NOTE: This ctor is necessary for Hive to be able to serialize and
   *       then instantiate it when deserializing back
   */
  public BootstrapBaseFileSplit() {}

  public BootstrapBaseFileSplit(FileSplit baseSplit, FileSplit bootstrapFileSplit)
      throws IOException {
    super(baseSplit.getPath(), baseSplit.getStart(), baseSplit.getLength(), baseSplit.getLocations());
    this.bootstrapFileSplit = bootstrapFileSplit;
  }

  public FileSplit getBootstrapFileSplit() {
    return bootstrapFileSplit;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    bootstrapFileSplit.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    bootstrapFileSplit = new WrapperFileSplit();
    bootstrapFileSplit.readFields(in);
  }

  /**
   * Wrapper for FileSplit just to expose default constructor to the outer class.
   */
  public static class WrapperFileSplit extends FileSplit {

    public WrapperFileSplit() {
      super();
    }
  }
}
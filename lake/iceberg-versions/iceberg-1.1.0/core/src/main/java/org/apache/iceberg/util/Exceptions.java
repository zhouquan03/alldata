/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.util;

import java.io.Closeable;
import java.io.IOException;
import org.apache.iceberg.exceptions.RuntimeIOException;

public class Exceptions {
  private Exceptions() {}

  public static void close(Closeable closeable, boolean suppressExceptions) {
    try {
      closeable.close();
    } catch (IOException e) {
      if (!suppressExceptions) {
        throw new RuntimeIOException(e, "Failed calling close");
      }
      // otherwise, ignore the exception
    }
  }

  public static <E extends Exception> E suppressExceptions(E alreadyThrown, Runnable run) {
    try {
      run.run();
    } catch (Exception e) {
      alreadyThrown.addSuppressed(e);
    }
    return alreadyThrown;
  }

  public static <E extends Exception> void suppressAndThrow(E alreadyThrown, Runnable run)
      throws E {
    throw suppressExceptions(alreadyThrown, run);
  }
}

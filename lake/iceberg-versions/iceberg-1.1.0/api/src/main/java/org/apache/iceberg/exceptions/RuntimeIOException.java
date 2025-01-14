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
package org.apache.iceberg.exceptions;

import com.google.errorprone.annotations.FormatMethod;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @deprecated Use java.io.UncheckedIOException directly instead.
 *     <p>Exception used to wrap {@link IOException} as a {@link RuntimeException} and add context.
 */
@Deprecated
public class RuntimeIOException extends UncheckedIOException {

  public RuntimeIOException(IOException cause) {
    super(cause);
  }

  @FormatMethod
  public RuntimeIOException(IOException cause, String message, Object... args) {
    super(String.format(message, args), cause);
  }

  @FormatMethod
  public RuntimeIOException(String message, Object... args) {
    super(new IOException(String.format(message, args)));
  }
}

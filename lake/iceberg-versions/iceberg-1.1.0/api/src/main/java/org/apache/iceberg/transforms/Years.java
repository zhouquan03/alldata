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
package org.apache.iceberg.transforms;

import java.io.ObjectStreamException;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

class Years<T> extends TimeTransform<T> {
  private static final Years<?> INSTANCE = new Years<>();

  @SuppressWarnings("unchecked")
  static <T> Years<T> get() {
    return (Years<T>) INSTANCE;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Transform<T, Integer> toEnum(Type type) {
    switch (type.typeId()) {
      case DATE:
        return (Transform<T, Integer>) Dates.YEAR;
      case TIMESTAMP:
        return (Transform<T, Integer>) Timestamps.YEAR;
      default:
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
  }

  @Override
  public Type getResultType(Type sourceType) {
    return Types.IntegerType.get();
  }

  @Override
  public boolean satisfiesOrderOf(Transform<?, ?> other) {
    if (this == other) {
      return true;
    }

    if (other instanceof Timestamps) {
      return Timestamps.YEAR.satisfiesOrderOf(other);
    } else if (other instanceof Dates) {
      return Dates.YEAR.satisfiesOrderOf(other);
    } else if (other instanceof Years) {
      return true;
    }

    return false;
  }

  @Override
  public String toHumanString(Type alwaysInt, Integer value) {
    return value != null ? TransformUtil.humanYear(value) : "null";
  }

  @Override
  public String toString() {
    return "year";
  }

  Object writeReplace() throws ObjectStreamException {
    return SerializationProxies.YearsTransformProxy.get();
  }
}

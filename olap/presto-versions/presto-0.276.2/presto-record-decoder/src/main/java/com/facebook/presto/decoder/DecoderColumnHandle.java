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
package com.facebook.presto.decoder;

import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.ColumnHandle;

// Connectors that have rows that can be decoded into
// columns using com.facebook.presto.decoder
// can choose to implement this interface
public interface DecoderColumnHandle
        extends ColumnHandle
{
    boolean isInternal();

    String getFormatHint();

    Type getType();

    String getName();

    String getMapping();

    String getDataFormat();
}

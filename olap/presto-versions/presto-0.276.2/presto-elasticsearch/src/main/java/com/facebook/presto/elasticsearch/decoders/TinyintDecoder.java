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
package com.facebook.presto.elasticsearch.decoders;

import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.spi.PrestoException;
import org.elasticsearch.search.SearchHit;

import java.util.function.Supplier;

import static com.facebook.presto.common.type.TinyintType.TINYINT;
import static com.facebook.presto.elasticsearch.ElasticsearchErrorCode.ELASTICSEARCH_TYPE_MISMATCH;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class TinyintDecoder
        implements Decoder
{
    private final String path;

    public TinyintDecoder(String path)
    {
        this.path = requireNonNull(path, "path is null");
    }

    @Override
    public void decode(SearchHit hit, Supplier<Object> getter, BlockBuilder output)
    {
        Object value = getter.get();
        if (value == null) {
            output.appendNull();
        }
        else if (value instanceof Number) {
            long decoded = ((Number) value).longValue();

            if (decoded < Byte.MIN_VALUE || decoded > Byte.MAX_VALUE) {
                throw new PrestoException(ELASTICSEARCH_TYPE_MISMATCH, format("Value out of range for field '%s' of type TINYINT: %s", path, decoded));
            }

            TINYINT.writeLong(output, decoded);
        }
        else {
            throw new PrestoException(ELASTICSEARCH_TYPE_MISMATCH, format("Expected a numeric value for field '%s' of type TINYINT: %s [%s]", path, value, value.getClass().getSimpleName()));
        }
    }
}

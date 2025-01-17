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
package com.facebook.presto.redis.decoder.zset;

import com.facebook.presto.decoder.DecoderColumnHandle;
import com.facebook.presto.decoder.RowDecoder;
import com.facebook.presto.decoder.RowDecoderFactory;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ZsetRedisRowDecoderFactory
        implements RowDecoderFactory
{
    private static final RowDecoder DECODER_INSTANCE = new ZsetRedisRowDecoder();

    @Override
    public RowDecoder create(Map<String, String> decoderParams, Set<DecoderColumnHandle> columns)
    {
        requireNonNull(columns, "columnHandles is null");
        checkArgument(columns.stream().noneMatch(DecoderColumnHandle::isInternal), "unexpected internal column");
        return DECODER_INSTANCE;
    }
}

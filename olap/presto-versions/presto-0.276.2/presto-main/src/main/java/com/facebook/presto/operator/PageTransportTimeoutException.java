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
package com.facebook.presto.operator;

import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.PrestoTransportException;

import static com.facebook.presto.spi.StandardErrorCode.PAGE_TRANSPORT_TIMEOUT;

public class PageTransportTimeoutException
        extends PrestoTransportException
{
    public PageTransportTimeoutException(HostAddress remoteHost, String message, Throwable cause)
    {
        super(PAGE_TRANSPORT_TIMEOUT, remoteHost, message, cause);
    }
}

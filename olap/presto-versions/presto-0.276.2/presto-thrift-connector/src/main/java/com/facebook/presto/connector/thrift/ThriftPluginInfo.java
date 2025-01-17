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
package com.facebook.presto.connector.thrift;

import com.facebook.presto.thrift.api.connector.PrestoThriftService;
import com.google.inject.Module;

import static com.facebook.drift.client.guice.DriftClientBinder.driftClientBinder;
import static com.facebook.presto.connector.thrift.location.ExtendedSimpleAddressSelectorBinder.extendedSimpleAddressSelector;
import static com.google.inject.Scopes.SINGLETON;

public class ThriftPluginInfo
{
    public String getName()
    {
        return "presto-thrift";
    }

    public Module getModule()
    {
        return binder -> {
            binder.bind(ThriftHeaderProvider.class).to(DefaultThriftHeaderProvider.class).in(SINGLETON);
            driftClientBinder(binder)
                    .bindDriftClient(PrestoThriftService.class)
                    .withAddressSelector(extendedSimpleAddressSelector());
        };
    }
}

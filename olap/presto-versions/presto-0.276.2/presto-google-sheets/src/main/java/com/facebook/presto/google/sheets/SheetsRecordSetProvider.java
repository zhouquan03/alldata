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
package com.facebook.presto.google.sheets;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.RecordSet;
import com.facebook.presto.spi.connector.ConnectorRecordSetProvider;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class SheetsRecordSetProvider
        implements ConnectorRecordSetProvider
{
    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transactionHandle,
                                  ConnectorSession session,
                                  ConnectorSplit split,
                                  List<? extends ColumnHandle> columns)
    {
        requireNonNull(split, "split is null");
        SheetsSplit sheetsSplit = (SheetsSplit) split;

        List<SheetsColumnHandle> handles = columns.stream().map(c -> (SheetsColumnHandle) c).collect(Collectors.toList());
        return new SheetsRecordSet(sheetsSplit, handles);
    }
}

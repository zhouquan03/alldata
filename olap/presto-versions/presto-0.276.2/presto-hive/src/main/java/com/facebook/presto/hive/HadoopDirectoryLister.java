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
package com.facebook.presto.hive;

import com.facebook.presto.hive.filesystem.ExtendedFileSystem;
import com.facebook.presto.hive.metastore.Partition;
import com.facebook.presto.hive.metastore.Table;
import com.facebook.presto.hive.util.HiveFileIterator;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import static com.facebook.presto.hive.HiveFileInfo.createHiveFileInfo;
import static java.util.Objects.requireNonNull;

public class HadoopDirectoryLister
        implements DirectoryLister
{
    @Override
    public Iterator<HiveFileInfo> list(
            ExtendedFileSystem fileSystem,
            Table table,
            Path path,
            Optional<Partition> partition,
            NamenodeStats namenodeStats,
            HiveDirectoryContext hiveDirectoryContext)
    {
        return new HiveFileIterator(
                path,
                p -> new HadoopFileInfoIterator(fileSystem.listLocatedStatus(p)),
                namenodeStats,
                hiveDirectoryContext.getNestedDirectoryPolicy());
    }

    public static class HadoopFileInfoIterator
            implements RemoteIterator<HiveFileInfo>
    {
        private final RemoteIterator<LocatedFileStatus> locatedFileStatusIterator;

        public HadoopFileInfoIterator(RemoteIterator<LocatedFileStatus> locatedFileStatusIterator)
        {
            this.locatedFileStatusIterator = requireNonNull(locatedFileStatusIterator, "locatedFileStatusIterator is null");
        }

        @Override
        public boolean hasNext()
                throws IOException
        {
            return locatedFileStatusIterator.hasNext();
        }

        @Override
        public HiveFileInfo next()
                throws IOException
        {
            return createHiveFileInfo(locatedFileStatusIterator.next(), Optional.empty());
        }
    }
}

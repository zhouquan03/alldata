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
package com.facebook.presto.common.predicate;

import java.util.List;

public interface Ranges
{
    int getRangeCount();

    /**
     * @return Allowed non-overlapping predicate ranges sorted in increasing order
     */
    List<Range> getOrderedRanges();

    /**
     * @return Single range encompassing all of allowed the ranges
     */
    Range getSpan();
}

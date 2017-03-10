/*
 * Copyright (c) 2016 Intel Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.icecp.bundle;

/**
 * See {@link StoredBundle} for general description.
 *
 * @param <T> the type of message stored in the storage module
 */
public interface StoredBatch<T> extends Bundle<StoredItem<T>> {

    /**
     * Returns unique identifier for the batch
     *
     * @return the unique identifier
     */
    long id();

    /**
     * Commit any pending finished markings to the storage module (e.g. may delete the finished messages in the
     * storage module)
     */
    void commit();
}
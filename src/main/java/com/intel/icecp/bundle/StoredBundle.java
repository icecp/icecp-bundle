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
 * A specialization of {@link Bundle} to model a three-tiered storage system: bundles contain batches, batches contain
 * items. Implementations of these APIs should use the {@link #markFinished()} functionality for informing the
 * underlying storage that the stored items have been used and are no longer necessary (e.g. could be removed, archived,
 * etc.).
 *
 * @param <T> the type of message stored in the storage module
 */
public interface StoredBundle<T> extends Bundle<StoredBatch<T>> {

    /**
     * @return eventually true if a new batch can be generated
     */
    StoredBatch<T> newBatch();

    /**
     * Add an item to the last available batch
     *
     * @param item the item to add
     */
    void addItem(T item);
}

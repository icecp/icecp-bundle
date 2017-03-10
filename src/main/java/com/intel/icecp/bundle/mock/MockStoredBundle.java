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

package com.intel.icecp.bundle.mock;

import com.intel.icecp.bundle.StoredBatch;
import com.intel.icecp.bundle.StoredBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class MockStoredBundle<T> implements StoredBundle<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final List<StoredBatch<T>> lists = new ArrayList<>();

    @Override
    public StoredBatch<T> newBatch() {
        StoredBatch<T> b = new MockStoredBatch<>();
        lists.add(b);
        return b;
    }

    @Override
    public boolean hasChildren() {
        LOGGER.info("Checking if {} has children", this);
        return !lists.isEmpty();
    }

    @Override
    public Iterable<StoredBatch<T>> children() {
        LOGGER.info("Checking if {} has children", this);
        return lists;
    }

    private StoredBatch<T> last() {
        if (lists.isEmpty()) throw new IllegalStateException("No batches available; create one before retrieving one");
        return lists.get(lists.size() - 1);
    }

    @Override
    public void addItem(T item) {
        ((MockStoredBatch<T>) last()).add(item);
    }
}

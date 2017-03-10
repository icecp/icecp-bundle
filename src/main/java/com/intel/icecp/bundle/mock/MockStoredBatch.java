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
import com.intel.icecp.bundle.StoredItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class MockStoredBatch<T> implements StoredBatch<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<StoredItem<T>> items = new ArrayList<>();
    private final long id;

    public MockStoredBatch() {
        id = new SecureRandom().nextLong();
    }

    public void add(T newItem) {
        items.add(new MockStoredItem<>(newItem));
    }

    @Override
    public boolean hasChildren() {
        LOGGER.info("Checking if {} has children", this);
        return !items.isEmpty();
    }

    @Override
    public Iterable<StoredItem<T>> children() {
        LOGGER.info("Iterating on children of {}", this);
        return items;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void commit() {
        // TODO Auto-generated method stub
    }

}
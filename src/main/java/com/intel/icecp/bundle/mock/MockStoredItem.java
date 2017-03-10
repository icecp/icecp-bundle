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

import com.intel.icecp.bundle.StoredItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 */
public class MockStoredItem<T> implements StoredItem<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final long timestamp;
    private final T value;
    private boolean finished = false;

    private MockStoredItem(long timestamp, T value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    MockStoredItem(T value) {
        this(System.currentTimeMillis(), value);
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public boolean isFinished() {
        LOGGER.info("Checking if {} is finished", this);
        return finished;
    }

    @Override
    public void markFinished() {
        LOGGER.info("Marking {} as finished", this);
        finished = true;
    }
}

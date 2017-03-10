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

import com.intel.icecp.bundle.Bundle;
import com.intel.icecp.bundle.StoredBatch;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MockStoredBundleTest {
    private MockStoredBundle<String> instance;

    @Before
    public void before() {
        instance = new MockStoredBundle<>();
    }

    @Test
    public void newBatch() throws Exception {
        StoredBatch<String> newStoredBatch = instance.newBatch();
        assertNotNull(newStoredBatch);
    }

    @Test
    public void iteration() throws Exception {
        Iterable<StoredBatch<String>> iterable = instance.children();
        assertNotNull(iterable);
        assertEquals(0, stream(iterable).count());

        instance.newBatch();
        instance.newBatch();

        assertTrue(instance.hasChildren());
        assertEquals(2, stream(instance.children()).count());
    }

    @Test
    public void marking() throws Exception {
        assertTrue(instance.isFinished());

        StoredBatch<String> newBatch = instance.newBatch();
        instance.addItem("...");
        assertTrue(instance.hasChildren());
        assertFalse(instance.isFinished());

        newBatch.markFinished();
        assertTrue(instance.isFinished());
    }

    private <T extends Bundle> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
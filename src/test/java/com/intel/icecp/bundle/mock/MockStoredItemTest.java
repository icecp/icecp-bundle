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

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class MockStoredItemTest {

    private MockStoredItem<BigInteger> instance;

    @Before
    public void before() {
        instance = new MockStoredItem<>(BigInteger.TEN);
    }

    @Test
    public void basicUsage() throws Exception {
        assertTrue(instance.timestamp() > 0);
        assertEquals(BigInteger.TEN, instance.value());
    }

    @Test
    public void finishedState() throws Exception {
        assertFalse(instance.isFinished());
        instance.markFinished();
        assertTrue(instance.isFinished());
    }
}
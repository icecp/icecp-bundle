/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
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

package com.intel.icecp.bundle.messages;

import com.intel.icecp.bundle.StoredBatch;
import com.intel.icecp.core.messages.BytesMessage;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class MessageBundleTest {

    private static final long INITIAL_SESSION_ID = 42L;
    private MessageBundle instance;
    private MessageBundle instanceWithSession;
    private StorageClient storage;
    @Before
    public void before() throws Exception {
        storage = mock(StorageClient.class);
        when(storage.retrieveSessions(anyLong())).thenReturn(Arrays.asList(Arrays.asList(40L), Arrays.asList(41L, 42L)));
        instance = new MessageBundle(storage, URI.create("icecp:/stored/messages"));
        instanceWithSession = new MessageBundle(storage, INITIAL_SESSION_ID);
    }

    @Test
    public void hasChildrenForInitialBundle() throws Exception {
        instance = new MessageBundle(mock(StorageClient.class), URI.create("icecp:/stored/messages"));
        assertFalse(instance.hasChildren());
    }

    @Test
    public void hasChildren() throws Exception {
        assertTrue(instanceWithSession.hasChildren());
    }

    @Test
    public void children() throws Exception {
        assertTrue(instanceWithSession.children().iterator().hasNext());
        StoredBatch<BytesMessage> firstBatch = instanceWithSession.children().iterator().next();
        assertTrue(firstBatch.isFinished()); // because it is empty
        assertEquals(42L, firstBatch.id());  // empty ones are cleansed during sync() except the current session
    }

    @Test
    public void newBatchFromEmptyBundle() throws Exception {
        long newId = 101L;
        when(storage.startSession(any())).thenReturn(newId);

        StoredBatch<BytesMessage> batch = instance.newBatch();
        assertEquals(newId, batch.id());
    }

    @Test
    public void newBatchFromStartedBundle() throws Exception {
        long newId = 101L;
        when(storage.renameSession(INITIAL_SESSION_ID)).thenReturn(newId);

        StoredBatch<BytesMessage> batch = instanceWithSession.newBatch();
        assertEquals(newId, batch.id());
    }
}
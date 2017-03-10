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
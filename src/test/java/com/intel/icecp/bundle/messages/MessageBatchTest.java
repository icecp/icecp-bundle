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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class MessageBatchTest {

    private static final int SESSION_ID = 42;
    private MessageBatch instance;
    private StorageClient storage;
    @Captor
    private ArgumentCaptor<Collection<Long>> idsCaptor;
    @Captor
    private ArgumentCaptor<Collection<String>> tagsCaptor;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        storage = mock(StorageClient.class);
        when(storage.retrieveSessions(any())).thenReturn(Collections.EMPTY_LIST);
        instance = new MessageBatch(storage, SESSION_ID);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void hasChildren() throws Exception {
        assertFalse(instance.hasChildren());
    }

    @Test
    public void children() throws Exception {
        assertFalse(instance.children().iterator().hasNext());
        verify(storage).retrieveMessages(SESSION_ID, 30);
    }

    @Test
    public void isFinishedWhenEmpty() throws Exception {
        assertTrue(instance.isFinished());
    }

    @Test
    public void isFinishedWithChildren() throws Exception {
        List<PersistentMessage> messages = Arrays.asList(new PersistentMessage(), new PersistentMessage());
        when(storage.retrieveMessages(any(Long.class), any(Integer.class))).thenReturn(messages);

        assertFalse(instance.isFinished());
    }

    @Test
    public void sessionId() throws Exception {
        assertEquals(SESSION_ID, instance.id());
    }

    @Test
    public void markFinishedWithoutChildren() throws Exception {
        instance.markFinished();

        verify(storage, times(0)).tag(any(), any());
        assertTrue(instance.isFinished());
    }

    @Test
    public void markFinished() throws Exception {
        PersistentMessage message1 = new PersistentMessage(1, 1, "".getBytes());
        PersistentMessage message2 = new PersistentMessage(2, 2, "".getBytes());
        List<PersistentMessage> messages = Arrays.asList(message1, message2);
        when(storage.retrieveMessages(any(Long.class), any(Integer.class))).thenReturn(messages);

        instance.markFinished();

        verify(storage, times(1)).tag(idsCaptor.capture(), tagsCaptor.capture());
        assertArrayEquals(new Object[]{1L, 2L}, idsCaptor.getValue().toArray());
        assertArrayEquals(new Object[]{"inactive"}, tagsCaptor.getValue().toArray());
        assertTrue(instance.isFinished());
    }

    @Test
    public void commitFinished() throws Exception {
        boolean expected = instance.isFinished();

        instance.commit();

        verify(storage, times(0)).tag(any(), any());
        assertEquals(expected, instance.isFinished());
    }

    @Test
    public void commitUnfinished() throws Exception {
        PersistentMessage message1 = new PersistentMessage(1, 1, "".getBytes());
        PersistentMessage message2 = new PersistentMessage(2, 2, "".getBytes());
        List<PersistentMessage> messages = Arrays.asList(message1, message2);
        when(storage.retrieveMessages(any(Long.class), any(Integer.class))).thenReturn(messages);

        assertTrue(instance.hasChildren());
        assertFalse(instance.isFinished());

        instance.children().iterator().next().markFinished(); // mark first as finished
        instance.commit();

        assertFalse(instance.isFinished());
        verify(storage, times(1)).tag(idsCaptor.capture(), tagsCaptor.capture());
        assertArrayEquals(new Object[]{1L}, idsCaptor.getValue().toArray());
    }
}
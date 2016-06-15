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
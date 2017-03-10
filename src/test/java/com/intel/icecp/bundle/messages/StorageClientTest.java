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

import com.intel.icecp.bundle.messages.StorageClient.Commands;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.mock.MockChannels;
import com.intel.icecp.rpc.CommandResponse;
import com.intel.icecp.rpc.RpcClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 */
public class StorageClientTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    RpcClient client;
    private StorageClient instance;

    @Before
    public void before() {
        instance = Mockito.spy(new StorageClient(new MockChannels(), 100));
    }

    @Test
    public void executeCommand() throws Exception {
        List<Long> expected = Arrays.asList(1L, 2L, 3L);
        when(client.call(any())).thenReturn(CompletableFuture.completedFuture(CommandResponse.fromValid(expected)));

        List<Long> sessionIds = instance.executeRemoteCommand(client, StorageClient.Commands.queryBySessionId, new Token<List<Long>>() {
        }, 99);

        assertNotNull(sessionIds);
        assertArrayEquals(expected.toArray(), sessionIds.toArray());
    }

    @Test
    public void emptyList() throws Exception {
        when(client.call(any())).thenReturn(CompletableFuture.completedFuture(CommandResponse.fromValid(Collections.EMPTY_LIST)));

        List<Long> sessionIds = instance.executeRemoteCommand(client, StorageClient.Commands.queryBySessionId, new Token<List<Long>>() {
        }, 99);

        assertEquals(0, sessionIds.size());
    }

    @Test(expected = StorageClientException.class)
    public void incorrectTypes() throws Exception {
        List<Long> expected = Arrays.asList(1L, 2L, 3L);
        when(client.call(any())).thenReturn(CompletableFuture.completedFuture(CommandResponse.fromValid(expected)));

        instance.executeRemoteCommand(client, StorageClient.Commands.queryBySessionId, new Token<String>() {
        }, 99);
    }

    @Test(expected = StorageClientException.class)
    public void failedRpc() throws Exception {
        when(client.call(any())).thenReturn(new CompletableFuture<>());

        instance.executeRemoteCommand(client, StorageClient.Commands.queryBySessionId, Token.of(String.class), 99);
    }
    
    @Test
    public void startSession() throws Exception {
        long expected = 1L;
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.LISTEN_CHANNEL_KEY_NAME, "icecp:/testChannel");

        doReturn(expected).when(instance).executeRemoteCommand(any(), any(), any(), any());

        assertEquals(expected, instance.startSession(new URI("icecp:/testURI")));
    }
    
    @Test
    public void renameSession() throws Exception {
        Long expected = 456L;
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, 456L);
    
        doReturn(expected).when(instance).executeRemoteCommand(client, Commands.rename, new Token<Long>() {}, map.getStorageInputMap());

        exception.expect(StorageClientException.class);
        assertEquals(instance.renameSession(123L), expected);
    }
    
    @Test
    public void retrieveSessionsChannel() throws Exception {
        List<Long> expected = Arrays.asList(7L, 8L, 9L);
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.QUERY_CHANNEL_KEY_NAME, "icecp:/123");
     
        doReturn(expected).when(instance).executeRemoteCommand(client, Commands.queryByChannelName, new Token<List<Long>>() {}, map.getStorageInputMap());

        exception.expect(StorageClientException.class);
        assertEquals(instance.retrieveSessions(new URI("testUri")), expected);
    }

    @Test
    public void retrieveSessionsUsingSessionId() throws Exception {
        List<Long> expected = Arrays.asList(10L, 11L, 12L);
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, 100L);
     
        doReturn(expected).when(instance).executeRemoteCommand(client, Commands.queryBySessionId, new Token<List<Long>>() {}, map.getStorageInputMap());

        exception.expect(StorageClientException.class);
        assertEquals(instance.retrieveSessions(123L), expected);
    }
    
    @Test
    public void tag() throws Exception {
        List<String> t = Collections.singletonList("inactive");
        List<Long> i = Arrays.asList(1L, 2L, 3L);

        doReturn(3).when(instance).executeRemoteCommand(any(), any(), any(), any());

        assertEquals(3, instance.tag(i, t));
    }

    @Test
    public void sessionSize() throws Exception {
        Integer expected = 3;
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, 123L);

        doReturn(expected).when(instance).executeRemoteCommand(client, Commands.size, new Token<Integer>() {}, map.getStorageInputMap());

        exception.expect(StorageClientException.class);
        assertEquals(instance.sessionSize(123L), expected);
    }

    @Test
    public void retrieveMessages() throws Exception {
        List<BytesMessage> expected = Collections.singletonList(new BytesMessage("test".getBytes()));
        Integer expectedSize = 0;
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, 123L);
        map.addEntry(StorageCommandMap.LIMIT_KEY_NAME, 5);
        map.addEntry(StorageCommandMap.SKIP_KEY_NAME, 0);
        map.addEntry(StorageCommandMap.REPLAY_CHANNEL_KEY_NAME, "icecp:/replayChannel");
    
        doReturn(expectedSize).when(instance).sessionSize(123L);
        doReturn(expected).when(instance).executeRemoteCommand(client, Commands.get, new Token<LinkedHashMap>() {}, map.getStorageInputMap());

        exception.expect(StorageClientException.class);
        assertEquals(instance.retrieveMessages(123L, 50000), expected);
    }
}
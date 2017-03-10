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

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.rpc.CommandRequest;
import com.intel.icecp.rpc.CommandResponse;
import com.intel.icecp.rpc.Rpc;
import com.intel.icecp.rpc.RpcClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represent a storage module that is remotely available over channels; this class should model the commands exposed in
 * <a href= "https://github.com/icecp/icecp-module-storage/wiki/Command-5%26-response-messages"> icecp-module-storage
 * response messages</a>
 *
 */
public class StorageClient {
    private static final URI BASE_RETRIEVAL_URI = URI.create("ndn:/intel/bundle/retrieve");
    private static final String COMMAND_CHANNEL_NAME = "ndn:/intel/storage/command";
    private static final Logger LOGGER = LogManager.getLogger();
    private final RpcClient client;
    private final int timeoutMs;
    private Channels channels;

    /**
     * Constructor
     *
     * @param channels the method name
     * @param timeoutMs timeout for calls to remote module
     */
    public StorageClient(Channels channels, int timeoutMs) {
        // TODO: STORAGE_COMMAND_CHANNEL: Need to get the URI storageModuleUri from the storage module somehow.
        // For now we will use a global command channel.
        this(Rpc.newClient(channels, URI.create(COMMAND_CHANNEL_NAME)), timeoutMs);
        this.channels = channels;
    }

    /**
     * Constructor
     *
     * @param client RPC client used to send commands to
     * @param timeoutMs timeout for calls to remote module
     */
    public StorageClient(RpcClient client, int timeoutMs) {
        this.client = client;
        this.timeoutMs = timeoutMs;
    }

    @SuppressWarnings("unchecked")
    <T> T executeRemoteCommand(RpcClient client, Commands commandName, Token<T> expectedOutput, Object... inputs)
            throws StorageClientException {
        LOGGER.info("Executing command {}, expecting response of {}", commandName, expectedOutput);
        CompletableFuture<CommandResponse> future;

        // TODO: remove this once RPC client is fixed
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageClientException("Could not complete command", e);
        }

        future = client.call(CommandRequest.from(commandName.name(), inputs));
        try {
            CommandResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (expectedOutput != null && !(expectedOutput.isAssignableFrom(response.out.getClass()))) {
                throw new StorageClientException("Response was not of the expected type: " + expectedOutput);
            }
            return (T) response.out;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new StorageClientException(e);
        }
    }

    /**
     * Start a session
     *
     * @param channel Channel to start the session on
     * @return session id returned by the storage module
     * @throws StorageClientException unable to communicate with the storage mechanism
     */
    long startSession(URI channel) throws StorageClientException {
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.LISTEN_CHANNEL_KEY_NAME, channel.toString());

        return executeRemoteCommand(client, Commands.start, new Token<Long>() {
        }, map.getStorageInputMap());
    }

    /**
     * Rename a session
     *
     * @param sessionId session identifier to rename
     * @return new session id returned from the storage module
     * @throws StorageClientException unable to communicate with the storage mechanism
     */
    Long renameSession(Long sessionId) throws StorageClientException {
        StorageCommandMap renameInputMap = new StorageCommandMap();
        renameInputMap.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, sessionId);

        return executeRemoteCommand(client, Commands.rename, new Token<Long>() {
        }, renameInputMap.getStorageInputMap());
    }

    /**
     * retrieve the linked session list for the channel
     *
     * @param channel Channel to retrieve sessions for
     * @return List of session IDs related to the channel
     * @throws StorageClientException unable to communicate with the storage mechanism
     */
    List<Long> retrieveSessions(URI channel) throws StorageClientException {
        // TODO: We can use this until EAPE-1364 is complete, but right now it
        // would return ALL sessions connected to a
        // channel...even though you may have independent modules connected to
        // the same channel (in which case you
        // would only want to use the session ids associated with the your
        // module.
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.QUERY_CHANNEL_KEY_NAME, channel.toString());

        return executeRemoteCommand(client, Commands.queryByChannelName, new Token<List<Long>>() {
        }, map.getStorageInputMap());
    }

    /**
     * Retrieve a set of connected sessions
     *
     * @param sessionId session ID to retrieve sessions for
     * @return list of a list of session IDs related to the sessionId
     * @throws StorageClientException if unable to communicate with the storage mechanism
     */
    @SuppressWarnings("unchecked")
    List<List<Long>> retrieveSessions(long sessionId) throws StorageClientException {
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, sessionId);
        map.addEntry(StorageCommandMap.ONLY_WITH_ACTIVE_MESSAGE_KEY_NAME, true);
        return executeRemoteCommand(client, Commands.queryBySessionId, new Token<List<List<Long>>>() {
        }, map.getStorageInputMap());
    }

    /**
     * Tag a message with the passed tags
     *
     * @param tags the tags to tag with
     * @throws StorageClientException if the request to the storage module fails
     */
    int tag(Collection<Long> messageIds, Collection<String> tags) throws StorageClientException {
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.IDS_KEY_NAME, messageIds);
        map.addEntry(StorageCommandMap.TAGS_KEY_NAME, tags);
        return executeRemoteCommand(client, Commands.tag, new Token<Integer>() {
        }, map.getStorageInputMap());
    }

    /**
     * Gets the current size of a session
     *
     * @param sessionId session identifier
     * @return number of messages currently in the session
     * @throws StorageClientException Unable to communicate with the storage mechanism
     */
    Integer sessionSize(long sessionId) throws StorageClientException {
        StorageCommandMap map = new StorageCommandMap();
        map.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, sessionId);

        return executeRemoteCommand(client, Commands.size, new Token<Integer>() {
        }, map.getStorageInputMap());
    }

    /**
     * Retrieve messages for a session ID
     *
     * @param sessionId session identifier to retrieve messages for.
     * @param maxWaitSeconds max wait time until
     * @return list of messages related to the session ID.
     * @throws StorageClientException Unable to communicate with the storage mechanism
     * @throws InterruptedException Did not receive all messages in the max wait time
     */
    List<PersistentMessage> retrieveMessages(long sessionId, int maxWaitSeconds)
            throws StorageClientException, InterruptedException {
        Integer sessionSize = sessionSize(sessionId);
        MessageCollector collector = new MessageCollector(sessionSize);
        long timeoutTime = 5000L * sessionSize;

        // Create the channel that will be used to receive session messages from
        // the storage module. Keep the
        // Channel unique for each request.
        URI channelURI = ChannelUtils.join(BASE_RETRIEVAL_URI, Long.toString(sessionId), UUID.randomUUID().toString());

        try (Channel<PersistentMessage> replayChannel = channels.openChannel(channelURI, PersistentMessage.class,
                new Persistence(timeoutTime))) {
            replayChannel.subscribe(collector);

            StorageCommandMap getInputMap = new StorageCommandMap();
            getInputMap.addEntry(StorageCommandMap.SESSION_ID_KEY_NAME, sessionId);
            getInputMap.addEntry(StorageCommandMap.LIMIT_KEY_NAME, sessionSize);
            getInputMap.addEntry(StorageCommandMap.SKIP_KEY_NAME, 0);
            getInputMap.addEntry(StorageCommandMap.REPLAY_CHANNEL_KEY_NAME, channelURI.toString());

            executeRemoteCommand(client, Commands.get, null, getInputMap.getStorageInputMap());

            if (collector.getCountdown().await(maxWaitSeconds, TimeUnit.SECONDS)) {
                List<PersistentMessage> getList = collector.getMessageList();
                getList.sort((PersistentMessage m1, PersistentMessage m2) -> Long.compare(m1.getId(), m2.getId()));
                return getList;
            } else {
                LOGGER.error("unable to retrieve messages");
                throw new StorageClientException("unable to retrieve messages");
            }
        } catch (ChannelLifetimeException e) {
            LOGGER.error("unable to create replay channel", e);
            throw new StorageClientException("Unable to create replay channel URI");
        } catch (ChannelIOException e) {
            LOGGER.error("unable to subscribe to channel", e);
            throw new StorageClientException("Unable to subscribe to replay channel");
        }
    }

    enum Commands {
        queryByChannelName, queryBySessionId, rename, size, get, start, tag
    }

    private class MessageCollector implements OnPublish<PersistentMessage> {
        private final CountDownLatch countdown;
        private final ArrayList<PersistentMessage> messageList;

        MessageCollector(int expectedCount) {
            this.countdown = new CountDownLatch(expectedCount);
            this.messageList = new ArrayList<>();
        }

        CountDownLatch getCountdown() {
            return countdown;
        }

        @Override
        public void onPublish(PersistentMessage message) {
            onPublish(message, null);
        }

        @Override
        public void onPublish(PersistentMessage message, Attributes attributes) {
            messageList.add(message);
            countdown.countDown();
        }

        List<PersistentMessage> getMessageList() {
            return messageList;
        }
    }
}

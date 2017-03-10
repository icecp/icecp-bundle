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
import com.intel.icecp.bundle.StoredBundle;
import com.intel.icecp.core.messages.BytesMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use the storage module to store bundles of batches of messages. Batches correspond to the storage module's sessions
 * and items correspond to the messages in those sessions. This class and its children ({@link MessageBatch} and {@link
 * MessageItem}) will use the given client to call methods on the storage module like START (to create a batch), SIZE
 * (to discover the batch size), QUERY (to retrieve the items), DELETE (to remove items), etc.
 *
 */
public class MessageBundle implements StoredBundle<BytesMessage> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final StorageClient storage;
    private final URI listenChannel;
    private final List<StoredBatch<BytesMessage>> batches = new ArrayList<>();
    private boolean synced = false;
    private StoredBatch<BytesMessage> currentBatch;

    /**
     * Constructor
     *
     * @param storage Supplies storage features
     * @param listenChannel the channel stored by the storage module
     */
    public MessageBundle(StorageClient storage, URI listenChannel) {
        this.storage = storage;
        this.listenChannel = listenChannel;
    }

    /**
     * Constructor
     *
     * @param storage Supplies storage features
     * @param currentSessionId Session ID to start with
     */
    public MessageBundle(StorageClient storage, long currentSessionId) {
        URI uri;
        this.storage = storage;
        setCurrentBatch(storage, currentSessionId);
        try {
            uri = new URI("");
        } catch (URISyntaxException e) {
            //This should never really happen
            LOGGER.error("Unable to create empty URI", e);
            uri = null;
        }
        listenChannel = uri;
    }
    
    /**
     * synchronized current batch set method
     *
     * @param storage Supplies storage features
     * @param currentSessionId Session ID to start with
     */
    private synchronized void setCurrentBatch(StorageClient storage, long currentSessionId) {
        currentBatch = new MessageBatch(storage, currentSessionId);
    }

    @Override
    public StoredBatch<BytesMessage> newBatch() {
        Long id = getNewBatchId();
        if (id != null) {
            setCurrentBatch(storage, id);
            setSyncedFalse();
            return currentBatch;
        } else {
            return null;
        }
    }
    
    /**
     * synchronized synced to false method
     *
     */
    private synchronized void setSyncedFalse() {
        synced = false;
    }
    
    private void cleanupFinishedBatches(){
        List<StoredBatch<BytesMessage>> finishedBatches = batches.stream()
                .filter(b -> b.id() != currentBatch.id() && b.isFinished()).collect(Collectors.toList());

        batches.removeAll(finishedBatches);
    }

    private Long getNewBatchId() {
        try {
            return currentBatch == null
                    ? storage.startSession(listenChannel)
                    : storage.renameSession(currentBatch.id());
        } catch (StorageClientException e) {
            LOGGER.error("Unable to create new session", e);
            return null;
        }
    }

    // Added to avoid compiler issues
    @Override
    public void addItem(BytesMessage item) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean hasChildren() {
        sync();
        return !batches.isEmpty();
    }

    @Override
    public Iterable<StoredBatch<BytesMessage>> children() {
        sync();
        return batches;
    }

    private synchronized void sync() {
        if (!synced || batches.isEmpty()) {
            try {
                if (currentBatch != null) {
                    // note that we receive back a list of lists and need to flatten before we create the batches
                    List<List<Long>> ids = storage.retrieveSessions(currentBatch.id());
                    List<Long> flattened = ids.stream().flatMap(Collection::stream).collect(Collectors.toList());
                    // Get the list of session ids for the current batches.
                    List<Long> sessionIdsInBatches = batches.stream().map(b->b.id()).collect(Collectors.toList());
                    // Remove the old sessions from the list
                    flattened.removeAll(sessionIdsInBatches);
                    // Create a batch for the brand new session
                    flattened.forEach(this::createBatch);
                }
                synced = true;
            } catch (StorageClientException e) {
                throw new IllegalStateException("Cannot proceed without retrieved batch SIDs", e);
            }
        }

        cleanupFinishedBatches();
    }

    private void createBatch(Long sid) {
        batches.add(new MessageBatch(storage, sid));
    }
}

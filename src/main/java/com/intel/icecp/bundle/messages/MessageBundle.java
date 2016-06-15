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

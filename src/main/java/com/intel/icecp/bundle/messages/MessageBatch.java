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

import com.intel.icecp.bundle.Bundle;
import com.intel.icecp.bundle.StoredBatch;
import com.intel.icecp.bundle.StoredItem;
import com.intel.icecp.core.messages.BytesMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Used to hold a batch of Message objects. A batch is defined as a collection of items.
 *
 */
class MessageBatch implements StoredBatch<BytesMessage> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_WAIT_SECONDS = 30;
    private final Long sessionId;
    private final StorageClient storage;
    private final List<StoredItem<BytesMessage>> messages = new ArrayList<>();
    private boolean synced = false;

    /**
     * Constructor
     *
     * @param storage Supplies storage features
     * @param sessionId Unique identifier associated with a batch
     */
    MessageBatch(StorageClient storage, long sessionId) {
        this.storage = storage;
        this.sessionId = sessionId;
    }

    @Override
    public synchronized void markFinished() {
        if (!isFinished()) {
            if (hasChildren()) {
                streamChildren().forEach(Bundle::markFinished);
            }
            commit();
        }
    }

    @Override
    public void commit() {
        List<MessageItem> messagesToCommit = streamChildren().filter(i -> i.isFinished() && !i.isCommitted())
                .collect(Collectors.toList());
        List<Long> messageIds = messagesToCommit.stream()
                .map(i -> i.getMessage().getId()).collect(Collectors.toList());

        if (!messagesToCommit.isEmpty()) {
            try {
                storage.tag(messageIds, Collections.singleton("inactive"));
                messagesToCommit.forEach(MessageItem::setCommitted);
            } catch (StorageClientException e) {
                LOGGER.error("Failed to tag the following items as inactive: {}", messageIds, e);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return !hasChildren() || (streamChildren().allMatch(Bundle::isFinished) && streamChildren().allMatch(MessageItem::isCommitted));
    }

    @Override
    public boolean hasChildren() {
        sync();
        return !messages.isEmpty();
    }

    @Override
    public Iterable<StoredItem<BytesMessage>> children() {
        sync();
        return messages;
    }

    private Stream<MessageItem> streamChildren() {
        return StreamSupport.stream(children().spliterator(), false).map(c -> (MessageItem) c);
    }

    private synchronized void sync() {
        if (synced)
            return;

        try {
            messages.clear();
            storage.retrieveMessages(sessionId, MAX_WAIT_SECONDS).forEach(m -> messages.add(new MessageItem(m)));
            synced = true;
        } catch (StorageClientException | InterruptedException e) {
            LOGGER.error("Cannot proceed without retrieved batch SIDs", e);
        }
    }

    @Override
    public long id() {
        return sessionId;
    }
}

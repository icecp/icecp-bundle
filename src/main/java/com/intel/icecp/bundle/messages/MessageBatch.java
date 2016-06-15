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

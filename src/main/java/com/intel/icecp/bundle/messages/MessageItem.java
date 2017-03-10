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

import com.intel.icecp.bundle.StoredItem;
import com.intel.icecp.core.messages.BytesMessage;

/**
 * Represent the stored messages inside a batched session.
 *
 */
class MessageItem implements StoredItem<BytesMessage> {
    private PersistentMessage message;
    private boolean itemFinished = false;
    private boolean isCommitted = false;

    MessageItem(PersistentMessage m) {
        this.message = m;
    }

    PersistentMessage getMessage() {
        return message;
    }

    @Override
    public long timestamp() {
        return message.getTimestamp();
    }

    @Override
    public BytesMessage value() {
        return new BytesMessage(message.getMessageContent());
    }

    @Override
    public void markFinished() {
        itemFinished = true;
    }

    @Override
    public boolean isFinished() {
        return itemFinished;
    }

    boolean isCommitted() {
        return isCommitted;
    }

    void setCommitted() {
        isCommitted = true;
    }
}

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

package com.intel.icecp.bundle;

import com.intel.icecp.bundle.messages.MessageBundle;
import com.intel.icecp.bundle.messages.StorageClient;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.messages.BytesMessage;

import java.net.URI;

/**
 * Factory class for building bundle implementations.
 *
 */
public class Bundles {

    private static final int REMOTE_CALL_TIMEOUT_MS = 10000;

    /**
     * Disable instantiation
     */
    private Bundles() {
    }

    /**
     * Build a stored bundle of messages
     *
     * @param channels the channels to use for connecting to the storage module
     * @param listenChannel the channel stored by the storage module
     * @return a bundle of stored messages
     */
    public static StoredBundle<BytesMessage> newStoredBundle(Channels channels, URI listenChannel) {
        StorageClient storage = new StorageClient(channels, REMOTE_CALL_TIMEOUT_MS);
        return new MessageBundle(storage, listenChannel);
    }
}

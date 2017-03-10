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

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for holding storage command parameters and specifying the key constants.
 */
class StorageCommandMap {
    private final Map<String, Object> storageInputMap = new HashMap<>();
    static final String REPLAY_CHANNEL_KEY_NAME = "replayChannel";
    static final String SESSION_ID_KEY_NAME = "sessionId";
    static final String ONLY_WITH_ACTIVE_MESSAGE_KEY_NAME = "onlyWithActiveMessages";
    static final String LISTEN_CHANNEL_KEY_NAME = "listenChannel";
    static final String LIMIT_KEY_NAME = "limit";
    static final String SKIP_KEY_NAME = "skip";
    static final String QUERY_CHANNEL_KEY_NAME = "queryChannel";
    static final String IDS_KEY_NAME = "ids";
    static final String TAGS_KEY_NAME = "tags";

    /**
     * Add a new parameter entry to the map
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    void addEntry(String key, Object value) {
        this.storageInputMap.put(key, value);
    }

    /**
     * Gets the storage input map
     *
     * @return returns the storage input map
     */
    Map<String, Object> getStorageInputMap() {
        return this.storageInputMap;
    }
}

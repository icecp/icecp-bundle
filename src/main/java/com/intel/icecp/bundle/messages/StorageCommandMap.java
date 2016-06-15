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

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

package com.intel.icecp.bundle.mock;

import com.intel.icecp.bundle.StoredBatch;
import com.intel.icecp.bundle.StoredBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class MockStoredBundle<T> implements StoredBundle<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final List<StoredBatch<T>> lists = new ArrayList<>();

    @Override
    public StoredBatch<T> newBatch() {
        StoredBatch<T> b = new MockStoredBatch<>();
        lists.add(b);
        return b;
    }

    @Override
    public boolean hasChildren() {
        LOGGER.info("Checking if {} has children", this);
        return !lists.isEmpty();
    }

    @Override
    public Iterable<StoredBatch<T>> children() {
        LOGGER.info("Checking if {} has children", this);
        return lists;
    }

    private StoredBatch<T> last() {
        if (lists.isEmpty()) throw new IllegalStateException("No batches available; create one before retrieving one");
        return lists.get(lists.size() - 1);
    }

    @Override
    public void addItem(T item) {
        ((MockStoredBatch<T>) last()).add(item);
    }
}

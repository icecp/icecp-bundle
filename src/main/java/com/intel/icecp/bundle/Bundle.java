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

package com.intel.icecp.bundle;

import java.util.Collections;
import java.util.stream.StreamSupport;

/**
 * Represent a tree-like structure of things that can be "finished". Once a is finished, all of its children are
 * finished; symmetrically, when its children are finished, it is finished. Default implementations are provided for
 * ease of implementation but note that the default behavior is to assume that the bundle is a "leaf", i.e. has no
 * children.
 *
 */
public interface Bundle<T extends Bundle> {

    /**
     * @return true if the bundle has children
     */
    default boolean hasChildren() {
        return false;
    }

    /**
     * @return an iteration over all the children
     */
    @SuppressWarnings("unchecked")
    default Iterable<T> children() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Mark this bundle and all of its children as finished
     */
    default void markFinished() {
        if (hasChildren())
            StreamSupport.stream(children().spliterator(), false).forEach(Bundle::markFinished);
    }

    /**
     * @return true if the bundle is finished, i.e. it contains no unfinished children
     */
    default boolean isFinished() {
        return !hasChildren() || StreamSupport.stream(children().spliterator(), false).allMatch(Bundle::isFinished);
    }
}

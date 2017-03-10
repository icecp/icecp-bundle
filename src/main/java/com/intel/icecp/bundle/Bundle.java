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

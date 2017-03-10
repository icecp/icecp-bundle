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

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.ModuleStateAttribute;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.module.storage.StorageModule;
import com.intel.icecp.module.storage.attributes.AckChannelAttribute;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.NodeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class StorageBundleIT {
    private static final URI STORAGE_MODULE_URI = URI.create("mock:/test/node/1234/module/12346789");
    private static final URI LISTEN_CHANNEL_URI = URI.create("mock:/test/incoming/data");
    private static final Logger LOGGER = LogManager.getLogger();

    private Node node;
    private StoredBundle<BytesMessage> bundle;
    private StoredBatch<BytesMessage> storageBatch;

    @Before
    public void beginStorageModule() throws Exception {
        node = NodeFactory.buildMockNode();

        StorageModule storageModule = new StorageModule();
        storageModule.run(node, getStorageModuleAttributes(node.channels(), STORAGE_MODULE_URI));

        bundle = Bundles.newStoredBundle(node.channels(), LISTEN_CHANNEL_URI);
        storageBatch = bundle.newBatch();

        LOGGER.info("Started batch with id: {}", storageBatch.id());
    }

    @After
    public void cleanStorageBundle() {
        StreamSupport.stream(bundle.children().spliterator(), false).forEach(StoredBatch::markFinished);
    }

    @Test
    public void canStartHasBatchIdTest() throws Exception {

        assertTrue(bundle.hasChildren()); // bundle created in @Before
        assertEquals(1, count(bundle.children())); // has one batch
        assertEquals(0, count(storageBatch.children())); // batch created in @Before
        assertFalse(storageBatch.id() == 0);
    }

    @Test
    public void hasStarted() throws Exception {
        sendMessagesToStorage(node, 10);

        assertTrue(bundle.hasChildren()); // started in @Before
        assertEquals(1, count(bundle.children()));
    }

    @Test
    public void hasStoredPublishedMessages() throws Exception {
        sendMessagesToStorage(node, 10);

        assertTrue(storageBatch.hasChildren()); // started in @Before
        assertEquals(10, count(storageBatch.children()));

        byte[] firstMessage = storageBatch.children().iterator().next().value().getBytes();
        assertArrayEquals(generateMessage(0).getBytes(), firstMessage); // first messages match
    }

    @Test
    public void commitNothing() throws Exception {
        sendMessagesToStorage(node, 10);

        assertTrue(storageBatch.hasChildren()); // started in @Before
        long numChildren = count(storageBatch.children());

        storageBatch.commit();

        assertTrue(storageBatch.hasChildren());
        assertEquals(numChildren, count(storageBatch.children()));
    }

    @Test
    public void markEveryItemFinishedAndCommit() throws Exception {
        sendMessagesToStorage(node, 10);

        assertTrue(storageBatch.hasChildren());
        assertFalse(storageBatch.isFinished());

        for (StoredItem item : storageBatch.children()) {
            item.markFinished();
        }
        storageBatch.commit();

        assertTrue(storageBatch.hasChildren());
        assertTrue(storageBatch.isFinished());
    }

    @Test
    public void markSomeItemsFinishedAndCommit() throws Exception {
        sendMessagesToStorage(node, 10);

        assertTrue(storageBatch.hasChildren());

        List<Integer> toRemove = Arrays.asList(3, 6, 7, 8);
        List<BytesMessage> removed = new ArrayList<>();
        int i = 0;
        for (StoredItem<BytesMessage> item : storageBatch.children()) {
            if (toRemove.contains(i)) {
                item.markFinished();
                removed.add(item.value());
            }
            i++;
        }
        storageBatch.commit();

        assertTrue(storageBatch.hasChildren());
        assertEquals(10, count(storageBatch.children()));
        assertDoesNotContain(removed, storageBatch.children());
    }

    @Test
    public void newBatchFromExistingBundle() throws Exception {
        sendMessagesToStorage(node, 10);

        StoredBatch<BytesMessage> renamedBatch = bundle.newBatch();

        assertFalse(renamedBatch.hasChildren());
    }

    @Test
    public void newBatchWithMessages() throws Exception {
        sendMessagesToStorage(node, 10);

        StoredBatch<BytesMessage> renamedBatch = bundle.newBatch();

        sendMessagesToStorage(node, 5);

        assertTrue(renamedBatch.hasChildren());
        assertEquals(5, count(renamedBatch.children()));
    }

    @Test
    public void testDoesSyncEachTimeNoBatchesAreReturnedFromStorage() throws Exception {
        assertEquals(1, count(bundle.children()));

        sendMessagesToStorage(node, 1);

        assertTrue(bundle.hasChildren());
        assertEquals(1, count(bundle.children()));
    }

    @Test
    public void testDoesSyncEachTimeBundleHasFinishedBatches() throws Exception {
        sendMessagesToStorage(node, 1);

        bundle.newBatch();

        assertEquals(2, count(bundle.children()));

        bundle.children().forEach(b -> {
            if (b.id() == storageBatch.id())
                b.markFinished();
        });

        assertTrue(bundle.hasChildren());
        assertEquals(1, count(bundle.children()));
    }

    @Test
    public void bundleWithFinishedSession() {
        sendMessagesToStorage(node, 10);

        assertTrue(storageBatch.hasChildren());

        bundle.newBatch();

        sendMessagesToStorage(node, 5);

        bundle.newBatch();

        storageBatch.markFinished();

        bundle.newBatch();

        assertEquals(2, StreamSupport.stream(bundle.children().spliterator(), false).count());
    }

    private void assertDoesNotContain(List<BytesMessage> removed, Iterable<StoredItem<BytesMessage>> children) {
        StreamSupport.stream(children.spliterator(), false).forEach(item ->
                removed.forEach(r -> {
                    if (Arrays.equals(r.getBytes(), item.value().getBytes()) && !item.isFinished()) {
                        fail("Found a removed message in the batch children: " + new String(r.getBytes()));
                    }
                }));
    }

    private void sendMessagesToStorage(Node node, int count) {
        try (Channel<BytesMessage> channel = node.openChannel(LISTEN_CHANNEL_URI, BytesMessage.class, Persistence.DEFAULT)) {
            for (int i = 0; i < count; i++) {
                channel.publish(generateMessage(i));
            }
        } catch (ChannelLifetimeException | ChannelIOException e) {
            LOGGER.error(e);
            fail("Failed to publish incoming messages to the storage module");
        }
    }

    private BytesMessage generateMessage(int id) {
        return new BytesMessage((".....#" + id).getBytes());
    }

    private Attributes getStorageModuleAttributes(Channels channels, URI storageModuleUri) throws AttributeRegistrationException, URISyntaxException {
        Attributes attributes = AttributesFactory.buildEmptyAttributes(channels, storageModuleUri);
        attributes.add(new IdAttribute(42));
        attributes.add(new AckChannelAttribute("ndn:/intel/storage/acknowledged"));
        attributes.add(new ModuleStateAttribute());
        return attributes;
    }

    private long count(Iterable iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).count();
    }
}
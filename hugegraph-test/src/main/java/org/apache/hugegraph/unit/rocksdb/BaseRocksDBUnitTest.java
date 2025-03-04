/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.unit.rocksdb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.rocksdb.RocksDBException;

import org.apache.hugegraph.backend.store.rocksdb.RocksDBSessions;
import org.apache.hugegraph.backend.store.rocksdb.RocksDBStdSessions;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.unit.BaseUnitTest;
import org.apache.hugegraph.unit.FakeObjects;

public class BaseRocksDBUnitTest extends BaseUnitTest {

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    protected static final String DB_PATH = TMP_DIR + "/" + "rocksdb";
    protected static final String SNAPSHOT_PATH = TMP_DIR + "/" + "snapshot";

    protected static final String TABLE = "test-table";

    protected RocksDBSessions rocks;

    @AfterClass
    public static void clear() throws IOException {
        /*
         * The FileUtils.forceDelete() can only accept a `File`
         * in `org.apache.commons.io` version 2.4
         */
        FileUtils.forceDelete(FileUtils.getFile(DB_PATH));
    }

    @Before
    public void setup() throws RocksDBException {
        this.rocks = open(TABLE);
        this.rocks.session();
    }

    @After
    public void teardown() throws RocksDBException {
        this.clearData();
        close(this.rocks);
    }

    protected void put(String key, String value) {
        this.rocks.session().put(TABLE, getBytes(key), getBytes(value));
        this.commit();
    }

    protected String get(String key) throws RocksDBException {
        return getString(this.rocks.session().get(TABLE, getBytes(key)));
    }

    protected void clearData() throws RocksDBException {
        for (String table : new ArrayList<>(this.rocks.openedTables())) {
            this.rocks.session().deleteRange(table,
                                             new byte[]{0}, new byte[]{-1});
        }
        this.commit();
    }

    protected void commit() {
        try {
            this.rocks.session().commit();
        } finally {
            this.rocks.session().rollback();
        }
    }

    protected static byte[] getBytes(String str) {
        return str.getBytes();
    }

    protected static String getString(byte[] bytes) {
        return bytes == null ? null : new String(bytes);
    }

    protected static byte[] getBytes(long val) {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());
        buf.putLong(val);
        return buf.array();
    }

    protected static long getLong(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        return buf.getLong();
    }

    private static RocksDBSessions open(String table) throws RocksDBException {
        HugeConfig config = FakeObjects.newConfig();
        RocksDBSessions rocks = new RocksDBStdSessions(config, "db", "store",
                                                       DB_PATH, DB_PATH);
        rocks.createTable(table);
        return rocks;
    }

    private static void close(RocksDBSessions rocks) throws RocksDBException {
        for (String table : new ArrayList<>(rocks.openedTables())) {
            if (table.equals("default")) {
                continue;
            }
            rocks.dropTable(table);
        }
        rocks.close();
    }
}

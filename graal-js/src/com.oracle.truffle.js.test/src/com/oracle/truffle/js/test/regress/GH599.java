/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.test.regress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.junit.Assert;
import org.junit.Test;

public class GH599 {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        String fsRoot = System.getProperty("os.name").contains("indows") ? "C:/" : "/";
        Map<String, String> files = new HashMap<>();
        files.put("/cjs-module.js", "exports.foo = 'foo';");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context context = Context.newBuilder("js").//
                        fileSystem(new TestFileSystem(files)).//
                        allowAllAccess(true).//
                        out(out).//
                        err(out).//
                        allowExperimentalOptions(true).//
                        option("js.commonjs-require", "true").//
                        option("js.commonjs-require-cwd", fsRoot).//
                        build();
        Value asyncImport = context.eval("js", "import('/cjs-module.js');");
        CompletableFuture<Value> javaPromise = new CompletableFuture<>();
        asyncImport.invokeMember("then",
                        (Consumer<Object>) r -> javaPromise.complete(context.asValue(r).getMember("foo")),
                        (Consumer<Object>) r -> javaPromise.completeExceptionally(new AssertionError(r)));
        Assert.assertTrue(javaPromise.isDone());
        Assert.assertFalse(javaPromise.isCompletedExceptionally());
        Value importedModule = javaPromise.get();
        Assert.assertEquals("foo", importedModule.asString());
    }

    private static class TestFileSystem implements FileSystem {

        private Map<String, String> files;

        TestFileSystem(Map<String, String> files) {
            this.files = files;
        }

        @Override
        public Path parsePath(String path) {
            return Paths.get(path);
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return Paths.get("/").resolve(path).normalize();
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            return toAbsolutePath(path);
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            if (path.equals(path.getRoot())) {
                return;
            }
            String filepath = path.toString().replace(File.separator, "/");
            if (!files.containsKey(filepath)) {
                throw new FileNotFoundException();
            }
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            String filepath = path.toString().replace(File.separator, "/");
            if (!files.containsKey(filepath)) {
                throw new FileNotFoundException();
            }
            Map<String, Object> attr = new HashMap<>();
            attr.put("isRegularFile", Boolean.TRUE);
            attr.put("isDirectory", Boolean.FALSE);
            return attr;
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            String filepath = path.toString().replace(File.separator, "/");
            String contents = files.get(filepath);
            byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
            long size = bytes.length;
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(bytes));
            return new SeekableByteChannel() {

                @Override
                public boolean isOpen() {
                    return channel.isOpen();
                }

                @Override
                public void close() throws IOException {
                    channel.close();
                }

                @Override
                public int write(ByteBuffer src) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public SeekableByteChannel truncate(long sizeParam) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public long size() {
                    return size;
                }

                @Override
                public int read(ByteBuffer dst) throws IOException {
                    return channel.read(dst);
                }

                @Override
                public SeekableByteChannel position(long newPosition) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public long position() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Path parsePath(URI uri) {
            return Path.of(uri);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}

/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * WebAssembly.Memory buffer should be reset after internal mem_grow
 * @option webassembly
 */

load('../js/assert.js');

// (module
//   (memory $mem (export "memory") 1 2)
//   (func $grow (export "grow")
//     i32.const 1
//     memory.grow
//     drop
//   )
// )
var bytes = [0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x01, 0x04, 0x01, 0x60, 0x00,
    0x00, 0x03, 0x02, 0x01, 0x00, 0x05, 0x04, 0x01, 0x01, 0x01, 0x02, 0x07, 0x11,
    0x02, 0x06, 0x6D, 0x65, 0x6D, 0x6F, 0x72, 0x79, 0x02, 0x00, 0x04, 0x67, 0x72,
    0x6F, 0x77, 0x00, 0x00, 0x0A, 0x09, 0x01, 0x07, 0x00, 0x41, 0x01, 0x40, 0x00,
    0x1A, 0x0B];
var module = new WebAssembly.Module(new Uint8Array(bytes));
var instance = new WebAssembly.Instance(module);
var mem = instance.exports.memory;
var grow = instance.exports.grow;

var buf1 = mem.buffer;

assertSame(65536, buf1.byteLength);
assertTrue(buf1 === mem.buffer);
grow();
assertSame(0, buf1.byteLength);
assertTrue(buf1 !== mem.buffer);

assertSame(131072, mem.buffer.byteLength);

// (module
//   (memory $mem (export "memory") 1 2)
//   (func $grow (export "grow")
//     i32.const 0
//     memory.grow
//     drop
//   )
// )
bytes = [0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, 0x01, 0x04, 0x01, 0x60, 0x00,
    0x00, 0x03, 0x02, 0x01, 0x00, 0x05, 0x04, 0x01, 0x01, 0x01, 0x02, 0x07, 0x11,
    0x02, 0x06, 0x6D, 0x65, 0x6D, 0x6F, 0x72, 0x79, 0x02, 0x00, 0x04, 0x67, 0x72,
    0x6F, 0x77, 0x00, 0x00, 0x0A, 0x09, 0x01, 0x07, 0x00, 0x41, 0x00, 0x40, 0x00,
    0x1A, 0x0B];
module = new WebAssembly.Module(new Uint8Array(bytes));
instance = new WebAssembly.Instance(module);
mem = instance.exports.memory;
grow = instance.exports.grow;

buf1 = mem.buffer;

assertSame(65536, buf1.byteLength);
assertTrue(buf1 === mem.buffer);
grow();
assertSame(0, buf1.byteLength);
assertTrue(buf1 !== mem.buffer);

assertSame(65536, mem.buffer.byteLength);
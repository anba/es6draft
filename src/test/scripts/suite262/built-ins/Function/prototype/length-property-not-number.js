/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
description: ...
info: >
  ...
features: [Symbol]
---*/

// Modify Function.prototype.length to ensure its value is not used as a fallback.
Object.defineProperty(Function.prototype, "length", {value: 5});

function f(a, b, c) {}
var bf;

Object.defineProperty(f, "length", {value: undefined});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is the undefined value");

Object.defineProperty(f, "length", {value: null});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is the null value");

Object.defineProperty(f, "length", {value: ""});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is the empty string");

Object.defineProperty(f, "length", {value: "1"});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is string value `'1'`");

Object.defineProperty(f, "length", {value: true});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is the boolean value `true`");

Object.defineProperty(f, "length", {value: {}});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is an object");

Object.defineProperty(f, "length", {value: Symbol()});
bf = f.bind();
assert.sameValue(bf.length, 0, "'length' is a symbol");

/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, fail
} = Assert;

// Access on typed array with invalid integer indices

// Invalid integer indices, value is not coerced with ToNumber
(new Int8Array(0))["-0"] = {valueOf() { fail `ToNumber called` }};
(new Int8Array(1))["-0"] = {valueOf() { fail `ToNumber called` }};
(new Int8Array(1))["-Infinity"] = {valueOf() { fail `ToNumber called` }};
(new Int8Array(1))["Infinity"] = {valueOf() { fail `ToNumber called` }};
(new Int8Array(1))["NaN"] = {valueOf() { fail `ToNumber called` }};
(new Int8Array(1))["0.5"] = {valueOf() { fail `ToNumber called` }};

// Valid integer indices, but out-of-bounds, value is still coerced with ToNumber
let callLog = "";
(new Int8Array(0))["0"] = {valueOf() { callLog += "a"; }};
(new Int8Array(1))["10"] = {valueOf() { callLog += "b"; }};
(new Int8Array(1))["1e+30"] = {valueOf() { callLog += "c"; }};
(new Int8Array(1))["-10"] = {valueOf() { callLog += "d"; }};
assertSame("abcd", callLog);

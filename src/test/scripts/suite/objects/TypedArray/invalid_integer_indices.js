/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

// Access on typed array with invalid integer indices

// Invalid integer indices, value is still coerced with ToNumber
let invalidIndexLog = "";
(new Int8Array(0))["-0"] = {valueOf() { invalidIndexLog += "a"; }};
(new Int8Array(1))["-0"] = {valueOf() { invalidIndexLog += "b"; }};
(new Int8Array(1))["-Infinity"] = {valueOf() { invalidIndexLog += "c"; }};
(new Int8Array(1))["Infinity"] = {valueOf() { invalidIndexLog += "d"; }};
(new Int8Array(1))["NaN"] = {valueOf() { invalidIndexLog += "e"; }};
(new Int8Array(1))["0.5"] = {valueOf() { invalidIndexLog += "f"; }};
assertSame("abcdef", invalidIndexLog);

// Valid integer indices, but out-of-bounds, value is still coerced with ToNumber
let callLog = "";
(new Int8Array(0))["0"] = {valueOf() { callLog += "a"; }};
(new Int8Array(1))["10"] = {valueOf() { callLog += "b"; }};
(new Int8Array(1))["1e+30"] = {valueOf() { callLog += "c"; }};
(new Int8Array(1))["-10"] = {valueOf() { callLog += "d"; }};
assertSame("abcd", callLog);

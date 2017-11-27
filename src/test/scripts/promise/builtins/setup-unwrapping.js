/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

function iterableFromArray(array) {
  return array[Symbol.iterator]();
}

function delayPromise(value, delay) {
  return new Promise(resolve => { setTimeout(() => { resolve(value) }, delay) });
}

function OrdinaryConstruct(constructor, args) {
  return new constructor(...args);
}

var atAtIterator = Symbol.iterator;

var {describe, specify, it, beforeEach, afterEach} = require("testapi");

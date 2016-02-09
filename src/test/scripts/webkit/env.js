/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
var global = this;

function __createIterableObject(array, methods = {}) {
  var i = 0, iterator = {
    next() {
      if (i < array.length) {
        return {value: array[i++], done: false};
      }
      return {value: void 0, done: true}
    },
    return: methods.return,
    throw: methods.throw,
  };
  return {
    [Symbol.iterator]() {
      return iterator;
    }
  };
}

// Redefine load() to load files relative from "stress" directory.
load = function(load, f) {
  return load("./stress/" + f);
}.bind(null, load);

// Stubs
function createRuntimeArray(...args) { return args; }
function DFGTrue() {}
function edenGC() {}
function effectful42() { return 42; }
function fiatInt52(v) { return +v; }
function fullGC() {}
function forceGCSlowPaths() {}
function hasCustomProperties(o) { return Reflect.ownKeys(o).length > 0; }
function isInt32(v) { return v === (v | 0); }
function noDFG() {}
function noInline() {}
function OSRExit() {}
function predictInt32() {}

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
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

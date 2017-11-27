/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// Allow BindingPattern in BindingRestElement
// https://github.com/tc39/ecma262/pull/170

function f(...[]) {}
function f(...[a]) {}
function f(...[a = 0]) {}

function f(...{}) {}
function f(...{a}) {}
function f(...{a = 0}) {}
function f(...{a: b}) {}
function f(...{a: b = 0}) {}

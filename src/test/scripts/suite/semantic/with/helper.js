/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function HasBinding(name) {
  return `get:Symbol(Symbol.unscopables);has:${name};`;
}

function GetBindingValue(name) {
  return `has:${name};get:${name};`;
}

function HasBindingIntercepted(name) {
  return `get:Symbol(Symbol.unscopables);`;
}

function HasBindingFail(name) {
  return `has:${name};`;
}

function HasBindingSuccess(name) {
  return `has:${name};get:Symbol(Symbol.unscopables);`;
}

function GetBindingValueFail(name) {
  return `has:${name};`;
}

function GetBindingValueSuccess(name) {
  return `has:${name};`;
}

function GetValue(name) {
  return `get:${name};`;
}

function BindingNotIntercepted() {
  return ``;
}

function BindingIntercepted() {
  return ``;
}

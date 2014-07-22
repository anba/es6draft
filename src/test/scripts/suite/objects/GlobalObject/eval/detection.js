/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

loadRelativeToScript("../../../lib/recorder.js");

function HasBinding(name) {
  return `get:Symbol(Symbol.unscopables);has:${name};`;
}

function GetBindingValue(name) {
  return `has:${name};get:${name};`;
}

function HasBindingFail(name) {
  return `getOwnPropertyDescriptor:${name};getPrototypeOf:`;
}

function HasBindingSuccess(name) {
  return `getOwnPropertyDescriptor:${name};getOwnPropertyDescriptor:Symbol(Symbol.unscopables);`;
}

function GetBindingValueFail(name) {
  return `getOwnPropertyDescriptor:${name};getPrototypeOf:`;
}

function GetBindingValueSuccess(name) {
  return `getOwnPropertyDescriptor:${name};getOwnPropertyDescriptor:Symbol(Symbol.unscopables);get:${name};`;
}

function CreatePrototype() {
  return `getPrototypeOf:`;
}

const global = this;

{
  let {object, record} = Recorder.createObject({});
  with (object) {
    eval("");
  }
  assertSame(HasBindingFail("eval"), record());
}

{
  let {object, record} = Recorder.createObject({eval: () => {}});
  with (object) {
    eval("");
  }
  assertSame(HasBindingSuccess("eval") + GetBindingValueSuccess("eval"), record());
}

{
  let logger = Recorder.createLogger();
  let {object: p} = Recorder.createObject({eval: () => {}}, logger);
  let {object, record} = Recorder.createObject({__proto__: p}, logger);
  with (object) {
    eval("");
  }
  assertSame(CreatePrototype() + HasBindingFail("eval") + HasBindingSuccess("eval") + GetBindingValueFail("eval") + GetBindingValueSuccess("eval"), record());
}

{
  let {object, record} = Recorder.createObject({eval: global.eval});
  with (object) {
    eval("");
  }
  assertSame(HasBindingSuccess("eval") + GetBindingValueSuccess("eval"), record());
}

{
  let logger = Recorder.createLogger();
  let {object: p} = Recorder.createObject({eval: global.eval}, logger);
  let {object, record} = Recorder.createObject({__proto__: p}, logger);
  with (object) {
    eval("");
  }
  assertSame(CreatePrototype() + HasBindingFail("eval") + HasBindingSuccess("eval") + GetBindingValueFail("eval") + GetBindingValueSuccess("eval"), record());
}

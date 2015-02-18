/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

System.load("lib/recorder.jsm");
const Recorder = System.get("lib/recorder.jsm");

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
  return `has:${name};get:${name};`;
}

function ProtoCheck() {
  return `get:Symbol(Symbol.unscopables);`;
}

function ProtoGet(name) {
  return `get:${name};`;
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
  assertSame(HasBindingFail("eval") + HasBindingSuccess("eval") + ProtoCheck() + GetBindingValueFail("eval") + GetBindingValueSuccess("eval") + ProtoGet("eval"), record());
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
  assertSame(HasBindingFail("eval") + HasBindingSuccess("eval") + ProtoCheck() + GetBindingValueFail("eval") + GetBindingValueSuccess("eval") + ProtoGet("eval"), record());
}

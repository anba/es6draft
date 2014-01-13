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

const global = this;

{
  let {object, record} = Recorder.createObject({});
  with (object) {
    eval("");
  }
  assertSame(HasBinding("eval"), record());
}

{
  let {object, record} = Recorder.createObject({eval: () => {}});
  with (object) {
    eval("");
  }
  assertSame(HasBinding("eval") + GetBindingValue("eval"), record());
}

{
  let {object, record} = Recorder.createObject({eval: global.eval});
  with (object) {
    eval("");
  }
  assertSame(HasBinding("eval") + GetBindingValue("eval"), record());
}

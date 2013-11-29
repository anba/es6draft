/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

loadRelativeToScript("../../lib/recorder.js");

function HasBinding(name) {
  return `get:Symbol(Symbol.unscopables);has:${name};`;
}

function GetBindingValue(name) {
  return `has:${name};get:${name};`;
}

// Object without @@unscopables, property is not present, single access
{
  let fallbackCalled = 0;
  let {object, record} = Recorder.createObject({});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertSame(HasBinding("property"), record());
  assertSame(1, fallbackCalled);
}

// Object without @@unscopables, property is not present, multi access
{
  let fallbackCalled = 0;
  let {object, record} = Recorder.createObject({});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertSame(HasBinding("property") + HasBinding("property"), record());
  assertSame(2, fallbackCalled);
}

// Object without @@unscopables, property is present, single access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let {object, record} = Recorder.createObject({get property() { getterCalled += 1 }});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertSame(HasBinding("property") + GetBindingValue("property"), record());
  assertSame(1, getterCalled);
  assertSame(0, fallbackCalled);
}

// Object without @@unscopables, property is present, multi access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let {object, record} = Recorder.createObject({get property() { getterCalled += 1 }});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertSame(HasBinding("property") + GetBindingValue("property") + HasBinding("property") + GetBindingValue("property"), record());
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}

/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

System.load("lib/recorder.jsm");
const Recorder = System.get("lib/recorder.jsm");

loadRelativeToScript("helper.js");

// Object without @@unscopables, property is not present, single access
{
  let fallbackCalled = 0;
  let {object, record} = Recorder.createObject({});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertSame(HasBindingFail("property"), record());
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
  assertSame(HasBindingFail("property").repeat(2), record());
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
  assertSame(HasBindingSuccess("property") + GetBindingValueSuccess("property") + GetValue("property"), record());
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
  assertSame((HasBindingSuccess("property") + GetBindingValueSuccess("property") + GetValue("property")).repeat(2), record());
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}

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

// Object with @@unscopables but no match, property is not present, single access
{
  let fallbackCalled = 0;
  let blackList = {otherProperty: true};
  let {object, record} = Recorder.createObject({[Symbol.unscopables]: blackList});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertSame(HasBindingFail("property"), record());
  assertSame(1, fallbackCalled);
}

// Object with @@unscopables but no match, property is not present, multi access
{
  let fallbackCalled = 0;
  let blackList = {otherProperty: true};
  let {object, record} = Recorder.createObject({[Symbol.unscopables]: blackList});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertSame(HasBindingFail("property").repeat(2), record());
  assertSame(2, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, single access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let blackList = {otherProperty: true};
  let {object, record} = Recorder.createObject({[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
    }
  }
  assertSame(HasBindingSuccess("property") + BindingNotIntercepted("property") + GetBindingValueSuccess("property") + BindingNotIntercepted("property") + GetValue("property"), record());
  assertSame(1, getterCalled);
  assertSame(0, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, multi access
{
  let fallbackCalled = 0;
  let getterCalled = 0;
  let blackList = {otherProperty: true};
  let {object, record} = Recorder.createObject({[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }});
  with ({get property() { fallbackCalled += 1 }}) {
    with (object) {
      property;
      property;
    }
  }
  assertSame((HasBindingSuccess("property") + BindingNotIntercepted("property") + GetBindingValueSuccess("property") + BindingNotIntercepted("property") + GetValue("property")).repeat(2), record());
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}

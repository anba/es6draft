/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

System.load("lib/recorder.jsm");
const Recorder = System.get("lib/recorder.jsm");

loadRelativeToScript("helper_history.js");

// Object without @@unscopables, property is not present, single access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup = WithLookup(history, {}, object => {
    with ({get property() { fallbackCalled += 1 }}) {
      with (object) {
        property;
      }
    }
  });
  assertEquals([
    ...HasBindingFail(lookup, "property"),
  ], history);
  assertSame(1, fallbackCalled);
}

// Object without @@unscopables, property is not present, multi access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup = WithLookup(history, {}, object => {
    with ({get property() { fallbackCalled += 1 }}) {
      with (object) {
        property;
        property;
      }
    }
  });
  assertEquals([
    ...HasBindingFail(lookup, "property"),
    ...HasBindingFail(lookup, "property"),
  ], history);
  assertSame(2, fallbackCalled);
}

// Object without @@unscopables, property is present, single access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup = WithLookup(history, {get property() { getterCalled += 1 }}, object => {
    with ({get property() { fallbackCalled += 1 }}) {
      with (object) {
        property;
      }
    }
  });
  assertEquals([
    ...HasBindingSuccess(lookup, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...GetValue(lookup, "property", void 0),
  ], history);
  assertSame(1, getterCalled);
  assertSame(0, fallbackCalled);
}

// Object without @@unscopables, property is present, multi access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup = WithLookup(history, {get property() { getterCalled += 1 }}, object => {
    with ({get property() { fallbackCalled += 1 }}) {
      with (object) {
        property;
        property;
      }
    }
  });
  assertEquals([
    ...HasBindingSuccess(lookup, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...GetValue(lookup, "property", void 0),
    ...HasBindingSuccess(lookup, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...GetValue(lookup, "property", void 0),
  ], history);
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}

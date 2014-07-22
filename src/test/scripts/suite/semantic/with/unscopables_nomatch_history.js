/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals
} = Assert;

loadRelativeToScript("../../lib/recorder.js");
loadRelativeToScript("helper_history.js");

// Object with @@unscopables but no match, property is not present, single access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {otherProperty: true}, blackList => {
    lookup = WithLookup(history, {[Symbol.unscopables]: blackList}, object => {
      with ({get property() { fallbackCalled += 1 }}) {
        with (object) {
          property;
        }
      }
    });
  });
  assertEquals([
    ...HasBindingFail(lookup, "property"),
  ], history);
  assertSame(1, fallbackCalled);
}

// Object with @@unscopables but no match, property is not present, multi access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {otherProperty: true}, blackList => {
    lookup = WithLookup(history, {[Symbol.unscopables]: blackList}, object => {
      with ({get property() { fallbackCalled += 1 }}) {
        with (object) {
          property;
          property;
        }
      }
    });
  });
  assertEquals([
    ...HasBindingFail(lookup, "property"),
    ...HasBindingFail(lookup, "property"),
  ], history);
  assertSame(2, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, single access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {otherProperty: true}, blackList => {
    lookup = WithLookup(history, {[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }}, object => {
      with ({get property() { fallbackCalled += 1 }}) {
        with (object) {
          property;
        }
      }
    });
  });
  assertEquals([
    ...HasBindingSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetValue(lookup, "property", void 0),
  ], history);
  assertSame(1, getterCalled);
  assertSame(0, fallbackCalled);
}

// Object with @@unscopables but no match, property is present, multi access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {otherProperty: true}, blackList => {
    lookup = WithLookup(history, {[Symbol.unscopables]: blackList, get property() { getterCalled += 1 }}, object => {
      with ({get property() { fallbackCalled += 1 }}) {
        with (object) {
          property;
          property;
        }
      }
    });
  });
  assertEquals([
    ...HasBindingSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetValue(lookup, "property", void 0),
    ...HasBindingSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetBindingValueSuccess(lookup, "property"),
    ...BindingNotIntercepted(lookup, lookupBlackList, "property"),
    ...GetValue(lookup, "property", void 0),
  ], history);
  assertSame(2, getterCalled);
  assertSame(0, fallbackCalled);
}

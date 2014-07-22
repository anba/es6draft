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

// Object with @@unscopables and match, property is not present, single access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {property: true}, blackList => {
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

// Object with @@unscopables and match, property is not present, multi access
{
  let history = [];
  let fallbackCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {property: true}, blackList => {
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

// Object with @@unscopables and match, property is present, single access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {property: true}, blackList => {
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
    ...BindingIntercepted(lookup, lookupBlackList, "property"),
  ], history);
  assertSame(0, getterCalled);
  assertSame(1, fallbackCalled);
}

// Object with @@unscopables and match, property is present, multi access
{
  let history = [];
  let fallbackCalled = 0, getterCalled = 0;
  let lookup, lookupBlackList = WithLookup(history, {property: true}, blackList => {
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
    ...BindingIntercepted(lookup, lookupBlackList, "property"),
    ...HasBindingSuccess(lookup, "property"),
    ...BindingIntercepted(lookup, lookupBlackList, "property"),
  ], history);
  assertSame(0, getterCalled);
  assertSame(2, fallbackCalled);
}

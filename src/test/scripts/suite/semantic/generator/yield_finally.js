/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// Test yield works when finally is present

function assertIteratorSeq(g, ...values) {
  let last = values.splice(-1)[0], init = values;
  let gen = g();
  for (let value of init) {
    assertEquals({value, done: false}, gen.next());
  }
  assertEquals({value: last, done: true}, gen.next());
  assertEquals({value: void 0, done: true}, gen.next());
}

{
  function* g() {
    try {
      return "try";
    } finally {
      return "finally";
    }
  }
  assertIteratorSeq(g, "finally");
}

{
  function* g() {
    try {
      return "try";
    } finally {
      yield "finally";
    }
  }
  assertIteratorSeq(g, "finally", "try");
}

{
  function* g() {
    try {
      yield "try";
    } finally {
      return "finally";
    }
  }
  assertIteratorSeq(g, "try", "finally");
}

{
  function* g() {
    try {
      yield "try";
    } finally {
      yield "finally";
    }
    return "return";
  }
  assertIteratorSeq(g, "try", "finally", "return");
}

{
  function* g() {
    try {
      try {
        return "try";
      } finally {
        yield "inner-finally";
      }
    } finally {
      yield "outer-finally";
    }
  }
  assertIteratorSeq(g, "inner-finally", "outer-finally", "try");
}

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

function f1() {
  try {
    return 0;
  } finally {
    L: try {
      return 1;
    } finally {
      break L;
    }
  }
}
assertSame(0, f1());

function f2() {
  try {
    try {
      return 0;
    } finally {
      L: try {
        return 1;
      } finally {
        break L;
      }
    }
  } finally {
    // empty
  }
}
assertSame(0, f2());

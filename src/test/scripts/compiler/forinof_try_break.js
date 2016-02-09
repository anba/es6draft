/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
function f1() {
  for (k in o) {
    try {
      break;
    } finally {
      continue;
    }
  }
}

function f2() {
  for (k in o) {
    try {
      break;
    } finally {
      return;
    }
  }
}

function f3() {
  for (k in o) {
    try {
      return;
    } finally {
      continue;
    }
  }
}

function f4() {
  for (k in o) {
    try {
      return;
    } finally {
      break;
    }
  }
}

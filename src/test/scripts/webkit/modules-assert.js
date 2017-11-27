/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
class AssertionError extends Error {}

export function shouldBe(actual, expected) {
  if (actual !== expected) {
    throw new AssertionError(`Expected "${expected}", but got "${actual}"`);
  }
}

export function shouldNotBe(actual, expected) {
  if (actual === expected) {
    throw new AssertionError(`Expected not "${expected}"`);
  }
}

export function shouldThrow(f, errorMessage) {
  var constructorName = errorMessage.substring(0, errorMessage.indexOf(":"));
  try {
    f();
  } catch (e) {
    if (e.constructor.name === constructorName) {
      return;
    }
    throw new AssertionError(`Expected "${constructorName}", but got "${e.constructor.name}": ${e}`);
  }
  throw new AssertionError(`Missing exception: ${constructorName}`);
}

export function shouldNotThrow(f) {
  f();
}

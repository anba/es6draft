/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 12.3.2.1 Evaluation: Missing ReturnIfAbrupt in step 9
// https://bugs.ecmascript.org/show_bug.cgi?id=3357

class ValidError extends Error {}
class InvalidError extends Error {}

assertThrows(ValidError, () => {
  var base = () => { throw new ValidError };
  var property = () => { throw new InvalidError };
  var value = () => { throw new InvalidError };

  base()[property()] = value();
});

assertThrows(ValidError, () => {
  var base = () => { return null };
  var property = () => { throw new ValidError };
  var value = () => { throw new InvalidError };

  base()[property()] = value();
});

assertThrows(ValidError, () => {
  var base = () => { return {} };
  var property = () => { throw new ValidError };
  var value = () => { throw new InvalidError };

  base()[property()] = value();
});

assertThrows(TypeError, () => {
  var base = () => { return null };
  var property = () => { return {toString() { throw new InvalidError }} };
  var value = () => { throw new InvalidError };

  base()[property()] = value();
});

assertThrows(ValidError, () => {
  var base = () => { return {} };
  var property = () => { return {toString() { throw new ValidError }} };
  var value = () => { throw new InvalidError };

  base()[property()] = value();
});

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;


assertSyntaxError(`[(a = 0)] = []`);
assertSyntaxError(`({a: (b = 0)} = {})`);
assertSyntaxError(`[...a,] = []`);
assertSyntaxError(`[(...a)] = []`);
assertSyntaxError(`[...(...a)] = []`);

// Similar but valid forms
function validDestructuring() {
  [a] = [];
  [(a)] = [];
  [a = 0] = [];
  [(a) = 0] = [];

  ({a: b} = {});
  ({a: (b)} = {});
  ({a: b = 0} = {});
  ({a: (b) = 0} = {});

  [...a] = [];
  [...(a)] = [];
}

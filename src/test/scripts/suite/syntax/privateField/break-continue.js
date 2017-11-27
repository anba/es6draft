/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

assertSyntaxError(`L: { class C { #private = do { break L; }; } }`);

assertSyntaxError(`do { class C { #private = do { break; }; } } while (0);`);
assertSyntaxError(`do { class C { #private = do { continue; }; } } while (0);`);

assertSyntaxError(`L: do { class C { #private = do { break L; }; } } while (0);`);
assertSyntaxError(`L: do { class C { #private = do { continue L; }; } } while (0);`);

// break/continue within do-expression allowed
class Break {
  #a = do {
    L: break L;
  };
  #b = do {
    do { break; } while (0);
  };
  #c = do {
    do { continue; } while (0);
  };
  #d = do {
    L: do { break L; } while (0);
  };
  #e = do {
    L: do { continue L; } while (0);
  };
}

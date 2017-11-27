/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-regexp.prototype-@@split
description: ...
info: >
  21.2.5.11 RegExp.prototype [ @@split ] ( string, limit )
    ...
    4. Let C be ? SpeciesConstructor(rx, %RegExp%).
    ...
    10. Let splitter be ? Construct(C, « rx, newFlags »).
    ...

  21.2.3.1 RegExp ( pattern, flags )
    1. Let patternIsRegExp be ? IsRegExp(pattern).
    ...

---*/

var getterCalled = 0;

var regExp = /(?:)/;
Object.defineProperty(regExp, Symbol.match, {
    get: function() {
      getterCalled += 1;
    }
});

regExp[Symbol.split]("abc");

assert.sameValue(getterCalled, 1);

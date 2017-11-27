/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

setSharedArrayBuffer(new SharedArrayBuffer(1));
let sab = getSharedArrayBuffer();
sab.constructor = {
  [Symbol.species]: function() {
    return getSharedArrayBuffer();
  }
};

assertThrows(TypeError, () => {
  sab.slice(0);
});

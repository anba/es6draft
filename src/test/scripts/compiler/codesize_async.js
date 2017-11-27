/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
eval(`
  async function f() {
    ${Array.from({length: 1000}, () => "f()").join(";")};
    await x;
  }
`);

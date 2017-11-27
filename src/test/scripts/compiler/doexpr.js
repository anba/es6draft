/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No crash
L1: {
  L2: [do {
    if (Math.random() < 0.5) break L1;
    if (Math.random() > 0.5) break L2;
  }];
}

// No crash
function* g() {
  void do { void do { yield } };
}

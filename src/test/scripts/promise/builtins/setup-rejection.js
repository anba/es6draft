/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

// Try to start a garbage collection.
function triggerGC() {
  function tryGC(i) {
    if (i > 0) {
      setTimeout(() => { gc(); tryGC(i - 1); }, 100);
    }
  }
  tryGC(5);
}

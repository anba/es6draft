/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Global() {
"use strict";

const global = %GlobalTemplate();

const {
  Object, Symbol
} = global;

/*
 * Add @@toStringTag to global object
 */
Object.defineProperty(global, Symbol.toStringTag, {
  value: "global", writable: true, enumerable: false, configurable: true
});

})();

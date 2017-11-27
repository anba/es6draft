/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Global() {
"use strict";

const global = %GlobalProperties();
const Symbol_toStringTag = %WellKnownSymbol("toStringTag");

/*
 * Add @@toStringTag to global object
 */
%CreateMethodProperties(global, {
  [Symbol_toStringTag]: "global"
});

})();

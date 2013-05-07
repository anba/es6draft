/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Generator(global) {
"use strict";

const GeneratorPrototype = Object.getPrototypeOf(function*(){}).prototype;

// remove close() function from prototype
delete GeneratorPrototype.close;

})(this);

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// Changes to default constructor breaks "existing" code
// https://bugs.ecmascript.org/show_bug.cgi?id=2491

function Base() {}
Base.prototype = {};
class Derived extends Base {}

assertTrue(new Derived() instanceof Base);
assertTrue(new Derived() instanceof Derived);

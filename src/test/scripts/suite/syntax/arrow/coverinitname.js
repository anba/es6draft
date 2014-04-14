/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
} = Assert;

// https://bugs.ecmascript.org/show_bug.cgi?id=2506

// CoverInitializedName in ArrowParameters is not a SyntaxError
({a = 0}) => {};
({a = 0, b = 0}) => {};
({a = 0}, {b = 0}) => {};

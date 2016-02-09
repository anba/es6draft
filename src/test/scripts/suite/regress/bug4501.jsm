/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// 15.2.1.16.4 ModuleDeclarationInstantiation: Duplicate var statements not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=4501

// Doesn't crash.
var x;
var x;

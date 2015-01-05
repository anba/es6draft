/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.14.5.2, 12.14.5.3, 12.14.5.4: Assign inferred function names?
// https://bugs.ecmascript.org/show_bug.cgi?id=3264

var fident;
({fident = function(){}} = {});
assertSame("", fident.name);

var fiter;
([fiter = function(){}] = []);
assertSame("", fiter.name);

var fkey;
({k: fkey = function(){}} = {});
assertSame("", fkey.name);

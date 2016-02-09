/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// 12.13.4, 13.2.2.3, 13.2.1.6, 13.2.3.5, 13.2.3.6: Missing HasOwnProperty("name") check for anonymous class expression
// https://bugs.ecmascript.org/show_bug.cgi?id=2206

c = class {static get name(){}};
var [c = class {static get name(){}}] = [];
var {c = class {static get name(){}}} = [];
var c = class {static get name(){}};

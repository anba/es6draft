/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertNotSame
} = Assert;

let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
let desc = Object.getOwnPropertyDescriptor(Proxy({}, {getOwnPropertyDescriptor: () => desc}));

// actual: assertSame(specialDesc, desc)
assertNotSame(specialDesc, desc, "[[Origin]] bug in ExoticProxy fixed");

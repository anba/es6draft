/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// B.3.5 VariableStatements in Catch blocks: Incorrect redefinition of steps
// https://bugs.ecmascript.org/show_bug.cgi?id=4175

assertThrows(TypeError, () => class extends Proxy {});

const proxyPrototype = {};
Object.defineProperty(Proxy, "prototype", {value: proxyPrototype});

class P extends Proxy { }

let p = new P({a: 1}, {
  get(t, pk, r) {
    return Reflect.get(t, pk, r) + 2;
  }
});
assertSame(3, p.a);

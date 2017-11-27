/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Side-effects in private field initializers do not allow repeated initialization.

{
  class Ext extends class { constructor(o) { return o; } } {
    #private() { return 123; }
    constructor(o) { super(o); }
    static init(o) { return new Ext(o); }
    static get(o) { return o.#private; }
  };

  let obj = {};

  Ext.init(obj);
  assertSame(123, Ext.get(obj)());

  assertThrows(TypeError, () => Ext.init(obj));
  assertSame(123, Ext.get(obj)());
}

{
  class Ext extends class { constructor(o) { return o; } } {
    get #private() { return 123; }
    constructor(o) { super(o); }
    static init(o) { return new Ext(o); }
    static get(o) { return o.#private; }
  };

  let obj = {};

  Ext.init(obj);
  assertSame(123, Ext.get(obj));

  assertThrows(TypeError, () => Ext.init(obj));
  assertSame(123, Ext.get(obj));
}

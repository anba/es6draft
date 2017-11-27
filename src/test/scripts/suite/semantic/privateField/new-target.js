/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertInstanceOf, fail
} = Assert;

// global scope
class C {
  #private = new.target;
  get() { return this.#private; }
  static get(o) { return o.#private; }
}
assertUndefined(new C().get());
assertUndefined(Reflect.construct(C, []).get());
assertUndefined(C.get(Reflect.construct(C, [], RegExp)));

class D {
  #private = eval("new.target");
  get() { return this.#private; }
  static get(o) { return o.#private; }
}
assertUndefined(new D().get());
assertUndefined(Reflect.construct(D, []).get());
assertUndefined(D.get(Reflect.construct(D, [], RegExp)));


// function scope
function F() {
  class C {
    #private = new.target;
    get() { return this.#private; }
    static get(o) { return o.#private; }
  }
  assertUndefined(new C().get());
  assertUndefined(Reflect.construct(C, []).get());
  assertUndefined(C.get(Reflect.construct(C, [], Array)));
}
F();
new F();

function G() {
  class C {
    #private = eval("new.target");
    get() { return this.#private; }
    static get(o) { return o.#private; }
  }
  assertUndefined(new C().get());
  assertUndefined(Reflect.construct(C, []).get());
  assertUndefined(C.get(Reflect.construct(C, [], Array)));
}
G();
new G();

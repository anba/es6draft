/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Abrupt completions not handled for HasProperty?
// https://bugs.ecmascript.org/show_bug.cgi?id=4276

// HasRestrictedGlobalProperty
{
  class MyError extends Error { }
  let realm = new Reflect.Realm(Object.create(null), {
    getOwnPropertyDescriptor(t, pk) {
      if (pk === "restricted") {
        throw new MyError();
      }
      return Reflect.getOwnPropertyDescriptor(t, pk);
    }
  });
  assertThrows(MyError, () => evalScript("let restricted", {realm}));
}

// CanDeclareGlobalFunction (1)
{
  class MyError extends Error { }
  let realm = new Reflect.Realm(Object.create(null), {
    getOwnPropertyDescriptor(t, pk) {
      if (pk === "restricted") {
        throw new MyError();
      }
      return Reflect.getOwnPropertyDescriptor(t, pk);
    }
  });
  assertThrows(MyError, () => evalScript("function restricted(){}", {realm}));
}

// CanDeclareGlobalFunction (2)
{
  class MyError extends Error { }
  let throwError = false;
  let realm = new Reflect.Realm(Object.create(null), {
    getOwnPropertyDescriptor(t, pk) {
      if (pk === "restricted") {
        throwError = true;
      }
      return Reflect.getOwnPropertyDescriptor(t, pk);
    },
    isExtensible(t) {
      if (throwError) {
        throw new MyError();
      }
      return Reflect.isExtensible(t);
    }
  });
  assertThrows(MyError, () => evalScript("function restricted(){}", {realm}));
}

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// 22.1.3.1 Array.prototype.concat

function assertSameArray(array1, array2) {
  assertSame(array1.length, array2.length);
  for (let i = 0, len = array1.length; i < len; ++i) {
    assertSame(array1[i], array2[i]);
  }
}

// concat() with same realm constructor
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  let array2 = array1.concat();

  // array1.constructor is from the same realm, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with different realm constructor (1)
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  let array2 = Array.prototype.concat.call(array1);

  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignMyArray, array2.constructor);
  assertSame(ForeignMyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with different realm constructor (2)
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  let array2 = ForeignArray.prototype.concat.call(array1);

  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with different realm constructor (3)
{
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new Array(obj1, obj2);
  let array2 = ForeignArray.prototype.concat.call(array1);

  // array1.constructor is from a different realm, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignArray, array2.constructor);
  assertSame(ForeignArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with different realm constructor (4)
{
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignArray(obj1, obj2);
  let array2 = Array.prototype.concat.call(array1);

  // array1.constructor is from a different realm, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with proxied constructor
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = array1.concat();

  // Proxy (function) objects do not have a [[Realm]] internal slot, realm retrieved from proxy target
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with proxied constructor and with different realm constructor (1)
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = Array.prototype.concat.call(array1);

  // Proxy (function) objects do not have a [[Realm]] internal slot, realm retrieved from proxy target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignMyArray, array2.constructor);
  assertSame(ForeignMyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with proxied constructor and with different realm constructor (2)
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Proxy (function) objects do not have a [[Realm]] internal slot, realm retrieved from proxy target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with proxied constructor and with different realm constructor (3)
{
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new Array(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Proxy (function) objects do not have a [[Realm]] internal slot, realm retrieved from proxy target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with proxied constructor and with different realm constructor (4)
{
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignArray(obj1, obj2);
  array1.constructor = new Proxy(array1.constructor, {});
  let array2 = Array.prototype.concat.call(array1);

  // Proxy (function) objects do not have a [[Realm]] internal slot, realm retrieved from proxy target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignArray, array2.constructor);
  assertSame(ForeignArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor, without @@species
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  let array2 = array1.concat();

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // @@species is undefined, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor, with @@species
{
  class MyArray extends Array { }
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  array1.constructor[Symbol.species] = array1.constructor;
  let array2 = array1.concat();

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // @@species is not undefined, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (1a), without @@species
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  let array2 = Array.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is undefined, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (1b), without @@species
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  array1.constructor = Function.prototype.bind.call(array1.constructor, null);
  let array2 = Array.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is undefined, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(Array, array2.constructor);
  assertSame(Array.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (1a), with @@species
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  array1.constructor[Symbol.species] = array1.constructor;
  let array2 = Array.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is not undefined, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignMyArray, array2.constructor);
  assertSame(ForeignMyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (1b), with @@species
{
  const ForeignMyArray = new Reflect.Realm().eval(`
    class MyArray extends Array { }
    MyArray;
  `);
  const obj1 = {}, obj2 = {};
  let array1 = new ForeignMyArray(obj1, obj2);
  array1.constructor = Function.prototype.bind.call(array1.constructor, null);
  array1.constructor[Symbol.species] = array1.constructor;
  let array2 = Array.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is not undefined, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignMyArray, array2.constructor);
  assertSame(ForeignMyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (2a), without @@species
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is undefined, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignArray, array2.constructor);
  assertSame(ForeignArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (2b), without @@species
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const ForeignFunction = ForeignArray.constructor;
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = ForeignFunction.prototype.bind.call(array1.constructor, null);
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is undefined, concat() creates default Array instances
  assertTrue(Array.isArray(array2));
  assertSame(ForeignArray, array2.constructor);
  assertSame(ForeignArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (2a), with @@species
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = array1.constructor.bind(null);
  array1.constructor[Symbol.species] = array1.constructor;
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is not undefined, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

// concat() with bound constructor and with different realm constructor (2b), with @@species
{
  class MyArray extends Array { }
  const ForeignArray = new Reflect.Realm().eval("Array");
  const ForeignFunction = ForeignArray.constructor;
  const obj1 = {}, obj2 = {};
  let array1 = new MyArray(obj1, obj2);
  array1.constructor = ForeignFunction.prototype.bind.call(array1.constructor, null);
  array1.constructor[Symbol.species] = array1.constructor;
  let array2 = ForeignArray.prototype.concat.call(array1);

  // Bound function objects do not have a [[Realm]] internal slot, realm retrieved from bound target
  // array1.constructor is from a different realm, but is not the %Array% intrinsic
  // @@species is not undefined, concat() creates Array sub-class instances
  assertTrue(Array.isArray(array2));
  assertSame(MyArray, array2.constructor);
  assertSame(MyArray.prototype, Object.getPrototypeOf(array2));
  assertSameArray(array1, array2);
}

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

const { intrinsics } = System.realm;

// 18.3  Constructor Properties of the Global Object
assertSame(Array, intrinsics.Array);
assertSame(ArrayBuffer, intrinsics.ArrayBuffer);
assertSame(Boolean, intrinsics.Boolean);
assertSame(DataView, intrinsics.DataView);
assertSame(Date, intrinsics.Date);
assertSame(Error, intrinsics.Error);
assertSame(EvalError, intrinsics.EvalError);
assertSame(Float32Array, intrinsics.Float32Array);
assertSame(Float64Array, intrinsics.Float64Array);
assertSame(Function, intrinsics.Function);
assertSame(Int8Array, intrinsics.Int8Array);
assertSame(Int16Array, intrinsics.Int16Array);
assertSame(Int32Array, intrinsics.Int32Array);
assertSame(Map, intrinsics.Map);
assertSame(Number, intrinsics.Number);
assertSame(Object, intrinsics.Object);
assertSame(RangeError, intrinsics.RangeError);
assertSame(ReferenceError, intrinsics.ReferenceError);
assertSame(RegExp, intrinsics.RegExp);
assertSame(Set, intrinsics.Set);
assertSame(String, intrinsics.String);
assertSame(Symbol, intrinsics.Symbol);
assertSame(SyntaxError, intrinsics.SyntaxError);
assertSame(TypeError, intrinsics.TypeError);
assertSame(Uint8Array, intrinsics.Uint8Array);
assertSame(Uint8ClampedArray, intrinsics.Uint8ClampedArray);
assertSame(Uint16Array, intrinsics.Uint16Array);
assertSame(Uint32Array, intrinsics.Uint32Array);
assertSame(URIError, intrinsics.URIError);
assertSame(WeakMap, intrinsics.WeakMap);
assertSame(WeakSet, intrinsics.WeakSet);

// 18.4  Other Properties of the Global Object
assertSame(JSON, intrinsics.JSON);
assertSame(Math, intrinsics.Math);
assertSame(Proxy, intrinsics.Proxy);
assertSame(Reflect, intrinsics.Reflect);
assertSame(System, intrinsics.System);


// 18.3  Constructor Properties of the Global Object (Prototypes)
assertSame(Array.prototype, intrinsics.ArrayPrototype);
assertSame(ArrayBuffer.prototype, intrinsics.ArrayBufferPrototype);
assertSame(Boolean.prototype, intrinsics.BooleanPrototype);
assertSame(DataView.prototype, intrinsics.DataViewPrototype);
assertSame(Date.prototype, intrinsics.DatePrototype);
assertSame(Error.prototype, intrinsics.ErrorPrototype);
assertSame(EvalError.prototype, intrinsics.EvalErrorPrototype);
assertSame(Float32Array.prototype, intrinsics.Float32ArrayPrototype);
assertSame(Float64Array.prototype, intrinsics.Float64ArrayPrototype);
assertSame(Function.prototype, intrinsics.FunctionPrototype);
assertSame(Int8Array.prototype, intrinsics.Int8ArrayPrototype);
assertSame(Int16Array.prototype, intrinsics.Int16ArrayPrototype);
assertSame(Int32Array.prototype, intrinsics.Int32ArrayPrototype);
assertSame(Map.prototype, intrinsics.MapPrototype);
assertSame(Number.prototype, intrinsics.NumberPrototype);
assertSame(Object.prototype, intrinsics.ObjectPrototype);
assertSame(RangeError.prototype, intrinsics.RangeErrorPrototype);
assertSame(ReferenceError.prototype, intrinsics.ReferenceErrorPrototype);
assertSame(RegExp.prototype, intrinsics.RegExpPrototype);
assertSame(Set.prototype, intrinsics.SetPrototype);
assertSame(String.prototype, intrinsics.StringPrototype);
assertSame(Symbol.prototype, intrinsics.SymbolPrototype);
assertSame(SyntaxError.prototype, intrinsics.SyntaxErrorPrototype);
assertSame(TypeError.prototype, intrinsics.TypeErrorPrototype);
assertSame(Uint8Array.prototype, intrinsics.Uint8ArrayPrototype);
assertSame(Uint8ClampedArray.prototype, intrinsics.Uint8ClampedArrayPrototype);
assertSame(Uint16Array.prototype, intrinsics.Uint16ArrayPrototype);
assertSame(Uint32Array.prototype, intrinsics.Uint32ArrayPrototype);
assertSame(URIError.prototype, intrinsics.URIErrorPrototype);
assertSame(WeakMap.prototype, intrinsics.WeakMapPrototype);
assertSame(WeakSet.prototype, intrinsics.WeakSetPrototype);

// 19.1.3.6  Object.prototype.toString ( )
assertSame(Object.prototype.toString, intrinsics.ObjProto_toString);

// 21.1.5  String Iterator Objects
assertSame(Object.getPrototypeOf(""[Symbol.iterator]()), intrinsics.StringIteratorPrototype);

// 22.1.3.29  Array.prototype.values ( )
assertSame(Array.prototype.values, intrinsics.ArrayProto_values);

// 22.1.5  Array Iterator Objects
assertSame(Object.getPrototypeOf([][Symbol.iterator]()), intrinsics.ArrayIteratorPrototype);

// 22.2.1  The %TypedArray% Intrinsic Object
assertSame(Object.getPrototypeOf(Int8Array), intrinsics.TypedArray);
assertSame(Object.getPrototypeOf(Int8Array.prototype), intrinsics.TypedArrayPrototype);

// 23.1.5  Map Iterator Objects
assertSame(Object.getPrototypeOf((new Map)[Symbol.iterator]()), intrinsics.MapIteratorPrototype);

// 23.2.5  Set Iterator Objects
assertSame(Object.getPrototypeOf((new Set)[Symbol.iterator]()), intrinsics.SetIteratorPrototype);

// 25.2  GeneratorFunction Objects
// 25.3  Generator Objects
assertSame((function*(){}).constructor, intrinsics.GeneratorFunction);
assertSame((function*(){}).constructor.prototype, intrinsics.Generator);
assertSame((function*(){}).constructor.prototype.prototype, intrinsics.GeneratorPrototype);

// 26  Reflection
assertSame(Reflect.Loader, intrinsics.Loader);
assertSame(Reflect.Loader.prototype, intrinsics.LoaderPrototype);
assertSame(Reflect.Realm, intrinsics.Realm);
assertSame(Reflect.Realm.prototype, intrinsics.RealmPrototype);

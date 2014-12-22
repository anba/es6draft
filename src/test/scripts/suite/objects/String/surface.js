/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertDataProperty,
  assertBuiltinConstructor,
  assertBuiltinPrototype,
} = Assert;

function assertFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name = "constructor", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertPrototypeProperty(object, name = "prototype", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

/* String Objects */

assertBuiltinConstructor(String, "String", 1);
assertBuiltinPrototype(String.prototype);
assertSame(String, String.prototype.constructor);


/* Properties of the String Constructor */

assertPrototypeProperty(String);
assertFunctionProperty(String, "fromCharCode");
assertFunctionProperty(String, "fromCodePoint");
assertFunctionProperty(String, "raw");


/* Properties of the String Prototype Object */

assertConstructorProperty(String.prototype);
assertFunctionProperty(String.prototype, "charAt");
assertFunctionProperty(String.prototype, "charCodeAt");
assertFunctionProperty(String.prototype, "codePointAt");
assertFunctionProperty(String.prototype, "concat");
assertFunctionProperty(String.prototype, "endsWith");
assertFunctionProperty(String.prototype, "includes");
assertFunctionProperty(String.prototype, "indexOf");
assertFunctionProperty(String.prototype, "lastIndexOf");
assertFunctionProperty(String.prototype, "localeCompare");
assertFunctionProperty(String.prototype, "match");
assertFunctionProperty(String.prototype, "normalize");
assertFunctionProperty(String.prototype, "repeat");
assertFunctionProperty(String.prototype, "replace");
assertFunctionProperty(String.prototype, "search");
assertFunctionProperty(String.prototype, "slice");
assertFunctionProperty(String.prototype, "split");
assertFunctionProperty(String.prototype, "startsWith");
assertFunctionProperty(String.prototype, "substring");
assertFunctionProperty(String.prototype, "toLocaleLowerCase");
assertFunctionProperty(String.prototype, "toLocaleUpperCase");
assertFunctionProperty(String.prototype, "toLowerCase");
assertFunctionProperty(String.prototype, "toString");
assertFunctionProperty(String.prototype, "toUpperCase");
assertFunctionProperty(String.prototype, "trim");
assertFunctionProperty(String.prototype, "valueOf");
assertFunctionProperty(String.prototype, Symbol.iterator);

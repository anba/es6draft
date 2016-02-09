/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ToSource() {
"use strict";

const global = %GlobalTemplate();

const {
  Object, Function, Array, String, Boolean,
  Number, Math, Date, RegExp, Error, Symbol,
  TypeError, JSON, Intl, WeakSet,
} = global;

const {
  getOwnPropertyDescriptor: Object_getOwnPropertyDescriptor,
  getOwnPropertyNames: Object_getOwnPropertyNames,
  getOwnPropertySymbols: Object_getOwnPropertySymbols,
  prototype: {
    hasOwnProperty: Object_prototype_hasOwnProperty
  }
} = Object;

const {
  join: Array_prototype_join
} = Array.prototype;

const {
  toString: Boolean_prototype_toString
} = Boolean.prototype;

const {
  toString: Date_prototype_toString
} = Date.prototype;

const {
  toString: Function_prototype_toString
} = Function.prototype;

const {
  toString: Number_prototype_toString
} = Number.prototype;

const {
  toString: RegExp_prototype_toString
} = RegExp.prototype;

const {
  charAt: String_prototype_charAt,
  charCodeAt: String_prototype_charCodeAt,
  indexOf: String_prototype_indexOf,
  substring: String_prototype_substring,
  toString: String_prototype_toString,
  toUpperCase: String_prototype_toUpperCase,
} = String.prototype;

const {
  keyFor: Symbol_keyFor,
  prototype: {
    toString: Symbol_prototype_toString,
    valueOf: Symbol_prototype_valueOf,
  }
} = Symbol;

const {
  add: WeakSet_prototype_add,
  delete: WeakSet_prototype_delete,
  has: WeakSet_prototype_has,
} = WeakSet.prototype;

function ToHexString(c) {
  return %CallFunction(String_prototype_toUpperCase, %CallFunction(Number_prototype_toString, c, 16));
}

function Quote(s, qc = '"') {
  var r = "";
  for (var i = 0, len = s.length; i < len; ++i) {
    var c = %CallFunction(String_prototype_charCodeAt, s, i);
    switch (c) {
      case 0x09: r += "\\t"; continue;
      case 0x0A: r += "\\n"; continue;
      case 0x0B: r += "\\b"; continue;
      case 0x0C: r += "\\v"; continue;
      case 0x0D: r += "\\r"; continue;
      case 0x22: r += "\\\""; continue;
      case 0x5C: r += "\\\\"; continue;
    }
    if (c == 0x27 && qc == "'") {
      r += "\\\'";
    } else if (c < 0x20) {
      r += "\\x" + (c < 0x10 ? "0" : "") + ToHexString(c);
    } else if (c < 0x7F) {
      r += %CallFunction(String_prototype_charAt, s, i);
    } else if (c < 0x100) {
      r += "\\x" + ToHexString(c);
    } else {
      r += "\\u" + (c < 0x1000 ? "0" : "") + ToHexString(c);
    }
  }
  return qc + r + qc;
}

function SymbolToSource(sym) {
  // Well-known symbols
  if (sym === Symbol.hasInstance) return "Symbol.hasInstance";
  if (sym === Symbol.isConcatSpreadable) return "Symbol.isConcatSpreadable";
  if (sym === Symbol.isRegExp) return "Symbol.isRegExp";
  if (sym === Symbol.iterator) return "Symbol.iterator";
  if (sym === Symbol.toPrimitive) return "Symbol.toPrimitive";
  if (sym === Symbol.toStringTag) return "Symbol.toStringTag";
  if (sym === Symbol.unscopables) return "Symbol.unscopables";
  // Registered symbols
  let key = Symbol_keyFor(sym);
  if (key !== void 0) {
    return `Symbol.for(${Quote(key)})`;
  }
  // Other symbols
  let desc = %SymbolDescription(sym);
  if (desc === void 0) {
    return "Symbol()";
  }
  return `Symbol(${Quote(desc)})`;
}

const ASCII_Ident = /^[_$a-zA-Z][_$a-zA-Z0-9]*$/;
const functionSource = /^\(?function /;
const accessorSource = /^(?:get|set) [_$a-zA-Z0-9]+/;

function toAccessorFunctionString(source) {
  if (%RegExpTest(functionSource, source)) {
    let leadingParen = (%CallFunction(String_prototype_charAt, source, 0) === '(');
    let start = %CallFunction(String_prototype_indexOf, source, '(', 0 + leadingParen);
    return %CallFunction(String_prototype_substring, source, start, source.length - leadingParen);
  }
  if (%RegExpTest(accessorSource, source)) {
    let start = %CallFunction(String_prototype_indexOf, source, '(', 0);
    return %CallFunction(String_prototype_substring, source, start);
  }
}

function IsInt32(name) {
  return ((name | 0) >= 0 && (name | 0) <= 0x7fffffff && (name | 0) + "" == name);
}

function ToPropertyName(name) {
  if (typeof name === 'symbol') {
    return `[${SymbolToSource(name)}]`;
  }
  if (%RegExpTest(ASCII_Ident, name) || IsInt32(name)) {
    return name;
  }
  return Quote(name, "'");
}

function ToSource(o) {
  switch (typeof o) {
    case 'undefined':
      return "(void 0)";
    case 'boolean':
    case 'number':
      return "" + o;
    case 'string':
      return Quote(o);
    case 'symbol':
      return SymbolToSource(o);
    case 'function':
    case 'object':
      if (o !== null) {
        return typeof o.toSource == 'function' ? %ToString(o.toSource()) : ObjectToSource(o);
      }
    default:
      return "null";
  }
}

// weakset for cycle detection
const weakset = new WeakSet();
var depth = 0;

function ObjectToSource(o) {
  if (o == null) throw TypeError();
  var obj = Object(o);
  if (%CallFunction(WeakSet_prototype_has, weakset, obj)) {
    return "{}";
  }
  %CallFunction(WeakSet_prototype_add, weakset, obj);
  depth += 1;
  try {
    var s = "";
    for (var i = 0; i < 2; ++i) {
      var names = (i === 0 ? Object_getOwnPropertyNames : Object_getOwnPropertySymbols)(obj);
      for (var j = 0, len = names.length; j < len; ++j) {
        var name = names[j];
        var desc = Object_getOwnPropertyDescriptor(obj, name);
        if (desc == null || !desc.enumerable) {
          // ignore removed or non-enumerable properties
          continue;
        }
        if (s.length) {
          s += ", ";
        }
        if ('value' in desc) {
          s += `${ToPropertyName(name)}:${ToSource(desc.value)}`;
        } else {
          if (desc.get !== void 0) {
            let getterSource = ToSource(desc.get);
            let accessorSource = toAccessorFunctionString(getterSource);
            if (accessorSource) {
              s += `get ${ToPropertyName(name)} ${accessorSource}`;
            } else {
              s += `${ToPropertyName(name)}:${getterSource}`;
            }
            if (desc.set !== void 0) s += ", ";
          }
          if (desc.set !== void 0) {
            let setterSource = ToSource(desc.set);
            let accessorSource = toAccessorFunctionString(setterSource);
            if (accessorSource) {
              s += `set ${ToPropertyName(name)} ${accessorSource}`;
            } else {
              s += `${ToPropertyName(name)}:${setterSource}`;
            }
          }
        }
      }
    }
    if (depth > 1) {
      return "{" + s + "}";
    }
    return "({" + s + "})";
  } finally {
    %CallFunction(WeakSet_prototype_delete, weakset, obj);
    depth -= 1;
  }
}

Object.defineProperty(Object.assign(global, {
  uneval(o) {
    return ToSource(o);
  }
}), "uneval", {enumerable: false});

// duplicated definition from cyclic.js to access shared 'weakset'
Object.defineProperty(Object.assign(Array.prototype, {
  join(separator) {
    const isObject = typeof this == 'function' || typeof this == 'object' && this !== null;
    if (isObject) {
      if (%CallFunction(WeakSet_prototype_has, weakset, this)) {
        return "";
      }
      %CallFunction(WeakSet_prototype_add, weakset, this);
    }
    try {
      return %CallFunction(Array_prototype_join, this, separator);
    } finally {
      if (isObject) {
        %CallFunction(WeakSet_prototype_delete, weakset, this);
      }
    }
  }
}), "join", {enumerable: false});

Object.defineProperty(Object.assign(String.prototype, {
  quote() {
    return Quote(%CallFunction(String_prototype_toString, this));
  }
}), "quote", {enumerable: false});

Object.defineProperty(Object.assign(Object.prototype, {
  toSource() {
    return ObjectToSource(this);
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Function.prototype, {
  toSource() {
    if (typeof this != 'function') {
      return ObjectToSource(this);
    }
    var source = %CallFunction(Function_prototype_toString, this);
    if (%IsFunctionExpression(this)) {
      return "(" + source + ")";
    }
    return source;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Array.prototype, {
  toSource() {
    if (!(typeof this == 'function' || typeof this == 'object' && this !== null)) throw TypeError();
    if (%CallFunction(WeakSet_prototype_has, weakset, this)) {
      return "[]";
    }
    %CallFunction(WeakSet_prototype_add, weakset, this);
    depth += 1;
    try {
      var s = "";
      for (var i = 0, len = this.length; i < len; ++i) {
        if (!%CallFunction(Object_prototype_hasOwnProperty, this, i)) {
          s += ",";
          if (i + 1 < len) s += " ";
        } else {
          s += ToSource(this[i]);
          if (i + 1 < len) s += ", ";
        }
      }
      return "[" + s + "]";
    } finally {
      %CallFunction(WeakSet_prototype_delete, weakset, this);
      depth -= 1;
    }
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(String.prototype, {
  toSource() {
    return `(new String(${ Quote(%CallFunction(String_prototype_toString, this)) }))`;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Boolean.prototype, {
  toSource() {
    return `(new Boolean(${ %CallFunction(Boolean_prototype_toString, this) }))`;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Number.prototype, {
  toSource() {
    return `(new Number(${ %CallFunction(Number_prototype_toString, this) }))`;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Math, {
  toSource() {
    return "Math";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Date.prototype, {
  toSource() {
    return `(new Date(${ %CallFunction(Date_prototype_toString, this) }))`;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(RegExp.prototype, {
  toSource() {
    return %CallFunction(RegExp_prototype_toString, this);
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Error.prototype, {
  toSource() {
    return `(new ${this.name}(${ToSource(this.message)}, ${ToSource(this.fileName)}, ${ToSource(this.lineNumber)}))`;
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(JSON, {
  toSource() {
    return "JSON";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Intl, {
  toSource() {
    return "Intl";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Symbol.prototype, {
  toSource() {
    return SymbolToSource(%CallFunction(Symbol_prototype_valueOf, this));
  }
}), "toSource", {enumerable: false});

})();

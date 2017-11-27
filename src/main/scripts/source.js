/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ToSource() {
"use strict";

const TypeError = %Intrinsic("TypeError");

const Object = %Intrinsic("Object");
const {
  getOwnPropertyDescriptor: Object_getOwnPropertyDescriptor,
  prototype: {
    hasOwnProperty: Object_prototype_hasOwnProperty
  }
} = Object;

const NumberPrototype = %Intrinsic("NumberPrototype");
const Number_prototype_toString = NumberPrototype.toString;

const Reflect_ownKeys = %Intrinsic("Reflect").ownKeys;

const StringPrototype = %Intrinsic("StringPrototype");
const {
  charCodeAt: String_prototype_charCodeAt,
  indexOf: String_prototype_indexOf,
  substring: String_prototype_substring,
  toUpperCase: String_prototype_toUpperCase,
} = StringPrototype;

const Symbol_keyFor = %Intrinsic("Symbol").keyFor;

const Set = %Intrinsic("Set");
const {
  add: Set_prototype_add,
  delete: Set_prototype_delete,
  has: Set_prototype_has,
} = Set.prototype;

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
      r += s[i];
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
  if (%IsWellKnownSymbol(sym)) {
    return %SymbolDescription(sym);
  }
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

function IsInt32(name) {
  return ((name | 0) >= 0 && (name | 0) <= 0x7fffffff && (name | 0) + "" == name);
}

function ToPropertyName(propertyKey) {
  if (typeof propertyKey === 'symbol') {
    return `[${SymbolToSource(propertyKey)}]`;
  }
  if (%RegExpTest(ASCII_Ident, propertyKey) || IsInt32(propertyKey)) {
    return propertyKey;
  }
  return Quote(propertyKey, "'");
}

function FunctionArgsAndBody(source) {
  let len = source.length;
  if (len === 0)
    return;

  let start = 0, end = len;
  if (source[start] === '(' && source[end - 1] === ')') {
    start++;
    end--;
  }

  function Consume(s) {
    if (start + s.length >= end)
      return false;
    for (let i = 0; i < s.length; ++i) {
      if (source[start + i] !== s[i]) {
        return false;
      }
    }
    start += s.length;
    return true;
  }
  function ConsumeSpaces() {
    while (start < end && source[start] === ' ') {
      start++;
    }
  }

  Consume("async");
  ConsumeSpaces();
  Consume("function") || Consume("get") || Consume("set");
  ConsumeSpaces();
  Consume("*");
  ConsumeSpaces();

  if (Consume("[")) {
    start = %CallFunction(String_prototype_indexOf, source, "]", start);
    if (start < 0)
      return;
    start++;
    ConsumeSpaces();
    if (start < end && source[start] !== "(")
      return;
  } else {
    start = %CallFunction(String_prototype_indexOf, source, "(", start);
    if (start < 0)
      return;
  }

  return %CallFunction(String_prototype_substring, source, start, end);
}

function PropertySource(kind, propertyKey, value) {
  let name = ToPropertyName(propertyKey);
  let valueSource = ToSource(value);

  if (typeof value === "function") {
    if (kind === "get" || kind === "set") {
      if (kind === %MethodKind(value) && name === %FunctionName(value) && typeof propertyKey !== 'symbol') {
        return valueSource;
      }

      let argsAndBody = FunctionArgsAndBody(valueSource);
      if (argsAndBody) {
        return `${kind} ${name}${argsAndBody}`;
      }
    } else {
      let methodKind = %MethodKind(value);
      if (methodKind) {
        if (methodKind !== "get" && methodKind !== "set" && name === %FunctionName(value) && typeof propertyKey !== 'symbol') {
          return valueSource;
        }

        let argsAndBody = FunctionArgsAndBody(valueSource);
        if (argsAndBody) {
          let methodPrefix = methodKind === "async*" || methodKind === "async"
                             ? methodKind + " "
                             : methodKind === "*"
                             ? methodKind
                             : "";
          return `${methodPrefix}${name}${argsAndBody}`;
        }
      }
    }
  }

  return `${name}:${valueSource}`;
}

function ToSource(o) {
  switch (typeof o) {
    case 'undefined':
      return "(void 0)";
    case 'boolean':
    case 'number':
    case 'bigint':
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

// set for cycle detection
const set = new Set();
var depth = 0;

function ObjectToSource(o) {
  if (o == null) throw TypeError();
  var obj = Object(o);
  if (%CallFunction(Set_prototype_has, set, obj)) {
    return "{}";
  }
  %CallFunction(Set_prototype_add, set, obj);
  depth += 1;
  try {
    var s = "";
    var propertyKeys = Reflect_ownKeys(obj);
    for (var i = 0; i < propertyKeys.length; ++i) {
      var propertyKey = propertyKeys[i];
      var desc = Object_getOwnPropertyDescriptor(obj, propertyKey);
      if (desc == null || !desc.enumerable) {
        // ignore removed or non-enumerable properties
        continue;
      }
      if (s.length) {
        s += ", ";
      }
      if ("value" in desc) {
        s += PropertySource("value", propertyKey, desc.value);
      } else {
        if (desc.get !== void 0) {
          s += PropertySource("get", propertyKey, desc.get);
          if (desc.set !== void 0) s += ", ";
        }
        if (desc.set !== void 0) {
          s += PropertySource("set", propertyKey, desc.set);
        }
      }
    }
    if (depth > 1) {
      return "{" + s + "}";
    }
    return "({" + s + "})";
  } finally {
    %CallFunction(Set_prototype_delete, set, obj);
    depth -= 1;
  }
}

%CreateMethodProperties(%GlobalProperties(), {
  uneval(o) {
    return ToSource(o);
  }
});

%CreateMethodProperties(Object.prototype, {
  toSource() {
    return ObjectToSource(this);
  }
});

const FunctionPrototype = %Intrinsic("FunctionPrototype");
const Function_prototype_toString = FunctionPrototype.toString;

%CreateMethodProperties(FunctionPrototype, {
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
});

const ArrayPrototype = %Intrinsic("ArrayPrototype");
const Array_prototype_join = ArrayPrototype.join;

%CreateMethodProperties(ArrayPrototype, {
  // Duplicated definition from cyclic.js to access shared 'set'.
  join(separator) {
    if (typeof this == 'function' || typeof this == 'object' && this !== null) {
      if (%CallFunction(Set_prototype_has, set, this)) {
        return "";
      }
      %CallFunction(Set_prototype_add, set, this);
    }
    try {
      return %CallFunction(Array_prototype_join, this, separator);
    } finally {
      if (typeof this == 'function' || typeof this == 'object' && this !== null) {
        %CallFunction(Set_prototype_delete, set, this);
      }
    }
  },
  toSource() {
    if (!(typeof this == 'function' || typeof this == 'object' && this !== null)) {
      throw TypeError();
    }
    if (%CallFunction(Set_prototype_has, set, this)) {
      return "[]";
    }
    %CallFunction(Set_prototype_add, set, this);
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
      %CallFunction(Set_prototype_delete, set, this);
      depth -= 1;
    }
  }
});

const String_prototype_toString = StringPrototype.toString;

%CreateMethodProperties(StringPrototype, {
  toSource() {
    return `(new String(${ Quote(%CallFunction(String_prototype_toString, this)) }))`;
  }
});

const BooleanPrototype = %Intrinsic("BooleanPrototype");
const Boolean_prototype_toString = BooleanPrototype.toString;

%CreateMethodProperties(BooleanPrototype, {
  toSource() {
    return `(new Boolean(${ %CallFunction(Boolean_prototype_toString, this) }))`;
  }
});

%CreateMethodProperties(NumberPrototype, {
  toSource() {
    return `(new Number(${ %CallFunction(Number_prototype_toString, this) }))`;
  }
});

%CreateMethodProperties(%Intrinsic("Math"), {
  toSource() {
    return "Math";
  }
});

const DatePrototype = %Intrinsic("DatePrototype");
const Date_prototype_toString = DatePrototype.toString;

%CreateMethodProperties(DatePrototype, {
  toSource() {
    return `(new Date(${ %CallFunction(Date_prototype_toString, this) }))`;
  }
});

const RegExpPrototype = %Intrinsic("RegExpPrototype");
const RegExp_prototype_toString = RegExpPrototype.toString;

%CreateMethodProperties(RegExpPrototype, {
  toSource() {
    return %CallFunction(RegExp_prototype_toString, this);
  }
});

%CreateMethodProperties(%Intrinsic("ErrorPrototype"), {
  toSource() {
    return `(new ${this.name}(${ToSource(this.message)}, ${ToSource(this.fileName)}, ${ToSource(this.lineNumber)}))`;
  }
});

%CreateMethodProperties(%Intrinsic("JSON"), {
  toSource() {
    return "JSON";
  }
});

%CreateMethodProperties(%Intrinsic("Intl"), {
  toSource() {
    return "Intl";
  }
});

const SymbolPrototype = %Intrinsic("SymbolPrototype");
const Symbol_prototype_valueOf = SymbolPrototype.valueOf;

%CreateMethodProperties(SymbolPrototype, {
  toSource() {
    return SymbolToSource(%CallFunction(Symbol_prototype_valueOf, this));
  }
});

for (const name of [
  "Float64x2", "Float32x4",
  "Int32x4", "Int16x8", "Int8x16",
  "Uint32x4", "Uint16x8", "Uint8x16",
  "Bool64x2", "Bool32x4", "Bool16x8", "Bool8x16",
]) {
  const proto = %Intrinsic(`SIMD_${name}Prototype`);
  const toString = proto.toString;
  %CreateMethodProperties(proto, {
    toSource() {
      return %CallFunction(toString, this);
    }
  });
}

})();

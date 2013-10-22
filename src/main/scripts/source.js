/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function ToSource(global) {
"use strict";

const {
  Object, Function, Array, String, Boolean,
  Number, Math, Date, RegExp, Error,
  TypeError, JSON, Intl, WeakSet,
} = global;

const Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty),
      Object_keys = Object.keys,
      Array_isArray = Array.isArray,
      Array_prototype_join = Array.prototype.join;

function Quote(s, qc = '"') {
  var r = "";
  for (var i = 0, len = s.length; i < len; ++i) {
    var c = s.charCodeAt(i);
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
    }
    if (c < 20) {
      r += "\\x" + (c < 0x10 ? "0" : "") + c.toString(16).toUpperCase();
    } else if (c < 0x7F) {
      r += s.charAt(i);
    } else if (c < 0xFF) {
      r += "\\x" + c.toString(16).toUpperCase();
    } else {
      r += "\\u" + (c < 0x1000 ? "0" : "") + c.toString(16).toUpperCase();
    }
  }
  return qc + r + qc;
}

const ASCII_Ident = /^[_$a-zA-Z][_$a-zA-Z0-9]*$/;

function IsInt32(name) {
  return ((name | 0) >= 0 && (name | 0) <= 0x7fffffff && (name | 0) + "" == name);
}

function ToPropertyName(name) {
  if (ASCII_Ident.test(name) || IsInt32(name)) {
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
      return String(Object(o));
    case 'function':
    case 'object':
      if (o !== null) {
        return typeof o.toSource == 'function' ? o.toSource() : ObjectToSource(o);
      }
    default:
      return "null";
  }
}

const weakset = new WeakSet();
var depth = 0;

function ObjectToSource(o) {
  if (o == null) throw new TypeError();
  var obj = Object(o);
  if (weakset.has(obj)) {
    return "{}";
  }
  weakset.add(obj);
  depth += 1;
  try {
    var s = "";
    var names = Object_keys(obj);
    for (var i = 0, len = names.length; i < len; ++i) {
      var name = names[i];
      var desc = Object_getOwnPropertyDescriptor(obj, name);
      if (desc == null) {
        // ignore removed properties
      } else if ('value' in desc) {
        s += ToPropertyName(name) + ":" + ToSource(desc.value);
      } else {
        if (desc.get !== void 0) {
          var fsrc = ToSource(desc.get);
          s += "get " + ToPropertyName(name) + fsrc.substr(fsrc.indexOf('('));
          if (desc.set !== void 0) s += ", ";
        }
        if (desc.set !== void 0) {
          var fsrc = ToSource(desc.set);
          s += "set " + ToPropertyName(name) + fsrc.substr(fsrc.indexOf('('));
        }
      }
      if (i + 1 < len) s += ", ";
    }
    if (depth > 1) {
      return "{" + s + "}";
    }
    return "({" + s + "})";
  } finally {
    weakset.delete(obj);
    depth -= 1;
  }
}

Object.defineProperty(Object.assign(global, {
  uneval(o) {
    return ToSource(o);
  }
}), "uneval", {enumerable: false});

// duplicated definition from array-join.js to access shared 'weakset'
Object.defineProperty(Object.assign(Array.prototype, {
  join(separator) {
    if (typeof this == 'function' || typeof this == 'object' && this !== null) {
      if (weakset.has(this)) {
        return "";
      }
      weakset.add(this);
    }
    try {
      return Array_prototype_join.call(this, separator);
    } finally {
      if (typeof this == 'function' || typeof this == 'object' && this !== null) {
        weakset.delete(this);
      }
    }
  }
}), "join", {enumerable: false});

Object.defineProperty(Object.assign(String.prototype, {
  quote() {
    return Quote(String.prototype.toString.call(this));
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
    if (this.name == "") {
      return "(" + Function.prototype.toString.call(this) + ")";
    }
    return Function.prototype.toString.call(this);
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Array.prototype, {
  toSource() {
    if (!Array_isArray(this)) throw new TypeError();
    if (weakset.has(this)) {
      return "[]";
    }
    weakset.add(this);
    depth += 1;
    try {
      var s = "";
      for (var i = 0, len = this.length; i < len; ++i) {
        if (!Object_hasOwnProperty(this, i)) {
          s += ",";
          if (i + 1 < len) s += " ";
        } else {
          s += ToSource(this[i]);
          if (i + 1 < len) s += ", ";
        }
      }
      return "[" + s + "]";
    } finally {
      weakset.delete(this);
      depth -= 1;
    }
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(String.prototype, {
  toSource() {
    return "(new String(" + Quote(String.prototype.toString.call(this)) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Boolean.prototype, {
  toSource() {
    return "(new Boolean(" + Boolean.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Number.prototype, {
  toSource() {
    return "(new Number(" + Number.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Math, {
  toSource() {
    return "Math";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(Date.prototype, {
  toSource() {
    return "(new Date(" + Date.prototype.valueOf.call(this) + "))";
  }
}), "toSource", {enumerable: false});

Object.defineProperty(Object.assign(RegExp.prototype, {
  toSource() {
    return RegExp.prototype.toString.call(this);
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

})(this);

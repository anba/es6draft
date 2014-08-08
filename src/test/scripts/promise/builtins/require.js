/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

var global = this;

(function Require(global) {
"use strict";

const {
  Function, Object, Map, Set, read,
} = global;

const Object_create = Object.create;

class Module extends null {
  constructor(exports = Object_create(null)){
    this.exports = exports;
  }
}

const modules = new Map();
const builtins = new Set(["assert", "testapi", "sinon"]);

function readFile(path) {
  if (builtins.has(path)) {
    var file = `../builtins/${path}.js`;
  } else {
    var file = path;
    if (!file.endsWith(".js")) {
      file += ".js";
    }
  }
  return read(file);
}

function evaluateModule(module, code) {
  var args = `module, exports, require`;
  var body = `${code};\n return exports;`;
  module.exports = Function(args, body)(module, module.exports, require);
}

function loadModule(path) {
  var code = readFile(path);
  var module = new Module();
  modules.set(path, module);
  return evaluateModule(module, code);
}

function require(path) {
  path = path + "";
  if (!modules.has(path)) {
    loadModule(path);
  }
  return modules.get(path).exports;
}

Object.defineProperty(global, "require", {
  value: require
});

})(this);

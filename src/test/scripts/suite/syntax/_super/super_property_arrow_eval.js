/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError, assertInstanceOf, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");


var home = {};

// 14.1 FunctionDeclaration
function fdecl() {
(() => {
  if (false) { super.add_super_binding; }
  eval("super.property_from_eval");
})();
}
fdecl.toMethod(home)();

// 14.1 FunctionExpression
var fexpr = function() {
(() => {
  if (false) { super.add_super_binding; }
  eval("super.property_from_eval");
})();
};
fexpr.toMethod(home)();

// 14.3 Method Definitions [Method]
var obj = {
  m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.m();

var obj = class {
  m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.prototype.m();

var obj = class {
  static m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.m();

// 14.3 Method Definitions [ConstructorMethod]
var obj = class {
  constructor() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
new obj();

var obj = class extends class {} {
  constructor() {
    super();
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
new obj();

// 14.3 Method Definitions [Getter]
var obj = {
  get x() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").get();

var obj = class {
  get x() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj.prototype, "x").get();

var obj = class {
  static get x() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").get();

// 14.3 Method Definitions [Setter]
var obj = {
  set x(_) {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").set();

var obj = class {
  set x(_) {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj.prototype, "x").set();

var obj = class {
  static set x(_) {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").set();

// 14.4 GeneratorDeclaration
function* gdecl() {
(() => {
  if (false) { super.add_super_binding; }
  eval("super.property_from_eval");
})();
}
gdecl.toMethod(home)().next();

// 14.4 GeneratorExpression
var gexpr = function*() {
(() => {
  if (false) { super.add_super_binding; }
  eval("super.property_from_eval");
})();
};
gexpr.toMethod(home)().next();

// 14.4 GeneratorMethod
var obj = {
  *m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.m().next();

var obj = class {
  *m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.prototype.m().next();

var obj = class {
  static *m() {
(() => {
    if (false) { super.add_super_binding; }
    eval("super.property_from_eval");
})();
  }
};
obj.m().next();

// 15.1 Scripts
assertThrows(SyntaxError, () => evalScript(`
(() => {
  eval("super.property_from_eval");
})();
`));

// 15.2 Modules
System
.define("parse-super-property-arrow-eval", `
(() => {
  eval("super.property_from_eval");
})();
`)
.then(() => fail `no SyntaxError`, e => assertInstanceOf(SyntaxError, e))
.catch(reportFailure);

/*
 * Copyright (c) André Bargull
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


// 14.1 FunctionDeclaration
function fdecl() {
  eval("super()");
}
assertThrows(SyntaxError, () => fdecl());
assertThrows(SyntaxError, () => new fdecl());

// 14.1 FunctionExpression
var fexpr = function() {
  eval("super()");
};
assertThrows(SyntaxError, () => fexpr());
assertThrows(SyntaxError, () => new fexpr());

// 14.3 Method Definitions [Method]
var obj = {
  m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.m());
assertThrows(TypeError, () => new obj.m());

var obj = class {
  m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.prototype.m());
assertThrows(TypeError, () => new obj.prototype.m());

var obj = class {
  static m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.m());
assertThrows(TypeError, () => new obj.m());

// 14.3 Method Definitions [ConstructorMethod]
var obj = class {
  constructor() {
    eval("super()");
  }
};
assertThrows(TypeError, () => obj());
assertThrows(SyntaxError, () => new obj());

var obj = class extends class {} {
  constructor() {
    eval("super()");
  }
};
assertThrows(TypeError, () => obj());
new obj();

// 14.3 Method Definitions [Getter]
var obj = {
  get x() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj, "x").get());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").get)());

var obj = class {
  get x() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj.prototype, "x").get());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj.prototype, "x").get)());

var obj = class {
  static get x() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj, "x").get());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").get)());

// 14.3 Method Definitions [Setter]
var obj = {
  set x(_) {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj, "x").set());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").set)());

var obj = class {
  set x(_) {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj.prototype, "x").set());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj.prototype, "x").set)());

var obj = class {
  static set x(_) {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => Object.getOwnPropertyDescriptor(obj, "x").set());
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").set)());

// 14.4 GeneratorDeclaration
function* gdecl() {
  eval("super()");
}
assertThrows(SyntaxError, () => gdecl().next());
assertThrows(TypeError, () => new gdecl().next());

// 14.4 GeneratorExpression
var gexpr = function*() {
  eval("super()");
};
assertThrows(SyntaxError, () => gexpr().next());
assertThrows(TypeError, () => new gexpr().next());

// 14.4 GeneratorMethod
var obj = {
  *m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.m().next());
assertThrows(TypeError, () => new obj.m().next());

var obj = class {
  *m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.prototype.m().next());
assertThrows(TypeError, () => new obj.prototype.m().next());

var obj = class {
  static *m() {
    eval("super()");
  }
};
assertThrows(SyntaxError, () => obj.m().next());
assertThrows(TypeError, () => new obj.m().next());

// 15.1 Scripts
assertThrows(SyntaxError, () => evalScript(`
  eval("super()");
`));

// 15.2 Modules
System
.define("parse-super-eval", `
  eval("super()");
`)
.then(() => fail `no SyntaxError`, e => assertInstanceOf(SyntaxError, e))
.catch(reportFailure);

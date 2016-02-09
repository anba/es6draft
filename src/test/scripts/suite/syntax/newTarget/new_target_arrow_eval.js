/*
 * Copyright (c) 2012-2016 André Bargull
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
(() => {
  eval("new.target");
})();
}
fdecl();
new fdecl();

// 14.1 FunctionExpression
var fexpr = function() {
(() => {
  eval("new.target");
})();
};
fexpr();
new fexpr();

// 14.3 Method Definitions [Method]
var obj = {
  m() {
(() => {
    eval("new.target");
})();
  }
};
obj.m();
assertThrows(TypeError, () => new obj.m());

var obj = class {
  m() {
(() => {
    eval("new.target");
})();
  }
};
obj.prototype.m();
assertThrows(TypeError, () => new obj.prototype.m());

var obj = class {
  static m() {
(() => {
    eval("new.target");
})();
  }
};
obj.m();
assertThrows(TypeError, () => new obj.m());

// 14.3 Method Definitions [ConstructorMethod]
var obj = class {
  constructor() {
(() => {
    eval("new.target");
})();
  }
};
assertThrows(TypeError, () => obj());
new obj();

var obj = class extends class {} {
  constructor() {
    super();
(() => {
    eval("new.target");
})();
  }
};
assertThrows(TypeError, () => obj());
new obj();

// 14.3 Method Definitions [Getter]
var obj = {
  get x() {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").get();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").get)());

var obj = class {
  get x() {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj.prototype, "x").get();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj.prototype, "x").get)());

var obj = class {
  static get x() {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").get();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").get)());

// 14.3 Method Definitions [Setter]
var obj = {
  set x(_) {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").set();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").set)());

var obj = class {
  set x(_) {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj.prototype, "x").set();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj.prototype, "x").set)());

var obj = class {
  static set x(_) {
(() => {
    eval("new.target");
})();
  }
};
Object.getOwnPropertyDescriptor(obj, "x").set();
assertThrows(TypeError, () => new (Object.getOwnPropertyDescriptor(obj, "x").set)());

// 14.4 GeneratorDeclaration
function* gdecl() {
(() => {
  eval("new.target");
})();
}
gdecl().next();
assertThrows(TypeError, () => new gdecl());

// 14.4 GeneratorExpression
var gexpr = function*() {
(() => {
  eval("new.target");
})();
};
gexpr().next();
assertThrows(TypeError, () => new gexpr());

// 14.4 GeneratorMethod
var obj = {
  *m() {
(() => {
    eval("new.target");
})();
  }
};
obj.m().next();
assertThrows(TypeError, () => new obj.m());

var obj = class {
  *m() {
(() => {
    eval("new.target");
})();
  }
};
obj.prototype.m().next();
assertThrows(TypeError, () => new obj.prototype.m());

var obj = class {
  static *m() {
(() => {
    eval("new.target");
})();
  }
};
() => obj.m().next();
assertThrows(TypeError, () => new obj.m());

// 15.1 Scripts
assertThrows(SyntaxError, () => evalScript(`
(() => {
  eval("new.target");
})();
`));

// 15.2 Modules
System
.define("parse-new-target-arrow-eval", `
(() => {
  eval("new.target");
})();
`)
.then(() => fail `no SyntaxError`, e => assertInstanceOf(SyntaxError, e))
.catch(reportFailure);

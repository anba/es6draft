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
assertSyntaxError(`
function fdecl() {
  super();
}
`);

// 14.1 FunctionExpression
assertSyntaxError(`
var fexpr = function() {
  super();
};
`);

// 14.3 Method Definitions [Method]
assertSyntaxError(`
var obj = {
  m() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  m() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static m() {
    super();
  }
};
`);

// 14.3 Method Definitions [ConstructorMethod]
assertSyntaxError(`
var obj = class {
  constructor() {
    super();
  }
};
`);

var obj = class extends class {} {
  constructor() {
    super();
  }
};
assertThrows(TypeError, () => obj());
new obj();

// 14.3 Method Definitions [Getter]
assertSyntaxError(`
var obj = {
  get x() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  get x() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static get x() {
    super();
  }
};
`);

// 14.3 Method Definitions [Setter]
assertSyntaxError(`
var obj = {
  set x(_) {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  set x(_) {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static set x(_) {
    super();
  }
};
`);

// 14.4 GeneratorDeclaration
assertSyntaxError(`
function* gdecl() {
  super();
}
`);

// 14.4 GeneratorExpression
assertSyntaxError(`
var gexpr = function*() {
  super();
};
`);

// 14.4 GeneratorMethod
assertSyntaxError(`
var obj = {
  *m() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  *m() {
    super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static *m() {
    super();
  }
};
`);

// 15.1 Scripts
assertThrows(SyntaxError, () => evalScript(`
  super();
`));

// 15.2 Modules
System
.define("parse-super", `
  super();
`)
.then(() => fail `no SyntaxError`, e => assertInstanceOf(SyntaxError, e))
.catch(reportFailure);

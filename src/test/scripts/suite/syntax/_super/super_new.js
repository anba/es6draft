/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError
} = Assert;

function SuperConstructor() { }

// 14.1 FunctionDeclaration
function fdecl() {
  new super();
}
Object.setPrototypeOf(fdecl, SuperConstructor);
assertThrows(TypeError, () => fdecl());
new fdecl();

// 14.1 FunctionExpression
var fexpr = function() {
  new super();
};
Object.setPrototypeOf(fexpr, SuperConstructor);
assertThrows(TypeError, () => fexpr());
new fexpr();

// 14.3 Method Definitions [Method]
assertSyntaxError(`
var obj = {
  m() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  m() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static m() {
    new super();
  }
};
`);

// 14.3 Method Definitions [ConstructorMethod]
var obj = class {
  constructor() {
    new super();
  }
};
Object.setPrototypeOf(obj, SuperConstructor);
assertThrows(TypeError, () => obj());
new obj();

var obj = class extends class {} {
  constructor() {
    super();
    new super();
  }
};
assertThrows(TypeError, () => obj());
new obj();

// 14.3 Method Definitions [Getter]
assertSyntaxError(`
var obj = {
  get x() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  get x() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static get x() {
    new super();
  }
};
`);

// 14.3 Method Definitions [Setter]
assertSyntaxError(`
var obj = {
  set x(_) {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  set x(_) {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static set x(_) {
    new super();
  }
};
`);

// 14.4 GeneratorDeclaration
assertSyntaxError(`
function* gdecl() {
  new super();
}
`);

// 14.4 GeneratorExpression
assertSyntaxError(`
var gexpr = function*() {
  new super();
};
`);

// 14.4 GeneratorMethod
assertSyntaxError(`
var obj = {
  *m() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  *m() {
    new super();
  }
};
`);

assertSyntaxError(`
var obj = class {
  static *m() {
    new super();
  }
};
`);

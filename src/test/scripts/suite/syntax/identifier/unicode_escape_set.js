/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError, assertSame
} = Assert;

// `\u0073et` is "set"
assertSame(`\u0073et`, "set");

const names = [
  "set",
  "\\u0073et",
];

// "set" is a valid BindingIdentifier / IdentifierReference / Label in non-strict mode
for (let name of names) {
  Function(`
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "set" is a valid BindingIdentifier / IdentifierReference / Label in strict mode
for (let name of names) {
  Function(`
    "use strict";
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "set" in class/object literal must not contain any Unicode escape sequences
for (let name of names) {
  let code = `class C { ${name} m(v) {} }`;
  if (name === "set") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `class C { static ${name} m(v) {} }`;
  if (name === "set") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `({ ${name} m(v) {} })`;
  if (name === "set") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}

// "set" is allowed to contain Unicode escape sequences when not used as setter
for (let name of names) {
  let code = `class C { ${name}(v) {} }`;
  Function(code);
}
for (let name of names) {
  let code = `class C { static ${name}(v) {} }`;
  Function(code);
}
for (let name of names) {
  let code = `({ ${name}() {} })`;
  Function(code);
}

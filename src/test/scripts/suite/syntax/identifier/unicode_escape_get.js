/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertSame
} = Assert;

// `\u0067et` is "get"
assertSame(`\u0067et`, "get");

const names = [
  "get",
  "\\u0067et",
];

// "get" is a valid BindingIdentifier / IdentifierReference / Label in non-strict mode
for (let name of names) {
  Function(`
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "get" is a valid BindingIdentifier / IdentifierReference / Label in strict mode
for (let name of names) {
  Function(`
    "use strict";
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "get" in class/object literal must not contain any Unicode escape sequences
for (let name of names) {
  let code = `class C { ${name} m() {} }`;
  if (name === "get") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `class C { static ${name} m() {} }`;
  if (name === "get") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `({ ${name} m() {} })`;
  if (name === "get") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}

// "get" is allowed to contain Unicode escape sequences when not used as getter
for (let name of names) {
  let code = `class C { ${name}() {} }`;
  Function(code);
}
for (let name of names) {
  let code = `class C { static ${name}() {} }`;
  Function(code);
}
for (let name of names) {
  let code = `({ ${name}() {} })`;
  Function(code);
}

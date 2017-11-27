/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertSame
} = Assert;

// `\u006f\u0066` is "of"
assertSame(`\u006ff`, "of");
assertSame(`o\u0066`, "of");
assertSame(`\u006f\u0066`, "of");

const names = [
  "of",
  "\\u006ff",
  "o\\u0066",
  "\\u006f\\u0066",
];

// "of" is a valid BindingIdentifier / IdentifierReference / Label in non-strict mode
for (let name of names) {
  Function(`
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "of" is a valid BindingIdentifier / IdentifierReference / Label in strict mode
for (let name of names) {
  Function(`
    "use strict";
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "of" in for-of must not contain any Unicode escape sequences
for (let name of names) {
  let code = `for (x ${name} y) {}`;
  if (name === "of") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `for (var x ${name} y) {}`;
  if (name === "of") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}
for (let name of names) {
  let code = `for (let x ${name} y) {}`;
  if (name === "of") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}

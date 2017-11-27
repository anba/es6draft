/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertSame
} = Assert;

// `\u0073tatic` is "static"
assertSame(`\u0073tatic`, "static");

const names = [
  "static",
  "\\u0073tatic",
];

// "static" is a valid BindingIdentifier / IdentifierReference / Label in non-strict mode
for (let name of names) {
  Function(`
    var ${name};
    { let ${name}; }
    ${name};
    ${name}: ;
  `);
}

// "static" is not a valid BindingIdentifier / IdentifierReference / Label in strict mode
for (let name of names) {
  assertSyntaxError(`
    "use strict";
    var ${name};
  `);
  assertSyntaxError(`
    "use strict";
    { let ${name}; }
  `);
  assertSyntaxError(`
    "use strict";
    ${name};
  `);
  assertSyntaxError(`
    "use strict";
    ${name}: ;
  `);
}

// "static" in class must not contain any Unicode escape sequences
for (let name of names) {
  let code = `class C { ${name} m() {} }`;
  if (name === "static") {
    Function(code);
  } else {
    assertSyntaxError(code);
  }
}

// "static" is allowed to contain Unicode escape sequences when not used as strict-modifier
for (let name of names) {
  let code = `class C { ${name}() {} }`;
  Function(code);
}
for (let name of names) {
  let code = `class C { static ${name}() {} }`;
  Function(code);
}

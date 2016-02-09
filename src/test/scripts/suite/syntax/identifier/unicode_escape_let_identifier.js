/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

const bindings = {
  letIsBoundName: [
    "let", "[let]", "{let}", "{let: let}", "{not_let: let}", "[{let}]", "[{not_let: let}]",  "{not_let: [let]}",
  ],
  letIsntBoundName: [
    "{let: not_let}", "[a = let()]",
  ]
};

function toStr(number, radix, pad) {
  let s = number.toString(radix);
  return "0".repeat(pad - s.length) + s;
}

const transformer = [
  function noEscape(name) {
    return name;
  },
  function escapeFirstChar(name) {
    return `\\u${toStr(name.charCodeAt(0), 16, 4)}${name.substring(1)}`
  },
  function escapeSecondChar(name) {
    return `${name.substring(0, 1)}\\u${toStr(name.charCodeAt(1), 16, 4)}${name.substring(2)}`
  },
].map(fn => (n => n.replace("let", fn)));

for (let t of transformer) {
  // 12.1.4.2.1  Static Semantics: Early Errors
  // It is a Syntax Error if the BoundNames of ForBinding contains "let".
  for (let binding of bindings.letIsBoundName) {
    binding = t(binding);
    assertSyntaxError(`[for (${binding} of []) 0];`);
    assertSyntaxError(`(for (${binding} of []) 0);`);
  }
  for (let binding of bindings.letIsntBoundName) {
    binding = t(binding);
    Function(`[for (${binding} of []) 0];`);
    Function(`(for (${binding} of []) 0);`);
  }
}

for (let t of transformer) {
  // 13.2.1.1  Static Semantics: Early Errors
  // It is a Syntax Error if the BoundNames of LexicalBinding contains "let".
  for (let binding of bindings.letIsBoundName) {
    binding = t(binding);
    assertSyntaxError(`let ${binding} = {};`);
    assertSyntaxError(`const ${binding} = {};`);
    assertSyntaxError(`for (let ${binding} = {};;);`);
    assertSyntaxError(`for (const ${binding} = {};;);`);
    assertSyntaxError(`for (let ${binding} in {});`);
    assertSyntaxError(`for (const ${binding} in {});`);
    assertSyntaxError(`for (let ${binding} of {});`);
    assertSyntaxError(`for (const ${binding} of {});`);
  }
  for (let binding of bindings.letIsntBoundName) {
    binding = t(binding);
    Function(`let ${binding} = {};`);
    Function(`const ${binding} = {};`);
    Function(`for (let ${binding} = {};;);`);
    Function(`for (const ${binding} = {};;);`);
    Function(`for (let ${binding} in {});`);
    Function(`for (const ${binding} in {});`);
    Function(`for (let ${binding} of {});`);
    Function(`for (const ${binding} of {});`);
  }
}

for (let t of transformer) {
  // "let" is always allowed as binding-identifier in VariableDeclaration
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    binding = t(binding);
    Function(`var ${binding} = {};`);
    Function(`for (var ${binding} = {};;);`);
    Function(`for (var ${binding} in {});`);
    Function(`for (var ${binding} of {});`);
  }
}

for (let t of transformer) {
  // "let" is always allowed as binding-identifier in CatchParameter
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    binding = t(binding);
    Function(`try {} catch (${binding}) {}`);
  }
}

for (let t of transformer) {
  // "let" is always allowed as binding-identifier in FormalParameters
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    binding = t(binding);
    Function(`function f(${binding}) {}`);
    Function(`function* f(${binding}) {}`);
    Function(`((${binding}) => {});`);
  }
}

for (let t of transformer) {
  // "let" is always allowed as binding-identifier in StrictFormalParameters
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    binding = t(binding);
    Function(`((${binding}) => {});`);
    Function(`({x(${binding}) {}});`);
    Function(`({*x(${binding}) {}});`);
  }
}

for (let t of transformer) {
  // "let" is always allowed as binding-identifier in PropertySetParameterList
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    binding = t(binding);
    Function(`({set x(${binding}) {}});`);
  }
}

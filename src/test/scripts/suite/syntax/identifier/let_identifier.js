/*
 * Copyright (c) 2012-2014 André Bargull
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

{
  // 12.1.4.2.1  Static Semantics: Early Errors
  // It is a Syntax Error if the BoundNames of ForBinding contains "let".
  for (let binding of bindings.letIsBoundName) {
    assertSyntaxError(`[for (${binding} of []) 0];`);
  }
  for (let binding of bindings.letIsntBoundName) {
    Function(`[for (${binding} of []) 0];`);
  }
}

{
  // 13.2.1.1  Static Semantics: Early Errors
  // It is a Syntax Error if the BoundNames of LexicalBinding contains "let".
  for (let binding of bindings.letIsBoundName) {
    assertSyntaxError(`let ${binding} = {};`);
    assertSyntaxError(`const ${binding} = {};`);
  }
  for (let binding of bindings.letIsntBoundName) {
    Function(`let ${binding} = {};`);
    Function(`const ${binding} = {};`);
  }
}

{
  // "let" is always allowed as binding-identifier in VariableDeclaration
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    Function(`var ${binding} = {};`);
  }
}

{
  // "let" is always allowed as binding-identifier in CatchParameter
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    Function(`try {} catch(${binding}) {}`);
  }
}

{
  // "let" is always allowed as binding-identifier in FormalParameters
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    Function(`function f(${binding}) {}`);
    Function(`function* f(${binding}) {}`);
    Function(`((${binding}) => {});`);
  }
}

{
  // "let" is always allowed as binding-identifier in StrictFormalParameters
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    Function(`((${binding}) => {});`);
    Function(`({x(${binding}) {}});`);
    Function(`({*x(${binding}) {}});`);
  }
}

{
  // "let" is always allowed as binding-identifier in PropertySetParameterList
  for (let binding of [...bindings.letIsBoundName, ...bindings.letIsntBoundName]) {
    Function(`({set x(${binding}) {}});`);
  }
}

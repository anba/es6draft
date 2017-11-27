/*
 * Copyright (c) André Bargull
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
    assertSyntaxError(`(for (${binding} of []) 0);`);
  }
  for (let binding of bindings.letIsntBoundName) {
    Function(`[for (${binding} of []) 0];`);
    Function(`(for (${binding} of []) 0);`);
  }
}

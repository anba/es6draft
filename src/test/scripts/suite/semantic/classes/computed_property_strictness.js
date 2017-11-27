/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// ClassDeclaration and ClassExpression is always strict code, that means computed property names
// within the class definitions also need to be evaluated under strict semantics.

// Strict eval semantics
function strictEvalSemantics() {
  // var-declaration within strict code is created in separate lexical environment,
  // other lexical environments cannot access the binding
  new class {
    [(eval("var id=0"), "a")]() { }

    // Assert not accessible in class definition
    [(assertSame("undefined", typeof id), "b")]() { }
    [(assertSame("undefined", eval("typeof id")), "c")]() { }
  };
  // Assert not accessible in surrounding function
  assertSame("undefined", typeof id);
  assertSame("undefined", eval("typeof id"));
}
strictEvalSemantics();

// Strict identifier resolution
assertThrows(ReferenceError, () => new class {[unresolvedIdentifier](){ }});

// Strict property assignment
assertThrows(TypeError, () => new class {[Object.freeze({}).prop = 0](){ }});

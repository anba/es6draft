/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// ClassDeclaration and ClassExpression is always strict code
// - test strictness in ClassHeritage

// 11.6.2.2 Future Reserved Words (Strict Mode)
const FutureReservedWordsStrict =
`
implements  let      private    public
interface   package  protected  static
`.trim().split(/\s+/);

const strictModeErrors = [
  "(delete id)",
  "function(eval) {}",
  "function(arguments) {}",
  "(eval = 0)",
  "(arguments = 0)",
  "yield",
  "(yield)",
  ...FutureReservedWordsStrict,
  ...FutureReservedWordsStrict.map(x => `(${x})`),
];

// ClassDeclaration
for (let error of strictModeErrors) {
  assertSyntaxError(`class C extends ${error} {}`);
}

// ClassExpression
for (let error of strictModeErrors) {
  assertSyntaxError(`(class C extends ${error} {})`);
  assertSyntaxError(`(class extends ${error} {})`);
}

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// ClassDeclaration and ClassExpression is always strict code
// - test strictness in BindingIdentifier of ClassDeclaration/ClassExpression

// 11.6.2.2 Future Reserved Words (Strict Mode)
const FutureReservedWordsStrict =
`
implements  let      private    public
interface   package  protected  static
`.trim().split(/\s+/);

const RestrictedBindingIdentifierStrict =
`
arguments
eval
yield
`.trim().split(/\s+/);

// ClassDeclaration
for (let w of [...FutureReservedWordsStrict, ...RestrictedBindingIdentifierStrict]) {
  assertSyntaxError(`class ${w} {}`);
}

// ClassExpression
for (let w of [...FutureReservedWordsStrict, ...RestrictedBindingIdentifierStrict]) {
  assertSyntaxError(`(class ${w} {})`);
}

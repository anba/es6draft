/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// Lookahead restriction for 'let' in for-of statement

// Valid syntax
Function(`for (x of o) ;`);
Function(`for (of of of) ;`);
Function(`for (of of let) ;`);
Function(`for (let x of o) ;`);
Function(`for (let of of o) ;`);
Function(`for (let of of of) ;`);
Function(`for (let of of let) ;`);

// Lookahead restriction
assertSyntaxError(`for (let.x of o) ;`);
assertSyntaxError(`for (let of o) ;`);

// BoundNames must not include "let"
assertSyntaxError(`for (let let of o) ;`);
assertSyntaxError(`for (let let of of) ;`);
assertSyntaxError(`for (let [let] of of) ;`);
assertSyntaxError(`for (let {let} of of) ;`);

// Invalid ArrayBindingPattern
assertSyntaxError(`for (let["x"] of o) ;`);

// Valid BindingPatterns
Function(`for (let[x] of o) ;`);
Function(`for (let{x} of o) ;`);

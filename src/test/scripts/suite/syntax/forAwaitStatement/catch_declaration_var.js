/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Catch variable is BindingIdentifier
assertSyntaxError(`async function f() { try {} catch (e) { for await(var e of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch (e) { for await(var [e] of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch (e) { for await(var {e} of []) ; } }`);


// Catch variable is ArrayBindingPattern
assertSyntaxError(`async function f() { try {} catch ([e]) { for await(var e of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch ([e]) { for await(var [e] of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch ([e]) { for await(var {e} of []) ; } }`);


// Catch variable is ObjectBindingPattern
assertSyntaxError(`async function f() { try {} catch ({e}) { for await(var e of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch ({e}) { for await(var [e] of []) ; } }`);
assertSyntaxError(`async function f() { try {} catch ({e}) { for await(var {e} of []) ; } }`);

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

assertSyntaxError(`function f(p = do { return; }){}`);
assertSyntaxError(`function f() { (p = do { return; }) => {}; }`);

assertSyntaxError(`function* g(p = do { return; }){}`);
assertSyntaxError(`function* g() { (p = do { return; }) => {}; }`);

assertSyntaxError(`async function f(p = do { return; }){}`);
assertSyntaxError(`async function f() { (p = do { return; }) => {}; }`);

assertSyntaxError(`(for (x of []) do { return; })`);
assertSyntaxError(`function f() { (for (x of []) do { return; }); }`);

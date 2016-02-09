/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Add early error for functions with non-simple parameter list and use-strict
// https://github.com/tc39/ecma262/pull/172

assertSyntaxError(`function f(a = 0) { "use strict"; }`);
assertSyntaxError(`function f([a]) { "use strict"; }`);
assertSyntaxError(`function f({a}) { "use strict"; }`);
assertSyntaxError(`function f(...a) { "use strict"; }`);
assertSyntaxError(`function f(...[a]) { "use strict"; }`);
assertSyntaxError(`function f(...{a}) { "use strict"; }`);

assertSyntaxError(`"use strict"; function f(a = 0) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f([a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f({a}) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f(...a) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f(...[a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f(...{a}) { "use strict"; }`);

assertSyntaxError(`function* f(a = 0) { "use strict"; }`);
assertSyntaxError(`function* f([a]) { "use strict"; }`);
assertSyntaxError(`function* f({a}) { "use strict"; }`);
assertSyntaxError(`function* f(...a) { "use strict"; }`);
assertSyntaxError(`function* f(...[a]) { "use strict"; }`);
assertSyntaxError(`function* f(...{a}) { "use strict"; }`);

assertSyntaxError(`"use strict"; function* f(a = 0) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* f([a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* f({a}) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* f(...a) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* f(...[a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* f(...{a}) { "use strict"; }`);

assertSyntaxError(`async function f(a = 0) { "use strict"; }`);
assertSyntaxError(`async function f([a]) { "use strict"; }`);
assertSyntaxError(`async function f({a}) { "use strict"; }`);
assertSyntaxError(`async function f(...a) { "use strict"; }`);
assertSyntaxError(`async function f(...[a]) { "use strict"; }`);
assertSyntaxError(`async function f(...{a}) { "use strict"; }`);

assertSyntaxError(`"use strict"; async function f(a = 0) { "use strict"; }`);
assertSyntaxError(`"use strict"; async function f([a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; async function f({a}) { "use strict"; }`);
assertSyntaxError(`"use strict"; async function f(...a) { "use strict"; }`);
assertSyntaxError(`"use strict"; async function f(...[a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; async function f(...{a}) { "use strict"; }`);

assertSyntaxError(`({f(a = 0) { "use strict"; }});`);
assertSyntaxError(`({f([a]) { "use strict"; }});`);
assertSyntaxError(`({f({a}) { "use strict"; }});`);
assertSyntaxError(`({f(...a) { "use strict"; }});`);
assertSyntaxError(`({f(...[a]) { "use strict"; }});`);
assertSyntaxError(`({f(...{a}) { "use strict"; }});`);

assertSyntaxError(`"use strict"; ({f(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({f([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({f({a}) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({f(...a) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({f(...[a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({f(...{a}) { "use strict"; }});`);

assertSyntaxError(`({*f(a = 0) { "use strict"; }});`);
assertSyntaxError(`({*f([a]) { "use strict"; }});`);
assertSyntaxError(`({*f({a}) { "use strict"; }});`);
assertSyntaxError(`({*f(...a) { "use strict"; }});`);
assertSyntaxError(`({*f(...[a]) { "use strict"; }});`);
assertSyntaxError(`({*f(...{a}) { "use strict"; }});`);

assertSyntaxError(`"use strict"; ({*f(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({*f([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({*f({a}) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({*f(...a) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({*f(...[a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({*f(...{a}) { "use strict"; }});`);

assertSyntaxError(`({async f(a = 0) { "use strict"; }});`);
assertSyntaxError(`({async f([a]) { "use strict"; }});`);
assertSyntaxError(`({async f({a}) { "use strict"; }});`);
assertSyntaxError(`({async f(...a) { "use strict"; }});`);
assertSyntaxError(`({async f(...[a]) { "use strict"; }});`);
assertSyntaxError(`({async f(...{a}) { "use strict"; }});`);

assertSyntaxError(`"use strict"; ({async f(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({async f([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({async f({a}) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({async f(...a) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({async f(...[a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({async f(...{a}) { "use strict"; }});`);

assertSyntaxError(`({set f(a = 0) { "use strict"; }});`);
assertSyntaxError(`({set f([a]) { "use strict"; }});`);
assertSyntaxError(`({set f({a}) { "use strict"; }});`);

assertSyntaxError(`"use strict"; ({set f(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({set f([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({set f({a}) { "use strict"; }});`);

assertSyntaxError(`class C {f(a = 0) { "use strict"; }}`);
assertSyntaxError(`class C {f([a]) { "use strict"; }}`);
assertSyntaxError(`class C {f({a}) { "use strict"; }}`);
assertSyntaxError(`class C {f(...a) { "use strict"; }}`);
assertSyntaxError(`class C {f(...[a]) { "use strict"; }}`);
assertSyntaxError(`class C {f(...{a}) { "use strict"; }}`);

assertSyntaxError(`class C {*f(a = 0) { "use strict"; }}`);
assertSyntaxError(`class C {*f([a]) { "use strict"; }}`);
assertSyntaxError(`class C {*f({a}) { "use strict"; }}`);
assertSyntaxError(`class C {*f(...a) { "use strict"; }}`);
assertSyntaxError(`class C {*f(...[a]) { "use strict"; }}`);
assertSyntaxError(`class C {*f(...{a}) { "use strict"; }}`);

assertSyntaxError(`class C {async f(a = 0) { "use strict"; }}`);
assertSyntaxError(`class C {async f([a]) { "use strict"; }}`);
assertSyntaxError(`class C {async f({a}) { "use strict"; }}`);
assertSyntaxError(`class C {async f(...a) { "use strict"; }}`);
assertSyntaxError(`class C {async f(...[a]) { "use strict"; }}`);
assertSyntaxError(`class C {async f(...{a}) { "use strict"; }}`);

assertSyntaxError(`class C {set f(a = 0) { "use strict"; }}`);
assertSyntaxError(`class C {set f([a]) { "use strict"; }}`);
assertSyntaxError(`class C {set f({a}) { "use strict"; }}`);

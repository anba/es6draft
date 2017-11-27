/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const assertSyntaxError = Assert.assertSyntaxError;

/* function declarations and function expressions */

// 'yield' in non-strict function declarations
(function() {

// generator comprehensions
function f(){ (for(yield of yield) yield) }
function f(){ (for(yield of yield) yield +2) }
function f(){ (for(a of yield) yield) }
function f(){ (for(a of yield) yield +2) }
function f(){ (for(yield of b) yield) }
function f(){ (for(yield of b) yield +2) }
function f(){ (for(yield of yield) c) }
function f(){ (for(yield of yield) c +2) }
function f(){ (for(a of b) yield) }
function f(){ (for(a of b) yield +2) }
assertSyntaxError(`function f(){ (for(a of b) yield d) }`);

})();

// 'yield' in non-strict function expressions
(function() {

// generator comprehensions
(function (){ (for(yield of yield) yield) });
(function (){ (for(yield of yield) yield +2) });
(function (){ (for(a of yield) yield) });
(function (){ (for(a of yield) yield +2) });
(function (){ (for(yield of b) yield) });
(function (){ (for(yield of b) yield +2) });
(function (){ (for(yield of yield) c) });
(function (){ (for(yield of yield) c +2) });
(function (){ (for(a of b) yield) });
(function (){ (for(a of b) yield +2) });
assertSyntaxError(`(function (){ (for(a of b) yield d) });`);

})();

// 'yield' in strict function declarations
(function() {

// generator comprehensions
assertSyntaxError(`function f(){"use strict"; (for(yield of yield) yield) }`);
assertSyntaxError(`function f(){"use strict"; (for(yield of yield) yield +2) }`);
assertSyntaxError(`function f(){"use strict"; (for(a of yield) yield) }`);
assertSyntaxError(`function f(){"use strict"; (for(a of yield) yield +2) }`);
assertSyntaxError(`function f(){"use strict"; (for(yield of b) yield) }`);
assertSyntaxError(`function f(){"use strict"; (for(yield of b) yield +2) }`);
assertSyntaxError(`function f(){"use strict"; (for(yield of yield) c) }`);
assertSyntaxError(`function f(){"use strict"; (for(yield of yield) c +2) }`);
assertSyntaxError(`function f(){"use strict"; (for(a of b) yield) }`);
assertSyntaxError(`function f(){"use strict"; (for(a of b) yield +2) }`);
assertSyntaxError(`function f(){"use strict"; (for(a of b) yield d) }`);

})();

// 'yield' in strict function expressions
(function() {

// generator comprehensions
assertSyntaxError(`(function (){"use strict"; (for(yield of yield) yield) });`);
assertSyntaxError(`(function (){"use strict"; (for(yield of yield) yield +2) });`);
assertSyntaxError(`(function (){"use strict"; (for(a of yield) yield) });`);
assertSyntaxError(`(function (){"use strict"; (for(a of yield) yield +2) });`);
assertSyntaxError(`(function (){"use strict"; (for(yield of b) yield) });`);
assertSyntaxError(`(function (){"use strict"; (for(yield of b) yield +2) });`);
assertSyntaxError(`(function (){"use strict"; (for(yield of yield) c) });`);
assertSyntaxError(`(function (){"use strict"; (for(yield of yield) c +2) });`);
assertSyntaxError(`(function (){"use strict"; (for(a of b) yield) });`);
assertSyntaxError(`(function (){"use strict"; (for(a of b) yield +2) });`);
assertSyntaxError(`(function (){"use strict"; (for(a of b) yield d) });`);

})();


/* generators declarations and generator expressions */

// 'yield' in non-strict generator declarations
(function() {

// generator comprehensions
assertSyntaxError(`function* g(){ (for(yield of yield) yield) }`);
assertSyntaxError(`function* g(){ (for(yield of yield) yield +2) }`);
assertSyntaxError(`function* g(){ (for(a of yield) yield) }`);
assertSyntaxError(`function* g(){ (for(a of yield) yield +2) }`);
assertSyntaxError(`function* g(){ (for(yield of b) yield) }`);
assertSyntaxError(`function* g(){ (for(yield of b) yield +2) }`);
assertSyntaxError(`function* g(){ (for(yield of yield) c) }`);
assertSyntaxError(`function* g(){ (for(yield of yield) c +2) }`);
assertSyntaxError(`function* g(){ (for(a of b) yield) }`);
assertSyntaxError(`function* g(){ (for(a of b) yield +2) }`);
assertSyntaxError(`function* g(){ (for(a of b) yield d) }`);

})();

// 'yield' in non-strict generator expressions
(function() {

// generator comprehensions
assertSyntaxError(`(function* (){ (for(yield of yield) yield) });`);
assertSyntaxError(`(function* (){ (for(yield of yield) yield +2) });`);
assertSyntaxError(`(function* (){ (for(a of yield) yield) });`);
assertSyntaxError(`(function* (){ (for(a of yield) yield +2) });`);
assertSyntaxError(`(function* (){ (for(yield of b) yield) });`);
assertSyntaxError(`(function* (){ (for(yield of b) yield +2) });`);
assertSyntaxError(`(function* (){ (for(yield of yield) c) });`);
assertSyntaxError(`(function* (){ (for(yield of yield) c +2) });`);
assertSyntaxError(`(function* (){ (for(a of b) yield) });`);
assertSyntaxError(`(function* (){ (for(a of b) yield +2) });`);
assertSyntaxError(`(function* (){ (for(a of b) yield d) });`);

})();

// 'yield' in strict generator declarations
(function() {

// generator comprehensions
assertSyntaxError(`function* g(){"use strict"; (for(yield of yield) yield) }`);
assertSyntaxError(`function* g(){"use strict"; (for(yield of yield) yield +2) }`);
assertSyntaxError(`function* g(){"use strict"; (for(a of yield) yield) }`);
assertSyntaxError(`function* g(){"use strict"; (for(a of yield) yield +2) }`);
assertSyntaxError(`function* g(){"use strict"; (for(yield of b) yield) }`);
assertSyntaxError(`function* g(){"use strict"; (for(yield of b) yield +2) }`);
assertSyntaxError(`function* g(){"use strict"; (for(yield of yield) c) }`);
assertSyntaxError(`function* g(){"use strict"; (for(yield of yield) c +2) }`);
assertSyntaxError(`function* g(){"use strict"; (for(a of b) yield) }`);
assertSyntaxError(`function* g(){"use strict"; (for(a of b) yield +2) }`);
assertSyntaxError(`function* g(){"use strict"; (for(a of b) yield d) }`);

})();

// 'yield' in strict generator expressions
(function() {

// generator comprehensions
assertSyntaxError(`(function* g(){ (for(yield of yield) yield) });`);
assertSyntaxError(`(function* g(){ (for(yield of yield) yield +2) });`);
assertSyntaxError(`(function* g(){ (for(a of yield) yield) });`);
assertSyntaxError(`(function* g(){ (for(a of yield) yield +2) });`);
assertSyntaxError(`(function* g(){ (for(yield of b) yield) });`);
assertSyntaxError(`(function* g(){ (for(yield of b) yield +2) });`);
assertSyntaxError(`(function* g(){ (for(yield of yield) c) });`);
assertSyntaxError(`(function* g(){ (for(yield of yield) c +2) });`);
assertSyntaxError(`(function* g(){ (for(a of b) yield) });`);
assertSyntaxError(`(function* g(){ (for(a of b) yield +2) });`);
assertSyntaxError(`(function* g(){ (for(a of b) yield d) });`);

})();


/* object methods */

// TODO: plain object literals?
// TODO: computed keys?
// TODO: get/set methods?

// 'yield' in non-strict normal methods
(function() {

// generator comprehensions
({ f(){ (for(yield of yield) yield) } });
({ f(){ (for(yield of yield) yield +2) } });
({ f(){ (for(a of yield) yield) } });
({ f(){ (for(a of yield) yield +2) } });
({ f(){ (for(yield of b) yield) } });
({ f(){ (for(yield of b) yield +2) } });
({ f(){ (for(yield of yield) c) } });
({ f(){ (for(yield of yield) c +2) } });
({ f(){ (for(a of b) yield) } });
({ f(){ (for(a of b) yield +2) } });
assertSyntaxError(`({ f(){ (for(a of b) yield d) } });`);

})();

// 'yield' in strict normal methods
(function() {

// generator comprehensions
assertSyntaxError(`({ f(){"use strict"; (for(yield of yield) yield) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(yield of yield) yield +2) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(a of yield) yield) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(a of yield) yield +2) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(yield of b) yield) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(yield of b) yield +2) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(yield of yield) c) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(yield of yield) c +2) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(a of b) yield) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(a of b) yield +2) } });`);
assertSyntaxError(`({ f(){"use strict"; (for(a of b) yield d) } });`);

})();

// 'yield' in non-strict generator methods
(function() {

// generator comprehensions
assertSyntaxError(`({ * g(){ (for(yield of yield) yield) } });`);
assertSyntaxError(`({ * g(){ (for(yield of yield) yield +2) } });`);
assertSyntaxError(`({ * g(){ (for(a of yield) yield) } });`);
assertSyntaxError(`({ * g(){ (for(a of yield) yield +2) } });`);
assertSyntaxError(`({ * g(){ (for(yield of b) yield) } });`);
assertSyntaxError(`({ * g(){ (for(yield of b) yield +2) } });`);
assertSyntaxError(`({ * g(){ (for(yield of yield) c) } });`);
assertSyntaxError(`({ * g(){ (for(yield of yield) c +2) } });`);
assertSyntaxError(`({ * g(){ (for(a of b) yield) } });`);
assertSyntaxError(`({ * g(){ (for(a of b) yield +2) } });`);
assertSyntaxError(`({ * g(){ (for(a of b) yield d) } });`);

})();

// 'yield' in strict generator methods
(function() {

// generator comprehensions
assertSyntaxError(`({ * g(){"use strict"; (for(yield of yield) yield) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(yield of yield) yield +2) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(a of yield) yield) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(a of yield) yield +2) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(yield of b) yield) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(yield of b) yield +2) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(yield of yield) c) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(yield of yield) c +2) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(a of b) yield) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(a of b) yield +2) } });`);
assertSyntaxError(`({ * g(){"use strict"; (for(a of b) yield d) } });`);

})();


/* class methods (classes are implicitly strict) */

// TODO: get/set methods?
// TODO: static methods?

// 'yield' in normal methods
(function() {

// generator comprehensions
assertSyntaxError(`class C{ f(){ (for(yield of yield) yield) } }`);
assertSyntaxError(`class C{ f(){ (for(yield of yield) yield +2) } }`);
assertSyntaxError(`class C{ f(){ (for(a of yield) yield) } }`);
assertSyntaxError(`class C{ f(){ (for(a of yield) yield +2) } }`);
assertSyntaxError(`class C{ f(){ (for(yield of b) yield) } }`);
assertSyntaxError(`class C{ f(){ (for(yield of b) yield +2) } }`);
assertSyntaxError(`class C{ f(){ (for(yield of yield) c) } }`);
assertSyntaxError(`class C{ f(){ (for(yield of yield) c +2) } }`);
assertSyntaxError(`class C{ f(){ (for(a of b) yield) } }`);
assertSyntaxError(`class C{ f(){ (for(a of b) yield +2) } }`);
assertSyntaxError(`class C{ f(){ (for(a of b) yield d) } }`);

})();

// 'yield' in generator methods
(function() {

// generator comprehensions
assertSyntaxError(`class C{ * g(){ (for(yield of yield) yield) } }`);
assertSyntaxError(`class C{ * g(){ (for(yield of yield) yield +2) } }`);
assertSyntaxError(`class C{ * g(){ (for(a of yield) yield) } }`);
assertSyntaxError(`class C{ * g(){ (for(a of yield) yield +2) } }`);
assertSyntaxError(`class C{ * g(){ (for(yield of b) yield) } }`);
assertSyntaxError(`class C{ * g(){ (for(yield of b) yield +2) } }`);
assertSyntaxError(`class C{ * g(){ (for(yield of yield) c) } }`);
assertSyntaxError(`class C{ * g(){ (for(yield of yield) c +2) } }`);
assertSyntaxError(`class C{ * g(){ (for(a of b) yield) } }`);
assertSyntaxError(`class C{ * g(){ (for(a of b) yield +2) } }`);
assertSyntaxError(`class C{ * g(){ (for(a of b) yield d) } }`);

})();


/* arrow functions */

// 'yield' in non-strict arrow functions
(function() {

// generator comprehensions
(() => { (for(yield of yield) yield) });
(() => { (for(yield of yield) yield +2) });
(() => { (for(a of yield) yield) });
(() => { (for(a of yield) yield +2) });
(() => { (for(yield of b) yield) });
(() => { (for(yield of b) yield +2) });
(() => { (for(yield of yield) c) });
(() => { (for(yield of yield) c +2) });
(() => { (for(a of b) yield) });
(() => { (for(a of b) yield +2) });
assertSyntaxError(`(() => { (for(a of b) yield d) });`);

})();

// 'yield' in strict arrow functions
(function() {

// generator comprehensions
assertSyntaxError(`(() => {"use strict"; (for(yield of yield) yield) });`);
assertSyntaxError(`(() => {"use strict"; (for(yield of yield) yield +2) });`);
assertSyntaxError(`(() => {"use strict"; (for(a of yield) yield) });`);
assertSyntaxError(`(() => {"use strict"; (for(a of yield) yield +2) });`);
assertSyntaxError(`(() => {"use strict"; (for(yield of b) yield) });`);
assertSyntaxError(`(() => {"use strict"; (for(yield of b) yield +2) });`);
assertSyntaxError(`(() => {"use strict"; (for(yield of yield) c) });`);
assertSyntaxError(`(() => {"use strict"; (for(yield of yield) c +2) });`);
assertSyntaxError(`(() => {"use strict"; (for(a of b) yield) });`);
assertSyntaxError(`(() => {"use strict"; (for(a of b) yield +2) });`);
assertSyntaxError(`(() => {"use strict"; (for(a of b) yield d) });`);

})();

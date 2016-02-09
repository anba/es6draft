/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const assertSyntaxError = Assert.assertSyntaxError;

/* function name tests */
(function() {

// function declaration
// -> function declaration
function f(){ function yield(){} }
assertSyntaxError(`function f(){"use strict"; function yield(){} }`);
// -> generator declaration
function f(){ function* yield(){} }
assertSyntaxError(`function f(){"use strict"; function* yield(){} }`);
// -> function expression
function f(){ (function yield(){}); }
assertSyntaxError(`function f(){"use strict"; (function yield(){}); }`);
// -> generator expression
assertSyntaxError(`function f(){ (function* yield(){}); }`);
assertSyntaxError(`function f(){"use strict"; (function* yield(){}); }`);
// -> class declaration
assertSyntaxError(`function f(){ class yield{} }`);
assertSyntaxError(`function f(){"use strict"; class yield{} }`);
// -> class expression
assertSyntaxError(`function f(){ (class yield{}); }`);
assertSyntaxError(`function f(){"use strict"; (class yield{}); }`);

// function expression
// -> function declaration
(function f(){ function yield(){} });
assertSyntaxError(`(function f(){"use strict"; function yield(){} });`);
// -> generator declaration
(function f(){ function* yield(){} });
assertSyntaxError(`(function f(){"use strict"; function* yield(){} });`);
// -> function expression
(function f(){ (function yield(){}); });
assertSyntaxError(`(function f(){"use strict"; (function yield(){}); });`);
// -> generator expression
assertSyntaxError(`(function f(){ (function* yield(){}); });`);
assertSyntaxError(`(function f(){"use strict"; (function* yield(){}); });`);
// -> class declaration
assertSyntaxError(`(function f(){ class yield{} });`);
assertSyntaxError(`(function f(){"use strict"; class yield{} });`);
// -> class expression
assertSyntaxError(`(function f(){ (class yield{}); });`);
assertSyntaxError(`(function f(){"use strict"; (class yield{}); });`);

// generator declaration
// -> function declaration
assertSyntaxError(`function* g(){ function yield(){} }`);
assertSyntaxError(`function* g(){"use strict"; function yield(){} }`);
// -> generator declaration
assertSyntaxError(`function* g(){ function* yield(){} }`);
assertSyntaxError(`function* g(){"use strict"; function* yield(){} }`);
// -> function expression
function* g(){ (function yield(){}); }
assertSyntaxError(`function* g(){"use strict"; (function yield(){}); }`);
// -> generator expression
assertSyntaxError(`function* g(){ (function* yield(){}); }`);
assertSyntaxError(`function* g(){"use strict"; (function* yield(){}); }`);
// -> class declaration
assertSyntaxError(`function* g(){ class yield{} }`);
assertSyntaxError(`function* g(){"use strict"; class yield{} }`);
// -> class expression
assertSyntaxError(`function* g(){ (class yield{}); }`);
assertSyntaxError(`function* g(){"use strict"; (class yield{}); }`);

// generator expression
// -> function declaration
assertSyntaxError(`(function* g(){ function yield(){} });`);
assertSyntaxError(`(function* g(){"use strict"; function yield(){} });`);
// -> generator declaration
assertSyntaxError(`(function* g(){ function* yield(){} });`);
assertSyntaxError(`(function* g(){"use strict"; function* yield(){} });`);
// -> function expression
(function* g(){ (function yield(){}); });
assertSyntaxError(`(function* g(){"use strict"; (function yield(){}); });`);
// -> generator expression
assertSyntaxError(`(function* g(){ (function* yield(){}); });`);
assertSyntaxError(`(function* g(){"use strict"; (function* yield(){}); });`);
// -> class declaration
assertSyntaxError(`(function* g(){ class yield{} });`);
assertSyntaxError(`(function* g(){"use strict"; class yield{} });`);
// -> class expression
assertSyntaxError(`(function* g(){ (class yield{}); });`);
assertSyntaxError(`(function* g(){"use strict"; (class yield{}); });`);

})();


/* function as default parameter inherits strict mode from enclosing function */
(function() {

// function + function name
function f(x = function yield(){}) {}
assertSyntaxError(`function f(x = function* yield(){}) {}`);
assertSyntaxError(`function f(x = function yield(){}) {"use strict";}`);
assertSyntaxError(`function f(x = function* yield(){}) {"use strict";}`);

// function + parameter name
function f(x = function (yield){}) {}
assertSyntaxError(`function f(x = function* (yield){}) {}`);
assertSyntaxError(`function f(x = function (yield){}) {"use strict";}`);
assertSyntaxError(`function f(x = function* (yield){}) {"use strict";}`);

// generator + function name
function* g0(x = function yield(){}) {}
assertSyntaxError(`function* g1(x = function* yield(){}) {}`);
assertSyntaxError(`function* g(x = function yield(){}) {"use strict";}`);
assertSyntaxError(`function* g(x = function* yield(){}) {"use strict";}`);

// generator + parameter name
function* g2(x = function (yield){}) {}
assertSyntaxError(`function* g(x = function* (yield){}) {}`);
assertSyntaxError(`function* g(x = function (yield){}) {"use strict";}`);
assertSyntaxError(`function* g(x = function* (yield){}) {"use strict";}`);

})();


/* function declarations and function expressions */

// 'yield' in non-strict function declarations
(function() {

// function name
function yield(){}
// parameter name
function f(yield){}
function f(yield, yield){}
function f(yield = 0){}
function f(yield = yield){}
function f(...yield){}
function f({yield}){}
function f([yield]){}
// var name
function f(){var yield}
// let name
function f(){let yield}
// const name
function f(){const yield = 0}
// identifier
function f(){yield}
function f(){yield(yield)}
function f(){yield = 0}
function f(){++yield}
function f(){yield + yield}
function f(){yield + 2}
function f(){yield: 0}
function f(){a: yield: 0}
function f(){yield: a: 0}
function f(){yield: break yield}
function f(){yield: do break yield; while(false)}
function f(){yield: do continue yield; while(false)}
// yield statement/expression
assertSyntaxError(`function f(){yield 0}`);
assertSyntaxError(`function f(){yield yield 0}`);
assertSyntaxError(`function f(){yield (yield 0)}`);
// identifier and ASI
function f(){
  /* not parsed as 'yield 0' */
  yield
  0;
}

// try some combinations
function yield(yield){var yield}
function yield(){let yield}
function yield(){const yield = 0}
function yield(yield = 0){}
function yield(...yield){}

// test syntax errors are still detected
assertSyntaxError(`function f(yield){let yield}`);
assertSyntaxError(`function f(yield){const yield = 0}`);
assertSyntaxError(`function f(yield, yield = 0){}`);
assertSyntaxError(`function f(yield = 0, yield = 0){}`);
assertSyntaxError(`function f(yield, ...yield){}`);
assertSyntaxError(`function f(yield = 0, ...yield){}`);
function f(yield = 0){var yield}
assertSyntaxError(`function f(yield = 0){let yield}`);
assertSyntaxError(`function f(yield = 0){const yield = 0}`);
function f(yield = 0){function yield(){}}
assertSyntaxError(`function f(){yield: yield: 0}`);

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

// function name
(function yield(){});
// parameter name
(function (yield){});
(function (yield, yield){});
(function (yield = 0){});
(function (yield = yield){});
(function (...yield){});
(function ({yield}){});
(function ([yield]){});
// var name
(function (){var yield});
// let name
(function (){let yield});
// const name
(function (){const yield = 0});
// identifier
(function (){yield});
(function (){yield(yield)});
(function (){yield = 0});
(function (){++yield});
(function (){yield + yield});
(function (){yield + 2});
(function (){yield: 0});
(function (){a: yield: 0});
(function (){yield: a: 0});
(function f(){yield: break yield});
(function f(){yield: do break yield; while(false)});
(function f(){yield: do continue yield; while(false)});
// yield statement/expression
assertSyntaxError(`function (){yield 0}`);
assertSyntaxError(`function (){yield yield 0}`);
assertSyntaxError(`function (){yield (yield 0)}`);
// identifier and ASI
(function (){
  /* not parsed as 'yield 0' */
  yield
  0;
});

// try some combinations
(function yield(yield){var yield});
(function yield(){let yield});
(function yield(){const yield = 0});
(function yield(yield = 0){});
(function yield(...yield){});

// test syntax errors are still detected
assertSyntaxError(`(function (yield){let yield});`);
assertSyntaxError(`(function (yield){const yield = 0});`);
assertSyntaxError(`(function (yield, yield = 0){});`);
assertSyntaxError(`(function (yield = 0, yield = 0){});`);
assertSyntaxError(`(function (yield, ...yield){});`);
assertSyntaxError(`(function (yield = 0, ...yield){});`);
(function (yield = 0){var yield});
assertSyntaxError(`(function (yield = 0){let yield});`);
assertSyntaxError(`(function (yield = 0){const yield = 0});`);
(function (yield = 0){function yield(){}});
assertSyntaxError(`(function (){yield: yield: 0});`);

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

// function name
assertSyntaxError(`function yield(){"use strict";}`);
// parameter name
assertSyntaxError(`function f(yield){"use strict";}`);
assertSyntaxError(`function f(yield, yield){"use strict";}`);
assertSyntaxError(`function f(yield = 0){"use strict";}`);
assertSyntaxError(`function f(yield = yield){"use strict";}`);
assertSyntaxError(`function f(...yield){"use strict";}`);
assertSyntaxError(`function f({yield}){"use strict";}`);
assertSyntaxError(`function f([yield]){"use strict";}`);
// var name
assertSyntaxError(`function f(){"use strict"; var yield}`);
// let name
assertSyntaxError(`function f(){"use strict"; let yield}`);
// const name
assertSyntaxError(`function f(){"use strict"; const yield = 0}`);
// identifier
assertSyntaxError(`function f(){"use strict"; yield}`);
assertSyntaxError(`function f(){"use strict"; yield(yield)}`);
assertSyntaxError(`function f(){"use strict"; yield = 0}`);
assertSyntaxError(`function f(){"use strict"; ++yield}`);
assertSyntaxError(`function f(){"use strict"; yield + yield}`);
assertSyntaxError(`function f(){"use strict"; yield + 2}`);
assertSyntaxError(`function f(){"use strict"; yield: 0}`);
assertSyntaxError(`function f(){"use strict"; a: yield: 0}`);
assertSyntaxError(`function f(){"use strict"; yield: a: 0}`);
assertSyntaxError(`function f(){"use strict"; yield: break yield}`);
assertSyntaxError(`function f(){"use strict"; yield: do break yield; while(false)}`);
assertSyntaxError(`function f(){"use strict"; yield: do continue yield; while(false)}`);
// yield statement/expression
assertSyntaxError(`function f(){"use strict"; yield 0}`);
assertSyntaxError(`function f(){"use strict"; yield yield 0}`);
assertSyntaxError(`function f(){"use strict"; yield (yield 0)}`);
// identifier and ASI
assertSyntaxError(`function f(){
  "use strict";
  /* not parsed as 'yield 0' */
  yield
  0;
}`);

// try some combinations
assertSyntaxError(`function yield(yield){"use strict"; var yield}`);
assertSyntaxError(`function yield(){"use strict"; let yield}`);
assertSyntaxError(`function yield(){"use strict"; const yield = 0}`);
assertSyntaxError(`function yield(yield = 0){"use strict";}`);
assertSyntaxError(`function yield(...yield){"use strict";}`);

// test syntax errors are still detected
assertSyntaxError(`function f(yield){"use strict"; let yield}`);
assertSyntaxError(`function f(yield){"use strict"; const yield = 0}`);
assertSyntaxError(`function f(yield, yield = 0){"use strict";}`);
assertSyntaxError(`function f(yield = 0, yield = 0){"use strict";}`);
assertSyntaxError(`function f(yield, ...yield){"use strict";}`);
assertSyntaxError(`function f(yield = 0, ...yield){"use strict";}`);
assertSyntaxError(`function f(yield = 0){"use strict"; var yield}`);
assertSyntaxError(`function f(yield = 0){"use strict"; let yield}`);
assertSyntaxError(`function f(yield = 0){"use strict"; const yield = 0}`);
assertSyntaxError(`function f(yield = 0){"use strict"; function yield(){}}`);
assertSyntaxError(`function f(){"use strict"; yield: yield: 0}`);

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

// function name
assertSyntaxError(`(function yield(){"use strict";});`);
// parameter name
assertSyntaxError(`(function (yield){"use strict";});`);
assertSyntaxError(`(function (yield, yield){"use strict";});`);
assertSyntaxError(`(function (yield = 0){"use strict";});`);
assertSyntaxError(`(function (yield = yield){"use strict";});`);
assertSyntaxError(`(function (...yield){"use strict";});`);
assertSyntaxError(`(function ({yield}){"use strict";});`);
assertSyntaxError(`(function ([yield]){"use strict";});`);
// var name
assertSyntaxError(`(function (){"use strict"; var yield});`);
// let name
assertSyntaxError(`(function (){"use strict"; let yield});`);
// const name
assertSyntaxError(`(function (){"use strict"; const yield = 0});`);
// identifier
assertSyntaxError(`(function (){"use strict"; yield});`);
assertSyntaxError(`(function (){"use strict"; yield(yield)});`);
assertSyntaxError(`(function (){"use strict"; yield = 0});`);
assertSyntaxError(`(function (){"use strict"; ++yield});`);
assertSyntaxError(`(function (){"use strict"; yield + yield});`);
assertSyntaxError(`(function (){"use strict"; yield + 2});`);
assertSyntaxError(`(function (){"use strict"; yield: 0});`);
assertSyntaxError(`(function (){"use strict"; a: yield: 0});`);
assertSyntaxError(`(function (){"use strict"; yield: a: 0});`);
assertSyntaxError(`(function (){"use strict"; yield: break yield});`);
assertSyntaxError(`(function (){"use strict"; yield: do break yield; while(false)});`);
assertSyntaxError(`(function (){"use strict"; yield: do continue yield; while(false)});`);
// yield statement/expression
assertSyntaxError(`function (){"use strict"; yield 0}`);
assertSyntaxError(`function (){"use strict"; yield yield 0}`);
assertSyntaxError(`function (){"use strict"; yield (yield 0)}`);
// identifier and ASI
assertSyntaxError(`(function (){
  "use strict";
  /* not parsed as 'yield 0' */
  yield
  0;
});`);

// try some combinations
assertSyntaxError(`(function yield(yield){"use strict"; var yield});`);
assertSyntaxError(`(function yield(){"use strict"; let yield});`);
assertSyntaxError(`(function yield(){"use strict"; const yield = 0});`);
assertSyntaxError(`(function yield(yield = 0){"use strict";});`);
assertSyntaxError(`(function yield(...yield){"use strict";});`);

// test syntax errors are still detected
assertSyntaxError(`(function (yield){"use strict"; let yield});`);
assertSyntaxError(`(function (yield){"use strict"; const yield = 0});`);
assertSyntaxError(`(function (yield, yield = 0){"use strict";});`);
assertSyntaxError(`(function (yield = 0, yield = 0){"use strict";});`);
assertSyntaxError(`(function (yield, ...yield){"use strict";});`);
assertSyntaxError(`(function (yield = 0, ...yield){"use strict";});`);
assertSyntaxError(`(function (yield = 0){"use strict"; var yield});`);
assertSyntaxError(`(function (yield = 0){"use strict"; let yield});`);
assertSyntaxError(`(function (yield = 0){"use strict"; const yield = 0});`);
assertSyntaxError(`(function (yield = 0){"use strict"; function yield(){}});`);
assertSyntaxError(`(function (){"use strict"; yield: yield: 0});`);

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

// function name
function* yield(){};
// parameter name
assertSyntaxError(`function* g(yield){}`);
assertSyntaxError(`function* g(yield, yield){}`);
assertSyntaxError(`function* g(yield = 0){}`);
assertSyntaxError(`function* g(yield = yield){}`);
assertSyntaxError(`function* g(...yield){}`);
assertSyntaxError(`function* g({yield}){}`);
assertSyntaxError(`function* g([yield]){}`);
// var name
assertSyntaxError(`function* g(){var yield}`);
// let name
assertSyntaxError(`function* g(){let yield}`);
// const name
assertSyntaxError(`function* g(){const yield = 0}`);
// identifier
assertSyntaxError(`function* g(){yield = 0}`);
assertSyntaxError(`function* g(){++yield}`);
assertSyntaxError(`function* g(){yield + yield}`);
assertSyntaxError(`function* g0(){yield: 0}`);
assertSyntaxError(`function* g1(){a: yield: 0}`);
assertSyntaxError(`function* g2(){yield: a: 0}`);
assertSyntaxError(`function* g3(){yield: break yield}`);
assertSyntaxError(`function* g4(){yield: do break yield; while(false)}`);
assertSyntaxError(`function* g5(){yield: do continue yield; while(false)}`);
// yield statement/expression
function* g6(){yield + 2}
function* g7(){yield 0}
function* g8(){yield yield 0}
function* g9(){yield (yield 0)}
// yield and ASI
function* g10(){
  /* parsed as 'yield 0' */
  yield
  0;
}
// AssignmentExpression optional in yield
function* g11(){yield}
function* g12(){yield(yield)}

// try some combinations
assertSyntaxError(`function* yield(yield){var yield}`);
assertSyntaxError(`function* yield(){let yield}`);
assertSyntaxError(`function* yield(){const yield = 0}`);
assertSyntaxError(`function* yield(yield = 0){}`);
assertSyntaxError(`function* yield(...yield){}`);

// test syntax errors are still detected
assertSyntaxError(`function* g(yield){let yield}`);
assertSyntaxError(`function* g(yield){const yield = 0}`);
assertSyntaxError(`function* g(yield, yield = 0){}`);
assertSyntaxError(`function* g(yield = 0, yield = 0){}`);
assertSyntaxError(`function* g(yield, ...yield){}`);
assertSyntaxError(`function* g(yield = 0, ...yield){}`);
assertSyntaxError(`function* g(yield = 0){var yield}`);
assertSyntaxError(`function* g(yield = 0){let yield}`);
assertSyntaxError(`function* g(yield = 0){const yield = 0}`);
assertSyntaxError(`function* g(yield = 0){function yield(){}}`);
assertSyntaxError(`function* g(){yield: yield: 0}`);

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

// function name
assertSyntaxError(`(function* yield(){});`);
// parameter name
assertSyntaxError(`(function* (yield){});`);
assertSyntaxError(`(function* (yield, yield){});`);
assertSyntaxError(`(function* (yield = 0){});`);
assertSyntaxError(`(function* (yield = yield){});`);
assertSyntaxError(`(function* (...yield){});`);
assertSyntaxError(`(function* ({yield}){});`);
assertSyntaxError(`(function* ([yield]){});`);
// var name
assertSyntaxError(`(function* (){var yield});`);
// let name
assertSyntaxError(`(function* (){let yield});`);
// const name
assertSyntaxError(`(function* (){const yield = 0});`);
// identifier
assertSyntaxError(`(function* (){yield = 0});`);
assertSyntaxError(`(function* (){++yield});`);
assertSyntaxError(`(function* (){yield + yield});`);
assertSyntaxError(`(function* (){yield: 0});`);
assertSyntaxError(`(function* (){a: yield: 0});`);
assertSyntaxError(`(function* (){yield: a: 0});`);
assertSyntaxError(`(function* (){yield: break yield});`);
assertSyntaxError(`(function* (){yield: do break yield; while(false)});`);
assertSyntaxError(`(function* (){yield: do continue yield; while(false)});`);
// yield statement/expression
(function* (){yield + 2});
(function* (){yield 0});
(function* (){yield yield 0});
(function* (){yield (yield 0)});
// yield and ASI
(function* (){
  /* parsed as 'yield 0' */
  yield
  0;
});
// AssignmentExpression optional in yield
(function* (){yield});
(function* (){yield(yield)});

// try some combinations
assertSyntaxError(`(function* yield(yield){var yield});`);
assertSyntaxError(`(function* yield(){let yield});`);
assertSyntaxError(`(function* yield(){const yield = 0});`);
assertSyntaxError(`(function* yield(yield = 0){});`);
assertSyntaxError(`(function* yield(...yield){});`);

// test syntax errors are still detected
assertSyntaxError(`(function* (yield){let yield});`);
assertSyntaxError(`(function* (yield){const yield = 0});`);
assertSyntaxError(`(function* (yield, yield = 0){});`);
assertSyntaxError(`(function* (yield = 0, yield = 0){});`);
assertSyntaxError(`(function* (yield, ...yield){});`);
assertSyntaxError(`(function* (yield = 0, ...yield){});`);
assertSyntaxError(`(function* (yield = 0){var yield});`);
assertSyntaxError(`(function* (yield = 0){let yield});`);
assertSyntaxError(`(function* (yield = 0){const yield = 0});`);
assertSyntaxError(`(function* (yield = 0){function yield(){}});`);
assertSyntaxError(`(function* (){yield: yield: 0});`);

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

// function name
assertSyntaxError(`function* yield(){"use strict";}`);
// parameter name
assertSyntaxError(`function* g(yield){"use strict";}`);
assertSyntaxError(`function* g(yield, yield){"use strict";}`);
assertSyntaxError(`function* g(yield = 0){"use strict";}`);
assertSyntaxError(`function* g(yield = yield){"use strict";}`);
assertSyntaxError(`function* g(...yield){"use strict";}`);
assertSyntaxError(`function* g({yield}){"use strict";}`);
assertSyntaxError(`function* g([yield]){"use strict";}`);
// var name
assertSyntaxError(`function* g(){"use strict"; var yield}`);
// let name
assertSyntaxError(`function* g(){"use strict"; let yield}`);
// const name
assertSyntaxError(`function* g(){"use strict"; const yield = 0}`);
// identifier
assertSyntaxError(`function* g(){"use strict"; yield = 0}`);
assertSyntaxError(`function* g(){"use strict"; ++yield}`);
assertSyntaxError(`function* g(){"use strict"; yield + yield}`);
assertSyntaxError(`function* g(){"use strict"; yield: 0}`);
assertSyntaxError(`function* g(){"use strict"; a: yield: 0}`);
assertSyntaxError(`function* g(){"use strict"; yield: a: 0}`);
assertSyntaxError(`function* g(){"use strict"; yield: break yield}`);
assertSyntaxError(`function* g(){"use strict"; yield: do break yield; while(false)}`);
assertSyntaxError(`function* g(){"use strict"; yield: do continue yield; while(false)}`);
// yield statement/expression
function* g0(){"use strict"; yield + 2}
function* g1(){"use strict"; yield 0}
function* g2(){"use strict"; yield yield 0}
function* g3(){"use strict"; yield (yield 0)}
// yield and ASI
function* g4(){
  "use strict";
  /* parsed as 'yield 0' */
  yield
  0;
}
// AssignmentExpression optional in yield
function* g5(){"use strict"; yield}
function* g6(){"use strict"; yield(yield)}

// try some combinations
assertSyntaxError(`function* yield(yield){"use strict"; var yield}`);
assertSyntaxError(`function* yield(){"use strict"; let yield}`);
assertSyntaxError(`function* yield(){"use strict"; const yield = 0}`);
assertSyntaxError(`function* yield(yield = 0){"use strict";}`);
assertSyntaxError(`function* yield(...yield){"use strict";}`);

// test syntax errors are still detected
assertSyntaxError(`function* g(yield){"use strict"; let yield}`);
assertSyntaxError(`function* g(yield){"use strict"; const yield = 0}`);
assertSyntaxError(`function* g(yield, yield = 0){"use strict";}`);
assertSyntaxError(`function* g(yield = 0, yield = 0){"use strict";}`);
assertSyntaxError(`function* g(yield, ...yield){"use strict";}`);
assertSyntaxError(`function* g(yield = 0, ...yield){"use strict";}`);
assertSyntaxError(`function* g(yield = 0){"use strict"; var yield}`);
assertSyntaxError(`function* g(yield = 0){"use strict"; let yield}`);
assertSyntaxError(`function* g(yield = 0){"use strict"; const yield = 0}`);
assertSyntaxError(`function* g(yield = 0){"use strict"; function yield(){}}`);
assertSyntaxError(`function* g(){"use strict"; yield: yield: 0}`);

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

// function name
assertSyntaxError(`(function* yield(){"use strict";});`);
// parameter name
assertSyntaxError(`(function* (yield){"use strict";});`);
assertSyntaxError(`(function* (yield, yield){"use strict";});`);
assertSyntaxError(`(function* (yield = 0){"use strict";});`);
assertSyntaxError(`(function* (yield = yield){"use strict";});`);
assertSyntaxError(`(function* (...yield){"use strict";});`);
assertSyntaxError(`(function* ({yield}){"use strict";});`);
assertSyntaxError(`(function* ([yield]){"use strict";});`);
// var name
assertSyntaxError(`(function* (){"use strict"; var yield});`);
// let name
assertSyntaxError(`(function* (){"use strict"; let yield});`);
// const name
assertSyntaxError(`(function* (){"use strict"; const yield = 0});`);
// identifier
assertSyntaxError(`(function* (){"use strict"; yield = 0});`);
assertSyntaxError(`(function* (){"use strict"; ++yield});`);
assertSyntaxError(`(function* (){"use strict"; yield + yield});`);
assertSyntaxError(`(function* (){"use strict"; yield: 0});`);
assertSyntaxError(`(function* (){"use strict"; a: yield: 0});`);
assertSyntaxError(`(function* (){"use strict"; yield: a: 0});`);
assertSyntaxError(`(function* (){"use strict"; yield: break yield});`);
assertSyntaxError(`(function* (){"use strict"; yield: do break yield; while(false)});`);
assertSyntaxError(`(function* (){"use strict"; yield: do continue yield; while(false)});`);
// yield statement/expression
(function* (){"use strict"; yield + 2});
(function* (){"use strict"; yield 0});
(function* (){"use strict"; yield yield 0});
(function* (){"use strict"; yield (yield 0)});
// yield and ASI
;(function* (){
  "use strict";
  /* parsed as 'yield 0' */
  yield
  0;
});
// AssignmentExpression optional in yield
(function* (){"use strict"; yield});
(function* (){"use strict"; yield(yield)});

// try some combinations
assertSyntaxError(`(function* yield(yield){"use strict"; var yield});`);
assertSyntaxError(`(function* yield(){"use strict"; let yield});`);
assertSyntaxError(`(function* yield(){"use strict"; const yield = 0});`);
assertSyntaxError(`(function* yield(yield = 0){"use strict";});`);
assertSyntaxError(`(function* yield(...yield){"use strict";});`);

// test syntax errors are still detected
assertSyntaxError(`(function* (yield){"use strict"; let yield});`);
assertSyntaxError(`(function* (yield){"use strict"; const yield = 0});`);
assertSyntaxError(`(function* (yield, yield = 0){"use strict";});`);
assertSyntaxError(`(function* (yield = 0, yield = 0){"use strict";});`);
assertSyntaxError(`(function* (yield, ...yield){"use strict";});`);
assertSyntaxError(`(function* (yield = 0, ...yield){"use strict";});`);
assertSyntaxError(`(function* (yield = 0){"use strict"; var yield});`);
assertSyntaxError(`(function* (yield = 0){"use strict"; let yield});`);
assertSyntaxError(`(function* (yield = 0){"use strict"; const yield = 0});`);
assertSyntaxError(`(function* (yield = 0){"use strict"; function yield(){}});`);
assertSyntaxError(`(function* (){"use strict"; yield: yield: 0});`);

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

// function name (= property name, yield allowed!)
({ yield(){} });
({ "yield"(){} });
({ ["yield"](){} });
// parameter name
({ f(yield){} });
assertSyntaxError(`({ f(yield, yield){} });`);
({ f(yield = 0){} });
({ f(yield = yield){} });
({ f(...yield){} });
({ f({yield}){} });
({ f([yield]){} });
// var name
({ f(){var yield} });
// let name
({ f(){let yield} });
// const name
({ f(){const yield = 0} });
// identifier
({ f(){yield} });
({ f(){yield(yield)} });
({ f(){yield = 0} });
({ f(){++yield} });
({ f(){yield + yield} });
({ f(){yield + 2} });
({ f(){yield: 0} });
({ f(){a: yield: 0} });
({ f(){yield: a: 0} });
({ f(){yield: break yield} });
({ f(){yield: do break yield; while(false)} });
({ f(){yield: do continue yield; while(false)} });
// yield statement/expression
assertSyntaxError(`({ f(){yield 0} });`);
assertSyntaxError(`({ f(){yield yield 0} });`);
assertSyntaxError(`({ f(){yield (yield 0)} });`);
// yield and ASI
({ f(){
  /* not parsed as 'yield 0' */
  yield
  0;
} });

// try some combinations
({ yield(yield){var yield} });
({ yield(){let yield} });
({ yield(){const yield = 0} });
({ yield(yield = 0){} });
({ yield(...yield){} });

// test syntax errors are still detected
assertSyntaxError(`({ f(yield){let yield} });`);
assertSyntaxError(`({ f(yield){const yield = 0} });`);
assertSyntaxError(`({ f(yield, yield = 0){} });`);
assertSyntaxError(`({ f(yield = 0, yield = 0){} });`);
assertSyntaxError(`({ f(yield, ...yield){} });`);
assertSyntaxError(`({ f(yield = 0, ...yield){} });`);
({ f(yield = 0){var yield} });
assertSyntaxError(`({ f(yield = 0){let yield} });`);
assertSyntaxError(`({ f(yield = 0){const yield = 0} });`);
({ f(yield = 0){function yield(){}} });
assertSyntaxError(`({ f(){yield: yield: 0} });`);

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

// function name (= property name, yield allowed!)
({ yield(){"use strict";} });
({ "yield"(){"use strict";} });
({ ["yield"](){"use strict";} });
// parameter name
assertSyntaxError(`({ f(yield){"use strict";} });`);
assertSyntaxError(`({ f(yield, yield){"use strict";} });`);
assertSyntaxError(`({ f(yield = 0){"use strict";} });`);
assertSyntaxError(`({ f(yield = yield){"use strict";} });`);
assertSyntaxError(`({ f(...yield){"use strict";} });`);
assertSyntaxError(`({ f({yield}){"use strict";} });`);
assertSyntaxError(`({ f([yield]){"use strict";} });`);
// var name
assertSyntaxError(`({ f(){"use strict"; var yield} });`);
// let name
assertSyntaxError(`({ f(){"use strict"; let yield} });`);
// const name
assertSyntaxError(`({ f(){"use strict"; const yield = 0} });`);
// identifier
assertSyntaxError(`({ f(){"use strict"; yield} });`);
assertSyntaxError(`({ f(){"use strict"; yield(yield)} });`);
assertSyntaxError(`({ f(){"use strict"; yield = 0} });`);
assertSyntaxError(`({ f(){"use strict"; ++yield} });`);
assertSyntaxError(`({ f(){"use strict"; yield + yield} });`);
assertSyntaxError(`({ f(){"use strict"; yield + 2} });`);
assertSyntaxError(`({ f(){"use strict"; yield: 0} });`);
assertSyntaxError(`({ f(){"use strict"; a: yield: 0} });`);
assertSyntaxError(`({ f(){"use strict"; yield: a: 0} });`);
assertSyntaxError(`({ f(){"use strict"; yield: break yield} });`);
assertSyntaxError(`({ f(){"use strict"; yield: do break yield; while(false)} });`);
assertSyntaxError(`({ f(){"use strict"; yield: do continue yield; while(false)} });`);
// yield statement/expression
assertSyntaxError(`({ f(){"use strict"; yield 0} });`);
assertSyntaxError(`({ f(){"use strict"; yield yield 0} });`);
assertSyntaxError(`({ f(){"use strict"; yield (yield 0)} });`);
// yield and ASI
assertSyntaxError(`({ f(){
  "use strict";
  /* not parsed as 'yield 0' */
  yield
  0;
} });`);

// try some combinations
assertSyntaxError(`({ yield(yield){"use strict"; var yield} });`);
assertSyntaxError(`({ yield(){"use strict"; let yield} });`);
assertSyntaxError(`({ yield(){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ yield(yield = 0){"use strict";} });`);
assertSyntaxError(`({ yield(...yield){"use strict";} });`);

// test syntax errors are still detected
assertSyntaxError(`({ f(yield){"use strict"; let yield} });`);
assertSyntaxError(`({ f(yield){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ f(yield, yield = 0){"use strict";} });`);
assertSyntaxError(`({ f(yield = 0, yield = 0){"use strict";} });`);
assertSyntaxError(`({ f(yield, ...yield){"use strict";} });`);
assertSyntaxError(`({ f(yield = 0, ...yield){"use strict";} });`);
assertSyntaxError(`({ f(yield = 0){"use strict"; var yield} });`);
assertSyntaxError(`({ f(yield = 0){"use strict"; let yield} });`);
assertSyntaxError(`({ f(yield = 0){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ f(yield = 0){"use strict"; function yield(){}} });`);
assertSyntaxError(`({ f(){"use strict"; yield: yield: 0} });`);

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

// function name (= property name, yield allowed!)
({ * yield(){} });
({ * "yield"(){} });
({ * ["yield"](){} });
// parameter name
assertSyntaxError(`({ * g(yield){} });`);
assertSyntaxError(`({ * g(yield, yield){} });`);
assertSyntaxError(`({ * g(yield = 0){} });`);
assertSyntaxError(`({ * g(yield = yield){} });`);
assertSyntaxError(`({ * g(...yield){} });`);
assertSyntaxError(`({ * g({yield}){} });`);
assertSyntaxError(`({ * g([yield]){} });`);
// var name
assertSyntaxError(`({ * g(){var yield} });`);
// let name
assertSyntaxError(`({ * g(){let yield} });`);
// const name
assertSyntaxError(`({ * g(){const yield = 0} });`);
// identifier
assertSyntaxError(`({ * g(){yield = 0} });`);
assertSyntaxError(`({ * g(){++yield} });`);
assertSyntaxError(`({ * g(){yield + yield} });`);
assertSyntaxError(`({ * g(){yield: 0} });`);
assertSyntaxError(`({ * g(){a: yield: 0} });`);
assertSyntaxError(`({ * g(){yield: a: 0} });`);
assertSyntaxError(`({ * g(){yield: break yield} });`);
assertSyntaxError(`({ * g(){yield: do break yield; while(false) } });`);
assertSyntaxError(`({ * g(){yield: do continue yield; while(false) } });`);
// yield statement/expression
({ * g(){yield + 2} });
({ * g(){yield 0} });
({ * g(){yield yield 0} });
({ * g(){yield (yield 0)} });
// yield and ASI
({ * g(){
  /* parsed as 'yield 0' */
  yield
  0;
} });
// AssignmentExpression optional in yield
({ * g(){yield} });
({ * g(){yield(yield)} });

// try some combinations
assertSyntaxError(`({ * yield(yield){var yield} });`);
assertSyntaxError(`({ * yield(){let yield} });`);
assertSyntaxError(`({ * yield(){const yield = 0} });`);
assertSyntaxError(`({ * yield(yield = 0){} });`);
assertSyntaxError(`({ * yield(...yield){} });`);

// test syntax errors are still detected
assertSyntaxError(`({ * g(yield){let yield} });`);
assertSyntaxError(`({ * g(yield){const yield = 0} });`);
assertSyntaxError(`({ * g(yield, yield = 0){} });`);
assertSyntaxError(`({ * g(yield = 0, yield = 0){} });`);
assertSyntaxError(`({ * g(yield, ...yield){} });`);
assertSyntaxError(`({ * g(yield = 0, ...yield){} });`);
assertSyntaxError(`({ * g(yield = 0){var yield} });`);
assertSyntaxError(`({ * g(yield = 0){let yield} });`);
assertSyntaxError(`({ * g(yield = 0){const yield = 0} });`);
assertSyntaxError(`({ * g(yield = 0){function yield(){}} });`);
assertSyntaxError(`({ * g(){yield: yield: 0} });`);

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

// function name (= property name, yield allowed!)
({ * yield(){"use strict";} });
({ * "yield"(){"use strict";} });
({ * ["yield"](){"use strict";} });
// parameter name
assertSyntaxError(`({ * g(yield){"use strict";} });`);
assertSyntaxError(`({ * g(yield, yield){"use strict";} });`);
assertSyntaxError(`({ * g(yield = 0){"use strict";} });`);
assertSyntaxError(`({ * g(yield = yield){"use strict";} });`);
assertSyntaxError(`({ * g(...yield){"use strict";} });`);
assertSyntaxError(`({ * g({yield}){"use strict";} });`);
assertSyntaxError(`({ * g([yield]){"use strict";} });`);
// var name
assertSyntaxError(`({ * g(){"use strict"; var yield} });`);
// let name
assertSyntaxError(`({ * g(){"use strict"; let yield} });`);
// const name
assertSyntaxError(`({ * g(){"use strict"; const yield = 0} });`);
// identifier
assertSyntaxError(`({ * g(){"use strict"; yield = 0} });`);
assertSyntaxError(`({ * g(){"use strict"; ++yield} });`);
assertSyntaxError(`({ * g(){"use strict"; yield + yield} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: 0} });`);
assertSyntaxError(`({ * g(){"use strict"; a: yield: 0} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: a: 0} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: break yield} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: do break yield; while(false)} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: do continue yield; while(false)} });`);
// yield statement/expression
({ * g(){"use strict"; yield + 2} });
({ * g(){"use strict"; yield 0} });
({ * g(){"use strict"; yield yield 0} });
({ * g(){"use strict"; yield (yield 0)} });
// yield and ASI
({ * g(){
  "use strict";
  /* parsed as 'yield 0' */
  yield
  0;
} });
// AssignmentExpression optional in yield
({ * g(){"use strict"; yield} });
({ * g(){"use strict"; yield(yield)} });

// try some combinations
assertSyntaxError(`({ * yield(yield){"use strict"; var yield} });`);
assertSyntaxError(`({ * yield(){"use strict"; let yield} });`);
assertSyntaxError(`({ * yield(){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ * yield(yield = 0){"use strict";} });`);
assertSyntaxError(`({ * yield(...yield){"use strict";} });`);

// test syntax errors are still detected
assertSyntaxError(`({ * g(yield){"use strict"; let yield} });`);
assertSyntaxError(`({ * g(yield){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ * g(yield, yield = 0){"use strict";} });`);
assertSyntaxError(`({ * g(yield = 0, yield = 0){"use strict";} });`);
assertSyntaxError(`({ * g(yield, ...yield){"use strict";} });`);
assertSyntaxError(`({ * g(yield = 0, ...yield){"use strict";} });`);
assertSyntaxError(`({ * g(yield = 0){"use strict"; var yield} });`);
assertSyntaxError(`({ * g(yield = 0){"use strict"; let yield} });`);
assertSyntaxError(`({ * g(yield = 0){"use strict"; const yield = 0} });`);
assertSyntaxError(`({ * g(yield = 0){"use strict"; function yield(){}} });`);
assertSyntaxError(`({ * g(){"use strict"; yield: yield: 0} });`);

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

// function name (= property name, yield allowed!)
class C{ yield(){} }
class D{ "yield"(){} }
class E{ ["yield"](){} }
// parameter name
assertSyntaxError(`class C{ f(yield){} }`);
assertSyntaxError(`class C{ f(yield, yield){} }`);
assertSyntaxError(`class C{ f(yield = 0){} }`);
assertSyntaxError(`class C{ f(yield = yield){} }`);
assertSyntaxError(`class C{ f(...yield){} }`);
assertSyntaxError(`class C{ f({yield}){} }`);
assertSyntaxError(`class C{ f([yield]){} }`);
// var name
assertSyntaxError(`class C{ f(){var yield} }`);
// let name
assertSyntaxError(`class C{ f(){let yield} }`);
// const name
assertSyntaxError(`class C{ f(){const yield = 0} }`);
// identifier
assertSyntaxError(`class C{ f(){yield} }`);
assertSyntaxError(`class C{ f(){yield(yield)} }`);
assertSyntaxError(`class C{ f(){yield = 0} }`);
assertSyntaxError(`class C{ f(){++yield} }`);
assertSyntaxError(`class C{ f(){yield + yield} }`);
assertSyntaxError(`class C{ f(){yield + 2} }`);
assertSyntaxError(`class C{ f(){yield: 0} }`);
assertSyntaxError(`class C{ f(){a: yield: 0} }`);
assertSyntaxError(`class C{ f(){yield: a: 0} }`);
assertSyntaxError(`class C{ f(){yield: break yield} }`);
assertSyntaxError(`class C{ f(){yield: do break yield; while(false)} }`);
assertSyntaxError(`class C{ f(){yield: do continue yield; while(false)} }`);
// yield statement/expression
assertSyntaxError(`class C{ f(){yield 0} }`);
assertSyntaxError(`class C{ f(){yield yield 0} }`);
assertSyntaxError(`class C{ f(){yield (yield 0)} }`);
// yield and ASI
assertSyntaxError(`class C{ f(){
  /* not parsed as 'yield 0' */
  yield
  0;
} }`);

// try some combinations
assertSyntaxError(`class C{ yield(yield){var yield} }`);
assertSyntaxError(`class C{ yield(){let yield} }`);
assertSyntaxError(`class C{ yield(){const yield = 0} }`);
assertSyntaxError(`class C{ yield(yield = 0){} }`);
assertSyntaxError(`class C{ yield(...yield){} }`);

// test syntax errors are still detected
assertSyntaxError(`class C{ f(yield){let yield} }`);
assertSyntaxError(`class C{ f(yield){const yield = 0} }`);
assertSyntaxError(`class C{ f(yield, yield = 0){} }`);
assertSyntaxError(`class C{ f(yield = 0, yield = 0){} }`);
assertSyntaxError(`class C{ f(yield, ...yield){} }`);
assertSyntaxError(`class C{ f(yield = 0, ...yield){} }`);
assertSyntaxError(`class C{ f(yield = 0){var yield} }`);
assertSyntaxError(`class C{ f(yield = 0){let yield} }`);
assertSyntaxError(`class C{ f(yield = 0){const yield = 0} }`);
assertSyntaxError(`class C{ f(yield = 0){function yield(){}} }`);
assertSyntaxError(`class C{ f(){yield: yield: 0} }`);

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

// function name (= property name, yield allowed!)
class C{ * yield(){} }
class D{ * "yield"(){} }
class E{ * ["yield"](){} }
// parameter name
assertSyntaxError(`class C{ * g(yield){} }`);
assertSyntaxError(`class C{ * g(yield, yield){} }`);
assertSyntaxError(`class C{ * g(yield = 0){} }`);
assertSyntaxError(`class C{ * g(yield = yield){} }`);
assertSyntaxError(`class C{ * g(...yield){} }`);
assertSyntaxError(`class C{ * g({yield}){} }`);
assertSyntaxError(`class C{ * g([yield]){} }`);
// var name
assertSyntaxError(`class C{ * g(){var yield} }`);
// let name
assertSyntaxError(`class C{ * g(){let yield} }`);
// const name
assertSyntaxError(`class C{ * g(){const yield = 0} }`);
// identifier
assertSyntaxError(`class C{ * g(){yield = 0} }`);
assertSyntaxError(`class C{ * g(){++yield} }`);
assertSyntaxError(`class C{ * g(){yield + yield} }`);
assertSyntaxError(`class C{ * g(){yield: 0} }`);
assertSyntaxError(`class C{ * g(){a: yield: 0} }`);
assertSyntaxError(`class C{ * g(){yield: a: 0} }`);
assertSyntaxError(`class C{ * g(){yield: break yield} }`);
assertSyntaxError(`class C{ * g(){yield: do break yield; while(false)} }`);
assertSyntaxError(`class C{ * g(){yield: do continue yield; while(false)} }`);
// yield statement/expression
class C0{ * g(){yield + 2} }
class C1{ * g(){yield 0} }
class C2{ * g(){yield yield 0} }
class C3{ * g(){yield (yield 0)} }
// yield and ASI
class C4{ * g(){
  /* parsed as 'yield 0' */
  yield
  0;
} }
// AssignmentExpression optional in yield
class C5{ * g(){yield} }
class C6{ * g(){yield(yield)} }

// try some combinations
assertSyntaxError(`class C{ * yield(yield){var yield} }`);
assertSyntaxError(`class C{ * yield(){let yield} }`);
assertSyntaxError(`class C{ * yield(){const yield = 0} }`);
assertSyntaxError(`class C{ * yield(yield = 0){} }`);
assertSyntaxError(`class C{ * yield(...yield){} }`);

// test syntax errors are still detected
assertSyntaxError(`class C{ * g(yield){let yield} }`);
assertSyntaxError(`class C{ * g(yield){const yield = 0} }`);
assertSyntaxError(`class C{ * g(yield, yield = 0){} }`);
assertSyntaxError(`class C{ * g(yield = 0, yield = 0){} }`);
assertSyntaxError(`class C{ * g(yield, ...yield){} }`);
assertSyntaxError(`class C{ * g(yield = 0, ...yield){} }`);
assertSyntaxError(`class C{ * g(yield = 0){var yield} }`);
assertSyntaxError(`class C{ * g(yield = 0){let yield} }`);
assertSyntaxError(`class C{ * g(yield = 0){const yield = 0} }`);
assertSyntaxError(`class C{ * g(yield = 0){function yield(){}} }`);
assertSyntaxError(`class C{ * g(){yield: yield: 0} }`);

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

// parameter name
((yield) => {});
assertSyntaxError(`((yield, yield) => {});`);
((yield = 0) => {});
((yield = yield) => {});
((...yield) => {});
(({yield}) => {});
(([yield]) => {});
// var name
(() => {var yield});
// let name
(() => {let yield});
// const name
(() => {const yield = 0});
// identifier
(() => {yield});
(() => {yield(yield)});
(() => {yield = 0});
(() => {++yield});
(() => {yield + yield});
(() => {yield + 2});
(() => {yield: 0});
(() => {a: yield: 0});
(() => {yield: a: 0});
(() => {yield: break yield});
(() => {yield: do break yield; while(false)});
(() => {yield: do continue yield; while(false)});
// yield statement/expression
assertSyntaxError(`() => {yield 0}`);
assertSyntaxError(`() => {yield yield 0}`);
assertSyntaxError(`() => {yield (yield 0)}`);
// identifier and ASI
(() => {
  /* not parsed as 'yield 0' */
  yield
  0;
});

// try some combinations
((yield) => {var yield});

// test syntax errors are still detected
assertSyntaxError(`((yield) => {let yield});`);
assertSyntaxError(`((yield) => {const yield = 0});`);
assertSyntaxError(`((yield, yield = 0) => {});`);
assertSyntaxError(`((yield = 0, yield = 0) => {});`);
assertSyntaxError(`((yield, ...yield) => {});`);
assertSyntaxError(`((yield = 0, ...yield) => {});`);
((yield = 0) => {var yield});
assertSyntaxError(`((yield = 0) => {let yield});`);
assertSyntaxError(`((yield = 0) => {const yield = 0});`);
((yield = 0) => {function yield(){}});
assertSyntaxError(`(() => {yield: yield: 0});`);

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

// parameter name
assertSyntaxError(`((yield) => {"use strict";});`);
assertSyntaxError(`((yield, yield) => {"use strict";});`);
assertSyntaxError(`((yield = 0) => {"use strict";});`);
assertSyntaxError(`((yield = yield) => {"use strict";});`);
assertSyntaxError(`((...yield) => {"use strict";});`);
assertSyntaxError(`(({yield}) => {"use strict";});`);
assertSyntaxError(`(([yield]) => {"use strict";});`);
// var name
assertSyntaxError(`(() => {"use strict"; var yield});`);
// let name
assertSyntaxError(`(() => {"use strict"; let yield});`);
// const name
assertSyntaxError(`(() => {"use strict"; const yield = 0});`);
// identifier
assertSyntaxError(`(() => {"use strict"; yield});`);
assertSyntaxError(`(() => {"use strict"; yield(yield)});`);
assertSyntaxError(`(() => {"use strict"; yield = 0});`);
assertSyntaxError(`(() => {"use strict"; ++yield});`);
assertSyntaxError(`(() => {"use strict"; yield + yield});`);
assertSyntaxError(`(() => {"use strict"; yield + 2});`);
assertSyntaxError(`(() => {"use strict"; yield: 0});`);
assertSyntaxError(`(() => {"use strict"; a: yield: 0});`);
assertSyntaxError(`(() => {"use strict"; yield: a: 0});`);
assertSyntaxError(`(() => {"use strict"; yield: break yield});`);
assertSyntaxError(`(() => {"use strict"; yield: do break yield; while(false)});`);
assertSyntaxError(`(() => {"use strict"; yield: do continue yield; while(false)});`);
// yield statement/expression
assertSyntaxError(`() => {"use strict"; yield 0}`);
assertSyntaxError(`() => {"use strict"; yield yield 0}`);
assertSyntaxError(`() => {"use strict"; yield (yield 0)}`);
// identifier and ASI
assertSyntaxError(`(() => {
  "use strict";
  /* not parsed as 'yield 0' */
  yield
  0;
});`);

// try some combinations
assertSyntaxError(`((yield) => {"use strict"; var yield});`);

// test syntax errors are still detected
assertSyntaxError(`((yield) => {"use strict"; let yield});`);
assertSyntaxError(`((yield) => {"use strict"; const yield = 0});`);
assertSyntaxError(`((yield, yield = 0) => {"use strict";});`);
assertSyntaxError(`((yield = 0, yield = 0) => {"use strict";});`);
assertSyntaxError(`((yield, ...yield) => {"use strict";});`);
assertSyntaxError(`((yield = 0, ...yield) => {"use strict";});`);
assertSyntaxError(`((yield = 0) => {"use strict"; var yield});`);
assertSyntaxError(`((yield = 0) => {"use strict"; let yield});`);
assertSyntaxError(`((yield = 0) => {"use strict"; const yield = 0});`);
assertSyntaxError(`((yield = 0) => {"use strict"; function yield(){}});`);
assertSyntaxError(`(() => {"use strict"; yield: yield: 0});`);

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

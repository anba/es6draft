/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// B.3.2  Labelled Function Declarations

function testParse() {
  // Function top level
  L1: function f1() {}
  L2a: L2b: function f2() {}

  // Block statement
  { L3: function f3() {} }
  { L4a: L4b: function f4() {} }

  // if, while, do-while, for, for-in/of, with statements + block
  if (1) { L5: function f5() {} }
  while (1) { L6: function f6() {} }
  do { L7: function f7() {} } while (1);
  for (;;) { L8: function f8() {} }
  for (v in o) { L9: function f9() {} }
  for (v of o) { L10: function f10() {} }
  with (o) { L11: function f11() {} }

  switch (s) {
    case 0: L12: function f12() {}
    case 1: L13a: L13b: function f13() {}
    case 2: { L14: function f14() {} }
    case 3: { L15a: L15b: function f15() {} }
  }
  switch (s) { default: L16: function f16() {} }
  switch (s) { default: L17a: L17b: function f17() {} }
  switch (s) { default: { L18: function f18() {} } }
  switch (s) { default: { L19a: L19b: function f19() {} } }
}

// Always SyntaxError in if, while, do-while, for, for-in/of and with statements without block
assertSyntaxError(`if (1) L1: function f1(){}`);
assertSyntaxError(`if (1) L1: L2: function f1(){}`);
assertSyntaxError(`while (1) L1: function f1(){}`);
assertSyntaxError(`while (1) L1: L2: function f1(){}`);
assertSyntaxError(`do L1: function f1(){} while (1)`);
assertSyntaxError(`do L1: L2: function f1(){} while (1)`);
assertSyntaxError(`for (;;) L1: function f1(){}`);
assertSyntaxError(`for (;;) L1: L2: function f1(){}`);
assertSyntaxError(`for (v in o) L1: function f1(){}`);
assertSyntaxError(`for (v in o) L1: L2: function f1(){}`);
assertSyntaxError(`for (v of o) L1: function f1(){}`);
assertSyntaxError(`for (v of o) L1: L2: function f1(){}`);
assertSyntaxError(`with (o) L1: function f1(){}`);
assertSyntaxError(`with (o) L1: L2: function f1(){}`);

// Always SyntaxError in strict mode
assertSyntaxError(`"use strict"; L1: function f1(){}`);
assertSyntaxError(`"use strict"; L1: L2: function f2(){}`);

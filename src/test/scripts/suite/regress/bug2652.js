/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
} = Assert;

// 9.4.4.7 CreateMappedArgumentsObject: Only map arguments if present
// https://bugs.ecmascript.org/show_bug.cgi?id=2652

function singleParam(a) {
  a = 100;
  return arguments[0];
}
assertSame(void 0, singleParam());
assertSame(100, singleParam(-1));

function singleParamChangeArguments(a) {
  arguments[0] = 100;
  return a;
}
assertSame(void 0, singleParamChangeArguments());
assertSame(100, singleParamChangeArguments(-1));

function duplicateParams(index, a, a) {
  a = 100;
  return arguments[index];
}
assertSame(void 0, duplicateParams(1));
assertSame(void 0, duplicateParams(2));
assertSame(-100, duplicateParams(1, -100));
assertSame(void 0, duplicateParams(2, -100));
assertSame(-100, duplicateParams(1, -100, -200));
assertSame(100, duplicateParams(2, -100, -200));

function duplicateParamsChangeArguments(index, a, a) {
  arguments[index] = 100;
  return a;
}
assertSame(void 0, duplicateParamsChangeArguments(1));
assertSame(void 0, duplicateParamsChangeArguments(2));
assertSame(void 0, duplicateParamsChangeArguments(1, -100));
assertSame(void 0, duplicateParamsChangeArguments(2, -100));
assertSame(-200, duplicateParamsChangeArguments(1, -100, -200));
assertSame(100, duplicateParamsChangeArguments(2, -100, -200));

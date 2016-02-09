/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// function scope, non-strict

function A1_1() { var a; var a; }
assertSyntaxError(`function A1_2() { let a; let a; }`);
function A1_3() { function a(){}; function a(){}; }

function B1_1() { var a; { var a; } }
function B1_2() { let a; { let a; } }
function B1_3() { function a(){}; { function a(){}; } }

function C1_1() { { var a; } var a; }
function C1_2() { { let a; } let a; }
function C1_3() { { function a(){}; } function a(){}; }

function D1_1() { { var a; } { var a; } }
function D1_2() { { let a; } { let a; } }
function D1_3() { { function a(){}; } { function a(){}; } }

assertSyntaxError(`function A2_1() { var a; let a; }`);
assertSyntaxError(`function A2_2() { let a; var a; }`);
assertSyntaxError(`function A2_3() { function a(){}; let a; }`);
assertSyntaxError(`function A2_4() { let a; function a(){}; }`);
function A2_5() { var a; function a(){}; }
function A2_6() { function a(){}; var a; }

function B2_1() { var a; { let a; } }
assertSyntaxError(`function B2_2() { let a; { var a; } }`);
function B2_3() { function a(){}; { let a; } }
function B2_4() { let a; { function a(){}; } }
function B2_5() { var a; { function a(){}; } }
function B2_6() { function a(){} { var a; } }

assertSyntaxError(`function C2_1() { { var a; } let a; }`);
function C2_2() { { let a; } var a; }
function C2_3() { { function a(){}; } let a; }
function C2_4() { { let a; } function a(){}; }
function C2_5() { { var a; } function a(){}; }
function C2_6() { { function a(){}; } var a; }

function D2_1() { { var a; } { let a; } }
function D2_2() { { let a; } { var a; } }
function D2_3() { { function a(){}; } { let a; } }
function D2_4() { { let a; } { function a(){}; } }
function D2_5() { { var a; } { function a(){}; } }
function D2_6() { { function a(){}; } { var a; } }

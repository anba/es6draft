/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 14.5.15 ClassDefinitionEvaluation, 9.2.11 SetFunctionName: Move binding initialisation after step 18 / Invalid assertion in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=2578

assertThrows(() => {
class X {
  [(Object.preventExtensions(X), "")](){}
}
}, ReferenceError);

assertThrows(() => {
(class X {
  [(Object.preventExtensions(X), "")](){}
});
}, ReferenceError);

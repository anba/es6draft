/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Wrong [[Scope]] for eval'ed function declarations
// https://bugs.ecmascript.org/show_bug.cgi?id=1390

var result = (function(){
  var x = "outer";
  try {
    throw "inner";
  } catch(x) {
    return eval("function f(){ return x } f()")
  }
})();
assertSame("inner", result);

var result = (function(){
  var x = "outer";
  with({x: "inner"}) {
    return eval("function f(){ return x } f()")
  }
})();
assertSame("inner", result);

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertUndefined, assertDataProperty, assertThrows, fail
} = Assert;

function testWithRef() {
  var count = 0;
  var scope = {
    get x() {
      count++;
    }
  };
  with (scope) {
    x;
  }
  assertSame(1, count);
}
testWithRef();

function testWithVarDeclaration() {
  var count = 0;
  var scope = {
    get x() {
      count++;
    }
  };
  with (scope) {
    var x;
  }
  assertSame(0, count);
}
testWithVarDeclaration();

function testCatchEvalVarDecl() {
  var x = 0;
  var result = (function() {
    try {
      throw null;
    } catch (e) {
      eval("var x;");
    }
    x = 1;
    return x;
  })();
  assertSame(0, x);
  assertSame(1, result);
}
testCatchEvalVarDecl();

function testCatchFunctionExpression() {
  var x = 0;
  var f;
  try {
    throw 1;
  } catch (x) {
    f = function() { return x; };
  }
  assertSame(0, x);
  assertSame(1, f());
}
testCatchFunctionExpression();

function testCatchEvalFunctionExpression() {
  var x = 0;
  var f;
  try {
    throw 1;
  } catch (x) {
    f = eval("(function() { return x; });");
  }
  assertSame(0, x);
  assertSame(1, f());
}
testCatchEvalFunctionExpression();

function testCatchEvalFunctionDeclaration() {
  var x = 0;
  try {
    throw 1;
  } catch (x) {
    eval("function f() { return x; }");
  }
  assertSame(0, x);
  assertSame(1, f());
}
testCatchEvalFunctionDeclaration();

function testFunctionExpressionNameBinding() {
  var fexpr = function fname() {
    assertSame(fexpr, fname);
    eval("var fname = 0;");
    assertNotSame(fexpr, fname);
    assertTrue(delete fname);
    assertSame(fexpr, fname);
  };
  fexpr();
}
testFunctionExpressionNameBinding();

function testAssignment_1() {
  var x = 0;
  var result = (function() {
    x = (eval("var x;"), 1);
    return x;
  })();
  assertSame(void 0, result);
  assertSame(1, x);
}
testAssignment_1();

function testAssignment_2() {
  var scope = {};
  with (scope) {
    var x = (scope.x = 0, 1);
  }
  assertSame(0, scope.x);
  assertSame(1, x);
}
testAssignment_2();

function testAssignment_3() {
  var scope = {};
  with (scope) {
    var x;
    x = (scope.x = 0, 1);
  }
  assertSame(0, scope.x);
  assertSame(1, x);
}
testAssignment_3();

function testAssignment_4a() {
  var scope = {x: 0};
  with (scope) {
    var x = (delete scope.x, 1);
  }
  assertSame(1, scope.x);
  assertSame(void 0, x);
}
testAssignment_4a();

function testAssignment_4b() {
  var scope = {x: 0};
  with (scope) {
    var x = (delete x, 1);
  }
  assertSame(1, scope.x);
  assertSame(void 0, x);
}
testAssignment_4b();

function testAssignment_5a() {
  var scope = {x: 0};
  with (scope) {
    var x;
    x = (delete scope.x, 1);
  }
  assertSame(1, scope.x);
  assertSame(void 0, x);
}
testAssignment_5a();

function testAssignment_5b() {
  var scope = {x: 0};
  with (scope) {
    var x;
    x = (delete x, 1);
  }
  assertSame(1, scope.x);
  assertSame(void 0, x);
}
testAssignment_5b();


// Test cases from https://bugs.ecmascript.org/show_bug.cgi?id=1751
// Coverage: Identifier reference resolution in compound assignment and increment/decrement operator

// Test Compound Assignment (+=)

function testCompoundAdditionAssignment_1a() {
  var x = 1;
  var result = (function() {
    x += eval("var x = 2; x;");
    return x;
  })();
  assertSame(2, result);
  assertSame(3, x);
}
testCompoundAdditionAssignment_1a();

function testCompoundAdditionAssignment_1b() {
  var x = 1;
  var result = (function() {
    x += (eval("var x = 2;"), x);
    return x;
  })();
  assertSame(2, result);
  assertSame(3, x);
}
testCompoundAdditionAssignment_1b();

function testCompoundAdditionAssignment_2a() {
  var x = 1;
  var result = (function() {
    eval("var x = 2;");
    // https://bugs.ecmascript.org/show_bug.cgi?id=159
    x += eval("delete x; x;")
    return x;
  })();
  assertSame(3, result);
  assertSame(1, x);
}
testCompoundAdditionAssignment_2a();

function testCompoundAdditionAssignment_2b() {
  var x = 1;
  var result = (function() {
    eval("var x = 2;");
    // https://bugs.ecmascript.org/show_bug.cgi?id=159
    x += (eval("delete x;"), x);
    return x;
  })();
  assertSame(3, result);
  assertSame(1, x);
}
testCompoundAdditionAssignment_2b();

function testCompoundAdditionAssignment_3() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    x += 1;
  }
  assertSame(3, scope.x);
  assertSame(0, x);
}
testCompoundAdditionAssignment_3();

function testCompoundAdditionAssignment_4() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      x += 1;
    }
  }
  assertSame(3, innerScope.x);
}
testCompoundAdditionAssignment_4();

function testCompoundAdditionAssignment_5() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      x += 1;
    })();
  }
  assertSame(3, scope.x);
}
testCompoundAdditionAssignment_5();

function testCompoundAdditionAssignment_6(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    x += 1;
  })();
  assertSame(3, global.x);
  assertDataProperty(global, "x", {value: 3, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testCompoundAdditionAssignment_6(this);


// Test Compound Assignment (*=)

function testCompoundMultiplicationAssignment_1a() {
  var x = 3;
  var result = (function() {
    x *= eval("var x = 2; x;");
    return x;
  })();
  assertSame(2, result);
  assertSame(6, x);
}
testCompoundMultiplicationAssignment_1a();

function testCompoundMultiplicationAssignment_1b() {
  var x = 3;
  var result = (function() {
    x *= (eval("var x = 2;"), x);
    return x;
  })();
  assertSame(2, result);
  assertSame(6, x);
}
testCompoundMultiplicationAssignment_1b();

function testCompoundMultiplicationAssignment_2a() {
  var x = 3;
  var result = (function() {
    eval("var x = 2;");
    // https://bugs.ecmascript.org/show_bug.cgi?id=159
    x *= eval("delete x; x;");
    return x;
  })();
  assertSame(6, result);
  assertSame(3, x);
}
testCompoundMultiplicationAssignment_2a();

function testCompoundMultiplicationAssignment_2b() {
  var x = 3;
  var result = (function() {
    eval("var x = 2;");
    // https://bugs.ecmascript.org/show_bug.cgi?id=159
    x *= (eval("delete x;"), x);
    return x;
  })();
  assertSame(6, result);
  assertSame(3, x);
}
testCompoundMultiplicationAssignment_2b();

function testCompoundMultiplicationAssignment_3() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    x *= 3;
  }
  assertSame(6, scope.x);
  assertSame(0, x);
}
testCompoundMultiplicationAssignment_3();

function testCompoundMultiplicationAssignment_4() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      x *= 3;
    }
  }
  assertSame(6, innerScope.x);
}
testCompoundMultiplicationAssignment_4();

function testCompoundMultiplicationAssignment_5() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      x *= 3;
    })();
  }
  assertSame(6, scope.x);
}
testCompoundMultiplicationAssignment_5();

function testCompoundMultiplicationAssignment_6(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    x *= 3;
  })();
  assertSame(6, global.x);
  assertDataProperty(global, "x", {value: 6, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testCompoundMultiplicationAssignment_6(this);


// Test Prefix Increment Operator

function testPreIncrement_0() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    ++x;
  }
  assertSame(3, scope.x);
  assertSame(0, x);
}
testPreIncrement_0();

function testPreIncrement_1() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      ++x;
    }
  }
  assertSame(3, innerScope.x);
}
testPreIncrement_1();

function testPreIncrement_2() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      ++x;
    })();
  }
  assertSame(3, scope.x);
}
testPreIncrement_2();

function testPreIncrement_3(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    ++x;
  })();
  assertSame(3, global.x);
  assertDataProperty(global, "x", {value: 3, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testPreIncrement_3(this);


// Test Prefix Decrement Operator

function testPreDecrement_0() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    --x;
  }
  assertSame(1, scope.x);
  assertSame(0, x);
}
testPreDecrement_0();

function testPreDecrement_1() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      --x;
    }
  }
  assertSame(1, innerScope.x);
}
testPreDecrement_1();

function testPreDecrement_2() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      --x;
    })();
  }
  assertSame(1, scope.x);
}
testPreDecrement_2();

function testPreDecrement_3(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    --x;
  })();
  assertSame(1, global.x);
  assertDataProperty(global, "x", {value: 1, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testPreDecrement_3(this);


// Test Postfix Increment Operator

function testPostIncrement_0() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    x++;
  }
  assertSame(3, scope.x);
  assertSame(0, x);
}
testPostIncrement_0();

function testPostIncrement_1() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      x++;
    }
  }
  assertSame(3, innerScope.x);
}
testPostIncrement_1();

function testPostIncrement_2() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      x++;
    })();
  }
  assertSame(3, scope.x);
}
testPostIncrement_2();

function testPostIncrement_3(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    x++;
  })();
  assertSame(3, global.x);
  assertDataProperty(global, "x", {value: 3, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testPostIncrement_3(this);


// Test Postfix Decrement Operator

function testPostDecrement_0() {
  var x = 0;
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    x--;
  }
  assertSame(1, scope.x);
  assertSame(0, x);
}
testPostDecrement_0();

function testPostDecrement_1() {
  var outerScope = {
    set x(v) {
      fail `setter in outer scope invoked`;
    }
  };
  var innerScope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (outerScope) {
    with (innerScope) {
      x--;
    }
  }
  assertSame(1, innerScope.x);
}
testPostDecrement_1();

function testPostDecrement_2() {
  var scope = {
    get x() {
      delete this.x;
      return 2;
    }
  };
  with (scope) {
    (function() {
      "use strict";
      x--;
    })();
  }
  assertSame(1, scope.x);
}
testPostDecrement_2();

function testPostDecrement_3(global) {
  assertUndefined(Object.getOwnPropertyDescriptor(global, "x"));
  Object.defineProperty(global, "x", {
    configurable: true,
    get: function() {
      delete this.x;
      return 2;
    }
  });
  (function() {
    "use strict";
    x--;
  })();
  assertSame(1, global.x);
  assertDataProperty(global, "x", {value: 1, writable: true, enumerable: true, configurable: true});
  delete global.x; // clean-up
}
testPostDecrement_3(this);

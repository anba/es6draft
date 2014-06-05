/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

assertEq(Function.prototype.toString(), "function () { [native code] }");
assertEq(Function.prototype.toString.toString(), "function toString() { [native code] }");
assertEq(Function.prototype.bind().toString(), "function BoundFunction() { [native code] }");
assertEq(Function[Symbol.create]().toString(), "function F() { [no source] }");

function testProxy() {
  let {proxy, revoke} = Proxy.revocable(() => {}, {});
  assertEq(proxy.toString(), "() => {}");
  revoke();
  assertEq(Function.prototype.toString.call(proxy), "function F() { [no source] }");
}
testProxy();

function testClassDefaultConstructor() {
  class C1 { }
  assertEq(C1.toString(), "constructor(){}");

  class C2 extends class { } { }
  assertEq(C2.toString(), "constructor(...args){super(...args);}");
}
testClassDefaultConstructor();

function testFunctionDeclaration() {
  function F1(){}
  assertEq(F1.toString(), "function F1(){}");

  function F2 (){}
  assertEq(F2.toString(), "function F2(){}");

  function F3( ){}
  assertEq(F3.toString(), "function F3( ){}");

  function F4() {}
  assertEq(F4.toString(), "function F4() {}");

  function F5(){ }
  assertEq(F5.toString(), "function F5(){ }");
}
testFunctionDeclaration();

function testGeneratorDeclaration() {
  function* F1(){}
  assertEq(F1.toString(), "function* F1(){}");

  function* F2 (){}
  assertEq(F2.toString(), "function* F2(){}");

  function* F3( ){}
  assertEq(F3.toString(), "function* F3( ){}");

  function* F4() {}
  assertEq(F4.toString(), "function* F4() {}");

  function* F5(){ }
  assertEq(F5.toString(), "function* F5(){ }");

  function *G1(){}
  assertEq(G1.toString(), "function* G1(){}");

  function * G2(){}
  assertEq(G2.toString(), "function* G2(){}");

  function
  /* comment 1 */
  *
  /* comment 2 */
  G3
  /* comment 3 */
  (){}
  assertEq(G3.toString(), "function* G3(){}");

  function* G4 (/* comment */){}
  assertEq(G4.toString(), "function* G4(/* comment */){}");

  function* G5 (){/* comment */}
  assertEq(G5.toString(), "function* G5(){/* comment */}");
}
testGeneratorDeclaration();

function testAsyncFunctionDeclaration() {
  async function F1(){}
  assertEq(F1.toString(), "async function F1(){}");

  async function F2 (){}
  assertEq(F2.toString(), "async function F2(){}");

  async function F3( ){}
  assertEq(F3.toString(), "async function F3( ){}");

  async function F4() {}
  assertEq(F4.toString(), "async function F4() {}");

  async function F5(){ }
  assertEq(F5.toString(), "async function F5(){ }");

  async    function A1(){}
  assertEq(A1.toString(), "async function A1(){}");

  async /* comment */ function A2(){}
  assertEq(A2.toString(), "async function A2(){}");
}
testAsyncFunctionDeclaration();

function testGeneratorComprehension() {
  function throwCaller() {
    throw throwCaller.caller;
  }
  function getHiddenGenerator(g) {
    try {
      g.next();
    } catch (e) {
      return e;
    }
    assertEq("no error", "expected error");
  }

  let g1 = (0, (for (v of [0]) throwCaller()));
  assertEq(getHiddenGenerator(g1).toString(), "function* gencompr() { [generator comprehension] }");

  let g2 = (for (v of [0]) throwCaller());
  assertEq(getHiddenGenerator(g2).toString(), "function* g2() { [generator comprehension] }");
}
testGeneratorComprehension();

function testFunctionExpression() {
  // Anonymous function expression, different white space in function
  let f1 = (0, function(){});
  assertEq(f1.toString(), "function (){}");

  let f2 = (0, function (){});
  assertEq(f2.toString(), "function (){}");

  let f3 = (0, function () {});
  assertEq(f3.toString(), "function () {}");

  let f4 = (0, function (){ });
  assertEq(f4.toString(), "function (){ }");

  // Anonymous function expression, auto-assigned name not part of source
  let f5 = function(){};
  assertEq(f5.toString(), "function (){}");
  assertEq(f5.name, "f5");

  // Named function expression, different white space in function
  let f6 = function F6(){};
  assertEq(f6.toString(), "function F6(){}");

  let f7 = function F7 (){};
  assertEq(f7.toString(), "function F7(){}");

  let f8 = function F8 () {};
  assertEq(f8.toString(), "function F8() {}");

  let f9 = function F9 (){ };
  assertEq(f9.toString(), "function F9(){ }");

  // Comments
  let f10 = (function/*a*/f/*b*/(/*c*/){/*d*/});
  assertEq(f10.toString(), "function f(/*c*/){/*d*/}");
}
testFunctionExpression();

function testArrowFunction() {
  // Braced arrow function, different white space
  let f1 = () => {};
  assertEq(f1.toString(), "() => {}");

  let f2 = ()=>{};
  assertEq(f2.toString(), "()=>{}");

  let f3 = ( ) => {};
  assertEq(f3.toString(), "( ) => {}");

  let f4 = () => { };
  assertEq(f4.toString(), "() => { }");

  // Concise arrow function, different white space
  let f5 = () => 0;
  assertEq(f5.toString(), "() => 0");

  let f6 = ()=>0;
  assertEq(f6.toString(), "()=>0");

  let f7 = ( ) => 0;
  assertEq(f7.toString(), "( ) => 0");

  let f8 = () =>  0 ;
  assertEq(f8.toString(), "() =>  0");
}
testArrowFunction();

function testClassDeclarationConstructor() {
  class C1 {
    constructor(){}
  }
  assertEq(C1.toString(), "constructor(){}");

  class C2 {
    constructor() {}
  }
  assertEq(C2.toString(), "constructor() {}");

  class C3 {
    constructor( ) {}
  }
  assertEq(C3.toString(), "constructor( ) {}");

  class C4 {
    constructor() { }
  }
  assertEq(C4.toString(), "constructor() { }");
}
testClassDeclarationConstructor();

function testClassDeclarationProtoMethod() {
  class C1 {
    method(){}
  }
  assertEq(C1.prototype.method.toString(), "method(){}");

  class C2 {
    method() {}
  }
  assertEq(C2.prototype.method.toString(), "method() {}");

  class C3 {
    method( ) {}
  }
  assertEq(C3.prototype.method.toString(), "method( ) {}");

  class C4 {
    method() { }
  }
  assertEq(C4.prototype.method.toString(), "method() { }");
}
testClassDeclarationProtoMethod();

function testClassDeclarationProtoGeneratorMethod() {
  class C1 {
    *method(){}
  }
  assertEq(C1.prototype.method.toString(), "*method(){}");

  class C2 {
    *method() {}
  }
  assertEq(C2.prototype.method.toString(), "*method() {}");

  class C3 {
    *method( ) {}
  }
  assertEq(C3.prototype.method.toString(), "*method( ) {}");

  class C4 {
    *method() { }
  }
  assertEq(C4.prototype.method.toString(), "*method() { }");
}
testClassDeclarationProtoGeneratorMethod();

function testClassDeclarationProtoAsyncMethod() {
  class C1 {
    async method(){}
  }
  assertEq(C1.prototype.method.toString(), "async method(){}");

  class C2 {
    async method() {}
  }
  assertEq(C2.prototype.method.toString(), "async method() {}");

  class C3 {
    async method( ) {}
  }
  assertEq(C3.prototype.method.toString(), "async method( ) {}");

  class C4 {
    async method() { }
  }
  assertEq(C4.prototype.method.toString(), "async method() { }");
}
testClassDeclarationProtoAsyncMethod();

function testClassDeclarationProtoGetter() {
  class C1 {
    get abc(){}
  }
  assertEq(Object.getOwnPropertyDescriptor(C1.prototype, "abc").get.toString(), "get abc(){}");

  class C2 {
    get abc() {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C2.prototype, "abc").get.toString(), "get abc() {}");

  class C3 {
    get abc( ) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C3.prototype, "abc").get.toString(), "get abc( ) {}");

  class C4 {
    get abc() { }
  }
  assertEq(Object.getOwnPropertyDescriptor(C4.prototype, "abc").get.toString(), "get abc() { }");
}
testClassDeclarationProtoGetter();

function testClassDeclarationProtoSetter() {
  class C1 {
    set abc(x){}
  }
  assertEq(Object.getOwnPropertyDescriptor(C1.prototype, "abc").set.toString(), "set abc(x){}");

  class C2 {
    set abc(x) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C2.prototype, "abc").set.toString(), "set abc(x) {}");

  class C3 {
    set abc( x) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C3.prototype, "abc").set.toString(), "set abc( x) {}");

  class C4 {
    set abc(x) { }
  }
  assertEq(Object.getOwnPropertyDescriptor(C4.prototype, "abc").set.toString(), "set abc(x) { }");
}
testClassDeclarationProtoSetter();

function testClassDeclarationStaticMethod() {
  class C1 {
    static method(){}
  }
  assertEq(C1.method.toString(), "static method(){}");

  class C2 {
    static method() {}
  }
  assertEq(C2.method.toString(), "static method() {}");

  class C3 {
    static method( ) {}
  }
  assertEq(C3.method.toString(), "static method( ) {}");

  class C4 {
    static method() { }
  }
  assertEq(C4.method.toString(), "static method() { }");
}
testClassDeclarationStaticMethod();

function testClassDeclarationStaticGeneratorMethod() {
  class C1 {
    static *method(){}
  }
  assertEq(C1.method.toString(), "static *method(){}");

  class C2 {
    static *method() {}
  }
  assertEq(C2.method.toString(), "static *method() {}");

  class C3 {
    static *method( ) {}
  }
  assertEq(C3.method.toString(), "static *method( ) {}");

  class C4 {
    static *method() { }
  }
  assertEq(C4.method.toString(), "static *method() { }");
}
testClassDeclarationStaticGeneratorMethod();

function testClassDeclarationStaticAsyncMethod() {
  class C1 {
    static async method(){}
  }
  assertEq(C1.method.toString(), "static async method(){}");

  class C2 {
    static async method() {}
  }
  assertEq(C2.method.toString(), "static async method() {}");

  class C3 {
    static async method( ) {}
  }
  assertEq(C3.method.toString(), "static async method( ) {}");

  class C4 {
    static async method() { }
  }
  assertEq(C4.method.toString(), "static async method() { }");
}
testClassDeclarationStaticAsyncMethod();

function testClassDeclarationStaticGetter() {
  class C1 {
    static get abc(){}
  }
  assertEq(Object.getOwnPropertyDescriptor(C1, "abc").get.toString(), "static get abc(){}");

  class C2 {
    static get abc() {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C2, "abc").get.toString(), "static get abc() {}");

  class C3 {
    static get abc( ) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C3, "abc").get.toString(), "static get abc( ) {}");

  class C4 {
    static get abc() { }
  }
  assertEq(Object.getOwnPropertyDescriptor(C4, "abc").get.toString(), "static get abc() { }");
}
testClassDeclarationStaticGetter();

function testClassDeclarationStaticSetter() {
  class C1 {
    static set abc(x){}
  }
  assertEq(Object.getOwnPropertyDescriptor(C1, "abc").set.toString(), "static set abc(x){}");

  class C2 {
    static set abc(x) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C2, "abc").set.toString(), "static set abc(x) {}");

  class C3 {
    static set abc( x) {}
  }
  assertEq(Object.getOwnPropertyDescriptor(C3, "abc").set.toString(), "static set abc( x) {}");

  class C4 {
    static set abc(x) { }
  }
  assertEq(Object.getOwnPropertyDescriptor(C4, "abc").set.toString(), "static set abc(x) { }");
}
testClassDeclarationStaticSetter();



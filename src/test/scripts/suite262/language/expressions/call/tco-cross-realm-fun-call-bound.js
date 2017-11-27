/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-function-calls-runtime-semantics-evaluation
info: Check TypeError is thrown from correct realm with tco-call to bound class constructor from [[Call]] invocation.
description: >
  12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )
    ...
    4. If tailPosition is true, perform PrepareForTailCall().
    5. Let result be Call(func, thisValue, argList).
    6. Assert: If tailPosition is true, the above call will not return here, but instead evaluation will continue as if the following return has already occurred.
    7. Assert: If result is not an abrupt completion, then Type(result) is an ECMAScript language type.
    8. Return result.

  9.4.1.1 [[Call]] ( thisArgument, argumentsList)
    ...
    5. Return ? Call(target, boundThis, args).

  9.2.1 [[Call]] ( thisArgument, argumentsList)
    ...
    2. If F.[[FunctionKind]] is "classConstructor", throw a TypeError exception.
    3. Let callerContext be the running execution context.
    4. Let calleeContext be PrepareForOrdinaryCall(F, undefined).
    5. Assert: calleeContext is now the running execution context.
    ...

features: [tail-call-optimization, class]
---*/

// - The class constructor call is in a valid tail-call position, which means PrepareForTailCall is performed.
// - The function call returns from `otherRealm` and proceeds the tail-call in this realm.
// - Bound functions don't push a new execution context on the execution context stack.
// - Calling the class constructor throws a TypeError from the current realm, that means this realm and not `otherRealm`.
var code = "'use strict'; (function() { return (class {}).bind()(); });";

var otherRealm = $262.createRealm();
var tco = otherRealm.evalScript(code);

assert.throws(TypeError, function() {
  tco();
});

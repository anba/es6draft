/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-function-calls-runtime-semantics-evaluation
info: Check TypeError is thrown from correct realm with tco-call to bound revoked proxy from [[Construct]] invocation.
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

  9.5.12 [[Call]] (thisArgument, argumentsList)
    1. Let handler be O.[[ProxyHandler]].
    2. If handler is null, throw a TypeError exception.
    ...

features: [tail-call-optimization, Proxy]
---*/

// - The proxy call is in a valid tail-call position, which means PrepareForTailCall is performed.
// - The function call returns from `otherRealm` and proceeds the tail-call in this realm.
// - Bound functions don't push a new execution context on the execution context stack.
// - Calling the revoked proxy throws a TypeError from the current realm, that means this realm and not `otherRealm`.
var code = "'use strict'; (function() { var p = Proxy.revocable(function(){}, {}); var f = p.proxy.bind(); p.revoke(); return f(); });";

var otherRealm = $262.createRealm();
var tco = otherRealm.evalScript(code);

assert.throws(TypeError, function() {
  new tco();
});

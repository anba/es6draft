/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-function-calls-runtime-semantics-evaluation
info: |
  TCO with cross-realm callers.
description: >
  12.3.3.1 Runtime Semantics: Evaluation
    ...
    8. Return ? Construct(constructor, argList).

  7.3.13 Construct ( F [ , argumentsList [ , newTarget ]] )
    ...
    5. Return ? F.[[Construct]](argumentsList, newTarget).

  12.3.4.1 Runtime Semantics: Evaluation
    ...
    7. Let thisCall be this CallExpression.
    8. Let tailCall be IsInTailPosition(thisCall).
    9. Return ? EvaluateCall(func, ref, arguments, tailCall).

  12.3.4.2 Runtime Semantics: EvaluateCall(func, ref, arguments, tailPosition )
    ...
    7. If tailPosition is true, perform PrepareForTailCall().
    8. Let result be Call(func, thisValue, argList).
    ...

  14.8.3 Runtime Semantics: PrepareForTailCall ( )
    1. Let leafContext be the running execution context.
    2. Suspend leafContext.
    3. Pop leafContext from the execution context stack. The execution context
       now on the top of the stack becomes the running execution context.
    ...

  7.3.12 Call ( F, V [ , argumentsList ] )
    ...
    3. Return ? F.[[Call]](V, argumentsList).

  9.4.1.1 [[Call]] ( thisArgument, argumentsList )
    ...
    5. Return ? Call(target, boundThis, args).

  9.2.1 [[Call]] ( thisArgument, argumentsList )
    ...
    2. If F.[[FunctionKind]] is "classConstructor", throw a TypeError exception.
    ...

features: [tail-call-optimization, class, template]
---*/

var code =
  `(class {
     constructor() {
       var f = (class {}).bind();

       // f() is in tail-call position, so the class constructor execution
       // context is already popped from the stack when the [[Call]] happens.
       // The current execution context is the caller |new tco()| below, so
       // when 9.2.1 throws the TypeError, the TypeError is from the same
       // realm as the caller and not from the class constructor's realm.
       return f();
     }
  })`;

var otherRealm = $262.createRealm();
var tco = otherRealm.evalScript(code);

assert.throws(TypeError, function() {
  new tco();
});

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-createdynamicfunction
description: |
  Create a Function with the function body being a html close comment.
info: >
  19.2.1.1.1 Runtime Semantics: CreateDynamicFunction(constructor, newTarget, kind, args)
    ...
    10. Let parameters be the result of parsing P, interpreted as UTF-16 encoded Unicode text as described in 6.1.4,
        using parameterGoal as the goal symbol. Throw a SyntaxError exception if the parse fails.
    11. Let body be the result of parsing bodyText, interpreted as UTF-16 encoded Unicode text as described in 6.1.4,
        using goal as the goal symbol. Throw a SyntaxError exception if the parse fails.
    ...
---*/

Function("-->");

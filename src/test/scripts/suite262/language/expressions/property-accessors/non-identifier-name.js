/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
info: Token following DOT must be a valid identifier-name, test with string literal.
description: >
  ...
negative:
  type: SyntaxError
  phase: early
---*/

unresolvableReference."";

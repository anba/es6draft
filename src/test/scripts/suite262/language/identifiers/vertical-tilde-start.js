/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-names-and-keywords
info: Test VERTICAL TILDE (U+2E2F) is not recognized as ID_Start character.
description: >
  VERTICAL TILDE is in General Category 'Lm' and [:Pattern_Syntax:].
negative:
  type: SyntaxError
  phase: early
---*/

var ⸯ; // U+2E2F

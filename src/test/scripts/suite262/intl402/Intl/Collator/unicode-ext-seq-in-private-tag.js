/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
description: >
  ...
info: >
  ...
---*/

var c = Intl.Collator("de-x-u-co-phonebk");
var resolved = c.resolvedOptions();

assert.sameValue(resolved.collation, "default");

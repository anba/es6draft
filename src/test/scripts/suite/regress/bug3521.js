/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertFalse
} = Assert;

// In strings and regular expressions, are `\uD834\uDF06` and `\u{D834}\u{DF06}` equivalent?
// https://bugs.ecmascript.org/show_bug.cgi?id=3521

// Pattern

assertSame("\uD834\udf06", "\u{D834}\u{df06}");

assertTrue(/\uD834\uDF06/u.test("\uD834\udf06"));
assertTrue(/\uD834\uDF06/ui.test("\uD834\udf06"));

assertFalse(/\uD834\u{DF06}/u.test("\uD834\udf06"));
assertFalse(/\uD834\u{DF06}/ui.test("\uD834\udf06"));

assertFalse(/\u{D834}\uDF06/u.test("\uD834\udf06"));
assertFalse(/\u{D834}\uDF06/ui.test("\uD834\udf06"));

assertFalse(/\u{D834}\u{DF06}/u.test("\uD834\udf06"));
assertFalse(/\u{D834}\u{DF06}/ui.test("\uD834\udf06"));



// Character class

assertTrue(/[\u{D834}\u{DF06}]/u.test("\uD834"));
assertTrue(/[\u{D834}\u{DF06}]/u.test("\uDF06"));

assertTrue(/[\u{D834}\u{DF06}]/ui.test("\uD834"));
assertTrue(/[\u{D834}\u{DF06}]/ui.test("\uDF06"));


// Character class range

assertTrue(/[\u{D832}-\u{D834}\u{DF06}]/u.test("\uD833"));
assertFalse(/[\u{D832}-\u{D834}\u{DF06}]/u.test("\uD835"));

assertTrue(/[\u{D832}-\u{D834}\uDF06]/u.test("\uD833"));
assertFalse(/[\u{D832}-\u{D834}\uDF06]/u.test("\uD835"));

assertTrue(/[\u{D832}-\uD834\u{DF06}]/u.test("\uD833"));
assertFalse(/[\u{D832}-\uD834\u{DF06}]/u.test("\uD835"));

assertTrue(/[\u{D832}-\uD834\uDF06]/u.test("\uD833"));
assertTrue(/[\u{D832}-\uD834\uDF06]/u.test("\uD835"));

assertTrue(/[\uD832-\u{D834}\u{DF06}]/u.test("\uD833"));
assertFalse(/[\uD832-\u{D834}\u{DF06}]/u.test("\uD835"));

assertTrue(/[\uD832-\u{D834}\uDF06]/u.test("\uD833"));
assertFalse(/[\uD832-\u{D834}\uDF06]/u.test("\uD835"));

assertTrue(/[\uD832-\uD834\u{DF06}]/u.test("\uD833"));
assertFalse(/[\uD832-\uD834\u{DF06}]/u.test("\uD835"));

assertTrue(/[\uD832-\uD834\uDF06]/u.test("\uD833"));
assertTrue(/[\uD832-\uD834\uDF06]/u.test("\uD835"));


// Character class range (ignoreCase)

assertTrue(/[\u{D832}-\u{D834}\u{DF06}]/ui.test("\uD833"));
assertFalse(/[\u{D832}-\u{D834}\u{DF06}]/ui.test("\uD835"));

assertTrue(/[\u{D832}-\u{D834}\uDF06]/ui.test("\uD833"));
assertFalse(/[\u{D832}-\u{D834}\uDF06]/ui.test("\uD835"));

assertTrue(/[\u{D832}-\uD834\u{DF06}]/ui.test("\uD833"));
assertFalse(/[\u{D832}-\uD834\u{DF06}]/ui.test("\uD835"));

assertTrue(/[\u{D832}-\uD834\uDF06]/ui.test("\uD833"));
assertTrue(/[\u{D832}-\uD834\uDF06]/ui.test("\uD835"));

assertTrue(/[\uD832-\u{D834}\u{DF06}]/ui.test("\uD833"));
assertFalse(/[\uD832-\u{D834}\u{DF06}]/ui.test("\uD835"));

assertTrue(/[\uD832-\u{D834}\uDF06]/ui.test("\uD833"));
assertFalse(/[\uD832-\u{D834}\uDF06]/ui.test("\uD835"));

assertTrue(/[\uD832-\uD834\u{DF06}]/ui.test("\uD833"));
assertFalse(/[\uD832-\uD834\u{DF06}]/ui.test("\uD835"));

assertTrue(/[\uD832-\uD834\uDF06]/ui.test("\uD833"));
assertTrue(/[\uD832-\uD834\uDF06]/ui.test("\uD835"));

/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.2.5.9 PropertyDefinitionEvaluation: Missing HasOwnProperty("name") check
// https://bugs.ecmascript.org/show_bug.cgi?id=3263

var nameString = "name";
var otherString = "other";

var o = {
  c1: class {
    static name(){}
  },
  c2: class {
    static ["name"](){}
  },
  c3: class {
    static [nameString](){}
  },
  c4: class {
    static [otherString](){}
  },
};

assertSame("function", typeof o.c1.name);
assertSame("function", typeof o.c2.name);
assertSame("function", typeof o.c3.name);
assertSame("c4", o.c4.name);

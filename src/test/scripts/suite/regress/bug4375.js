/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// Treating !IsValidSimpleAssignmentTarget target as early ReferenceError for assignments/incops inconsistent with implementations
// https://bugs.ecmascript.org/show_bug.cgi?id=4375

assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments = 0`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments += 0`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments++`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); ++arguments`));

assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments = 0`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments += 0`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); arguments++`));
assertThrows(SyntaxError, () => eval(`"use strict"; fail("unreachable"); ++arguments`));

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 'eval' and 'arguments' in destructuring binding patterns

// Non-strict, object binding pattern
function nonStrict() {
  { let {eval} = {}; }
  { let {eval = 0} = {}; }
  { let {a: eval} = {}; }
  { let {a: eval = 0} = {}; }

  { let {arguments} = {}; }
  { let {arguments = 0} = {}; }
  { let {a: arguments} = {}; }
  { let {a: arguments = 0} = {}; }
}

// Non-strict, array binding pattern
function nonStrict() {
  { let [eval] = {}; }
  { let [eval = 0] = {}; }
  { let [...eval] = {}; }

  { let [arguments] = {}; }
  { let [arguments = 0] = {}; }
  { let [...arguments] = {}; }
}

// Strict, object binding pattern
{
  assertSyntaxError(`"use strict"; { let {eval} = {}; }`);
  assertSyntaxError(`"use strict"; { let {eval = 0} = {}; }`);
  assertSyntaxError(`"use strict"; { let {a: eval} = {}; }`);
  assertSyntaxError(`"use strict"; { let {a: eval = 0} = {}; }`);

  assertSyntaxError(`"use strict"; { let {arguments} = {}; }`);
  assertSyntaxError(`"use strict"; { let {arguments = 0} = {}; }`);
  assertSyntaxError(`"use strict"; { let {a: arguments} = {}; }`);
  assertSyntaxError(`"use strict"; { let {a: arguments = 0} = {}; }`);
}

// Strict, array binding pattern
{
  assertSyntaxError(`"use strict"; { let [eval] = {}; }`);
  assertSyntaxError(`"use strict"; { let [eval = 0] = {}; }`);
  assertSyntaxError(`"use strict"; { let [...eval] = {}; }`);

  assertSyntaxError(`"use strict"; { let [arguments] = {}; }`);
  assertSyntaxError(`"use strict"; { let [arguments = 0] = {}; }`);
  assertSyntaxError(`"use strict"; { let [...arguments] = {}; }`);
}

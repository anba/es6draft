/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 'eval' and 'arguments' in destructuring assignment patterns

// Non-strict, object assignment pattern
function nonStrict() {
  { ({eval} = {}); }
  { ({eval = 0} = {}); }
  { ({a: eval} = {}); }
  { ({a: eval = 0} = {}); }

  { ({arguments} = {}); }
  { ({arguments = 0} = {}); }
  { ({a: arguments} = {}); }
  { ({a: arguments = 0} = {}); }
}

// Non-strict, array assignment pattern
function nonStrict() {
  { [eval] = {}; }
  { [eval = 0] = {}; }
  { [...eval] = {}; }

  { [arguments] = {}; }
  { [arguments = 0] = {}; }
  { [...arguments] = {}; }
}

// Strict, object assignment pattern
{
  assertSyntaxError(`"use strict"; { ({eval} = {}); }`);
  assertSyntaxError(`"use strict"; { ({eval = 0} = {}); }`);
  assertSyntaxError(`"use strict"; { ({a: eval} = {}); }`);
  assertSyntaxError(`"use strict"; { ({a: eval = 0} = {}); }`);

  assertSyntaxError(`"use strict"; { ({arguments} = {}); }`);
  assertSyntaxError(`"use strict"; { ({arguments = 0} = {}); }`);
  assertSyntaxError(`"use strict"; { ({a: arguments} = {}); }`);
  assertSyntaxError(`"use strict"; { ({a: arguments = 0} = {}); }`);
}

// Strict, array assignment pattern
{
  assertSyntaxError(`"use strict"; { [eval] = {}; }`);
  assertSyntaxError(`"use strict"; { [eval = 0] = {}; }`);
  assertSyntaxError(`"use strict"; { [...eval] = {}; }`);

  assertSyntaxError(`"use strict"; { [arguments] = {}; }`);
  assertSyntaxError(`"use strict"; { [arguments = 0] = {}; }`);
  assertSyntaxError(`"use strict"; { [...arguments] = {}; }`);
}

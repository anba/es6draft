/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

function f(a) { "use strict"; }
assertSyntaxError(`function f(a = 0) { "use strict"; }`);
assertSyntaxError(`function f([a]) { "use strict"; }`);
assertSyntaxError(`function f({a}) { "use strict"; }`);
assertSyntaxError(`function f(...a) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f(a = 0) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f([a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f({a}) { "use strict"; }`);
assertSyntaxError(`"use strict"; function f(...a) { "use strict"; }`);

function* g(a) { "use strict"; }
assertSyntaxError(`function* g(a = 0) { "use strict"; }`);
assertSyntaxError(`function* g([a]) { "use strict"; }`);
assertSyntaxError(`function* g({a}) { "use strict"; }`);
assertSyntaxError(`function* g(...a) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* g(a = 0) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* g([a]) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* g({a}) { "use strict"; }`);
assertSyntaxError(`"use strict"; function* g(...a) { "use strict"; }`);

a => { "use strict"; };
(a) => { "use strict"; };
assertSyntaxError(`(a = 0) => { "use strict"; };`);
assertSyntaxError(`([a]) => { "use strict"; };`);
assertSyntaxError(`({a}) => { "use strict"; };`);
assertSyntaxError(`(...a) => { "use strict"; };`);
assertSyntaxError(`"use strict"; (a = 0) => { "use strict"; };`);
assertSyntaxError(`"use strict"; ([a]) => { "use strict"; };`);
assertSyntaxError(`"use strict"; ({a}) => { "use strict"; };`);
assertSyntaxError(`"use strict"; (...a) => { "use strict"; };`);

({m(a) { "use strict"; }});
assertSyntaxError(`({m(a = 0) { "use strict"; }});`);
assertSyntaxError(`({m([a]) { "use strict"; }});`);
assertSyntaxError(`({m({a}) { "use strict"; }});`);
assertSyntaxError(`({m(...a) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({m(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({m([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({m({a}) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({m(...a) { "use strict"; }});`);

({set v(a) { "use strict"; }});
assertSyntaxError(`({set v(a = 0) { "use strict"; }});`);
assertSyntaxError(`({set v([a]) { "use strict"; }});`);
assertSyntaxError(`({set v({a}) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({set v(a = 0) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({set v([a]) { "use strict"; }});`);
assertSyntaxError(`"use strict"; ({set v({a}) { "use strict"; }});`);

function strict() {
  "use strict";
  function f(a) { }
  function f(a = 0) { }
  function f([a]) { }
  function f({a}) { }
  function f(...a) { }

  function* g(a) { }
  function* g(a = 0) { }
  function* g([a]) { }
  function* g({a}) { }
  function* g(...a) { }

  a => { };
  (a) => { };
  (a = 0) => { };
  ([a]) => { };
  ({a}) => { };
  (...a) => { };

  ({m(a) { }});
  ({m(a = 0) { }});
  ({m([a]) { }});
  ({m({a}) { }});
  ({m(...a) { }});

  ({set v(a) { }});
  ({set v(a = 0) { }});
  ({set v([a]) { }});
  ({set v({a}) { }});
}

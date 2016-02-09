/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// HTML comments syntax must be documented
// https://bugs.ecmascript.org/show_bug.cgi?id=2610

Function(`
<!-- -->
`);

Function(`
  <!-- -->
`);

Function(`
<!--
`);

Function(`
  <!--
`);

Function(`
-->
`);

Function(`
  -->
`);


// Allowed in strict mode

Function(`
"use strict";
<!-- -->
`);

Function(`
"use strict";
  <!-- -->
`);

Function(`
"use strict";
<!--
`);

Function(`
"use strict";
  <!--
`);

Function(`
"use strict";
-->
`);

Function(`
"use strict";
  -->
`);


// Not allowed in module code

assertThrows(SyntaxError, () => parseModule(`
<!-- -->
`));

assertThrows(SyntaxError, () => parseModule(`
  <!-- -->
`));

assertThrows(SyntaxError, () => parseModule(`
<!--
`));

assertThrows(SyntaxError, () => parseModule(`
  <!--
`));

assertThrows(SyntaxError, () => parseModule(`
-->
`));

assertThrows(SyntaxError, () => parseModule(`
  -->
`));

// PostDecrement(Ident) > Ident
parseModule(`a-->b`);

// Ident < Not(PreDecrement(Ident))
parseModule(`a<!--b`);

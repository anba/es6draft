/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// B.3.1  __proto__ Property Names in Object Initializers

// Duplicate __proto__ property definition is a syntax error
assertSyntaxError(`({__proto__: null, __proto__: null})`);
assertSyntaxError(`({__proto__: null, "__proto__": null})`);
assertSyntaxError(`({__proto__: null, '__proto__': null})`);
assertSyntaxError(`"use strict"; ({__proto__: null, __proto__: null})`);
assertSyntaxError(`"use strict"; ({__proto__: null, "__proto__": null})`);
assertSyntaxError(`"use strict"; ({__proto__: null, '__proto__': null})`);

// __proto__ and get __proto__() is allowed
Function(`({__proto__: null, get __proto__(){}})`);
Function(`({get __proto__(){}, __proto__: null})`);
Function(`"use strict"; ({__proto__: null, get __proto__(){}})`);
Function(`"use strict"; ({get __proto__(){}, __proto__: null})`);

// __proto__ and set __proto__(v) is allowed
Function(`({__proto__: null, set __proto__(v){}})`);
Function(`({set __proto__(v){}, __proto__: null})`);
Function(`"use strict"; ({__proto__: null, set __proto__(v){}})`);
Function(`"use strict"; ({set __proto__(v){}, __proto__: null})`);

// __proto__ and __proto__() is allowed
Function(`({__proto__: null, __proto__(){}})`);
Function(`({__proto__(){}, __proto__: null})`);
Function(`"use strict"; ({__proto__: null, __proto__(){}})`);
Function(`"use strict"; ({__proto__(){}, __proto__: null})`);

// __proto__ and *__proto__() is allowed
Function(`({__proto__: null, *__proto__(){}})`);
Function(`({*__proto__(){}, __proto__: null})`);
Function(`"use strict"; ({__proto__: null, *__proto__(){}})`);
Function(`"use strict"; ({*__proto__(){}, __proto__: null})`);

// __proto__ and ["__proto__"] is allowed
Function(`({__proto__: null, ["__proto__"]: null})`);
Function(`({["__proto__"]: null, __proto__: null})`);
Function(`"use strict"; ({__proto__: null, ["__proto__"]: null})`);
Function(`"use strict"; ({["__proto__"]: null, __proto__: null})`);

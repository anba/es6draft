/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

function toStr(number, radix, pad) {
  let s = number.toString(radix);
  return "0".repeat(pad - s.length) + s;
}

const transformer = [
  function noEscape(name) {
    return name;
  },
  function escapeFirstChar(name) {
    return `\\u${toStr(name.charCodeAt(0), 16, 4)}${name.substring(1)}`
  },
  function escapeSecondChar(name) {
    return `${name.substring(0, 1)}\\u${toStr(name.charCodeAt(1), 16, 4)}${name.substring(2)}`
  },
];

// non-strict function code
for (let t of transformer) {
  let name = t("yield");
  Function(`function f(){ function ${name}(){} }`);
  Function(`function f(){ (function ${name}(){}) }`);
  Function(`function f(){ function* ${name}(){} }`);
  assertSyntaxError(`function f(){ (function* ${name}(){}) }`);
}

// strict function code
for (let t of transformer) {
  let name = t("yield");
  assertSyntaxError(`function f(){"use strict"; function ${name}(){} }`);
  assertSyntaxError(`function f(){"use strict"; (function ${name}(){}) }`);
  assertSyntaxError(`function f(){"use strict"; function* ${name}(){} }`);
  assertSyntaxError(`function f(){"use strict"; (function* ${name}(){}) }`);
}

// non-strict generator code
for (let t of transformer) {
  let name = t("yield");
  assertSyntaxError(`function* f(){ function ${name}(){} }`);
  Function(`function* f(){ (function ${name}(){}) }`);
  assertSyntaxError(`function* f(){ function* ${name}(){} }`);
  assertSyntaxError(`function* f(){ (function* ${name}(){}) }`);
}

// strict generator code
for (let t of transformer) {
  let name = t("yield");
  assertSyntaxError(`function* f(){"use strict"; function ${name}(){} }`);
  assertSyntaxError(`function* f(){"use strict"; (function ${name}(){}) }`);
  assertSyntaxError(`function* f(){"use strict"; function* ${name}(){} }`);
  assertSyntaxError(`function* f(){"use strict"; (function* ${name}(){}) }`);
}

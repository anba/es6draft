/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

let tests = [
  {
    pattern: `...{}`,
    asserts: ``,
    arguments: ``,
  },
  {
    pattern: `...{length}`,
    asserts: `assertSame(0, length);`,
    arguments: ``,
  },
  {
    pattern: `...{length: len}`,
    asserts: `assertSame(2, len);`,
    arguments: `1, 2`,
  },
  {
    pattern: `...{2: item}`,
    asserts: `assertSame(3, item);`,
    arguments: `1, 2, 3`,
  },
  {
    pattern: `...{1: item = 123}`,
    asserts: `assertSame(123, item);`,
    arguments: `6, void 0, 7`,
  },
  {
    pattern: `...[]`,
    asserts: ``,
    arguments: ``,
  },
  {
    pattern: `...[item1, item2, item3]`,
    asserts: `
      assertSame(1, item1);
      assertSame(2, item2);
      assertSame(void 0, item3);
    `,
    arguments: `1, 2`,
  },
  {
    pattern: `...[...[item1, item2, item3]]`,
    asserts: `
      assertSame(1, item1);
      assertSame(2, item2);
      assertSame(3, item3);
    `,
    arguments: `1, 2, 3`,
  },
];

for (let make of [
  (p, a, r) => `var [${p}] = [${r}]; ${a}`,
  (p, a, r) => `let [${p}] = [${r}]; ${a}`,
  (p, a, r) => `const [${p}] = [${r}]; ${a}`,
  (p, a, r) => `(function(${p}) { ${a} })(${r})`,
  (p, a, r) => `(function*(${p}) { ${a} })(${r}).next()`,
  (p, a, r) => `((${p}) => { ${a} })(${r})`,
]) {
  for (let {pattern, asserts, arguments} of tests) {
    let code = make(pattern, asserts, arguments);
    Function(code)();
  }
}

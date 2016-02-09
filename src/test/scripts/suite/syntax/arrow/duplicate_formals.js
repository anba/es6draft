/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

var parametersList = [
  `(a, a)`,
  `(a, ...a)`,

  // Binding array elements
  `(a, [a])`,
  `([a], a)`,
  `([a], ...a)`,
  `([a, a])`,
  `([a], [a])`,

  // Binding object elements
  `(a, {a})`,
  `(a, {b: a})`,
  `({a}, a)`,
  `({a}, ...a)`,
  `({b: a}, a)`,
  `({b: a}, ...a)`,
  `({a, a})`,
  `({b: a, a})`,
  `({a, c: a})`,
  `({b: a, c: a})`,
  `({a}, {a})`,
  `({b: a}, {a})`,
  `({a}, {c: a})`,
  `({b: a}, {c: a})`,
];

for (let parameters of parametersList) {
  assertSyntaxError(`${parameters} => { }`);
  assertSyntaxError(`"use strict"; ${parameters} => { }`);
}

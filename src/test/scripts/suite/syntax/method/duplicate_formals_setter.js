/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

var parametersList = [
  // Binding array elements
  `([a, a])`,

  // Binding object elements
  `({a, a})`,
  `({b: a, a})`,
  `({a, c: a})`,
  `({b: a, c: a})`,
];

for (let parameters of parametersList) {
  assertSyntaxError(`({ set p ${parameters} { } })`);
  assertSyntaxError(`"use strict"; ({ set ${parameters} { } })`);
}

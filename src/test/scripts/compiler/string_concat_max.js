/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function obj(n, r) {
  return {
    valueOf() {
      r.push(n);
      return n;
    },
    toString() {
      throw new Error();
    }
  };
}

// Test arguments length
{
  // Test more arguments than |max_jvm_args|.
  const max_jvm_args = 255;
  const start_args = max_jvm_args - 10;
  const max_args = max_jvm_args + 10;

  function* names(i) {
    for (let j = 0; j < i; ++j) {
      yield `_${j.toString(36)}`;
    }
  }
  let d = Date.now();
  for (let i = start_args; i <= max_args; ++i) {
    let source = `
      let result = [];
      let ${[...names(i)].map(name =>
          `${name} = obj("${name}", result)`
        ).join(",\n")
      };
      let concat = ${[`""`, ...names(i)].join(" + ")};
      assertEq(concat, "${[...names(i)].join("")}");
      assertEq(result.join(""), "${[...names(i)].join("")}");
    `;
    Function(source)();
  }
  Date.now() - d;
}

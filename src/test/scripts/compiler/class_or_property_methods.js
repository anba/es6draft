/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function methods(separator) {
  var s = "";
  for (var i = 0; i < 500; ++i) s += `m${i}() {}${separator}`;
  return s
}

function id(f) { return f; }

// No crash
eval(`
  class C {
    ${methods(";")}

    static name() {}
  }
`);

// No crash
eval(`
  class C {
    ${methods(";")}

    @id(() => {})
    decorated() {}
  }
`);

// No crash
eval(`
  ({
    ${methods(",")}

    @id(() => {})
    decorated() {}
  });
`);

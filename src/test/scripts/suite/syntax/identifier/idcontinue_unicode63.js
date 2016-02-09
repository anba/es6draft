/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function test(start, end) {
  for (let cp = start; cp <= end;) {
    let source = "var obj = {};\n";
    for (let i = 0; cp <= end && i < 1000; ++cp, ++i) {
      source += `obj.A${String.fromCodePoint(cp)};\n`;
    }
    eval(source);
  }
}

// Delta compared to Unicode 6.2
test(0x1a19, 0x1a1a);
test(0x1a1b, 0x1a1b);

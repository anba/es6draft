/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function test(start, end) {
  for (let cp = start; cp <= end;) {
    let source = "var obj = {};\n";
    for (let i = 0; cp <= end && i < 1000; ++cp, ++i) {
      source += `obj.${String.fromCodePoint(cp)};\n`;
    }
    eval(source);
  }
}

// Delta compared to Unicode 8.0
test(0x08b6, 0x08bd);
test(0x0c80, 0x0c80);
test(0x0d54, 0x0d56);
test(0x1880, 0x1884);
test(0x1885, 0x1886);
test(0x1887, 0x18a8);
test(0x1c80, 0x1c88);
test(0xa790, 0xa7ae);
test(0x104b0, 0x104d3);
test(0x104d8, 0x104fb);
test(0x11400, 0x11434);
test(0x11447, 0x1144a);
test(0x11c00, 0x11c08);
test(0x11c0a, 0x11c2e);
test(0x11c40, 0x11c40);
test(0x11c72, 0x11c8f);
test(0x16fe0, 0x16fe0);
test(0x17000, 0x187ec);
test(0x18800, 0x18af2);
test(0x1e900, 0x1e943);

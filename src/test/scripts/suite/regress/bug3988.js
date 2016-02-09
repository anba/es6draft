/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 10.1: "not required to perform any normalization of text"
// https://bugs.ecmascript.org/show_bug.cgi?id=3988

const chars = [
  {
    charCodes: [0x03D3],
    normalized: {
      NFC: [0x03D3],
      NFD: [0x03D2, 0x0301],
      NFKC: [0x038E],
      NFKD: [0x03A5, 0x0301],
    }
  },
  {
    charCodes: [0x03D4],
    normalized: {
      NFC: [0x03D4],
      NFD: [0x03D2, 0x0308],
      NFKC: [0x03AB],
      NFKD: [0x03A5, 0x0308],
    }
  },
  {
    charCodes: [0x1E9B],
    normalized: {
      NFC: [0x1E9B],
      NFD: [0x017F, 0x0307],
      NFKC: [0x1E61],
      NFKD: [0x0073, 0x0307],
    }
  },
];

function assertCharCodes(charCodes, s) {
  assertSame(charCodes.length, s.length);
  for (let i = 0; i < s.length; ++i) {
    assertSame(charCodes[i], s.charCodeAt(i));
  }
}

for (let {charCodes, normalized} of chars) {
  let s = String.fromCharCode(...charCodes);
  assertCharCodes(charCodes, s);
  for (let form in normalized) {
    let r = String.fromCharCode(...normalized[form]);
    assertCharCodes(normalized[form], r);
    assertSame(r, s.normalize(form));
    assertSame(r, eval(`"${r}"`));
    let source = normalized[form].reduce((a, c) => `${a}\\u${"000".slice(Math.log2(c) >> 2)}${c.toString(16)}`, "");
    assertSame(r, eval(`"${source}"`));
  }
}

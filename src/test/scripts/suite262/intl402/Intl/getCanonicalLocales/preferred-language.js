/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Call Intl.getCanonicalLocales function with grandfathered language tags.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    7. Repeat, while k < len
      ...
      c. If kPresent is true, then
        ...
        v. Let canonicalizedTag be CanonicalizeLanguageTag(tag).
        vi. If canonicalizedTag is not an element of seen, append canonicalizedTag as the last element of seen.
      ...
includes: [testIntl.js]
---*/

// Generated from http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
// File-Date: 2017-08-15
var canonicalizedTags = {
  "in": "id",
  "iw": "he",
  "ji": "yi",
  "jw": "jv",
  "mo": "ro",
  "aam": "aas",
  "adp": "dz",
  "aue": "ktz",
  "ayx": "nun",
  "bgm": "bcg",
  "bjd": "drl",
  "ccq": "rki",
  "cjr": "mom",
  "cka": "cmr",
  "cmk": "xch",
  "coy": "pij",
  "cqu": "quh",
  "drh": "khk",
  "drw": "prs",
  "gav": "dev",
  "gfx": "vaj",
  "ggn": "gvr",
  "gti": "nyc",
  "guv": "duz",
  "hrr": "jal",
  "ibi": "opa",
  "ilw": "gal",
  "jeg": "oyb",
  "kgc": "tdf",
  "kgh": "kml",
  "koj": "kwv",
  "krm": "bmf",
  "ktr": "dtp",
  "kvs": "gdj",
  "kwq": "yam",
  "kxe": "tvd",
  "kzj": "dtp",
  "kzt": "dtp",
  "lii": "raq",
  "lmm": "rmx",
  "meg": "cir",
  "mst": "mry",
  "mwj": "vaj",
  "myt": "mry",
  "nad": "xny",
  "nnx": "ngv",
  "nts": "pij",
  "oun": "vaj",
  "pcr": "adx",
  "pmc": "huw",
  "pmu": "phr",
  "ppa": "bfy",
  "ppr": "lcq",
  "pry": "prt",
  "puz": "pub",
  "sca": "hle",
  "skk": "oyb",
  "tdu": "dtp",
  "thc": "tpo",
  "thx": "oyb",
  "tie": "ras",
  "tkk": "twm",
  "tlw": "weo",
  "tmp": "tyj",
  "tne": "kak",
  "tnf": "prs",
  "tsf": "taj",
  "uok": "ema",
  "xba": "cax",
  "xia": "acn",
  "xkh": "waw",
  "xsj": "suj",
  "ybd": "rki",
  "yma": "lrr",
  "ymt": "mtm",
  "yos": "zom",
  "yuu": "yug",
};

// make sure the data above is correct
Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var canonicalizedTag = canonicalizedTags[tag];
  assert(
    isCanonicalizedStructurallyValidLanguageTag(canonicalizedTag),
    "Test data \"" + canonicalizedTag + "\" is not canonicalized and structurally valid language tag."
  );
});

Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var canonicalLocales = Intl.getCanonicalLocales(tag);
  assert.sameValue(canonicalLocales.length, 1);
  assert.sameValue(canonicalLocales[0], canonicalizedTags[tag]);
});

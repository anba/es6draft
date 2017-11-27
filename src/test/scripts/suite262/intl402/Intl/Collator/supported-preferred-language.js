/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
description: >
  ...
info: >
  ...
includes: [testIntl.js]
---*/

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
  "bjd": "drl",
  "ccq": "rki",
  "cjr": "mom",
  "cka": "cmr",
  "cmk": "xch",
  "drh": "khk",
  "drw": "prs",
  "gav": "dev",
  "gfx": "vaj",
  "gti": "nyc",
  "hrr": "jal",
  "ibi": "opa",
  "ilw": "gal",
  "kgh": "kml",
  "koj": "kwv",
  "kwq": "yam",
  "kxe": "tvd",
  "lii": "raq",
  "lmm": "rmx",
  "meg": "cir",
  "mst": "mry",
  "mwj": "vaj",
  "myt": "mry",
  "nnx": "ngv",
  "oun": "vaj",
  "pcr": "adx",
  "pmu": "phr",
  "ppr": "lcq",
  "puz": "pub",
  "sca": "hle",
  "thx": "oyb",
  "tie": "ras",
  "tkk": "twm",
  "tlw": "weo",
  "tnf": "prs",
  "tsf": "taj",
  "uok": "ema",
  "xia": "acn",
  "xsj": "suj",
  "ybd": "rki",
  "yma": "lrr",
  "ymt": "mtm",
  "yos": "zom",
  "yuu": "yug",
};

//make sure the data above is correct
Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var canonicalizedTag = canonicalizedTags[tag];
  assert(
    isCanonicalizedStructurallyValidLanguageTag(canonicalizedTag),
    "Test data \"" + canonicalizedTag + "\" is not canonicalized and structurally valid language tag."
  );
});

// TODO: Also test with Unicode extension sequence?
// TODO: Also test with private tag?
Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var oldName = Intl.Collator.supportedLocalesOf(tag);
  var newName = Intl.Collator.supportedLocalesOf(canonicalizedTags[tag]);

  assert.sameValue(oldName.length, newName.length);
  if (oldName.length > 0) {
    assert.sameValue(oldName[0], newName[0]);
  }
});

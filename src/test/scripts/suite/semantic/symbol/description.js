/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 6.1.5.1 Well-Known Symbols
const WellKnownSymbols = [
  "hasInstance",
  "isConcatSpreadable",
  "iterator",
  "match",
  "observable",
  "replace",
  "search",
  "species",
  "split",
  "toPrimitive",
  "toStringTag",
  "unscopables",
];

// assert `WellKnownSymbols` contains all exported symbol-typed names
let exportedNames = Object.getOwnPropertyNames(Symbol)
                          .filter(n => n !== "caller" && n !== "arguments")
                          .filter(n => typeof Symbol[n] === "symbol");
let difference = [
  ...WellKnownSymbols.filter(n => exportedNames.indexOf(n) < 0),
  ...exportedNames.filter(n => WellKnownSymbols.indexOf(n) < 0),
];
assertSame(0, difference.length, "symmetric difference not empty: " + difference);

function getDescription(sym) {
  let s = Object(sym).toString();
  return s.match(/^Symbol\((.*)\)$/)[1];
}

// test [[Description]] for well-known symbols
for (let name of WellKnownSymbols) {
  assertSame(`Symbol.${name}`, getDescription(Symbol[name]));
}

// test [[Description]] for custom symbols
assertSame("", getDescription(Symbol()));
assertSame("", getDescription(Symbol("")));
assertSame("abc", getDescription(Symbol("abc")));

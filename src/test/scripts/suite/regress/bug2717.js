/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Identifier names: clarify that `ID_Start` / `Other_ID_Start` are included
// https://bugs.ecmascript.org/show_bug.cgi?id=2717

// Unicode 6.3, Other_ID_Start
const Other_ID_Start = [
  '\u2118', '\u212E', '\u309B', '\u309C'
];
const Other_ID_Continue = [
  '\u00B7', '\u0387', '\u1369', '\u136A',
  '\u136B', '\u136C', '\u136D', '\u136E',
  '\u136F', '\u1370', '\u1371', '\u19DA', 
];

// Other_ID_Start as first character
{
  let source = "";
  for (let c of Other_ID_Start) {
    source += `var ${c};\n`;
  }
  Function(source);
}

// Other_ID_Start as non-first character
{
  let source = "";
  for (let c of Other_ID_Start) {
    source += `var a${c};\n`;
  }
  Function(source);
}

// Other_ID_Continue as first character
{
  for (let c of Other_ID_Continue) {
    let source = `var ${c};\n`;
    assertSyntaxError(source);
  }
}

// Other_ID_Continue as non-first character
{
  let source = "";
  for (let c of Other_ID_Continue) {
    source += `var a${c};\n`;
  }
  Function(source);
}

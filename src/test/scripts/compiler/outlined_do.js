/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No crash
eval(`L: { void { ${"a:0,".repeat(1000)} b: do { break L; } } }`);

// No crash
Function(`void { ${"a:0,".repeat(1000)} b: do { return; } }`);

// No crash
eval(`L: void [ ${"0,".repeat(1000)} do { break L; } ]`);

// No crash
Function(`L: void [ ${"0,".repeat(1000)} do { return; } ]`);

// No crash
eval(`L: void ( ${"0,".repeat(1000)} do { break L; } )`);

// No crash
Function(`void ( ${"0,".repeat(1000)} do { return; } )`);

// No crash
eval(`L: void class { ${"a(){};".repeat(200)} [do { break L; }](){} }`);

// No crash
Function(`void class { ${"a(){};".repeat(200)} [do { return; }](){} }`);

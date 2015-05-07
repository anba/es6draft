/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

const True = () => true;
const False = () => false;
const Value = (v = Math.random()) => v;

// Empty completion returns undefined
assertSame(void 0, eval(``));
assertSame(void 0, eval(`;`));
assertSame(void 0, eval(`;;;`));
assertSame(void 0, eval(`{}`));
assertSame(void 0, eval(`{;}`));
assertSame(void 0, eval(`var foo;`));
assertSame(void 0, eval(`var foo = 0;`));
assertSame(void 0, eval(`let foo;`));
assertSame(void 0, eval(`let foo = 0;`));
assertSame(void 0, eval(`const foo = 0;`));
assertSame(void 0, eval(`function f(){}`));
assertSame(void 0, eval(`function* g(){}`));
assertSame(void 0, eval(`class C {}`));

// Expression statement
assertSame(1, eval(`1;`));
assertSame(2, eval(`1; 2;`));

// If-Statement
assertSame(void 0, eval(`if (True()) ;`));
assertSame(void 0, eval(`if (False()) ;`));
assertSame(1, eval(`if (True()) 1;`));
assertSame(void 0, eval(`if (False()) 1;`));
assertSame(1, eval(`if (True()) 1; else 2;`));
assertSame(2, eval(`if (False()) 1; else 2;`));

// If-Statement with leading expression statement
assertSame(1, eval(`0; if (True()) 1;`));
assertSame(void 0, eval(`0; if (False()) 1;`));
assertSame(1, eval(`0; if (True()) 1; else 2;`));
assertSame(2, eval(`0; if (False()) 1; else 2;`));

// Do-while Statement
assertSame(void 0, eval(`do ; while (False());`));
assertSame(void 0, eval(`0; do ; while (False());`));
assertSame(1, eval(`do 1; while (False());`));
assertSame(1, eval(`0; do 1; while (False());`));

// Do-while Statement with break/continue
assertSame(1, eval(`0; do { 1; break; } while (False());`));
assertSame(1, eval(`0; do { 1; continue; } while (False());`));

// Do-while, If, Break/Continue
assertSame(1, eval(`var c = 0; 0; do if (c) break; else c = 1; while (True());`));
assertSame(1, eval(`var c = 0; 0; do if (c++) continue; else c = 1; while (c < 2);`));

// With-Statement
assertSame(void 0, eval(`0; with ({}) ;`));
assertSame(1, eval(`0; with ({}) 1;`));
assertSame(void 0, eval(`0; L: with ({}) break L;`));
assertSame(1, eval(`0; L: { 1; with ({}) break L; }`));

// Switch-Statement
assertSame(void 0, eval(`0; switch (Value()) { }`));
assertSame(void 0, eval(`0; switch (Value(0)) { case Value(1): 1; }`));
assertSame(1, eval(`0; switch (Value(0)) { case Value(0): 1; }`));
assertSame(1, eval(`0; switch (Value(0)) { default: 1; }`));
assertSame(1, eval(`0; switch (Value(0)) { case Value(0): 1; break; default: 2; }`));
assertSame(2, eval(`0; switch (Value(1)) { case Value(0): 1; break; default: 2; }`));
assertSame(2, eval(`0; switch (Value(0)) { case Value(0): 1; default: 2; }`));
assertSame(2, eval(`0; switch (Value(1)) { case Value(0): 1; default: 2; }`));
assertSame(1, eval(`0; switch (Value(0)) { default: 2; break; case Value(0): 1; }`));
assertSame(2, eval(`0; switch (Value(1)) { default: 2; break; case Value(0): 1; }`));
assertSame(1, eval(`0; switch (Value(0)) { default: 2; case Value(0): 1; }`));
assertSame(1, eval(`0; switch (Value(1)) { default: 2; case Value(0): 1; }`));

// Labelled-Statement
assertSame(0, eval(`0; L: ;`));
assertSame(void 0, eval(`0; L: break L;`));
assertSame(void 0, eval(`0; L: if (True()) break L;`));
assertSame(void 0, eval(`0; L: if (False()) break L;`));
assertSame(void 0, eval(`0; L: if (True()) break L; else 1;`));
assertSame(1, eval(`0; L: if (False()) break L; else 1;`));
assertSame(1, eval(`0; L: { if (False()) break L; if (True()) 1; else 2; break L; }`));
assertSame(2, eval(`0; L: { if (False()) break L; if (False()) 1; else 2; break L; }`));
assertSame(void 0, eval(`0; L: { if (True()) break L; if (True()) 1; else 2; break L; }`));
assertSame(void 0, eval(`0; L: { if (True()) break L; if (False()) 1; else 2; break L; }`));
assertSame(void 0, eval(`K: { 1; L: if (True()) break L; else break K; }`));
assertSame(1, eval(`K: { 1; L: if (False()) break L; else break K; }`));

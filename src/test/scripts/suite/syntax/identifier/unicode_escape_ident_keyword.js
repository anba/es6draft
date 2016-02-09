/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

function toStr(number, radix, pad) {
  let s = number.toString(radix);
  return "0".repeat(pad - s.length) + s;
}

function escapeFirstChar(name) {
  return `\\u${toStr(name.charCodeAt(0), 16, 4)}${name.substring(1)}`
}

function escapeSecondChar(name) {
  return `${name.substring(0, 1)}\\u${toStr(name.charCodeAt(1), 16, 4)}${name.substring(2)}`
}

// SyntaxError, not a CallExpression / IdentifierReference
const data = {
  break: "do{\n break; \n}while(0);",
  case: "switch(a){ default: case\nL:; }",
  class: "class\nF\n{};",
  const: "const[a] = [];",
  continue: "do{\n continue; \n}while(0);",
  debugger: "debugger;",
  delete: "delete(0);",
  do: "do;while(0);",
  for: "for(a in b);",
  function: "function\nF\n(a)\n{}",
  if: "if(0);",
  in: "a\nin\nb",
  instanceof: "a\ninstanceof\nb",
  else: "if(0);else;",
  new: "new(0);",
  return: "return(0);",
  super: "super.x;",
  switch: "switch(0)\n{}",
  this: "this.x;",
  throw: "throw(0);",
  typeof: "typeof(0);",
  var: "var[a] = [];",
  void: "void(0);",
  while: "while(0);",
  with: "with(0);",
  yield: "function*g() { yield(0) }"
};

Object.keys(data).forEach(key => {
  let d = data[key];

  // Ensure example data is correct
  if (key !== "super") {
    Function(d);
  }
  Function(d.replace(key, key + key));

  // Test SyntaxError is issued
  assertSyntaxError(d.replace(key, escapeFirstChar(key)));
  assertSyntaxError(d.replace(key, escapeSecondChar(key)));
});

Object.keys(data).forEach(key => {
  let d = data[key];

  // Ensure example data is correct
  Function(`({m(){ ${d} }});`);
  Function(`({m(){ ${d.replace(key, key + key)} }});`);

  // Test SyntaxError is issued
  assertSyntaxError(`({m(){ ${d.replace(key, escapeFirstChar(key))} }});`);
  assertSyntaxError(`({m(){ ${d.replace(key, escapeSecondChar(key))} }});`);
});

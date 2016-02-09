/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Test "arguments" binding in functions


// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = true, needsParameterEnvironment = false
{
  let arguments = 0;
  function f() {
    var arguments = () => {};
    return arguments;
  }
  assertSame("function", typeof f());
}

// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = true, needsParameterEnvironment = true
{
  let arguments = 0;
  function f(a = 0) {
    var arguments = () => {};
    return arguments;
  }
  assertSame("function", typeof f());
}


// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = false, needsParameterEnvironment = false
{
  let arguments = 0;
  function f() {
    function arguments(){}
    return arguments;
  }
  assertSame("function", typeof f());
}

// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = false, needsParameterEnvironment = true
{
  let arguments = 0;
  function f(a = 0) {
    function arguments(){}
    return arguments;
  }
  assertSame("function", typeof f());
}


// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = false, needsParameterEnvironment = false
{
  let arguments = 0;
  function f() {
    var arguments = "";
    function arguments(){}
    return arguments;
  }
  assertSame("string", typeof f());
}

// needsSpecialArgumentsBinding = true, argumentsObjectNeeded = false, needsParameterEnvironment = true
{
  let arguments = 0;
  function f(a = 0) {
    var arguments = "";
    function arguments(){}
    return arguments;
  }
  assertSame("string", typeof f());
}


// FIXME: spec bug xxxx
// needsSpecialArgumentsBinding = ?, argumentsObjectNeeded = false, needsParameterEnvironment = false
{
  let arguments = 0;
  function f() {
    let arguments = () => {};
    return arguments;
  }
  assertSame("function", typeof f());
}

// FIXME: spec bug xxxx
// needsSpecialArgumentsBinding = ?, argumentsObjectNeeded = false, needsParameterEnvironment = true
{
  let arguments = 0;
  function f(a = 0) {
    let arguments = () => {};
    return arguments;
  }
  assertSame("function", typeof f());
}


// needsSpecialArgumentsBinding = false, argumentsObjectNeeded = false, needsParameterEnvironment = false
{
  let arguments = 0;
  function f(arguments) {
    return arguments;
  }
  assertSame("undefined", typeof f());
  assertSame("function", typeof f(() => {}));
}

// needsSpecialArgumentsBinding = false, argumentsObjectNeeded = false, needsParameterEnvironment = true
{
  let arguments = 0;
  function f(arguments = () => {}) {
    return arguments;
  }
  assertSame("function", typeof f());
  assertSame("string", typeof f(""));
}

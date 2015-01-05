/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertTrue, assertThrows, fail
} = Assert;

// 8.1.1.4.14, 8.1.1.4.15, 8.1.1.4.16: Change HasBinding to HasOwnProperty
// https://bugs.ecmascript.org/show_bug.cgi?id=3279

var indirectEval = eval;
var global = indirectEval("this");

// We assume Object.prototype is part of the prototype chain of the global object
assertTrue(Object.prototype.isPrototypeOf(global));

function loggingAccessor(name) {
  var log = [];
  function getAccessor() {
    log.push("get '" + name + "'");
  }
  function setAccessor(value) {
    log.push("set '" + name + "' to: " + value);
  }
  return {
    accessor: {configurable: true, get: getAccessor, set: setAccessor},
    log,
  };
}

function poisonedAccessor(name) {
  function getAccessor() {
    fail `unexpected get '${name}'`;
  }
  function setAccessor(value) {
    fail `unexpected set '${name}' to: ${value}`;
  }
  return {configurable: true, get: getAccessor, set: setAccessor};
}

// Test 1a: non-configurable data property on global, try var declaration
function testNonConfigDataVar() {
  Object.defineProperty(global, "nonConfigDataVar", {configurable: false, value: -1});
  indirectEval("var nonConfigDataVar = 0");
  assertSame(-1, indirectEval("nonConfigDataVar"));
}

// Test 1b: non-configurable data property on global, try function declaration
function testNonConfigDataFun() {
  Object.defineProperty(global, "nonConfigDataFun", {configurable: false, value: -1});
  assertThrows(TypeError, () => indirectEval("function nonConfigDataFun(){}"));
  assertSame(-1, indirectEval("nonConfigDataFun"));
}

// Test 2a: non-configurable data property on prototype, try var declaration
function testNonConfigDataProtoVar() {
  Object.defineProperty(Object.prototype, "nonConfigDataProtoVar", {configurable: false, value: -1});
  indirectEval("var nonConfigDataProtoVar = 0");
  assertSame(-1, indirectEval("nonConfigDataProtoVar"));
}

// Test 2b: non-configurable data property on prototype, try function declaration
function testNonConfigDataProtoFun() {
  Object.defineProperty(Object.prototype, "nonConfigDataProtoFun", {configurable: false, value: -1});
  indirectEval("function nonConfigDataProtoFun(){}");
  assertSame("function", typeof indirectEval("nonConfigDataProtoFun"));
}

// Test 3a: configurable accessor property on global, try var declaration
function testConfigAccVar() {
  var {accessor, log} = loggingAccessor("configAccVar");
  Object.defineProperty(global, "configAccVar", accessor);
  indirectEval("var configAccVar = 0");
  indirectEval("configAccVar");
  assertEquals(["set 'configAccVar' to: 0", "get 'configAccVar'"], log);
}

// Test 3b: non-configurable data property on global, try function declaration
function testConfigAccFun() {
  var accessor = poisonedAccessor("configAccFun");
  Object.defineProperty(global, "configAccFun", accessor);
  indirectEval("function configAccFun(){}");
  assertSame("function", typeof indirectEval("configAccFun"));
}

// Test 4a: non-configurable data property on prototype, try var declaration
function testConfigAccProtoVar() {
  var {accessor, log} = loggingAccessor("configAccProtoVar");
  Object.defineProperty(Object.prototype, "configAccProtoVar", accessor);
  indirectEval("var configAccProtoVar = 0");
  indirectEval("configAccProtoVar");
  assertEquals(["set 'configAccProtoVar' to: 0", "get 'configAccProtoVar'"], log);
}

// Test 4b: non-configurable data property on prototype, try function declaration
function testConfigAccProtoFun() {
  var accessor = poisonedAccessor("configAccProtoFun");
  Object.defineProperty(Object.prototype, "configAccProtoFun", accessor);
  indirectEval("function configAccProtoFun(){}");
  assertSame("function", typeof indirectEval("configAccProtoFun"));
}

testNonConfigDataVar();
testNonConfigDataFun();
testNonConfigDataProtoVar();
testNonConfigDataProtoFun();
testConfigAccVar();
testConfigAccFun();
testConfigAccProtoVar();
testConfigAccProtoFun();

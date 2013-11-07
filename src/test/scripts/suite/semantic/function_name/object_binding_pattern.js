/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertClassName(c, name) {
  assertTrue(c.hasOwnProperty("name"));
  return assertSame(name, c.name);
}

// TODO: assignment destructuring

// anonymous function/generator/arrow/class expression
(function() {
  // 13.2.3.6 Runtime Semantics: KeyedBindingInitialisation
  // - SingleNameBinding : BindingIdentifier Initialiser
  var {f7 = function (){}} = [];
  assertFunctionName(f7, "f7");

  var {g7 = function* (){}} = [];
  assertFunctionName(g7, "g7");

  var {a7 = () => {}} = [];
  assertFunctionName(a7, "a7");

  var {c7 = class {}} = [];
  assertFunctionName(c7, "c7");

  var {d7 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d7, "<class-name>");

  let {f8 = function (){}} = [];
  assertFunctionName(f8, "f8");

  let {g8 = function* (){}} = [];
  assertFunctionName(g8, "g8");

  let {a8 = () => {}} = [];
  assertFunctionName(a8, "a8");

  let {c8 = class {}} = [];
  assertFunctionName(c8, "c8");

  let {d8 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d8, "<class-name>");

  const {f9 = function (){}} = [];
  assertFunctionName(f9, "f9");

  const {g9 = function* (){}} = [];
  assertFunctionName(g9, "g9");

  const {a9 = () => {}} = [];
  assertFunctionName(a9, "a9");

  const {c9 = class {}} = [];
  assertFunctionName(c9, "c9");

  const {d9 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d9, "<class-name>");

  var {key: f10 = function (){}} = [];
  assertFunctionName(f10, "f10");

  var {key: g10 = function* (){}} = [];
  assertFunctionName(g10, "g10");

  var {key: a10 = () => {}} = [];
  assertFunctionName(a10, "a10");

  var {key: c10 = class {}} = [];
  assertFunctionName(c10, "c10");

  var {key: d10 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d10, "<class-name>");

  let {key: f11 = function (){}} = [];
  assertFunctionName(f11, "f11");

  let {key: g11 = function* (){}} = [];
  assertFunctionName(g11, "g11");

  let {key: a11 = () => {}} = [];
  assertFunctionName(a11, "a11");

  let {key: c11 = class {}} = [];
  assertFunctionName(c11, "c11");

  let {key: d11 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d11, "<class-name>");

  const {key: f12 = function (){}} = [];
  assertFunctionName(f12, "f12");

  const {key: g12 = function* (){}} = [];
  assertFunctionName(g12, "g12");

  const {key: a12 = () => {}} = [];
  assertFunctionName(a12, "a12");

  const {key: c12 = class {}} = [];
  assertFunctionName(c12, "c12");

  const {key: d12 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d12, "<class-name>");

  var {["key"]: f13 = function (){}} = [];
  assertFunctionName(f13, "f13");

  var {["key"]: g13 = function* (){}} = [];
  assertFunctionName(g13, "g13");

  var {["key"]: a13 = () => {}} = [];
  assertFunctionName(a13, "a13");

  var {["key"]: c13 = class {}} = [];
  assertFunctionName(c13, "c13");

  var {["key"]: d13 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d13, "<class-name>");

  let {["key"]: f14 = function (){}} = [];
  assertFunctionName(f14, "f14");

  let {["key"]: g14 = function* (){}} = [];
  assertFunctionName(g14, "g14");

  let {["key"]: a14 = () => {}} = [];
  assertFunctionName(a14, "a14");

  let {["key"]: c14 = class {}} = [];
  assertFunctionName(c14, "c14");

  let {["key"]: d14 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d14, "<class-name>");

  const {["key"]: f15 = function (){}} = [];
  assertFunctionName(f15, "f15");

  const {["key"]: g15 = function* (){}} = [];
  assertFunctionName(g15, "g15");

  const {["key"]: a15 = () => {}} = [];
  assertFunctionName(a15, "a15");

  const {["key"]: c15 = class {}} = [];
  assertFunctionName(c15, "c15");

  const {["key"]: d15 = class { static get name() { return "<class-name>" } }} = [];
  assertClassName(d15, "<class-name>");
})();

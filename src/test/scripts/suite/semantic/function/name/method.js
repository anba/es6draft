/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertMethodName(o, pk, name) {
  let desc = Object.getOwnPropertyDescriptor(o, pk);
  let m = 'value' in desc ? desc.value : desc.get !== void 0 ? desc.get : desc.set;
  return assertFunctionName(m, name);
}

// method definition in object literal
{
  const symbolWithoutDescription = Symbol();
  const symbolWithDescription = Symbol("desc");
  const symbolWithEmptyDescription = Symbol("");

  // 14.3.9  Runtime Semantics: PropertyDefinitionEvaluation
  // - MethodDefinition : PropertyName (StrictFormalParameters ) { FunctionBody }
  // - MethodDefinition : get PropertyName ( ) { FunctionBody }
  // - MethodDefinition : set PropertyName ( PropertySetParameterList ) { FunctionBody }
  // 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
  // - GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
  let m0 = {m(){}};
  assertMethodName(m0, "m", "m");

  let g0 = {get g(){}};
  assertMethodName(g0, "g", "get g");

  let s0 = {set s(_){}};
  assertMethodName(s0, "s", "set s");

  let gm0 = {*gm(){}};
  assertMethodName(gm0, "gm", "gm");

  let m1 = {["m"](){}};
  assertMethodName(m1, "m", "m");

  let g1 = {get ["g"](){}};
  assertMethodName(g1, "g", "get g");

  let s1 = {set ["s"](_){}};
  assertMethodName(s1, "s", "set s");

  let gm1 = {*["gm"](){}};
  assertMethodName(gm1, "gm", "gm");

  let m2 = {[symbolWithoutDescription](){}};
  assertMethodName(m2, symbolWithoutDescription, "");

  let g2 = {get [symbolWithoutDescription](){}};
  assertMethodName(g2, symbolWithoutDescription, "");

  let s2 = {set [symbolWithoutDescription](_){}};
  assertMethodName(s2, symbolWithoutDescription, "");

  let gm2 = {*[symbolWithoutDescription](){}};
  assertMethodName(gm2, symbolWithoutDescription, "");

  let m3 = {[symbolWithDescription](){}};
  assertMethodName(m3, symbolWithDescription, "[desc]");

  let g3 = {get [symbolWithDescription](){}};
  assertMethodName(g3, symbolWithDescription, "get [desc]");

  let s3 = {set [symbolWithDescription](_){}};
  assertMethodName(s3, symbolWithDescription, "set [desc]");

  let gm3 = {*[symbolWithDescription](){}};
  assertMethodName(gm3, symbolWithDescription, "[desc]");

  let m4 = {[symbolWithEmptyDescription](){}};
  assertMethodName(m4, symbolWithEmptyDescription, "[]");

  let g4 = {get [symbolWithEmptyDescription](){}};
  assertMethodName(g4, symbolWithEmptyDescription, "get []");

  let s4 = {set [symbolWithEmptyDescription](_){}};
  assertMethodName(s4, symbolWithEmptyDescription, "set []");

  let gm4 = {*[symbolWithEmptyDescription](){}};
  assertMethodName(gm4, symbolWithEmptyDescription, "[]");
}

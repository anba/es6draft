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

// accesor method definition in object literal
{
  // 14.3.9  Runtime Semantics: PropertyDefinitionEvaluation
  // - MethodDefinition : get PropertyName ( ) { FunctionBody }
  // - MethodDefinition : set PropertyName ( PropertySetParameterList ) { FunctionBody }
  // 9.2.10  SetFunctionName Abstract Operation, step 4
  let g0 = {get ""(){}};
  assertMethodName(g0, "", "");

  let s0 = {set ""(_){}};
  assertMethodName(s0, "", "");

  let g1 = {get [""](){}};
  assertMethodName(g1, "", "");

  let s1 = {set [""](_){}};
  assertMethodName(s1, "", "");
}

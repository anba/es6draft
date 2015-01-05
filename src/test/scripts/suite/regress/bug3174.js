/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSame
} = Assert;

// 13.2.3.6 IteratorBindingInitialization, 13.2.3.7 KeyedBindingInitialization: ToObject only called for initializer expression
// https://bugs.ecmascript.org/show_bug.cgi?id=3174

// IteratorBindingInitialization
{
  let [{}] = [void 0];
  assertThrows(TypeError, () => { let [{p}] = [void 0]; });

  let [{}] = [null];
  assertThrows(TypeError, () => { let [{p}] = [null]; });

  {
    let [{valueOf}] = [true];
    assertSame(Boolean.prototype.valueOf, valueOf);
  }

  let [{} = void 0] = [void 0];
  assertThrows(TypeError, () => { let [{p} = void 0] = [void 0]; });

  let [{} = null] = [void 0];
  assertThrows(TypeError, () => { let [{p} = null] = [void 0]; });

  {
    let [{valueOf} = true] = [void 0];
    assertSame(Boolean.prototype.valueOf, valueOf);
  }
}

// KeyedBindingInitialization
{
  let {p: {}} = {p: void 0};
  assertThrows(TypeError, () => { let {p: {q}} = {p: void 0}; });

  let {p: {}} = {p: null};
  assertThrows(TypeError, () => { let {p: {q}} = {p: null}; });

  {
    let {p: {valueOf}} = {p: true};
    assertSame(Boolean.prototype.valueOf, valueOf);
  }

  let {p: {} = void 0} = {p: void 0};
  assertThrows(TypeError, () => { let {p: {q} = void 0} = {p: void 0}; });

  let {p: {} = null} = {p: void 0};
  assertThrows(TypeError, () => { let {p: {q} = null} = {p: void 0}; });

  {
    let {p: {valueOf} = true} = {p: void 0};
    assertSame(Boolean.prototype.valueOf, valueOf);
  }
}

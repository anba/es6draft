/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

%Include("lib/assert.js");

// assertEq(%IsUninitializedMap(Map[Symbol.create]()), true);
assertEq(%IsUninitializedMap(new Map), false);
assertEq(%IsUninitializedMap(new Object), false);
assertEq(%IsUninitializedMap(null), false);
assertEq(%IsUninitializedMap(void 0), false);
assertEq(%IsUninitializedMap(1), false);

// assertEq(%IsUninitializedSet(Set[Symbol.create]()), true);
assertEq(%IsUninitializedSet(new Set), false);
assertEq(%IsUninitializedSet(new Object), false);
assertEq(%IsUninitializedSet(null), false);
assertEq(%IsUninitializedSet(void 0), false);
assertEq(%IsUninitializedSet(1), false);

// assertEq(%IsUninitializedWeakMap(WeakMap[Symbol.create]()), true);
assertEq(%IsUninitializedWeakMap(new WeakMap), false);
assertEq(%IsUninitializedWeakMap(new Object), false);
assertEq(%IsUninitializedWeakMap(null), false);
assertEq(%IsUninitializedWeakMap(void 0), false);
assertEq(%IsUninitializedWeakMap(1), false);

// assertEq(%IsUninitializedWeakSet(WeakSet[Symbol.create]()), true);
assertEq(%IsUninitializedWeakSet(new WeakSet), false);
assertEq(%IsUninitializedWeakSet(new Object), false);
assertEq(%IsUninitializedWeakSet(null), false);
assertEq(%IsUninitializedWeakSet(void 0), false);
assertEq(%IsUninitializedWeakSet(1), false);

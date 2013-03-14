/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * @see OrdinaryObject#ObjectCreate(Realm, Intrinsics, ObjectAllocator)
 * @see OrdinaryObject#ObjectCreate(Realm, Scriptable, ObjectAllocator)
 */
public interface ObjectAllocator<OBJECT extends Scriptable> {
    OBJECT newInstance(Realm realm);
}

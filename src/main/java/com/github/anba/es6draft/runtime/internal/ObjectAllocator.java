/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Object allocator for ordinary objects.
 * 
 * @see OrdinaryObject#ObjectCreate(ExecutionContext, Intrinsics, ObjectAllocator)
 * @see OrdinaryObject#ObjectCreate(ExecutionContext, ScriptObject, ObjectAllocator)
 */
public interface ObjectAllocator<OBJECT extends OrdinaryObject> {
    /**
     * Allocates a new instance of the requested class.
     * 
     * @param realm
     *            the realm instance
     * @return the new instance
     */
    OBJECT newInstance(Realm realm);
}

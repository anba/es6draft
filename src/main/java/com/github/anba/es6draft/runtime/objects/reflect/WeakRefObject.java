/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import java.lang.ref.WeakReference;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>WeakRef Objects</h1>
 * <ul>
 * <li>Properties of WeakRef Instances
 * </ul>
 */
public final class WeakRefObject extends OrdinaryObject {
    private final WeakReference<ScriptObject> reference;
    private final Object reachableKey = new Object();

    /**
     * Constructs a new WeakRef object.
     * 
     * @param realm
     *            the realm object
     * @param target
     *            the target object
     * @param finalizer
     *            the optional finalizer or {@code null}
     * @param prototype
     *            the prototype object
     */
    public WeakRefObject(Realm realm, ScriptObject target, Runnable finalizer, ScriptObject prototype) {
        super(realm, prototype);
        this.reference = realm.getWorld().makeWeakRef(target, finalizer);
        realm.getWorld().ensureReachable(reachableKey, target);
    }

    /**
     * Returns the underlying weak reference.
     * 
     * @return the weak reference
     */
    public WeakReference<ScriptObject> getReference() {
        return reference;
    }

    /**
     * Returns the target object or {@code null} if cleared.
     * 
     * @param cx
     *            the execution context
     * @return the target object
     */
    public ScriptObject getTarget(ExecutionContext cx) {
        ScriptObject target = reference.get();
        if (target != null) {
            cx.getRealm().getWorld().ensureReachable(reachableKey, target);
        }
        return target;
    }

    /**
     * Clears this weak reference.
     */
    public void clear() {
        reference.clear();
    }
}

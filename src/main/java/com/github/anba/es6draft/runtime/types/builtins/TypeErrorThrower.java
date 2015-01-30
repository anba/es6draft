/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * <ul>
 * <li>9.2.8.1 %ThrowTypeError% ( )
 * </ul>
 * </ul>
 */
public final class TypeErrorThrower extends BuiltinFunction {
    /**
     * Constructs a new ThrowTypeError function.
     * 
     * @param realm
     *            the realm object
     */
    private TypeErrorThrower(Realm realm) {
        super(realm, ANONYMOUS, 0);
        setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        // 'length' is non-configurable to ensure %ThrowTypeError% is not a communication channel
        infallibleDefineOwnProperty("length", new Property(0, false, false, false));
        // [[Extensible]] slot is false
        setExtensible(false);
    }

    private TypeErrorThrower(Realm realm, Void ignore) {
        super(realm, ANONYMOUS, 0);
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.StrictModePoisonPill);
    }

    @Override
    public TypeErrorThrower clone() {
        return new TypeErrorThrower(getRealm(), null);
    }

    /**
     * 9.2.8.1 %ThrowTypeError% ( )
     * 
     * @param realm
     *            the realm instance
     * @return the %ThrowTypeError% function object
     */
    public static TypeErrorThrower createThrowTypeError(Realm realm) {
        assert realm.getIntrinsic(Intrinsics.FunctionPrototype) != null : "%FunctionPrototype% not initialized";
        return new TypeErrorThrower(realm);
    }
}

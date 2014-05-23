/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Support class for native function calls.
 */
public final class NativeCalls {
    private NativeCalls() {
    }

    /**
     * Extension: Native function calls
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the native function
     * @param args
     *            the function arguments
     * @return the return value
     */
    static Object nativeCall(ExecutionContext cx, String name, Object[] args) {
        switch (name) {
        case "Intrinsic": {
            if (args.length == 1 && Type.isString(args[0])) {
                String intrinsicName = Type.stringValue(args[0]).toString();
                return Intrinsic(cx, intrinsicName);
            }
            break;
        }
        case "SetIntrinsic": {
            if (args.length == 2 && Type.isString(args[0]) && args[1] instanceof OrdinaryObject) {
                String intrinsicName = Type.stringValue(args[0]).toString();
                OrdinaryObject intrinsicValue = (OrdinaryObject) args[1];
                return SetIntrinsic(cx, intrinsicName, intrinsicValue);
            }
            break;
        }
        case "GlobalObject": {
            if (args.length == 0) {
                return GlobalObject(cx);
            }
            break;
        }
        case "GlobalThis": {
            if (args.length == 0) {
                return GlobalThis(cx);
            }
            break;
        }
        }
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call: " + name);
    }

    /**
     * Native function: {@code %Intrinsic(<name>)}.
     * <p>
     * Returns the intrinsic by name.
     * 
     * @param cx
     *            the execution class
     * @param name
     *            the intrinsic name
     * @return the intrinsic
     */
    public static OrdinaryObject Intrinsic(ExecutionContext cx, String name) {
        Intrinsics id = Intrinsics.valueOf(name);
        return cx.getRealm().getIntrinsic(id);
    }

    /**
     * Native function: {@code %SetIntrinsic(<name>, <realm>)}.
     * <p>
     * Sets the intrinsic to a new value.
     * 
     * @param cx
     *            the execution class
     * @param name
     *            the intrinsic name
     * @param intrinsic
     *            the new intrinsic value
     * @return the intrinsic
     */
    public static OrdinaryObject SetIntrinsic(ExecutionContext cx, String name,
            OrdinaryObject intrinsic) {
        Intrinsics id = Intrinsics.valueOf(name);
        cx.getRealm().setIntrinsic(id, intrinsic);
        return intrinsic;
    }

    /**
     * Native function: {@code %GlobalObject()}.
     * <p>
     * Returns the global object.
     * 
     * @param cx
     *            the execution class
     * @return the global object
     */
    public static GlobalObject GlobalObject(ExecutionContext cx) {
        return cx.getRealm().getGlobalObject();
    }

    /**
     * Native function: {@code %GlobalThis()}.
     * <p>
     * Returns the global this.
     * 
     * @param cx
     *            the execution class
     * @return the global this
     */
    public static ScriptObject GlobalThis(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
    }
}

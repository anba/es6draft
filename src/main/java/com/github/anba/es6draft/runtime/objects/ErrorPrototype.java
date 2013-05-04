/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.4 Properties of the Error Prototype Object
 * </ul>
 */
public class ErrorPrototype extends OrdinaryObject implements Initialisable {
    public ErrorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.11.4 Properties of the Error Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.11.4.1 Error.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Error;

        /**
         * 15.11.4.2 Error.prototype.name
         */
        @Value(name = "name")
        public static final String name = "Error";

        /**
         * 15.11.4.3 Error.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";

        /**
         * 15.11.4.4 Error.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject o = Type.objectValue(thisValue);
            Object name = Get(cx, o, "name");
            CharSequence sname = (Type.isUndefined(name) ? "Error" : ToString(cx, name));
            Object msg = Get(cx, o, "message");
            CharSequence smsg = (Type.isUndefined(msg) ? "" : ToString(cx, msg));
            if (sname.length() == 0) {
                return smsg;
            }
            if (smsg.length() == 0) {
                return sname;
            }
            return sname + ": " + smsg;
        }

        @Accessor(name = "stack", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object stack(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof ErrorObject)) {
                return UNDEFINED;
            }
            ScriptException e = ((ErrorObject) thisValue).getException();
            return getStackTrace(e);
        }
    }

    private static String getStackTrace(ScriptException e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (className.charAt(0) == '#' && methodName.charAt(0) == '!') {
                int i = methodName.lastIndexOf('~');
                sb.append(methodName.substring(1, (i != -1 ? i : methodName.length())));
                sb.append('@').append(element.getFileName()).append(':')
                        .append(element.getLineNumber()).append('\n');
            }
        }
        return sb.toString();
    }
}

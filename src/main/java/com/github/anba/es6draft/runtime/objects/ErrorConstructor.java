/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.util.stream.Stream;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StackTraces;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.5 Error Objects</h2>
 * <ul>
 * <li>19.5.1 The Error Constructor
 * <li>19.5.2 Properties of the Error Constructor
 * </ul>
 */
public final class ErrorConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Error constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ErrorConstructor(Realm realm) {
        super(realm, "Error", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * GetStack ( error )
     * 
     * @param cx
     *            the execution context
     * @param error
     *            the error object
     * @return the stack descriptor object
     */
    public static ScriptObject GetStack(ExecutionContext cx, ErrorObject error) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject frames = GetStackFrames(cx, error);
        /* step 3 */
        String string = GetStackString(cx, error);
        /* step 4 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 5 */
        boolean status = CreateDataProperty(cx, obj, "frames", frames);
        /* step 6 */
        assert status;
        /* step 7 */
        status = CreateDataProperty(cx, obj, "string", string);
        /* step 8 */
        assert status;
        /* step 9 */
        status = SetIntegrityLevel(cx, obj, IntegrityLevel.Frozen);
        /* step 10 */
        assert status;
        /* step 11 */
        return obj;
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * GetStackString ( error )<br>
     * GetFrameString ( frame )<br>
     * GetStackFrameSpanString( span )<br>
     * GetStackFramePositionString( position )
     * 
     * @param cx
     *            the execution context
     * @param error
     *            the error object
     * @return the error stack string
     */
    public static String GetStackString(ExecutionContext cx, ErrorObject error) {
        /* step 1 (not applicable) */
        /* step 2 */
        String errorString = ToFlatString(cx, error);
        /* step 3 */
        Stream<StackTraceElement> frames = StackTraces.stackTraceStream(error.getException());
        /* steps 4-5 */
        String frameString = frames.collect(StringBuilder::new, (sb, frame) -> {
            // FIXME: Spec bug - formatting unclear and missing ':' between source and span
            sb.append("\n  at ").append(frame.getMethodName()).append(" (").append(frame.getFileName()).append(':')
                    .append(frame.getLineNumber()).append(')');
        }, StringBuilder::append).toString();
        /* steps 6-7 */
        return errorString + frameString;
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * GetStackFrames( error )
     * 
     * @param cx
     *            the execution context
     * @param error
     *            the error object
     * @return the stack frames object
     */
    private static ScriptObject GetStackFrames(ExecutionContext cx, ErrorObject error) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Stream<ScriptObject> frames = StackTraces.stackTraceStream(error.getException())
                .map(element -> FromStackFrame(cx, element));
        /* step 4 */
        ArrayObject array = CreateArrayFromList(cx, frames);
        /* step 5 */
        boolean status = SetIntegrityLevel(cx, array, IntegrityLevel.Frozen);
        /* step 6 */
        assert status;
        /* step 7 */
        return array;
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * FromStackFrame ( frame )
     * 
     * @param cx
     *            the execution context
     * @param frame
     *            the stack frame
     * @return the stack frame object
     */
    private static ScriptObject FromStackFrame(ExecutionContext cx, StackTraceElement frame) {
        /* step 1 (not applicable) */
        /* step 2 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 3 */
        assert obj.isExtensible(cx) && obj.ownPropertyKeys(cx).isEmpty();
        /* step 4 */
        boolean status = CreateDataProperty(cx, obj, "name", frame.getMethodName());
        /* steps 5-7 */
        status &= CreateDataProperty(cx, obj, "source", frame.getFileName());
        /* step 8 */
        status &= CreateDataProperty(cx, obj, "span", FromStackFrameSpan(cx, frame));
        /* step 9 */
        assert status;
        /* step 10 */
        status = SetIntegrityLevel(cx, obj, IntegrityLevel.Frozen);
        /* step 11 */
        assert status;
        /* step 12 */
        return obj;
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * FromStackFrame ( frame )
     * 
     * @param cx
     *            the execution context
     * @param frame
     *            the stack frame
     * @return the stack frame span object
     */
    private static ScriptObject FromStackFrameSpan(ExecutionContext cx, StackTraceElement frame) {
        /* step 1 (not applicable) */
        /* steps 2-5 */
        ArrayObject array = CreateArrayFromList(cx, FromStackFramePosition(cx, frame));
        /* step 6 */
        boolean status = SetIntegrityLevel(cx, array, IntegrityLevel.Frozen);
        /* step 7 */
        assert status;
        /* step 8 */
        return array;
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * FromStackFramePosition ( position )
     * 
     * @param cx
     *            the execution context
     * @param frame
     *            the stack frame
     * @return the stack frame position object
     */
    private static ScriptObject FromStackFramePosition(ExecutionContext cx, StackTraceElement frame) {
        /* step 1 (not applicable) */
        /* steps 2-5 */
        ArrayObject array = CreateArrayFromList(cx, frame.getLineNumber());
        /* step 6 */
        boolean status = SetIntegrityLevel(cx, array, IntegrityLevel.Frozen);
        /* step 7 */
        assert status;
        /* step 8 */
        return array;
    }

    /**
     * 19.5.1.1 Error (message)
     * <p>
     * <strong>Extension</strong>: Error (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-4 */
        return construct(callerContext, this, args);
    }

    /**
     * 19.5.1.1 Error (message)
     * <p>
     * <strong>Extension</strong>: Error (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        ErrorObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget, Intrinsics.ErrorPrototype,
                ErrorObject::new);
        /* step 3 */
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(calleeContext, message);
            obj.defineErrorProperty("message", msg, false);
        }

        /* extension: fileName, lineNumber and columnNumber arguments */
        if (args.length > 1) {
            CharSequence fileName = ToString(calleeContext, args[1]);
            obj.defineErrorProperty("fileName", fileName, true);
        }
        if (args.length > 2) {
            int line = ToInt32(calleeContext, args[2]);
            obj.defineErrorProperty("lineNumber", line, true);
        }
        if (args.length > 3) {
            int column = ToInt32(calleeContext, args[3]);
            obj.defineErrorProperty("columnNumber", column, true);
        }

        /* step 4 */
        return obj;
    }

    /**
     * 19.5.2 Properties of the Error Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Error";

        /**
         * 19.5.2.1 Error.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.ErrorPrototype;
    }
}

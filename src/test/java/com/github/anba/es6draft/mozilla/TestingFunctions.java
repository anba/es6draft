/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;

import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    /**
     * shell-function: {@code version([number])}
     *
     * @return the version string "185"
     */
    @Function(name = "version", arity = 1)
    public String version() {
        return "185";
    }

    /**
     * shell-function: {@code options([name])}
     *
     * @return the empty string
     */
    @Function(name = "options", arity = 0)
    public String options() {
        return "";
    }

    /**
     * shell-function: {@code gc()}
     *
     * @return the empty string
     */
    @Function(name = "gc", arity = 0)
    public String gc() {
        return "";
    }

    /**
     * shell-function: {@code gczeal()}
     *
     * @return the empty string
     */
    @Function(name = "gczeal", arity = 0)
    public String gczeal() {
        return "";
    }

    /**
     * shell-function: {@code getBuildConfiguration()}
     *
     * @param cx
     *            the execution context
     * @return an empty object
     */
    @Function(name = "getBuildConfiguration", arity = 0)
    public ScriptObject getBuildConfiguration(ExecutionContext cx) {
        return OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
    }

    /**
     * shell-function: {@code terminate()}
     */
    @Function(name = "terminate", arity = 0)
    public void terminate() {
        throw new StopExecutionException(StopExecutionException.Reason.Terminate);
    }

    /**
     * shell-function: {@code enableOsiPointRegisterChecks()}
     */
    @Function(name = "enableOsiPointRegisterChecks", arity = 0)
    public void enableOsiPointRegisterChecks() {
        // empty
    }

    /**
     * shell-function: {@code isAsmJSCompilationAvailable()}
     * 
     * @return always {@code false}
     */
    @Function(name = "isAsmJSCompilationAvailable", arity = 0)
    public boolean isAsmJSCompilationAvailable() {
        return false;
    }

    /**
     * shell-function: {@code helperThreadCount()}
     * 
     * @return always {@code 0}
     */
    @Function(name = "helperThreadCount", arity = 0)
    public int helperThreadCount() {
        return 0;
    }

    /**
     * shell-function: {@code getSelfHostedValue(name)}
     * 
     * @return a placeholder function
     */
    @Function(name = "getSelfHostedValue", arity = 1)
    public ScriptObject getSelfHostedValue(ExecutionContext cx, String name) {
        if ("ToNumber".equals(name)) {
            return new BuiltinFunction(cx.getRealm(), "ToNumber", 1) {
                @Override
                public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
                    return AbstractOperations.ToNumber(calleeContext(), argument(args, 0));
                }

                @Override
                protected BuiltinFunction clone() {
                    return this;
                }
            };
        }
        if ("ToLength".equals(name)) {
            return new BuiltinFunction(cx.getRealm(), "ToLength", 1) {
                @Override
                public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
                    return AbstractOperations.ToLength(calleeContext(), argument(args, 0));
                }

                @Override
                protected BuiltinFunction clone() {
                    return this;
                }
            };
        }
        throw new IllegalArgumentException(name);
    }

    /**
     * shell-function: {@code neuter(arrayBuffer, type)}
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     * @param type
     *            the neuter type
     */
    @Function(name = "neuter", arity = 2)
    public void neuter(ExecutionContext cx, ArrayBufferObject arrayBuffer, String type) {
        assert "change-data".equals(type) || "same-data".equals(type);
        if (arrayBuffer.getData() != null) {
            DetachArrayBuffer(cx, arrayBuffer);
        }
    }
}

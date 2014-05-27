/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    /**
     * shell-function: {@code version([number])}
     *
     * @return the version string "185"
     **/
    @Function(name = "version", arity = 1)
    public String version() {
        return "185";
    }

    /**
     * shell-function: {@code options([name])}
     *
     * @return the empty string
     **/
    @Function(name = "options", arity = 0)
    public String options() {
        return "";
    }

    /**
     * shell-function: {@code gc()}
     *
     * @return the empty string
     **/
    @Function(name = "gc", arity = 0)
    public String gc() {
        return "";
    }

    /**
     * shell-function: {@code gczeal()}
     *
     * @return the empty string
     **/
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
     **/
    @Function(name = "getBuildConfiguration", arity = 0)
    public ScriptObject getBuildConfiguration(ExecutionContext cx) {
        return OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
    }

    /**
     * shell-function: {@code terminate()}
     **/
    @Function(name = "terminate", arity = 0)
    public void terminate() {
        throw new StopExecutionException(StopExecutionException.Reason.Terminate);
    }

    /**
     * shell-function: {@code enableOsiPointRegisterChecks()}
     **/
    @Function(name = "enableOsiPointRegisterChecks", arity = 0)
    public void enableOsiPointRegisterChecks() {
        // empty
    }

    /**
     * shell-function: {@code isAsmJSCompilationAvailable()}
     * 
     * @return always {@code false}
     **/
    @Function(name = "isAsmJSCompilationAvailable", arity = 0)
    public boolean isAsmJSCompilationAvailable() {
        return false;
    }
}

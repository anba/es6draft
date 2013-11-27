/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 *
 */
public final class TailCallInvocation {
    private enum InvokeType {
        Call, Construct
    }

    private final InvokeType type;
    private final Callable function;
    private final Object thisValue;
    private final Object[] argumentsList;
    private final ScriptObject object;

    public TailCallInvocation(Callable function, Object thisValue, Object[] argumentsList) {
        this.type = InvokeType.Call;
        this.function = function;
        this.thisValue = thisValue;
        this.argumentsList = argumentsList;
        this.object = null;
    }

    private TailCallInvocation(Callable function, Object thisValue, Object[] argumentsList,
            ScriptObject object) {
        this.type = InvokeType.Construct;
        this.function = function;
        this.thisValue = thisValue;
        this.argumentsList = argumentsList;
        this.object = object;
    }

    public Object apply(ExecutionContext callerContext) throws Throwable {
        Object result;
        if (thisValue == null) {
            // cf. ScriptRuntime#EvaluateConstructorTailCall
            result = ((Constructor) function).tailConstruct(callerContext, argumentsList);
        } else {
            result = function.tailCall(callerContext, thisValue, argumentsList);
        }
        // cf. OrdinaryFunction#OrdinaryConstructTailCall, steps 9-10
        if (type == InvokeType.Construct && !(result instanceof TailCallInvocation)
                && !Type.isObject(result)) {
            result = object;
        }
        return result;
    }

    public TailCallInvocation toConstructTailCall(ScriptObject object) {
        if (this.type == InvokeType.Construct) {
            return this;
        }
        return new TailCallInvocation(function, thisValue, argumentsList, object);
    }
}

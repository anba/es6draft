/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.3 Generator Objects</h2>
 * <ul>
 * <li>25.3.3 Iteration Related Abstract Operations
 * </ul>
 */
public final class IterationAbstractOperations {
    private IterationAbstractOperations() {
    }

    /**
     * 25.3.3.1 GeneratorStart (generator, generatorBody)
     */
    public static GeneratorObject GeneratorStart(ExecutionContext cx, GeneratorObject generator,
            RuntimeInfo.Code generatorBody) {
        /* steps 1-6 */
        generator.start(cx, generatorBody);
        /* step 7 */
        return generator;
    }

    /**
     * 25.3.3.2 GeneratorResume (generator, value)
     */
    public static Object GeneratorResume(ExecutionContext cx, Object generator, Object value) {
        /* step 1 */
        if (!Type.isObject(generator)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 2 */
        if (!(generator instanceof GeneratorObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 3-14 */
        return ((GeneratorObject) generator).resume(cx, value);
    }

    /**
     * 25.3.3.3 GeneratorYield (iterNextObj)
     */
    public static Object GeneratorYield(ExecutionContext genContext, ScriptObject iterNextObj) {
        /* step 1 (?) */
        /* steps 2-4 */
        GeneratorObject generator = genContext.getCurrentGenerator();
        assert generator != null;
        /* steps 5-11 */
        return generator.yield(iterNextObj);
    }
}

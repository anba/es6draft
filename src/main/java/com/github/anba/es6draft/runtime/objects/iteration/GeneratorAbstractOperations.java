/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.3 Generator Objects</h2>
 * <ul>
 * <li>25.3.3 Generator Abstract Operations
 * </ul>
 */
public final class GeneratorAbstractOperations {
    private GeneratorAbstractOperations() {
    }

    /**
     * 25.3.3.1 GeneratorStart (generator, generatorBody)
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param generatorBody
     *            the runtime function code
     * @return the generator object
     */
    public static GeneratorObject GeneratorStart(ExecutionContext cx, GeneratorObject generator,
            RuntimeInfo.Function generatorBody) {
        /* steps 1-6 */
        generator.start(cx, generatorBody);
        /* step 7 */
        return generator;
    }

    /**
     * 25.3.3.2 GeneratorResume (generator, value)
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param value
     *            the resumption value
     * @return the iterator result object
     */
    public static ScriptObject GeneratorResume(ExecutionContext cx, Object generator, Object value) {
        /* step 1 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 2 */
        if (!(generator instanceof GeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 3-15 */
        return ((GeneratorObject) generator).resume(cx, value);
    }

    /**
     * 25.3.3.3 GeneratorYield (iterNextObj)
     * 
     * @param genContext
     *            the execution context
     * @param iterNextObj
     *            the iterator result object
     * @return the yield value
     */
    public static Object GeneratorYield(ExecutionContext genContext, ScriptObject iterNextObj) {
        /* step 1 (?) */
        /* steps 2-4 */
        GeneratorObject generator = genContext.getCurrentGenerator();
        assert generator != null;
        /* steps 5-11 */
        return generator.yield(iterNextObj);
    }

    /**
     * GeneratorThrow(generator, value)
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param exception
     *            the exception value
     * @return the iterator result object
     */
    public static ScriptObject GeneratorThrow(ExecutionContext cx, Object generator,
            Object exception) {
        /* steps 1-2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof GeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-18 */
        return ((GeneratorObject) generator)._throw(cx, exception);
    }
}

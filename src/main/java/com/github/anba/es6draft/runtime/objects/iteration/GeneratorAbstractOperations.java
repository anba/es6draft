/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
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
     * 25.3.3.2 GeneratorValidate ( generator )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @return the generator object
     */
    public static GeneratorObject GeneratorValidate(ExecutionContext cx, Object generator) {
        /* step 1 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 2 */
        if (!(generator instanceof GeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 3-6 (execution state checked in GeneratorObject) */
        return (GeneratorObject) generator;
    }

    /**
     * 25.3.3.3 GeneratorResume (generator, value)
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
        /* steps 1-2 */
        GeneratorObject gen = GeneratorValidate(cx, generator);
        /* steps 3-12 */
        return gen.resume(cx, value);
    }

    /**
     * 25.3.3.4 GeneratorResumeAbrupt(generator, abruptCompletion)
     * <p>
     * GeneratorReturn(generator, value)
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param value
     *            the return value
     * @return the iterator result object
     */
    public static ScriptObject GeneratorReturn(ExecutionContext cx, Object generator, Object value) {
        /* steps 1-2 */
        GeneratorObject gen = GeneratorValidate(cx, generator);
        /* steps 4-13 */
        return gen._return(cx, value);
    }

    /**
     * 25.3.3.4 GeneratorResumeAbrupt(generator, abruptCompletion)
     * <p>
     * GeneratorThrow(generator, exception)
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
        GeneratorObject gen = GeneratorValidate(cx, generator);
        /* steps 4-13 */
        return gen._throw(cx, exception);
    }

    /**
     * 25.3.3.5 GeneratorYield (iterNextObj)
     * 
     * @param genContext
     *            the execution context
     * @param iterNextObj
     *            the iterator result object
     * @return the yield value
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    public static Object GeneratorYield(ExecutionContext genContext, ScriptObject iterNextObj)
            throws ReturnValue {
        /* step 1 (?) */
        /* steps 2-4 */
        GeneratorObject generator = genContext.getCurrentGenerator();
        assert generator != null;
        /* steps 5-11 */
        return generator.yield(iterNextObj);
    }
}

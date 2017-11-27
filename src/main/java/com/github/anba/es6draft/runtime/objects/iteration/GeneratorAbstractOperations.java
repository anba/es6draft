/**
 * Copyright (c) André Bargull
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
     */
    public static void GeneratorStart(ExecutionContext cx, GeneratorObject generator,
            RuntimeInfo.Function generatorBody) {
        /* steps 1-6 */
        generator.start(cx, generatorBody);
        /* step 7 (return) */
    }

    /**
     * 25.3.3.2 GeneratorValidate ( generator )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param method
     *            the method name
     * @return the generator object
     */
    public static GeneratorObject GeneratorValidate(ExecutionContext cx, Object generator, String method) {
        /* steps 1-2 */
        if (!(generator instanceof GeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(generator).toString());
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
     * @param method
     *            the method name
     * @return the iterator result object
     */
    public static ScriptObject GeneratorResume(ExecutionContext cx, Object generator, Object value, String method) {
        /* step 1 */
        GeneratorObject gen = GeneratorValidate(cx, generator, method);
        /* steps 2-11 */
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
     * @param method
     *            the method name
     * @return the iterator result object
     */
    public static ScriptObject GeneratorReturn(ExecutionContext cx, Object generator, Object value, String method) {
        /* step 1 */
        GeneratorObject gen = GeneratorValidate(cx, generator, method);
        /* steps 2-12 */
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
     * @param method
     *            the method name
     * @return the iterator result object
     */
    public static ScriptObject GeneratorThrow(ExecutionContext cx, Object generator, Object exception, String method) {
        /* step 1 */
        GeneratorObject gen = GeneratorValidate(cx, generator, method);
        /* steps 2-12 */
        return gen._throw(cx, exception);
    }
}

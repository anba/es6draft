/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * Extension: Async Function Definitions
 */
public final class AsyncAbstractOperations {
    private AsyncAbstractOperations() {
    }

    /**
     * <pre>
     * function spawn() {
     *     return new Promise(spawnExecutor);
     * }
     * </pre>
     */
    public static final class Spawn extends BuiltinFunction {
        private final GeneratorObject generator;

        public Spawn(Realm realm, GeneratorObject generator) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
        }

        @Override
        public PromiseObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Callable executor = new SpawnExecutor(calleeContext.getRealm(), generator);
            return PromiseNew(calleeContext, executor);
        }
    }

    /**
     * <pre>
     * function spawnExecutor(resolve, reject) {
     *     step(callNext);
     * }
     * </pre>
     */
    public static final class SpawnExecutor extends BuiltinFunction {
        private final GeneratorObject generator;

        public SpawnExecutor(Realm realm, GeneratorObject generator) {
            super(realm, ANONYMOUS, 2);
            this.generator = generator;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = getArgument(args, 0);
            Object reject = getArgument(args, 1);
            if (!IsCallable(resolve)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            if (!IsCallable(reject)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            Callable nextF = new CallNext(calleeContext.getRealm(), generator, UNDEFINED);
            Callable step = new StepFunction(calleeContext.getRealm(), generator,
                    (Callable) resolve, (Callable) reject);
            step.call(calleeContext, UNDEFINED, nextF);
            return UNDEFINED;
        }
    }

    /**
     * <pre>
     * function step(nextF) {
     *     try {
     *         var next = nextF();
     *     } catch (e) {
     *         reject(e);
     *         return;
     *     }
     *     if (next.done) {
     *         resolve(next.value);
     *         return;
     *     }
     *     Promise(next.value).then(callStepWithNext, callStepWithThrow);
     * }
     * </pre>
     */
    public static final class StepFunction extends BuiltinFunction {
        private final GeneratorObject generator;
        private final Callable resolve;
        private final Callable reject;

        public StepFunction(Realm realm, GeneratorObject generator, Callable resolve,
                Callable reject) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
            this.resolve = resolve;
            this.reject = reject;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object nextF = getArgument(args, 0);
            if (!IsCallable(nextF)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            Object next;
            try {
                next = ((Callable) nextF).call(calleeContext, UNDEFINED);
            } catch (ScriptException e) {
                reject.call(calleeContext, UNDEFINED, e.getValue());
                return UNDEFINED;
            }
            if (!Type.isObject(next)) {
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            ScriptObject nextObject = Type.objectValue(next);
            if (IteratorComplete(calleeContext, nextObject)) {
                resolve.call(calleeContext, UNDEFINED, IteratorValue(calleeContext, nextObject));
                return UNDEFINED;
            }
            ScriptObject p = PromiseOf(calleeContext, IteratorValue(calleeContext, nextObject));
            Callable resolveAction = new CallStepWithNext(calleeContext.getRealm(), generator, this);
            Callable rejectAction = new CallStepWithThrow(calleeContext.getRealm(), generator, this);
            PromiseThen(calleeContext, p, resolveAction, rejectAction);
            return UNDEFINED;
        }
    }

    /**
     * <pre>
     * function callNext() {
     *     generator.next(value);
     * }
     * </pre>
     */
    public static final class CallNext extends BuiltinFunction {
        private final GeneratorObject generator;
        private final Object value;

        public CallNext(Realm realm, GeneratorObject generator, Object value) {
            super(realm, ANONYMOUS, 0);
            this.generator = generator;
            this.value = value;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            return IteratorNext(calleeContext, generator, value);
        }
    }

    /**
     * <pre>
     * function callThrow() {
     *     generator.throw(value);
     * }
     * </pre>
     */
    public static final class CallThrow extends BuiltinFunction {
        private final GeneratorObject generator;
        private final Object value;

        public CallThrow(Realm realm, GeneratorObject generator, Object value) {
            super(realm, ANONYMOUS, 0);
            this.generator = generator;
            this.value = value;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            return IteratorThrow(calleeContext, generator, value);
        }
    }

    /**
     * <pre>
     * function callStepWithNext() {
     *     step(callNext);
     * }
     * </pre>
     */
    public static final class CallStepWithNext extends BuiltinFunction {
        private final GeneratorObject generator;
        private final Callable step;

        public CallStepWithNext(Realm realm, GeneratorObject generator, Callable step) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
            this.step = step;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = getArgument(args, 0);
            Callable nextF = new CallNext(calleeContext.getRealm(), generator, value);
            step.call(calleeContext, UNDEFINED, nextF);
            return UNDEFINED;
        }
    }

    /**
     * <pre>
     * function callStepWithThrow() {
     *     step(callThrow);
     * }
     * </pre>
     */
    public static final class CallStepWithThrow extends BuiltinFunction {
        private final GeneratorObject generator;
        private final Callable step;

        public CallStepWithThrow(Realm realm, GeneratorObject generator, Callable step) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
            this.step = step;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = getArgument(args, 0);
            Callable nextF = new CallThrow(calleeContext.getRealm(), generator, value);
            step.call(calleeContext, UNDEFINED, nextF);
            return UNDEFINED;
        }
    }
}

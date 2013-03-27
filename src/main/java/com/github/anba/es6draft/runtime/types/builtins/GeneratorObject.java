/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * 
 *
 */
public class GeneratorObject extends OrdinaryObject implements Initialisable {
    /**
     * [[State]]
     */
    private enum GeneratorState {
        Newborn, Executing, Suspended, Closed
    }

    /** [[State]] */
    private GeneratorState state = GeneratorState.Newborn;

    /** [[Code]] */
    private RuntimeInfo.Code code;

    /** [[ExecutionContext]] */
    private ExecutionContext context;

    // internal
    private static final Object RETURN = new Object();
    private Future<Object> future;
    private SynchronousQueue<Object> out;
    private SynchronousQueue<Object> in;

    public GeneratorObject(Realm realm, OrdinaryGenerator generator, ExecutionContext context) {
        this(realm, generator.getCode(), context);
    }

    public GeneratorObject(Realm realm, RuntimeInfo.Code code, ExecutionContext context) {
        super(realm);
        this.code = code;
        this.context = context;
        this.in = new SynchronousQueue<>();
        this.out = new SynchronousQueue<>();
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    @SuppressWarnings("serial")
    private static class CloseGenerator extends RuntimeException {
    }

    private void start0() {
        this.state = GeneratorState.Executing;
        this.context.setCurrentGenerator(this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result;
                try {
                    result = code.evaluate(context);
                } catch (ScriptException | CloseGenerator e) {
                    result = e;
                } catch (Throwable t) {
                    out.put(RETURN);
                    throw t;
                }
                out.put(RETURN);
                return result;
            }
        });
        executor.shutdown();
    }

    private void resume0(Object value) {
        this.state = GeneratorState.Executing;
        try {
            in.put(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Object execute0(Realm realm) {
        if (!future.isDone()) {
            Object result;
            try {
                result = out.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (result != RETURN) {
                state = GeneratorState.Suspended;
                return result;
            }
        }

        state = GeneratorState.Closed;
        try {
            Object result = future.get();
            if (result instanceof ScriptException) {
                throw (ScriptException) result;
            }
            if (result instanceof CloseGenerator) {
                return UNDEFINED;
            }
            // TODO: StopIteration with value
            // throw new StopIteration(result);
            return ScriptRuntime._throw(realm.getIntrinsic(Intrinsics.StopIteration));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [[Send]]
     */
    public Object send(Realm realm, Object value) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(realm, Messages.Key.GeneratorExecuting);
        case Closed:
            throw throwTypeError(realm, Messages.Key.GeneratorClosed);
        case Newborn: {
            if (value != UNDEFINED) {
                throw throwTypeError(realm, Messages.Key.GeneratorNewbornSend);
            }
            start0();
            return execute0(realm);
        }
        case Suspended:
        default: {
            resume0(value);
            return execute0(realm);
        }
        }
    }

    /**
     * [[Throw]]
     */
    public Object _throw(Realm realm, Object value) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(realm, Messages.Key.GeneratorExecuting);
        case Closed:
            throw throwTypeError(realm, Messages.Key.GeneratorClosed);
        case Newborn: {
            this.state = GeneratorState.Closed;
            throw ScriptRuntime._throw(value);
        }
        case Suspended:
        default: {
            resume0(new ScriptException(value));
            return execute0(realm);
        }
        }
    }

    /**
     * [[Close]]
     */
    public Object close(Realm realm) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(realm, Messages.Key.GeneratorExecuting);
        case Closed:
            return UNDEFINED;
        case Newborn: {
            this.state = GeneratorState.Closed;
            return UNDEFINED;
        }
        case Suspended:
        default: {
            resume0(new CloseGenerator());
            return execute0(realm);
        }
        }
    }

    /**
     * 13.4 Generator Definitions
     */
    public Object yield(Object value) {
        try {
            this.out.put(value);
            Object result = this.in.take();
            if (result instanceof ScriptException) {
                // [[Throw]]
                throw (ScriptException) result;
            }
            if (result instanceof CloseGenerator) {
                // [[Close]]
                throw (CloseGenerator) result;
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Properties {
        ;

        private static GeneratorObject generatorObject(Realm realm, Object object) {
            if (object instanceof GeneratorObject) {
                return (GeneratorObject) object;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "send", arity = 1)
        public static Object send(Realm realm, Object thisValue, Object value) {
            return generatorObject(realm, thisValue).send(realm, value);
        }

        @Function(name = "next", arity = 0)
        public static Object next(Realm realm, Object thisValue) {
            return generatorObject(realm, thisValue).send(realm, UNDEFINED);
        }

        @Function(name = "throw", arity = 1)
        public static Object _throw(Realm realm, Object thisValue, Object value) {
            return generatorObject(realm, thisValue)._throw(realm, value);
        }

        @Function(name = "close", arity = 0)
        public static Object close(Realm realm, Object thisValue) {
            return generatorObject(realm, thisValue).close(realm);
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }
    }
}

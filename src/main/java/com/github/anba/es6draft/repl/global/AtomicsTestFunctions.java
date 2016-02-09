/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.repl.loader.NodeModuleLoader;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * 
 */
public final class AtomicsTestFunctions {
    private final ReentrantLock sharedLock = new ReentrantLock();
    private ByteBuffer sharedBufferData;
    private long sharedBufferLength;

    /**
     * shell-function: {@code getSharedArrayBuffer()}
     * 
     * @param cx
     *            the execution context
     * @return the shared array buffer
     */
    @Function(name = "getSharedArrayBuffer", arity = 0)
    public SharedArrayBufferObject getSharedArrayBuffer(ExecutionContext cx) {
        sharedLock.lock();
        try {
            if (sharedBufferData == null) {
                return null;
            }
            return new SharedArrayBufferObject(cx.getRealm(), sharedBufferData, sharedBufferLength,
                    cx.getIntrinsic(Intrinsics.SharedArrayBufferPrototype));
        } finally {
            sharedLock.unlock();
        }
    }

    /**
     * shell-function: {@code setSharedArrayBuffer(buffer)}
     * 
     * @param cx
     *            the execution context
     * @param buffer
     *            the shared array buffer
     */
    @Function(name = "setSharedArrayBuffer", arity = 1)
    public void setSharedArrayBuffer(ExecutionContext cx, Object buffer) {
        sharedLock.lock();
        try {
            if (Type.isUndefinedOrNull(buffer)) {
                sharedBufferData = null;
                sharedBufferLength = 0;
            } else if (buffer instanceof SharedArrayBufferObject) {
                SharedArrayBufferObject sharedBuffer = (SharedArrayBufferObject) buffer;
                sharedBufferData = sharedBuffer.getData();
                sharedBufferLength = sharedBuffer.getByteLength();
            } else {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
        } finally {
            sharedLock.unlock();
        }
    }

    /**
     * shell-function: {@code evalInWorker(sourceString)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param sourceString
     *            the script source code
     * @return {@code true} if a new script worker was started, otherwise returns {@code false}
     */
    @Function(name = "evalInWorker", arity = 1)
    public boolean evalInWorker(ExecutionContext cx, ExecutionContext caller, String sourceString) {
        Source baseSource = Objects.requireNonNull(cx.getRealm().sourceInfo(caller));
        try {
            // TODO: Initialize extensions (console.jsm, window timers?).
            CompletableFuture.supplyAsync(() -> {
                // Set 'executor' to null so it doesn't get shared with the current runtime context.
                /* @formatter:off */
                RuntimeContext context = new RuntimeContext.Builder(cx.getRuntimeContext())
                                                           .setExecutor(null)
                                                           .build();
                /* @formatter:on */

                World world = new World(context);
                Realm realm;
                try {
                    realm = world.newInitializedRealm();
                } catch (IOException | URISyntaxException e) {
                    throw new CompletionException(e);
                }
                // Bind test functions to this instance.
                realm.createGlobalProperties(this, AtomicsTestFunctions.class);

                // TODO: Add proper abstraction.
                ModuleLoader moduleLoader = world.getModuleLoader();
                if (moduleLoader instanceof NodeModuleLoader) {
                    try {
                        ((NodeModuleLoader) moduleLoader).initialize(realm);
                    } catch (IOException | URISyntaxException | MalformedNameException | ResolutionException e) {
                        throw new CompletionException(e);
                    }
                }

                // Evaluate the script source code and then run pending jobs.
                Source source = new Source(baseSource, "evalInWorker-script", 1);
                Script script = realm.getScriptLoader().script(source, sourceString);
                Object result = script.evaluate(realm);
                world.runEventLoop();
                return result;
            } , cx.getRuntimeContext().getWorkerExecutor()).whenComplete((r, e) -> {
                if (e instanceof CompletionException) {
                    Throwable cause = ((CompletionException) e).getCause();
                    cx.getRuntimeContext().getWorkerErrorReporter().accept(cx, (cause != null ? cause : e));
                } else if (e != null) {
                    cx.getRuntimeContext().getWorkerErrorReporter().accept(cx, e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }
}

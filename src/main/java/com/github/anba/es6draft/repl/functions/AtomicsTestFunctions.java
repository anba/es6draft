/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.functions;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.atomics.SharedByteBuffer;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * 
 */
public final class AtomicsTestFunctions {
    public static final class Mailbox {
        private final ReentrantLock sharedLock = new ReentrantLock();
        private SharedByteBuffer sharedBufferData;
        private long sharedBufferLength;

        void setSharedBufferData(SharedArrayBufferObject buffer) {
            sharedLock.lock();
            try {
                if (buffer == null) {
                    sharedBufferData = null;
                    sharedBufferLength = 0;
                } else {
                    sharedBufferData = buffer.getSharedData();
                    sharedBufferLength = buffer.getByteLength();
                }
            } finally {
                sharedLock.unlock();
            }
        }

        SharedArrayBufferObject getSharedArrayBuffer(ExecutionContext cx) {
            sharedLock.lock();
            try {
                if (sharedBufferData == null) {
                    return null;
                }
                return new SharedArrayBufferObject(cx.getRealm(), sharedBufferData.duplicate(), sharedBufferLength,
                        cx.getIntrinsic(Intrinsics.SharedArrayBufferPrototype));
            } finally {
                sharedLock.unlock();
            }
        }
    }

    public interface MailboxProvider {
        Mailbox getMailbox();
    }

    private Mailbox getMailbox(ExecutionContext cx) {
        RuntimeContext.Data contextData = cx.getRuntimeContext().getContextData();
        if (contextData instanceof MailboxProvider) {
            return ((MailboxProvider) contextData).getMailbox();
        }
        throw new IllegalStateException();
    }

    /**
     * shell-function: {@code getSharedArrayBuffer()}
     * 
     * @param cx
     *            the execution context
     * @return the shared array buffer
     */
    @Function(name = "getSharedArrayBuffer", arity = 0)
    public SharedArrayBufferObject getSharedArrayBuffer(ExecutionContext cx) {
        return getMailbox(cx).getSharedArrayBuffer(cx);
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
        SharedArrayBufferObject sharedBuffer;
        if (Type.isUndefinedOrNull(buffer)) {
            sharedBuffer = null;
        } else if (buffer instanceof SharedArrayBufferObject) {
            sharedBuffer = (SharedArrayBufferObject) buffer;
        } else {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        getMailbox(cx).setSharedBufferData(sharedBuffer);
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
        Source baseSource = Objects.requireNonNull(caller.sourceInfo());
        try {
            // TODO: Initialize extensions (console, window timers?).
            CompletableFuture.supplyAsync(() -> {
                // Share the context data with the new context. And set 'executor' to null so it doesn't get shared
                // with the current runtime context.
                /* @formatter:off */
                RuntimeContext context = new RuntimeContext.Builder(cx.getRuntimeContext())
                                                           .setRuntimeData(cx.getRuntimeContext()::getContextData)
                                                           .setExecutor(null)
                                                           .build();
                /* @formatter:on */

                World world = new World(context);
                Realm realm;
                try {
                    realm = Realm.InitializeHostDefinedRealm(world);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }

                // Evaluate the script source code and then run pending jobs.
                Source source = new Source(baseSource, "evalInWorker-script", 1);
                Script script = realm.getScriptLoader().script(source, sourceString);
                Object result = script.evaluate(realm);
                world.runEventLoop();
                return result;
            }, cx.getRuntimeContext().getWorkerExecutor()).whenComplete((r, e) -> {
                if (e != null) {
                    if (e instanceof CompletionException && e.getCause() != null) {
                        e = e.getCause();
                    }
                    cx.getRuntimeContext().getWorkerErrorReporter().accept(cx, e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }
}

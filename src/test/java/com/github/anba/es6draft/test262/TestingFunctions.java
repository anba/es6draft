/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.Realm.InitializeHostDefinedRealm;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.sql.Types;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Supplier;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.HTMLDDAObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Testing functions.
 */
public final class TestingFunctions {
    /**
     * shell-function: {@code createRealm()}
     * 
     * @param cx
     *            the execution context
     * @return the new realm object
     * @throws IOException
     * @throws CompilationException
     * @throws ParserException
     */
    @Function(name = "createRealm", arity = 0)
    public Object createRealm(ExecutionContext cx) throws IOException {
        Realm realm = InitializeHostDefinedRealm(cx.getRealm().getWorld());
        return Get(cx, realm.getGlobalThis(), "$262");
    }

    /**
     * shell-function: {@code detachArrayBuffer(arrayBuffer)}
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     */
    @Function(name = "detachArrayBuffer", arity = 1)
    public void detachArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        DetachArrayBuffer(cx, arrayBuffer);
    }

    /**
     * shell-function: {@code evalScript(sourceString)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param sourceString
     *            the source string
     * @return the evaluation result
     */
    @Function(name = "evalScript", arity = 1)
    public Object evalScript(ExecutionContext cx, ExecutionContext caller, String sourceString) {
        Source source = new Source(caller.sourceInfo(), "<evalScript>", 1);
        Script script = cx.getRealm().getScriptLoader().script(source, sourceString);
        return script.evaluate(cx.getRealm());
    }

    /**
     * shell-value: {@code global}
     * 
     * @param cx
     *            the execution context
     * @return the new global this-value
     */
    @Value(name = "global")
    public ScriptObject global(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
    }

    /**
     * shell-value: {@code IsHTMLDDA}
     * 
     * @param cx
     *            the execution context
     * @return the IsHTMLDDA object
     */
    @Value(name = "IsHTMLDDA")
    public Object IsHTMLDDA(ExecutionContext cx) {
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.IsHTMLDDAObjects)) {
            return new CallableHTMLDDAObject(cx.getRealm());
        }
        return UNDEFINED;
    }

    private static final class CallableHTMLDDAObject extends OrdinaryObject implements HTMLDDAObject, Callable {
        private final Realm realm;

        public CallableHTMLDDAObject(Realm realm) {
            super(realm);
            this.realm = realm;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return Types.NULL;
        }

        @Override
        public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
            return Types.NULL;
        }

        @Override
        public Realm getRealm(ExecutionContext cx) {
            return realm;
        }

        @Override
        public String toSource(ExecutionContext cx) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
    }

    /**
     * shell-value: {@code agent}
     * 
     * @param cx
     *            the execution context
     * @return the new agent object
     */
    @Value(name = "agent")
    public ScriptObject agent(ExecutionContext cx) {
        return createObject(cx, Agent::new, Agent.class);
    }

    private static <T> OrdinaryObject createObject(ExecutionContext cx, Supplier<T> supplier, Class<T> clazz) {
        OrdinaryObject object = ObjectCreate(cx, (ScriptObject) null);
        Properties.createProperties(cx, object, supplier.get(), clazz);
        return object;
    }

    public static final class Agent {
        private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
        private final CopyOnWriteArrayList<Worker> workerList = new CopyOnWriteArrayList<>();

        private static final class Message {
            private final CountDownLatch latch = new CountDownLatch(1);
            private final SharedArrayBufferObject buffer;
            private final int id;

            Message(SharedArrayBufferObject buffer, int id) {
                this.buffer = buffer;
                this.id = id;
            }

            void accept(ExecutionContext cx, Callable receiver) throws InterruptedException {
                latch.await();
                SharedArrayBufferObject clone = new SharedArrayBufferObject(cx.getRealm(),
                        buffer.getSharedData().duplicate(), buffer.getByteLength(),
                        cx.getIntrinsic(Intrinsics.SharedArrayBufferPrototype));
                receiver.call(cx, UNDEFINED, clone, id);
            }

            void done() {
                latch.countDown();
            }
        }

        public final class Worker {
            final Realm realm;
            final SynchronousQueue<Message> message = new SynchronousQueue<>();

            Worker(RuntimeContext parentContext) {
                // Set 'executor' to null so it doesn't get shared with the current runtime context.
                // Set 'realmdata' to the default value to create an empty global object.
                /* @formatter:off */
                RuntimeContext context = new RuntimeContext.Builder(parentContext)
                                                           .setExecutor(null)
                                                           .setRealmData(RealmData::new)
                                                           .build();
                /* @formatter:on */

                World world = new World(context);
                try {
                    realm = Realm.InitializeHostDefinedRealm(world);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }

                realm.createGlobalProperties(this, Worker.class);
            }

            @Value(name = "$262")
            public ScriptObject $262(ExecutionContext cx) {
                return createObject(cx, WorkerTestFunctions::new, WorkerTestFunctions.class);
            }

            public final class WorkerTestFunctions {
                @Value(name = "agent")
                public ScriptObject agent(ExecutionContext cx) {
                    return createObject(cx, WorkerAgent::new, WorkerAgent.class);
                }
            }

            public final class WorkerAgent {
                @Function(name = "receiveBroadcast", arity = 1)
                public void receiveBroadcast(ExecutionContext cx, Callable receiver) throws InterruptedException {
                    Message message = Worker.this.message.take();
                    message.accept(cx, receiver);
                }

                @Function(name = "report", arity = 1)
                public void report(String message) {
                    messageQueue.offer(message);
                }

                @Function(name = "sleep", arity = 1)
                public void sleep(int duration) throws InterruptedException {
                    Thread.sleep(Math.max(duration, 0));
                }

                @Function(name = "leaving", arity = 0)
                public void leaving() {
                    workerList.remove(Worker.this);
                }
            }
        }

        /**
         * shell-function: {@code start(script)}
         * 
         * @param cx
         *            the execution context
         * @param caller
         *            the caller execution context
         * @param sourceCode
         *            the script source code
         * @throws InterruptedException
         *             if interrupted while waiting
         */
        @Function(name = "start", arity = 1)
        public void start(ExecutionContext cx, ExecutionContext caller, String sourceCode) throws InterruptedException {
            Source baseSource = Objects.requireNonNull(caller.sourceInfo());
            try {
                CountDownLatch initialized = new CountDownLatch(1);

                CompletableFuture.runAsync(() -> {
                    Worker worker;
                    try {
                        worker = new Worker(cx.getRuntimeContext());
                        workerList.add(worker);
                    } finally {
                        initialized.countDown();
                    }

                    // Evaluate the script source code and then wait for pending jobs.
                    Realm realm = worker.realm;
                    Source source = new Source(baseSource, "agent-script", 1);
                    Script script = realm.getScriptLoader().script(source, sourceCode);
                    script.evaluate(realm);
                    realm.getWorld().runEventLoop();

                    workerList.remove(worker);
                }, cx.getRuntimeContext().getWorkerExecutor()).whenComplete((r, e) -> {
                    if (e != null) {
                        if (e instanceof CompletionException && e.getCause() != null) {
                            e = e.getCause();
                        }
                        cx.getRuntimeContext().getWorkerErrorReporter().accept(cx, e);
                    }
                });

                initialized.await();
            } catch (RejectedExecutionException e) {
                cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            }
        }

        /**
         * shell-function: {@code broadcast(sharedArrayBuffer, id)}
         * 
         * @param sharedArrayBuffer
         *            the shared array buffer object
         * @param id
         *            the identifier to broadcast
         * @throws InterruptedException
         */
        @Function(name = "broadcast", arity = 1)
        public void broadcast(SharedArrayBufferObject sharedArrayBuffer, int id) throws InterruptedException {
            Message message = new Message(sharedArrayBuffer, id);
            for (Worker worker : workerList) {
                worker.message.put(message);
            }
            message.done();
        }

        /**
         * shell-function: {@code getReport()}
         */
        @Function(name = "getReport", arity = 0)
        public Object getReport() {
            String message = messageQueue.poll();
            return message != null ? message : NULL;
        }

        /**
         * shell-function: {@code sleep(duration)}
         * 
         * @param duration
         *            the sleep duration in milliseconds
         * @throws InterruptedException
         *             if interrupted while sleeping
         */
        @Function(name = "sleep", arity = 1)
        public void sleep(int duration) throws InterruptedException {
            Thread.sleep(Math.max(duration, 0));
        }
    }
}

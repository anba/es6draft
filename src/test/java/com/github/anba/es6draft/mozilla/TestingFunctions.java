/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.SetIntegrityLevel;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.github.anba.es6draft.repl.functions.StopExecutionException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.StringModuleSource;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.objects.promise.PromisePrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    /**
     * shell-function: {@code version([number])}
     *
     * @return the version string "185"
     */
    @Function(name = "version", arity = 1)
    public String version() {
        return "185";
    }

    /**
     * shell-function: {@code options([name])}
     *
     * @return the empty string
     */
    @Function(name = "options", arity = 0)
    public String options() {
        return "";
    }

    /**
     * shell-function: {@code gc()}
     *
     * @return the empty string
     */
    @Function(name = "gc", arity = 0)
    public String gc() {
        return "";
    }

    /**
     * shell-function: {@code gczeal()}
     *
     * @return the empty string
     */
    @Function(name = "gczeal", arity = 0)
    public String gczeal() {
        return "";
    }

    /**
     * shell-function: {@code getBuildConfiguration()}
     *
     * @param cx
     *            the execution context
     * @return an empty object
     */
    @Function(name = "getBuildConfiguration", arity = 0)
    public ScriptObject getBuildConfiguration(ExecutionContext cx) {
        return OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
    }

    /**
     * shell-function: {@code terminate()}
     */
    @Function(name = "terminate", arity = 0)
    public void terminate() {
        throw new StopExecutionException(StopExecutionException.Reason.Terminate);
    }

    /**
     * shell-function: {@code enableOsiPointRegisterChecks()}
     */
    @Function(name = "enableOsiPointRegisterChecks", arity = 0)
    public void enableOsiPointRegisterChecks() {
        // empty
    }

    /**
     * shell-function: {@code isAsmJSCompilationAvailable()}
     * 
     * @return always {@code false}
     */
    @Function(name = "isAsmJSCompilationAvailable", arity = 0)
    public boolean isAsmJSCompilationAvailable() {
        return false;
    }

    /**
     * shell-function: {@code helperThreadCount()}
     * 
     * @return always {@code 0}
     */
    @Function(name = "helperThreadCount", arity = 0)
    public int helperThreadCount() {
        return 0;
    }

    /**
     * shell-function: {@code getSelfHostedValue(name)}
     * 
     * @return a placeholder function
     */
    @Function(name = "getSelfHostedValue", arity = 1)
    public ScriptObject getSelfHostedValue(ExecutionContext cx, String name) {
        if ("ToNumber".equals(name)) {
            return function(cx.getRealm(), "ToNumber", 1, AbstractOperations::ToNumber);
        }
        if ("ToLength".equals(name)) {
            return function(cx.getRealm(), "ToLength", 1, AbstractOperations::ToLength);
        }
        throw new IllegalArgumentException(name);
    }

    private static final <R> BuiltinFunction function(Realm realm, String name, int arity,
            BiFunction<ExecutionContext, Object, R> fn) {
        return new BuiltinFunction(realm, name, arity) {
            @Override
            public R call(ExecutionContext callerContext, Object thisValue, Object... args) {
                return fn.apply(calleeContext(), argument(args, 0));
            }

            @Override
            protected Lookup lookup() {
                return MethodHandles.lookup();
            }
        };
    }

    /**
     * shell-function: {@code detachArrayBuffer(arrayBuffer)}
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     */
    @Function(name = "detachArrayBuffer", arity = 2)
    public void detachArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        if (!arrayBuffer.isDetached()) {
            DetachArrayBuffer(cx, arrayBuffer);
        }
    }

    /**
     * shell-function: {@code immutablePrototypesEnabled()}
     */
    @Function(name = "immutablePrototypesEnabled", arity = 0)
    public boolean immutablePrototypesEnabled() {
        return true;
    }

    /**
     * shell-function: {@code addPromiseReactions(promise, onFulfilled, onRejected)}
     * 
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param onFulfilled
     *            the onFulfilled handler
     * @param onRejected
     *            the onRejected handler
     * @return the new promise object
     */
    @Function(name = "addPromiseReactions", arity = 3)
    public PromiseObject addPromiseReactions(ExecutionContext cx, PromiseObject promise, Callable onFulfilled,
            Callable onRejected) {
        PromiseCapability<PromiseObject> resultCapability = PromiseAbstractOperations.PromiseBuiltinCapability(cx);
        return PromisePrototype.PerformPromiseThen(cx, promise, onFulfilled, onRejected, resultCapability);
    }

    /**
     * shell-function: {@code syntaxParse(sourceCode)}
     * 
     * @param cx
     *            the execution context
     */
    @Function(name = "syntaxParse", arity = 1)
    public void syntaxParse(ExecutionContext cx, String sourceCode) {
        Source source = new Source("<script>", 1);
        cx.getRealm().getScriptLoader().parseScript(source, sourceCode);
    }

    private final WeakHashMap<ScriptObject, SourceTextModuleRecord> modules = new WeakHashMap<>();
    private final AtomicInteger moduleCounter = new AtomicInteger();

    /**
     * shell-function: {@code parseModule(sourceCode)}
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the module source code
     * @return the module descriptor object
     */
    @Function(name = "parseModule", arity = 1)
    public ScriptObject parseModule(ExecutionContext cx, String sourceCode) throws MalformedNameException, IOException {
        ModuleLoader moduleLoader = cx.getRealm().getModuleLoader();
        SourceIdentifier sourceId = moduleLoader.normalizeName("parseModule" + moduleCounter.incrementAndGet(), null);
        StringModuleSource moduleSource = new StringModuleSource(sourceId, "<module>", sourceCode);
        ModuleRecord module = moduleLoader.define(sourceId, moduleSource, cx.getRealm());

        OrdinaryObject moduleObject = ObjectCreate(cx, (ScriptObject) null);
        if (module instanceof SourceTextModuleRecord) {
            Properties.createProperties(cx, moduleObject, new ModuleObjectProperties((SourceTextModuleRecord) module),
                    ModuleObjectProperties.class);
            modules.put(moduleObject, (SourceTextModuleRecord) module);
        }
        SetIntegrityLevel(cx, moduleObject, IntegrityLevel.Frozen);
        return moduleObject;
    }

    /**
     * shell-function: {@code getModuleEnvironmentNames(moduleObject)}
     * 
     * @param cx
     *            the execution context
     * @param moduleObject
     *            the module object
     * @return the module environment value
     */
    @Function(name = "getModuleEnvironmentNames", arity = 1)
    public Object getModuleEnvironmentNames(ExecutionContext cx, ScriptObject moduleObject) {
        ModuleRecord module = getModuleRecord(cx, moduleObject);
        LexicalEnvironment<? extends EnvironmentRecord> environment = module.getEnvironment();
        if (environment == null) {
            throw newInternalError(cx, Messages.Key.InternalError, "Not instantiated");
        }
        return CreateArrayFromList(cx, environment.getEnvRec().bindingNames());
    }

    /**
     * shell-function: {@code getModuleEnvironmentValue(moduleObject, name)}
     * 
     * @param cx
     *            the execution context
     * @param moduleObject
     *            the module object
     * @param name
     *            the binding name
     * @return the module environment value
     */
    @Function(name = "getModuleEnvironmentValue", arity = 2)
    public Object getModuleEnvironmentValue(ExecutionContext cx, ScriptObject moduleObject, String name) {
        ModuleRecord module = getModuleRecord(cx, moduleObject);
        LexicalEnvironment<? extends EnvironmentRecord> environment = module.getEnvironment();
        if (environment == null) {
            throw newInternalError(cx, Messages.Key.InternalError, "Not instantiated");
        }
        if (!environment.getEnvRec().hasBinding(name)) {
            throw newInternalError(cx, Messages.Key.InternalError, "Binding not found");
        }
        return module.getEnvironment().getEnvRec().getBindingValue(name, true);
    }

    /**
     * shell-function: {@code setModuleResolveHook(resolveHook)}
     * 
     * @param cx
     *            the execution context
     * @param moduleObject
     *            the module object
     * @param name
     *            the binding name
     * @return the module environment value
     */
    @Function(name = "setModuleResolveHook", arity = 1)
    public void setModuleResolveHook(ExecutionContext cx, Callable resolveHook) {
        ModuleLoader moduleLoader = cx.getRealm().getModuleLoader();
        if (moduleLoader instanceof MozJitFileModuleLoader) {
            ((MozJitFileModuleLoader) moduleLoader).setModuleResolveHook(moduleName -> {
                Object value = resolveHook.call(cx, UNDEFINED, UNDEFINED, moduleName);
                return getModuleRecord(cx, value);
            });
        }
    }

    private SourceTextModuleRecord getModuleRecord(ExecutionContext cx, Object value) {
        SourceTextModuleRecord module = modules.get(value);
        if (module == null) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid object");
        }
        return module;
    }

    public static final class ModuleObjectProperties {
        private final SourceTextModuleRecord module;

        ModuleObjectProperties(SourceTextModuleRecord module) {
            this.module = module;
        }

        @Properties.Accessor(name = "namespace", type = Properties.Accessor.Type.Getter)
        public Object namespace(ExecutionContext cx) {
            return module.getNamespace() != null ? module.getNamespace() : NULL;
        }

        @Properties.Value(name = "state")
        public Object state() {
            return 1;
        }

        @Properties.Value(name = "requestedModules")
        public Object requestedModules(ExecutionContext cx) {
            return CreateArrayFromList(cx, module.getRequestedModules());
        }

        @Properties.Value(name = "importEntries")
        public Object importEntries(ExecutionContext cx) {
            return CreateArrayFromList(cx, module.getImportEntries().stream().map(entry -> {
                OrdinaryObject entryObject = ObjectCreate(cx, (ScriptObject) null);
                CreateDataProperty(cx, entryObject, "moduleRequest", entry.getModuleRequest());
                CreateDataProperty(cx, entryObject, "importName", entry.getImportName());
                CreateDataProperty(cx, entryObject, "localName", entry.getLocalName());
                return entryObject;
            }));
        }

        @Properties.Value(name = "localExportEntries")
        public Object localExportEntries(ExecutionContext cx) {
            return CreateArrayFromList(cx, module.getLocalExportEntries().stream().map(entry -> {
                OrdinaryObject entryObject = ObjectCreate(cx, (ScriptObject) null);
                CreateDataProperty(cx, entryObject, "exportName", entry.getExportName());
                CreateDataProperty(cx, entryObject, "moduleRequest", NULL);
                CreateDataProperty(cx, entryObject, "importName", NULL);
                String localName = entry.getLocalName();
                if ("*default*".equals(localName)) {
                    localName = "default";
                }
                CreateDataProperty(cx, entryObject, "localName", localName);
                return entryObject;
            }));
        }

        @Properties.Value(name = "indirectExportEntries")
        public Object indirectExportEntries(ExecutionContext cx) {
            return CreateArrayFromList(cx, module.getIndirectExportEntries().stream().map(entry -> {
                OrdinaryObject entryObject = ObjectCreate(cx, (ScriptObject) null);
                CreateDataProperty(cx, entryObject, "exportName", entry.getExportName());
                CreateDataProperty(cx, entryObject, "moduleRequest", entry.getModuleRequest());
                CreateDataProperty(cx, entryObject, "importName", entry.getImportName());
                CreateDataProperty(cx, entryObject, "localName", NULL);
                return entryObject;
            }));
        }

        @Properties.Value(name = "starExportEntries")
        public Object starExportEntries(ExecutionContext cx) {
            return CreateArrayFromList(cx, module.getStarExportEntries().stream().map(entry -> {
                OrdinaryObject entryObject = ObjectCreate(cx, (ScriptObject) null);
                CreateDataProperty(cx, entryObject, "exportName", NULL);
                CreateDataProperty(cx, entryObject, "moduleRequest", entry.getModuleRequest());
                CreateDataProperty(cx, entryObject, "importName", entry.getImportName());
                CreateDataProperty(cx, entryObject, "localName", NULL);
                return entryObject;
            }));
        }

        @Properties.Function(name = "getExportedNames", arity = 1)
        public ArrayObject getExportedNames(ExecutionContext cx, String name)
                throws IOException, MalformedNameException, ResolutionException {
            return CreateArrayFromList(cx, module.getExportedNames(new HashSet<>()));
        }

        @Properties.Function(name = "resolveExport", arity = 1)
        public Object resolveExport(ExecutionContext cx, String name)
                throws IOException, MalformedNameException, ResolutionException {
            ResolvedBinding resolvedExport = module.resolveExport(name, new HashMap<>());
            if (resolvedExport == null || resolvedExport.isNameSpaceExport()) {
                return NULL;
            }
            if (resolvedExport.isAmbiguous()) {
                return "ambiguous";
            }
            return resolvedExport.getBindingName();
        }

        @Properties.Function(name = "declarationInstantiation", arity = 0)
        public void declarationInstantiation(ExecutionContext cx)
                throws IOException, MalformedNameException, ResolutionException {
            switch (module.getStatus()) {
            case Instantiating:
            case Evaluating:
                throw newInternalError(cx, Messages.Key.InternalError, "Instantiating/Evaluating");
            case Uninstantiated:
            case Instantiated:
            case Evaluated:
                module.instantiate();
                break;
            default:
                throw new AssertionError();
            }
            module.instantiate();
        }

        @Properties.Function(name = "evaluation", arity = 0)
        public Object evaluation(ExecutionContext cx) throws IOException, MalformedNameException, ResolutionException {
            switch (module.getStatus()) {
            case Uninstantiated:
            case Instantiating:
            case Evaluating:
                throw newInternalError(cx, Messages.Key.InternalError, "Not instantiated");
            case Instantiated:
            case Evaluated:
                return module.evaluate();
            default:
                throw new AssertionError();
            }
        }
    }
}

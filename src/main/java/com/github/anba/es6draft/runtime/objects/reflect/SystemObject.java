/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.InternalThrowable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.StringModuleSource;
import com.github.anba.es6draft.runtime.objects.ErrorConstructor;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.4 The System Object</h2>
 */
public final class SystemObject extends LoaderObject implements Initializable {
    /**
     * Constructs a new System object.
     * 
     * @param realm
     *            the realm object
     */
    public SystemObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        setLoader(new Loader(realm, this));

        createProperties(realm, this, Properties.class);
        createProperties(realm, this, GlobalProperty.class);
        createProperties(realm, this, WeakReferenceProperty.class);
        createProperties(realm, this, ErrorStackProperties.class);
        createProperties(realm, this, AdditionalProperties.class);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Loader)) {
            setPrototype(realm.getIntrinsic(Intrinsics.LoaderPrototype));
        }
    }

    /**
     * Properties of the System Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;
    }

    /**
     * Properties of the System Object
     */
    @CompatibilityExtension(CompatibilityOption.SystemGlobal)
    public enum GlobalProperty {
        ;

        /**
         * System.global
         * 
         * @param cx
         *            the execution context
         * @return the global object
         */
        @Value(name = "global", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object global(ExecutionContext cx) {
            return cx.getRealm().getGlobalThis();
        }
    }

    /**
     * Properties of the System Object
     */
    @CompatibilityExtension(CompatibilityOption.WeakReference)
    public enum WeakReferenceProperty {
        ;

        /**
         * System.makeWeakRef(target, executor = void 0, holdings = void 0)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param executor
         *            the optional executor function
         * @param holdings
         *            the optional holdings arguments
         * @return the new WeakRef object
         */
        @Function(name = "makeWeakRef", arity = 1)
        public static Object makeWeakRef(ExecutionContext cx, Object thisValue, Object target, Object executor,
                Object holdings) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 2 */
            if (!(Type.isUndefined(executor) || IsCallable(executor))) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 3 */
            if (target == holdings) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 4 */
            if (target == executor) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 5 (not applicable) */
            /* step 6 (FIXME: spec bug - GetFunctionRealm only applicable for functions) */
            /* step 7 */
            Realm thisRealm = cx.getRealm();
            /* step 8 */
            /* steps 8.i-vi */
            Runnable finalizer;
            if (IsCallable(executor)) {
                finalizer = () -> ((Callable) executor).call(cx, UNDEFINED, holdings);
            } else {
                finalizer = null;
            }
            WeakRefObject weakRef = new WeakRefObject(thisRealm, Type.objectValue(target), finalizer,
                    cx.getIntrinsic(Intrinsics.WeakRefPrototype));
            /* step 9 (not applicable - see step 6) */
            /* step 10 */
            return weakRef;
        }
    }

    /**
     * <h1>Extension: Error stacks</h1>
     * <p>
     * Properties of the System Object
     */
    @CompatibilityExtension(CompatibilityOption.ErrorStacks)
    public enum ErrorStackProperties {
        ;

        /**
         * System.getStack ( error )
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param error
         *            the error object
         * @return the error stack object
         */
        @Function(name = "getStack", arity = 1)
        public static Object getStack(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object error) {
            /* step 1 */
            if (!(error instanceof ErrorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleArgument, "System.getStack", Type.of(error).toString());
            }
            /* step 2 */
            return ErrorConstructor.GetStack(cx, (ErrorObject) error);
        }

        /**
         * System.getStackString ( error )
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param error
         *            the error object
         * @return the error stack string
         */
        @Function(name = "getStackString", arity = 1)
        public static Object getStackString(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object error) {
            /* step 1 */
            if (!(error instanceof ErrorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleArgument, "System.getStackString",
                        Type.of(error).toString());
            }
            /* step 2 */
            return ErrorConstructor.GetStackString(cx, (ErrorObject) error);
        }
    }

    /**
     * Properties of the System Object
     */
    @CompatibilityExtension(CompatibilityOption.System)
    public enum AdditionalProperties {
        ;

        /**
         * Abstract Operation: thisLoader(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @param method
         *            the method
         * @return the loader object
         */
        private static LoaderObject thisLoader(ExecutionContext cx, Object value, String method) {
            if (value instanceof LoaderObject) {
                return (LoaderObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        private static SourceIdentifier normalize(ExecutionContext cx, ExecutionContext callerContext,
                ModuleLoader moduleLoader, String unnormalizedName) {
            // TODO: Compute referrerId from caller context?
            SourceIdentifier referrerId = null;
            try {
                return moduleLoader.normalizeName(unnormalizedName, referrerId);
            } catch (MalformedNameException e) {
                throw e.toScriptException(cx);
            }
        }

        private static ScriptException toScriptException(ExecutionContext cx, Throwable e) {
            if (e instanceof CompletionException) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    e = cause;
                }
            }
            if (e instanceof InternalThrowable) {
                return ((InternalThrowable) e).toScriptException(cx);
            }
            if (e instanceof IOException) {
                return newInternalError(cx, e, Messages.Key.ModulesIOException, Objects.toString(e.getMessage(), ""));
            }
            cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            return newInternalError(cx, e, Messages.Key.InternalError, Objects.toString(e.getMessage(), ""));
        }

        /**
         * System.define(moduleName, source)
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @param source
         *            the module source code
         * @return a promise object or undefined
         */
        @Function(name = "define", arity = 2)
        public static Object define(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object moduleName, Object source) {
            LoaderObject loader = thisLoader(cx, thisValue, "System.define");
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            String sourceCode = ToFlatString(cx, source);
            SourceIdentifier identifier = normalize(cx, callerContext, moduleLoader, unnormalizedName);
            ModuleSource src = new StringModuleSource(identifier, unnormalizedName, sourceCode);

            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            moduleLoader.defineAsync(identifier, src, realm).whenComplete((module, err) -> {
                realm.enqueueAsyncJob(() -> {
                    if (module != null) {
                        ScriptObject namespace;
                        try {
                            module.instantiate();
                            module.evaluate();
                            namespace = GetModuleNamespace(cx, module);
                        } catch (ScriptException | IOException | MalformedNameException | ResolutionException e) {
                            promiseCapability.getReject().call(cx, UNDEFINED, toScriptException(cx, e).getValue());
                            return;
                        }
                        promiseCapability.getResolve().call(cx, UNDEFINED, namespace);
                    } else {
                        promiseCapability.getReject().call(cx, UNDEFINED, toScriptException(cx, err).getValue());
                    }
                });
            });
            return promiseCapability.getPromise();
        }

        /**
         * System.import(moduleName)
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return a promise object or undefined
         */
        @Function(name = "import", arity = 1)
        public static Object _import(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue, "System.import");
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader, unnormalizedName);

            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            moduleLoader.resolveAsync(normalizedModuleName, realm).whenComplete((module, err) -> {
                realm.enqueueAsyncJob(() -> {
                    if (module != null) {
                        ScriptObject namespace;
                        try {
                            module.instantiate();
                            namespace = GetModuleNamespace(cx, module);
                        } catch (ScriptException | IOException | MalformedNameException | ResolutionException e) {
                            promiseCapability.getReject().call(cx, UNDEFINED, toScriptException(cx, e).getValue());
                            return;
                        }
                        promiseCapability.getResolve().call(cx, UNDEFINED, namespace);
                    } else {
                        promiseCapability.getReject().call(cx, UNDEFINED, toScriptException(cx, err).getValue());
                    }
                });
            });
            return promiseCapability.getPromise();
        }

        /**
         * System.load(moduleName)
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return undefined
         */
        @Function(name = "load", arity = 1)
        public static Object load(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue, "System.load");
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader, unnormalizedName);

            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            moduleLoader.loadAsync(normalizedModuleName).whenComplete((module, exception) -> {
                realm.enqueueAsyncJob(() -> {
                    if (module != null) {
                        promiseCapability.getResolve().call(cx, UNDEFINED);
                    } else {
                        promiseCapability.getReject().call(cx, UNDEFINED, toScriptException(cx, exception).getValue());
                    }
                });
            });
            return promiseCapability.getPromise();
        }

        /**
         * System.get(moduleName)
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return the module or undefined
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue, "System.get");
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader, unnormalizedName);
            ModuleRecord module = moduleLoader.get(normalizedModuleName, realm);
            if (module == null) {
                return UNDEFINED;
            }
            try {
                module.instantiate();
                module.evaluate();
                return GetModuleNamespace(cx, module);
            } catch (IOException | MalformedNameException | ResolutionException | ParserException
                    | CompilationException e) {
                throw toScriptException(cx, e);
            }
        }

        /**
         * System.normalize(moduleName)
         * 
         * @param cx
         *            the execution context
         * @param callerContext
         *            the caller execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return the normalized module name
         */
        @Function(name = "normalize", arity = 1)
        public static Object normalize(ExecutionContext cx, ExecutionContext callerContext, Object thisValue,
                Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue, "System.normalize");
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            return normalize(cx, callerContext, moduleLoader, unnormalizedName).toString();
        }
    }
}

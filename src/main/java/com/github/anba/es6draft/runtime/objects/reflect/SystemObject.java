/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseOf;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
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
import com.github.anba.es6draft.runtime.types.Intrinsics;

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
        createProperties(realm, this, AdditionalProperties.class);
        if (realm.isEnabled(CompatibilityOption.Loader)) {
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
         * @return the loader object
         */
        private static LoaderObject thisLoader(ExecutionContext cx, Object value) {
            if (value instanceof LoaderObject) {
                return (LoaderObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static SourceIdentifier normalize(ExecutionContext cx,
                ExecutionContext callerContext, ModuleLoader moduleLoader, String unnormalizedName) {
            // TODO: Compute referrerId from caller context?
            SourceIdentifier referrerId = null;
            try {
                return moduleLoader.normalizeName(unnormalizedName, referrerId);
            } catch (MalformedNameException e) {
                throw e.toScriptException(cx);
            }
        }

        private static ScriptException toScriptException(ExecutionContext cx, IOException e) {
            return newInternalError(cx, e, Messages.Key.ModulesIOException, Objects.toString(e.getMessage(), ""));
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
        public static Object define(ExecutionContext cx, ExecutionContext callerContext,
                Object thisValue, Object moduleName, Object source) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            String sourceCode = ToFlatString(cx, source);
            SourceIdentifier identifier = normalize(cx, callerContext, moduleLoader,
                    unnormalizedName);
            ModuleSource src = new StringModuleSource(identifier, sourceCode);
            try {
                ModuleRecord module = moduleLoader.define(identifier, src, realm);
                module.instantiate();
                module.evaluate();
                return PromiseOf(cx, GetModuleNamespace(cx, module));
            } catch (IOException e) {
                return PromiseOf(cx, toScriptException(cx, e));
            } catch (MalformedNameException | ResolutionException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            } catch (ScriptException | ParserException | CompilationException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            }
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
        public static Object _import(ExecutionContext cx, ExecutionContext callerContext,
                Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader,
                    unnormalizedName);
            try {
                ModuleRecord module = moduleLoader.resolve(normalizedModuleName, realm);
                module.instantiate();
                return PromiseOf(cx, GetModuleNamespace(cx, module));
            } catch (IOException e) {
                return PromiseOf(cx, toScriptException(cx, e));
            } catch (MalformedNameException | ResolutionException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            } catch (ScriptException | ParserException | CompilationException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            }
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
        public static Object load(ExecutionContext cx, ExecutionContext callerContext,
                Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader,
                    unnormalizedName);
            try {
                moduleLoader.load(normalizedModuleName);
            } catch (IOException e) {
                return PromiseOf(cx, toScriptException(cx, e));
            } catch (MalformedNameException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            } catch (ParserException | CompilationException e) {
                return PromiseOf(cx, e.toScriptException(cx));
            }
            return PromiseOf(cx, UNDEFINED);
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
        public static Object get(ExecutionContext cx, ExecutionContext callerContext,
                Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, callerContext, moduleLoader,
                    unnormalizedName);
            ModuleRecord module = moduleLoader.get(normalizedModuleName, realm);
            if (module == null) {
                return UNDEFINED;
            }
            try {
                module.instantiate();
                module.evaluate();
                return GetModuleNamespace(cx, module);
            } catch (IOException e) {
                throw toScriptException(cx, e);
            } catch (MalformedNameException | ResolutionException e) {
                throw e.toScriptException(cx);
            } catch (ParserException | CompilationException e) {
                throw e.toScriptException(cx);
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
        public static Object normalize(ExecutionContext cx, ExecutionContext callerContext,
                Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            return normalize(cx, callerContext, moduleLoader, unnormalizedName).toString();
        }
    }
}

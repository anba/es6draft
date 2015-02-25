/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.Loader.CreateLoader;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseOf;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

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
        Loader loaderRecord = CreateLoader(realm, this);
        setLoader(loaderRecord);

        createProperties(realm, this, Properties.class);
        if (realm.isEnabled(CompatibilityOption.Loader)) {
            setPrototype(realm.getIntrinsic(Intrinsics.LoaderPrototype));
        }
    }

    private static final class StringModuleSource implements ModuleSource {
        private final SourceIdentifier sourceId;
        private final String sourceCode;

        StringModuleSource(SourceIdentifier sourceId, String sourceCode) {
            this.sourceId = sourceId;
            this.sourceCode = sourceCode;
        }

        @Override
        public String sourceCode() {
            return sourceCode;
        }

        @Override
        public Source toSource() {
            return new Source(sourceId.toString(), 1);
        }
    }

    /**
     * Properties of the System Object
     */
    public enum Properties {
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
                LoaderObject loader = (LoaderObject) value;
                if (loader.getLoader() != null) {
                    return loader;
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static SourceIdentifier normalize(ExecutionContext cx, ModuleLoader moduleLoader,
                String unnormalizedName) {
            try {
                return moduleLoader.normalizeName(unnormalizedName, null);
            } catch (MalformedNameException e) {
                throw e.toScriptException(cx);
            }
        }

        private static ScriptException toScriptException(ExecutionContext cx, IOException e) {
            return newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }

        @Prototype
        public static final ScriptObject __proto__ = null;

        /**
         * System.define(moduleName, source)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @param source
         *            the module source code
         * @return a promise object or undefined
         */
        @Function(name = "define", arity = 2)
        public static Object define(ExecutionContext cx, Object thisValue, Object moduleName,
                Object source) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            String sourceCode = ToFlatString(cx, source);
            SourceIdentifier identifier = normalize(cx, moduleLoader, unnormalizedName);
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
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return a promise object or undefined
         */
        @Function(name = "import", arity = 1)
        public static Object _import(ExecutionContext cx, Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, moduleLoader, unnormalizedName);
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
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return undefined
         */
        @Function(name = "load", arity = 1)
        public static Object load(ExecutionContext cx, Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, moduleLoader, unnormalizedName);
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
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return the module or undefined
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = normalize(cx, moduleLoader, unnormalizedName);
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
         * @param thisValue
         *            the function this-value
         * @param moduleName
         *            the module name
         * @return the normalized module name
         */
        @Function(name = "normalize", arity = 1)
        public static Object normalize(ExecutionContext cx, Object thisValue, Object moduleName) {
            LoaderObject loader = thisLoader(cx, thisValue);
            Realm realm = loader.getLoader().getRealm();
            ModuleLoader moduleLoader = realm.getModuleLoader();

            String unnormalizedName = ToFlatString(cx, moduleName);
            return normalize(cx, moduleLoader, unnormalizedName).toString();
        }
    }
}

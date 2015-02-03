/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.Loader.CreateLoader;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.*;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseOf;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

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
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
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
            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = NormalizeModuleName(cx, realm,
                    unnormalizedName, null);
            String sourceCode = ToFlatString(cx, source);
            ModuleSource src = new StringModuleSource(normalizedModuleName, sourceCode);
            try {
                ModuleEvaluationJob(cx, realm, normalizedModuleName, src);
            } catch (ScriptException e) {
                return PromiseOf(cx, e);
            }
            return PromiseOf(cx, GetModuleNamespace(cx, realm, normalizedModuleName));
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
            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = NormalizeModuleName(cx, realm,
                    unnormalizedName, null);
            try {
                ModuleEvaluationJob(cx, realm, normalizedModuleName);
            } catch (ScriptException e) {
                return PromiseOf(cx, e);
            }
            return PromiseOf(cx, GetModuleNamespace(cx, realm, normalizedModuleName));
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
            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = NormalizeModuleName(cx, realm,
                    unnormalizedName, null);
            try {
                LoadModule(cx, realm, normalizedModuleName);
            } catch (ScriptException e) {
                return PromiseOf(cx, e);
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
            String unnormalizedName = ToFlatString(cx, moduleName);
            SourceIdentifier normalizedModuleName = NormalizeModuleName(cx, realm,
                    unnormalizedName, null);
            ModuleRecord module = ModuleAt(realm.getModules(), normalizedModuleName);
            if (module == null) {
                return UNDEFINED;
            }
            ModuleEvaluation(module, realm);
            return GetModuleNamespace(cx, realm, normalizedModuleName);
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
            String unnormalizedName = ToFlatString(cx, moduleName);
            return NormalizeModuleName(cx, realm, unnormalizedName, null).toString();
        }
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.LinkSet.CreateLinkSet;
import static com.github.anba.es6draft.runtime.modules.Load.CreateLoadFromAddress;
import static com.github.anba.es6draft.runtime.modules.ModuleEvaluation.EnsureEvaluated;
import static com.github.anba.es6draft.runtime.modules.ModuleLinkage.CreateLinkedModuleInstance;
import static com.github.anba.es6draft.runtime.modules.ModuleLoading.LoadModule;
import static com.github.anba.es6draft.runtime.modules.ModuleLoading.ProceedToTranslate;
import static com.github.anba.es6draft.runtime.modules.ModuleLoading.PromiseOfStartLoadPartwayThrough;
import static com.github.anba.es6draft.runtime.objects.reflect.LoaderIteratorPrototype.CreateLoaderIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.LinkSet;
import com.github.anba.es6draft.runtime.modules.Load;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.modules.ModuleEvaluation.EvaluateLoadedModule;
import com.github.anba.es6draft.runtime.modules.ModuleLinkage;
import com.github.anba.es6draft.runtime.modules.ModuleLoading.AsyncStartLoadPartwayThrough;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderIteratorPrototype.LoaderIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.2 Loader Objects</h2>
 * <ul>
 * <li>26.2.3 Properties of the Reflect.Loader Prototype Object
 * </ul>
 */
public final class LoaderPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Loader prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public LoaderPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, RealmProperty.class);
    }

    /**
     * 26.2.3 Properties of the Reflect.Loader Prototype Object
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
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.2.3.1 Reflect.Loader.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Loader;

        /**
         * 26.2.3.6 get Reflect.Loader.prototype.global
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global object
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            return loaderRecord.getRealm().getGlobalThis();
        }

        /**
         * 26.2.3.2 Reflect.Loader.prototype.define ( name, source, options = undefined )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @param source
         *            the module source
         * @param options
         *            the options object
         * @return a new promise object
         */
        @Function(name = "define", arity = 2)
        public static Object define(ExecutionContext cx, Object thisValue, Object name,
                Object source, Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            String moduleName = ToFlatString(cx, name);
            /* steps 6-7 */
            Object address = GetOption(cx, options, "address");
            /* steps 8-9 */
            Object metadata = GetOption(cx, options, "metadata");
            /* step 10 */
            if (Type.isUndefined(metadata)) {
                metadata = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            }
            /* steps 11-12 */
            PromiseObject p = PromiseOfStartLoadPartwayThrough(cx,
                    AsyncStartLoadPartwayThrough.Step.Translate, loaderRecord, moduleName,
                    metadata, source, address);
            /* step 13 */
            ReturnUndefined g = new ReturnUndefined(cx.getRealm());
            /* steps 14-15 */
            return PromiseThen(cx, p, g);
        }

        /**
         * 26.2.3.10 Reflect.Loader.prototype.load ( name, options = undefined )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @param options
         *            the options object
         * @return a new promise object
         */
        @Function(name = "load", arity = 1)
        public static Object load(ExecutionContext cx, Object thisValue, Object name, Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            PromiseObject p = LoadModule(cx, loaderRecord, name, options);
            /* step 5 */
            ReturnUndefined f = new ReturnUndefined(cx.getRealm());
            /* steps 6-7 */
            return PromiseThen(cx, p, f);
        }

        /**
         * 26.2.3.11 Reflect.Loader.prototype.module ( source [, options ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the module source
         * @param options
         *            the options object
         * @return a new promise object
         */
        @Function(name = "module", arity = 1)
        public static Object module(ExecutionContext cx, Object thisValue, Object source,
                Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            Object address = GetOption(cx, options, "address");
            /* steps 6-7 */
            Load load = CreateLoadFromAddress(cx, address);
            /* step 8 */
            LinkSet linkSet = CreateLinkSet(cx, loaderRecord, load);
            /* steps 9-11 */
            EvaluateLoadedModule successCallback = new EvaluateLoadedModule(cx.getRealm(),
                    loaderRecord);
            /* step 11 */
            ScriptObject p = PromiseThen(cx, linkSet.getDone(), successCallback);
            /* step 12 */
            PromiseObject sourcePromise = PromiseOf(cx, source);
            /* step 13 */
            ProceedToTranslate(cx, loaderRecord, load, sourcePromise);
            /* steps 14 */
            return p;
        }

        /**
         * 26.2.3.8 Reflect.Loader.prototype.import ( name, options = undefined )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @param options
         *            the options object
         * @return a new promise object
         */
        @Function(name = "import", arity = 1)
        public static Object _import(ExecutionContext cx, Object thisValue, Object name,
                Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* step 4-5 */
            PromiseObject p = LoadModule(cx, loaderRecord, name, options);
            /* steps 6-7 */
            EvaluateLoadedModule f = new EvaluateLoadedModule(cx.getRealm(), loaderRecord);
            /* steps 8-9 */
            return PromiseThen(cx, p, f);
        }

        /**
         * 26.2.3.12 newModule ( obj )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param obj
         *            the module entries
         * @return the new module object
         */
        @Function(name = "newModule", arity = 1)
        public static Object newModule(ExecutionContext cx, Object thisValue, Object obj) {
            /* step 1 */
            if (!Type.isObject(obj)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject object = Type.objectValue(obj);
            /* step 2 */
            ModuleObject mod = CreateLinkedModuleInstance(cx).getModuleObject();
            /* steps 3-4 */
            List<String> keys = EnumerableOwnNames(cx, object);
            /* step 5 */
            for (String key : keys) {
                Object value = Get(cx, object, key);
                Callable f = CreateConstantGetter(cx, key, value);
                PropertyDescriptor desc = new PropertyDescriptor(f, null, true, false);
                DefinePropertyOrThrow(cx, mod, key, desc);
            }
            /* step 6 */
            mod.preventExtensions(cx);
            /* step 7 */
            return mod;
        }

        /**
         * 26.2.3.5 Reflect.Loader.prototype.get ( name )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @return the module object or undefined
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            String sname = ToFlatString(cx, name);
            /* steps 6-7 */
            ModuleLinkage module = loaderRecord.getModules().get(sname);
            if (module != null) {
                /* step 7a */
                EnsureEvaluated(cx, module, loaderRecord);
                return module.getModuleObject();
            }
            /* step 8 */
            return UNDEFINED;
        }

        /**
         * 26.2.3.7 Reflect.Loader.prototype.has ( name )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @return {@code true} if the module was found in the module registry
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            String sname = ToFlatString(cx, name);
            /* steps 6-7 */
            return loaderRecord.getModules().containsKey(sname);
        }

        /**
         * 26.2.3.14 Reflect.Loader.prototype.set ( name, module )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @param module
         *            the module object
         * @return this loader object
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object name, Object module) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            String sname = ToFlatString(cx, name);
            /* step 6 */
            if (!(module instanceof ModuleObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ModuleObject m = (ModuleObject) module;
            assert m.getModuleLinkage() != null;
            /* steps 7-10 */
            loaderRecord.getModules().put(sname, m.getModuleLinkage());
            /* steps 8.a.ii, 11 */
            return loader;
        }

        /**
         * 26.2.3.3 Reflect.Loader.prototype.delete ( name )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @return {@code true} if the module was successfully deleted
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* steps 4-5 */
            String sname = ToFlatString(cx, name);
            /* steps 6-8 */
            return loaderRecord.getModules().remove(sname) != null;
        }

        /**
         * 26.2.3.4 Reflect.Loader.prototype.entries ( )<br>
         * 26.2.3.16 Reflect.Loader.prototype[@@iterator] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the loader entries iterator
         */
        @Function(name = "entries", arity = 0)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, LoaderIterationKind.KeyValue);
        }

        /**
         * 26.2.3.9 Reflect.Loader.prototype.keys ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the loader keys iterator
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, LoaderIterationKind.Key);
        }

        /**
         * 26.2.3.15 Reflect.Loader.prototype.values ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the loader values iterator
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, LoaderIterationKind.Value);
        }

        /**
         * 26.2.3.17 Reflect.Loader.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Reflect.Loader";

        /**
         * 26.2.3.18 Loader Pipeline Hook Properties
         * <p>
         * 26.2.3.18.1 Reflect.Loader.prototype.normalize ( name, referrerName, referrerAddress )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the module name
         * @param referrerName
         *            the referrer name
         * @param referrerAddress
         *            the referrer address
         * @return the normalized module name
         */
        @Function(name = "normalize", arity = 3)
        public static Object normalize(ExecutionContext cx, Object thisValue, Object name,
                Object referrerName, Object referrerAddress) {
            /* step 1 */
            return name;
        }

        /**
         * 26.2.3.18 Loader Pipeline Hook Properties
         * <p>
         * 26.2.3.18.2 Reflect.Loader.prototype.locate (loadRequest)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param loadRequest
         *            the load request object
         * @return the resolved module name
         */
        @Function(name = "locate", arity = 1)
        public static Object locate(ExecutionContext cx, Object thisValue, Object loadRequest) {
            // FIXME: spec bug - missing type check for load (bug 2457)
            if (!Type.isObject(loadRequest)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            return Get(cx, Type.objectValue(loadRequest), "name");
        }

        /**
         * 26.2.3.18 Loader Pipeline Hook Properties
         * <p>
         * 26.2.3.18.3 Reflect.Loader.prototype.fetch (loadRequest)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param loadRequest
         *            the load request object
         * @return the module source code
         */
        @Function(name = "fetch", arity = 1)
        public static Object fetch(ExecutionContext cx, Object thisValue, Object loadRequest) {
            /* step 1 */
            throw newTypeError(cx, Messages.Key.LoaderFetchNotImplemented);
        }

        /**
         * 26.2.3.18 Loader Pipeline Hook Properties
         * <p>
         * 26.2.3.18.4 Reflect.Loader.prototype.translate ( loadRequest )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param loadRequest
         *            the load request object
         * @return the translated module source code
         */
        @Function(name = "translate", arity = 1)
        public static Object translate(ExecutionContext cx, Object thisValue, Object loadRequest) {
            // FIXME: spec bug - missing type check for load (bug 2456)
            if (!Type.isObject(loadRequest)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            return Get(cx, Type.objectValue(loadRequest), "source");
        }

        /**
         * 26.2.3.18 Loader Pipeline Hook Properties
         * <p>
         * 26.2.3.18.5 Reflect.Loader.prototype.instantiate (loadRequest)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param loadRequest
         *            the load request object
         * @return the instantiated module
         */
        @Function(name = "instantiate", arity = 1)
        public static Object instantiate(ExecutionContext cx, Object thisValue, Object loadRequest) {
            /* step 1 */
            return UNDEFINED;
        }
    }

    /**
     * ReturnUndefined Functions
     */
    public static final class ReturnUndefined extends BuiltinFunction {
        public ReturnUndefined(Realm realm) {
            super(realm, ANONYMOUS);
            createDefaultFunctionProperties(ANONYMOUS, 0);
        }

        private ReturnUndefined(Realm realm, Void ignore) {
            super(realm, ANONYMOUS);
        }

        @Override
        public ReturnUndefined clone() {
            return new ReturnUndefined(getRealm(), null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return UNDEFINED;
        }
    }

    /**
     * Additional Properties of the Reflect.Loader Prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.Realm)
    public enum RealmProperty {
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

        /**
         * 26.2.3.13 get Reflect.Loader.prototype.realm
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the realm object
         */
        @Accessor(name = "realm", type = Accessor.Type.Getter)
        public static Object realm(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            Loader loaderRecord = loader.getLoader();
            /* step 4 */
            return loaderRecord.getRealm().getRealmObject();
        }
    }

    /**
     * Constant Functions
     */
    public static final class ConstantFunction<VALUE> extends BuiltinFunction {
        /** [[ConstantValue]] */
        private final VALUE constantValue;

        public ConstantFunction(Realm realm, String name, VALUE constantValue) {
            this(realm, name, constantValue, null);
            createDefaultFunctionProperties(name, 0);
        }

        private ConstantFunction(Realm realm, String name, VALUE constantValue, Void ignore) {
            super(realm, name);
            this.constantValue = constantValue;
        }

        @Override
        public ConstantFunction<VALUE> clone() {
            return new ConstantFunction<>(getRealm(), getName(), constantValue, null);
        }

        @Override
        public VALUE call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return constantValue;
        }
    }

    /**
     * CreateConstantGetter(key, value) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the getter key
     * @param value
     *            the getter value
     * @return the new getter function
     */
    public static Callable CreateConstantGetter(ExecutionContext cx, String key, Object value) {
        /* steps 1-3 */
        return new ConstantFunction<>(cx.getRealm(), "get " + key, value);
    }
}

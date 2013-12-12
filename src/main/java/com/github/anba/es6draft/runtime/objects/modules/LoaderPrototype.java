/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.LinkSet.CreateLinkSet;
import static com.github.anba.es6draft.runtime.modules.LinkSet.LoadModule;
import static com.github.anba.es6draft.runtime.modules.Load.CreateLoadFromAddress;
import static com.github.anba.es6draft.runtime.modules.Load.ProceedToTranslate;
import static com.github.anba.es6draft.runtime.modules.ModuleEvaluation.EnsureEvaluated;
import static com.github.anba.es6draft.runtime.objects.modules.LoaderConstructor.GetOption;
import static com.github.anba.es6draft.runtime.objects.modules.LoaderIteratorPrototype.CreateLoaderIterator;
import static com.github.anba.es6draft.runtime.objects.modules.RealmConstructor.IndirectEval;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseCreate;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseResolve;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashSet;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.LinkSet;
import com.github.anba.es6draft.runtime.modules.LinkSet.AsyncStartLoadPartwayThrough;
import com.github.anba.es6draft.runtime.modules.Load;
import com.github.anba.es6draft.runtime.modules.ModuleObject;
import com.github.anba.es6draft.runtime.objects.modules.LoaderIteratorPrototype.MapIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.6 Loader Objects</h2>
 * <ul>
 * <li>1.6.3 Properties of the Loader Prototype Object
 * </ul>
 */
public class LoaderPrototype extends OrdinaryObject implements Initialisable {
    public LoaderPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 1.6.3 Properties of the Loader Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract Operation: thisLoader(value)
         */
        private static LoaderObject thisLoader(ExecutionContext cx, Object value) {
            if (value instanceof LoaderObject) {
                LoaderObject loader = (LoaderObject) value;
                if (loader.getModules() == null) {
                    throw throwTypeError(cx, Messages.Key.UninitialisedObject);
                }
                return loader;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Loader.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Loader;

        /**
         * 1.6.3.1 get Loader.prototype.realm
         */
        @Accessor(name = "realm", type = Accessor.Type.Getter)
        public static Object realm(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return loader.getRealm().getRealmObject();
        }

        /**
         * 1.6.3.2 get Loader.prototype.global
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return loader.getRealm().getGlobalThis();
        }

        /**
         * 1.6.3.3 Loader.prototype.define ( name, source, options = undefined )
         */
        @Function(name = "define", arity = 2)
        public static Object define(ExecutionContext cx, Object thisValue, Object name,
                Object source, Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            String moduleName = ToFlatString(cx, name);
            /* steps 5-6 */
            Object address = GetOption(cx, options, "address");
            /* steps 7-8 */
            Object metadata = GetOption(cx, options, "metadata");
            /* step 9 */
            if (Type.isUndefined(metadata)) {
                metadata = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            }
            /* steps 11-16 */
            AsyncStartLoadPartwayThrough f = new AsyncStartLoadPartwayThrough(cx.getRealm(),
                    loader, moduleName, AsyncStartLoadPartwayThrough.Step.Translate, metadata,
                    address, source);
            /* step 17 */
            ScriptObject p = PromiseCreate(cx, f);
            /* step 18 */
            ReturnUndefined g = new ReturnUndefined(cx.getRealm());
            /* step 19 */
            p = PromiseThen(cx, p, g);
            /* step 20 */
            return p;
        }

        /**
         * 1.6.3.5 Loader.prototype.load ( request, options = undefined )
         */
        @Function(name = "load", arity = 1)
        public static Object load(ExecutionContext cx, Object thisValue, Object request,
                Object options) {
            // FIXME: spec bug - 'name' variable is not defined
            String name = "";
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            ScriptObject p = LoadModule(cx, loader, name, options);
            /* step 5 */
            ReturnUndefined f = new ReturnUndefined(cx.getRealm());
            /* step 6 */
            p = PromiseThen(cx, p, f);
            /* step 7 */
            return p;
        }

        /**
         * 1.6.3.6 Loader.prototype.module ( source, options )<br>
         * FIXME: spec bug - options not declared as optional (options = undefined)
         */
        @Function(name = "module", arity = 1)
        public static Object module(ExecutionContext cx, Object thisValue, Object source,
                Object options) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            Object address = GetOption(cx, options, "address");
            /* steps 5-6 */
            // FIXME: spec bug - typo 'addres' -> 'address'
            Load load = CreateLoadFromAddress(cx, address);
            /* step 7 */
            LinkSet linkSet = CreateLinkSet(cx, loader, load);
            /* steps 8-10 */
            EvaluateLoadedModule successCallback = new EvaluateLoadedModule(cx.getRealm(), loader,
                    load);
            /* step 11 */
            ScriptObject p = PromiseThen(cx, linkSet.getDone(), successCallback);
            /* step 12 */
            // FIXME: spec bug - PromiseOf
            ScriptObject sourcePromise = PromiseResolve(cx, source);
            /* step 13 */
            ProceedToTranslate(cx, loader, load, sourcePromise);
            /* step 14 */
            return p;
        }

        /**
         * 1.6.3.7 Loader.prototype.import ( name, options )<br>
         * FIXME: spec bug - options not declared as optional (options = undefined)
         */
        @Function(name = "import", arity = 1)
        public static Object _import(ExecutionContext cx, Object thisValue, Object name,
                Object options) {
            // FIXME: spec bug - no load defined
            Load load = null;
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3-4 */
            ScriptObject p = LoadModule(cx, loader, name, options);
            /* steps 5-6 */
            EvaluateLoadedModule f = new EvaluateLoadedModule(cx.getRealm(), loader, load);
            /* step 7 */
            p = PromiseThen(cx, p, f);
            /* step 8 */
            return p;
        }

        /**
         * 1.6.3.8 Loader.prototype.eval ( source )
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, Object thisValue, Object source) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return IndirectEval(loader.getRealm(), source);
        }

        /**
         * 1.6.3.9 Loader.prototype.get ( name )
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            String sname = ToFlatString(cx, name);
            /* step 5 */
            ModuleObject module = loader.getModules().get(sname);
            /* step 5a */
            if (module != null) {
                EnsureEvaluated(cx, module, loader);
                return module;
            }
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * 1.6.3.10 Loader.prototype.has ( name )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            String sname = ToFlatString(cx, name);
            /* steps 5-6 */
            return loader.getModules().containsKey(sname);
        }

        /**
         * 1.6.3.11 Loader.prototype.set ( name, module )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object name, Object module) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            String sname = ToFlatString(cx, name);
            /* step 5 */
            if (!(module instanceof ModuleObject)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ModuleObject m = (ModuleObject) module;
            /* steps 6-8 */
            loader.getModules().put(sname, m);
            /* step 9 */
            return loader;
        }

        /**
         * 1.6.3.12 Loader.prototype.delete ( name )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object name) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* steps 3-4 */
            String sname = ToFlatString(cx, name);
            /* steps 5-6 */
            return loader.getModules().remove(sname);
        }

        /**
         * 1.6.3.13 Loader.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, MapIterationKind.KeyValue);
        }

        /**
         * 1.6.3.14 Loader.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, MapIterationKind.Key);
        }

        /**
         * 1.6.3.15 Loader.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            LoaderObject loader = thisLoader(cx, thisValue);
            /* step 3 */
            return CreateLoaderIterator(cx, loader, MapIterationKind.Value);
        }

        /**
         * 1.6.3.16 Loader.prototype.normalize ( name, refererName, refererAddress )
         */
        @Function(name = "normalize", arity = 3)
        public static Object normalize(ExecutionContext cx, Object thisValue, Object name,
                Object refererName, Object refererAddress) {
            /* step 1 */
            return name;
        }

        /**
         * 1.6.3.17 Loader.prototype.locate ( load )
         */
        @Function(name = "locate", arity = 1)
        public static Object locate(ExecutionContext cx, Object thisValue, Object load) {
            // FIXME: spec bug - missing type check for load
            if (!Type.isObject(load)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            return Get(cx, Type.objectValue(load), "name");
        }

        /**
         * 1.6.3.18 Loader.prototype.fetch ( load )
         */
        @Function(name = "fetch", arity = 1)
        public static Object fetch(ExecutionContext cx, Object thisValue, Object load) {
            throw throwTypeError(cx, Messages.Key.InternalError);
        }

        /**
         * 1.6.3.19 Loader.prototype.translate ( load )
         */
        @Function(name = "translate", arity = 1)
        public static Object translate(ExecutionContext cx, Object thisValue, Object load) {
            // FIXME: spec bug - missing type check for load
            if (!Type.isObject(load)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            return Get(cx, Type.objectValue(load), "source");
        }

        /**
         * 1.6.3.20 Loader.prototype.instantiate ( load )
         */
        @Function(name = "instantiate", arity = 1)
        public static Object instantiate(ExecutionContext cx, Object thisValue, Object load) {
            return UNDEFINED;
        }
    }

    /**
     * 1.1.4 EvaluateLoadedModule Functions
     */
    public static final class EvaluateLoadedModule extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        // FIXME: spec bug - internal slot not listed in description, actually argument to function?
        private final Load load;

        public EvaluateLoadedModule(Realm realm, LoaderObject loader, Load load) {
            super(realm, ANONYMOUS, 0);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            assert load.getStatus() == Load.Status.Linked;
            /* step 3 */
            ModuleObject module = load.getModule();
            /* steps 4-5 */
            EnsureEvaluated(calleeContext, module, new HashSet<ModuleObject>(), loader);
            /* step 6 */
            return module;
        }
    }

    /**
     * 1.6.3.4 ReturnUndefined Functions
     */
    public static final class ReturnUndefined extends BuiltinFunction {
        public ReturnUndefined(Realm realm) {
            super(realm, ANONYMOUS, 0);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return UNDEFINED;
        }
    }
}

/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.modules.LinkSet.AddLoadToLinkSet;
import static com.github.anba.es6draft.runtime.modules.LinkSet.LinkSetFailed;
import static com.github.anba.es6draft.runtime.modules.LinkSet.UpdateLinkSetOnLoad;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>Modules: Semantics</h1><br>
 * <h2>Module Loading</h2>
 * <ul>
 * <li>Load Records
 * </ul>
 */
public final class Load {
    public enum Status {
        Loading, Loaded, Linked, Failed
    }

    public enum Kind {
        Declarative, Dynamic
    }

    public static final class Dependency {
        private final String moduleName;
        private final String normalisedModuleName;

        public Dependency(String moduleName, String normalisedModuleName) {
            this.moduleName = moduleName;
            this.normalisedModuleName = normalisedModuleName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getNormalisedModuleName() {
            return normalisedModuleName;
        }
    }

    /** [[Status]] */
    private Status status;

    /** [[Name]] */
    private final String name;

    /** [[LinkSets]] */
    private final List<LinkSet> linkSets;

    /** [[Metadata]] */
    private final Object metadata;

    /** [[Address]] */
    private Object address;

    /** [[Source]] */
    private String source;

    /** [[Kind]] */
    private Kind kind;

    /** [[Body]] */
    private ModuleBody body;

    /** [[Execute]] */
    private Callable execute; // sometimes referred to as [[Factory]]

    /** [[Dependencies]] */
    private List<Dependency> dependencies;

    /** [[Exception]] */
    private Object exception;

    /** [[Module]] */
    private ModuleObject module;

    private Load(String name, Object metadata) {
        this.status = Status.Loading;
        this.name = name;
        this.linkSets = new ArrayList<>();
        this.metadata = metadata;
    }

    /** [[Status]] */
    public Status getStatus() {
        return status;
    }

    /** [[Name]] */
    public String getName() {
        return name;
    }

    /** [[Name]] */
    public Object getNameOrNull() {
        return name != null ? name : NULL;
    }

    /** [[LinkSets]] */
    public List<LinkSet> getLinkSets() {
        return linkSets;
    }

    /** [[LinkSets]] */
    public List<LinkSet> getSortedLinkSets() {
        List<LinkSet> sorted = new ArrayList<>(linkSets);
        Collections.sort(sorted, LinkSet.comparator());
        return sorted;
    }

    /** [[Metadata]] */
    public Object getMetadata() {
        return metadata;
    }

    /** [[Address]] */
    public void setAddress(Object address) {
        assert this.address == null && address != null;
        this.address = address;
    }

    /** [[Kind]] */
    public Kind getKind() {
        return kind;
    }

    /** [[Body]] */
    public ModuleBody getBody() {
        return body;
    }

    /** [[Execute]] */
    public Callable getExecute() {
        return execute;
    }

    /** [[Dependencies]] */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /** [[Module]] */
    public ModuleObject getModule() {
        return module;
    }

    /**
     * Links this {@link Load} record to {@code module}
     */
    void link(ModuleObject module) {
        assert status != Status.Linked;
        this.status = Status.Linked;
        this.module = module;
    }

    /**
     * 1.1.1.1 CreateLoad(name) Abstract Operation
     */
    public static Load CreateLoad(ExecutionContext cx, String name) {
        assert name != null : "expected non-anonymous module";
        /* steps 1-13 */
        Load load = new Load(name, ObjectCreate(cx, Intrinsics.ObjectPrototype));
        /* step 14 */
        return load;
    }

    /**
     * 1.1.1.1 CreateLoad(name) Abstract Operation
     */
    public static Load CreateLoad(ExecutionContext cx, String name, Object metadata) {
        assert name != null : "expected non-anonymous module";
        /* steps 1-13 */
        Load load = new Load(name, metadata);
        /* step 14 */
        return load;
    }

    /**
     * CreateLoadFromAddress(address) [not in spec]
     */
    public static Load CreateLoadFromAddress(ExecutionContext cx, Object address) {
        // NB: Only loads created by this methods are anonymous
        Load load = new Load(null, ObjectCreate(cx, Intrinsics.ObjectPrototype));
        load.address = address;
        return load;
    }

    /**
     * 1.1.1.2 LoadFailed Functions
     */
    public static final class LoadFailed extends BuiltinFunction {
        /** [[Load]] */
        private final Load load;

        public LoadFailed(Realm realm, Load load) {
            super(realm, ANONYMOUS, 1);
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object exc = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            Load load = this.load;
            /* step 2 */
            assert load.status == Load.Status.Loading;
            /* step 3 */
            load.status = Load.Status.Failed;
            /* step 4 */
            load.exception = exc;
            /* step 5 */
            List<LinkSet> linkSets = load.getSortedLinkSets();
            /* step 6 */
            for (LinkSet linkSet : linkSets) {
                LinkSetFailed(calleeContext, linkSet, exc);
            }
            /* step 7 */
            assert load.linkSets.isEmpty();
            /* step 8 */
            return UNDEFINED;
        }
    }

    /**
     * 1.1.1.3 RequestLoad(loader, request, refererName, refererAddress) Abstract Operation
     */
    public static ScriptObject RequestLoad(ExecutionContext cx, LoaderObject loader,
            String request, Object refererName, Object refererAddress) {
        /* steps 1-5 */
        CallNormalize f = new CallNormalize(cx.getRealm(), loader, request, refererName,
                refererAddress);
        /* step 6 */
        ScriptObject p = PromiseCreate(cx, f);
        /* steps 7-8 */
        GetOrCreateLoad g = new GetOrCreateLoad(cx.getRealm(), loader);
        /* step 9 */
        p = PromiseThen(cx, p, g);
        /* step 10 */
        return p;
    }

    /**
     * 1.1.1.4 CallNormalize Functions
     */
    public static final class CallNormalize extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Request]] */
        private final String request;
        /** [[RefererName]] */
        private final Object refererName;
        /** [[RefererAddress]] */
        private final Object refererAddress;

        public CallNormalize(Realm realm, LoaderObject loader, String request, Object refererName,
                Object refererAddress) {
            super(realm, ANONYMOUS, 2);
            this.loader = loader;
            this.request = request;
            this.refererName = refererName;
            this.refererAddress = refererAddress;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = args.length > 0 ? args[0] : UNDEFINED;
            @SuppressWarnings("unused")
            Object reject = args.length > 1 ? args[1] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            String request = this.request;
            /* step 3 */
            Object refererName = this.refererName;
            /* step 4 */
            Object refererAddress = this.refererAddress;
            /* step 5 */
            Object normalizeHook = Get(calleeContext, loader, "normalize");
            if (!IsCallable(normalizeHook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* steps 6-7 */
            Object name = ((Callable) normalizeHook).call(calleeContext, loader, request,
                    refererName, refererAddress);
            /* step 8 */
            assert IsCallable(resolve);
            return ((Callable) resolve).call(calleeContext, UNDEFINED, name);
        }
    }

    /**
     * 1.1.1.5 GetOrCreateLoad Functions
     */
    public static final class GetOrCreateLoad extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;

        public GetOrCreateLoad(Realm realm, LoaderObject loader) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object nameArg = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* steps 2-3 */
            String name = ToFlatString(calleeContext, nameArg);
            /* steps 4-5 */
            if (loader.getModules().containsKey(name)) {
                /* step 4 */
                ModuleObject existingModule = loader.getModules().get(name);
                Load load = CreateLoad(calleeContext, name);
                load.link(existingModule);
                return load;
            } else if (loader.getLoads().containsKey(name)) {
                /* step 5 */
                Load load = loader.getLoads().get(name);
                assert load.status == Load.Status.Loading || load.status == Load.Status.Loaded;
                return load;
            }
            /* step 6 */
            Load load = CreateLoad(calleeContext, name);
            /* step 7 */
            loader.getLoads().put(name, load);
            /* step 8 */
            ProceedToLocate(calleeContext, loader, load);
            /* step 9 */
            return load;
        }
    }

    /**
     * 1.1.1.6 ProceedToLocate(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToLocate(ExecutionContext cx, LoaderObject loader, Load load) {
        /* step 1 */
        ScriptObject p = PromiseResolve(cx, UNDEFINED);
        /* steps 2-4 */
        CallLocate f = new CallLocate(cx.getRealm(), loader, load);
        /* step 5 */
        p = PromiseThen(cx, p, f);
        /* step 6 */
        return ProceedToFetch(cx, loader, load, p);
    }

    /**
     * 1.1.1.7 ProceedToFetch(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToFetch(ExecutionContext cx, LoaderObject loader, Load load,
            ScriptObject p) {
        /* steps 1-4 */
        CallFetch f = new CallFetch(cx.getRealm(), loader, load);
        /* step 5 */
        p = PromiseThen(cx, p, f);
        /* step 6 */
        return ProceedToTranslate(cx, loader, load, p);
    }

    /**
     * 1.1.1.8 ProceedToTranslate(loader, load, p) Abstract Operation
     */
    public static ScriptObject ProceedToTranslate(ExecutionContext cx, LoaderObject loader,
            Load load, ScriptObject p) {
        /* step 1-3 */
        CallTranslate f1 = new CallTranslate(cx.getRealm(), loader, load);
        /* step 4 */
        p = PromiseThen(cx, p, f1);
        /* steps 5-7 */
        CallInstantiate f2 = new CallInstantiate(cx.getRealm(), loader, load);
        /* step 8 */
        p = PromiseThen(cx, p, f2);
        /* steps 9-11 */
        InstantiateSucceeded f3 = new InstantiateSucceeded(cx.getRealm(), loader, load);
        /* step 12 */
        p = PromiseThen(cx, p, f3);
        /* steps 13-14 */
        LoadFailed f4 = new LoadFailed(cx.getRealm(), load);
        /* step 15 */
        return PromiseCatch(cx, p, f4);
    }

    /**
     * 1.1.1.9 SimpleDefine(obj, name, value) Abstract Operation
     */
    public static boolean SimpleDefine(ExecutionContext cx, ScriptObject obj, String name,
            Object value) {
        return CreateDataProperty(cx, obj, name, value);
    }

    /**
     * 1.1.1.10 CallLocate Functions
     */
    public static final class CallLocate extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        private final Load load;

        public CallLocate(Realm realm, LoaderObject loader, Load load) {
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
            Load load = this.load;
            /* step 3-4 */
            Object hook = Get(calleeContext, loader, "locate");
            /* step 5 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 6 */
            ScriptObject obj = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
            /* step 7 */
            SimpleDefine(calleeContext, obj, "name", load.getNameOrNull());
            /* step 8 */
            SimpleDefine(calleeContext, obj, "metadata", load.metadata);
            /* step 9 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loader, obj);
        }
    }

    /**
     * 1.1.1.11 CallFetch Functions
     */
    public static final class CallFetch extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        private final Load load;

        public CallFetch(Realm realm, LoaderObject loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object address = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.linkSets.isEmpty()) {
                return UNDEFINED;
            }
            /* step 4 */
            load.address = address;
            /* steps 5-6 */
            Object hook = Get(calleeContext, loader, "fetch");
            /* step 7 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 8 */
            ScriptObject obj = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
            /* step 9 */
            SimpleDefine(calleeContext, obj, "name", load.getNameOrNull());
            /* step 10 */
            SimpleDefine(calleeContext, obj, "metadata", load.metadata);
            /* step 11 */
            SimpleDefine(calleeContext, obj, "address", load.address);
            /* step 12 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loader, obj);
        }
    }

    /**
     * 1.1.1.12 CallTranslate Functions
     */
    public static final class CallTranslate extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        private final Load load;

        public CallTranslate(Realm realm, LoaderObject loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object source = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.linkSets.isEmpty()) {
                return UNDEFINED;
            }
            /* steps 4-5 */
            Object hook = Get(calleeContext, loader, "translate");
            /* step 6 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 7 */
            ScriptObject obj = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
            /* step 8 */
            SimpleDefine(calleeContext, obj, "name", load.getNameOrNull());
            /* step 9 */
            SimpleDefine(calleeContext, obj, "metadata", load.metadata);
            /* step 10 */
            SimpleDefine(calleeContext, obj, "address", load.address);
            /* step 11 */
            SimpleDefine(calleeContext, obj, "source", source);
            /* step 12 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loader, obj);
        }
    }

    /**
     * 1.1.1.13 CallInstantiate Functions
     */
    public static final class CallInstantiate extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        private final Load load;

        public CallInstantiate(Realm realm, LoaderObject loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object source = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.linkSets.isEmpty()) {
                return UNDEFINED;
            }
            /* step 4 */
            load.source = ToFlatString(calleeContext, source); // FIXME: add ToString() ?
            /* steps 5-6 */
            Object hook = Get(calleeContext, loader, "instantiate");
            /* step 7 */
            if (!IsCallable(hook)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            /* step 8 */
            ScriptObject obj = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
            /* step 9 */
            SimpleDefine(calleeContext, obj, "name", load.getNameOrNull());
            /* step 10 */
            SimpleDefine(calleeContext, obj, "metadata", load.metadata);
            /* step 11 */
            SimpleDefine(calleeContext, obj, "address", load.address);
            /* step 12 */
            SimpleDefine(calleeContext, obj, "source", source);
            /* step 13 */
            // FIXME: spec bug - missing 'loader as thisArgument'
            return ((Callable) hook).call(calleeContext, loader, obj);
        }
    }

    /**
     * 1.1.1.14 InstantiateSucceeded Functions
     */
    public static final class InstantiateSucceeded extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[Load]] */
        private final Load load;

        public InstantiateSucceeded(Realm realm, LoaderObject loader, Load load) {
            super(realm, ANONYMOUS, 1);
            this.loader = loader;
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object instantiateResult = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            Load load = this.load;
            /* step 3 */
            if (load.linkSets.isEmpty()) {
                return UNDEFINED;
            }
            /* steps 4-6 */
            Iterable<String> depsList;
            if (Type.isUndefined(instantiateResult)) {
                /* step 4 */
                Parser parser = new Parser("", 1, calleeContext.getRealm().getOptions());
                Module parsedModule;
                try {
                    parsedModule = parser.parseModule(load.source);
                } catch (ParserException e) {
                    throw e.toScriptException(calleeContext);
                }
                // TODO: parsedModule as ModuleBody
                ModuleBody body = null;
                load.body = body;
                load.kind = Load.Kind.Declarative;
                depsList = parsedModule.getScope().getModuleRequests();
            } else if (Type.isObject(instantiateResult)) {
                /* step 5 */
                ScriptObject instantiateResultObject = Type.objectValue(instantiateResult);
                Object deps = Get(calleeContext, instantiateResultObject, "deps");
                if (Type.isUndefined(deps)) {
                    depsList = new ArrayList<>();
                } else {
                    // TODO: convert ToString() here?
                    depsList = IterableToArray(calleeContext, deps);
                }
                Object execute = Get(calleeContext, instantiateResultObject, "execute");
                // TODO: assert callable here?
                if (!IsCallable(execute)) {
                    throw newTypeError(calleeContext, Messages.Key.NotCallable);
                }
                load.execute = (Callable) execute;
                load.kind = Load.Kind.Dynamic;
            } else {
                /* step 6 */
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            return ProcessLoadDependencies(calleeContext, load, loader, depsList);
        }
    }

    /**
     * 1.1.1.15 ProcessLoadDependencies(load, loader, depsList) Abstract Operation
     */
    public static ScriptObject ProcessLoadDependencies(ExecutionContext cx, Load load,
            LoaderObject loader, Iterable<String> depsList) {
        /* step 1 */
        Object refererName = load.getNameOrNull();
        /* step 2 */
        load.dependencies = new ArrayList<>();
        /* step 3 */
        List<ScriptObject> loadPromises = new ArrayList<>();
        /* step 4 */
        for (String request : depsList) {
            /* step 4a */
            ScriptObject p = RequestLoad(cx, loader, request, refererName, load.address);
            /* steps 4b-4d */
            // FIXME: spec bug [[Load]] instead of [[ParentLoad]]
            AddDependencyLoad f = new AddDependencyLoad(cx.getRealm(), load, request);
            /* step 4e */
            p = PromiseThen(cx, p, f);
            /* step 4f */
            loadPromises.add(p);
        }
        /* step 5 */
        ScriptObject p = PromiseAll(cx, loadPromises);
        /* steps 6-7 */
        LoadSucceeded f = new LoadSucceeded(cx.getRealm(), load);
        /* step 8 */
        p = PromiseThen(cx, p, f);
        /* step 9 */
        return p;
    }

    /**
     * 1.1.1.16 AddDependencyLoad Functions
     */
    public static final class AddDependencyLoad extends BuiltinFunction {
        /** [[ParentLoad]] */
        private final Load parentLoad;
        /** [[Request]] */
        private final String request;

        public AddDependencyLoad(Realm realm, Load parentLoad, String request) {
            super(realm, ANONYMOUS, 1);
            this.parentLoad = parentLoad;
            this.request = request;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            @SuppressWarnings("unused")
            ExecutionContext calleeContext = calleeContext();
            Object depLoadArg = args.length > 0 ? args[0] : null;
            assert depLoadArg instanceof Load;
            Load depLoad = (Load) depLoadArg;
            /* step 1 */
            Load parentLoad = this.parentLoad;
            /* step 2 */
            String request = this.request;
            /* step 3 */
            // FIXME: implement
            // assert parentLoad.dependencies;
            /* step 4 */
            // FIXME: [[key]] / [[value]] relation unclear
            assert depLoad.name != null : "unexpected anonymous dependency load";
            parentLoad.dependencies.add(new Dependency(request, depLoad.name));
            /* step 5 */
            if (depLoad.status != Load.Status.Linked) {
                List<LinkSet> linkSets = new ArrayList<>(parentLoad.linkSets);
                for (LinkSet linkSet : linkSets) {
                    AddLoadToLinkSet(linkSet, depLoad);
                }
            }
            return UNDEFINED;
        }
    }

    /**
     * 1.1.1.17 LoadSucceeded Functions
     */
    public static final class LoadSucceeded extends BuiltinFunction {
        /** [[Load]] */
        private final Load load;

        public LoadSucceeded(Realm realm, Load load) {
            super(realm, ANONYMOUS, 0);
            this.load = load;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 */
            Load load = this.load;
            /* step 2 */
            assert load.status == Load.Status.Loading;
            /* step 3 */
            load.status = Load.Status.Loaded;
            /* step 4 */
            List<LinkSet> linkSets = load.getSortedLinkSets();
            /* step 5 */
            for (LinkSet linkSet : linkSets) {
                UpdateLinkSetOnLoad(calleeContext, linkSet, load);
            }
            return UNDEFINED;
        }
    }

    // FIXME: missing definition in spec
    private static List<String> IterableToArray(ExecutionContext cx, Object iterable) {
        Iterator<?> iterator = FromScriptIterator(cx, GetIterator(cx, iterable));
        List<String> array = new ArrayList<>();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            array.add(ToFlatString(cx, value));
        }
        return array;
    }
}

package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.modules.Load.CreateLoad;
import static com.github.anba.es6draft.runtime.modules.Load.ProceedToFetch;
import static com.github.anba.es6draft.runtime.modules.Load.ProceedToLocate;
import static com.github.anba.es6draft.runtime.modules.Load.ProceedToTranslate;
import static com.github.anba.es6draft.runtime.modules.ModuleAbstractOperations.Link;
import static com.github.anba.es6draft.runtime.objects.modules.LoaderConstructor.GetOption;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.GetDeferred;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseCreate;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseResolve;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.modules.Load.Dependency;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;
import com.github.anba.es6draft.runtime.objects.promise.Deferred;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.1 Module Loading</h2>
 * <ul>
 * <li>1.1.2 LinkSet Records
 * </ul>
 */
public final class LinkSet {
    /** [[Loader]] */
    private final LoaderObject loader;

    /** [[Loads]] */
    private final List<Load> loads;

    /** [[Done]] */
    private final ScriptObject done;

    /** [[Resolve]] */
    private final Callable resolve;

    /** [[Reject]] */
    private final Callable reject;

    private static final AtomicLong idGen = new AtomicLong(Long.MIN_VALUE);
    private final long id = idGen.getAndIncrement();

    private LinkSet(LoaderObject loader, Deferred deferred) {
        this.loader = loader;
        this.loads = new ArrayList<>();
        this.done = deferred.getPromise();
        this.resolve = deferred.getResolve();
        this.reject = deferred.getReject();
    }

    private static final class LinkSetComparator implements Comparator<LinkSet> {
        @Override
        public int compare(LinkSet o1, LinkSet o2) {
            return Long.compare(o1.id, o2.id);
        }
    }

    static Comparator<LinkSet> comparator() {
        return new LinkSetComparator();
    }

    /** [[Done]] */
    public ScriptObject getDone() {
        return done;
    }

    /**
     * 1.1.2.1 CreateLinkSet(loader, startingLoad) Abstract Operation
     */
    public static LinkSet CreateLinkSet(ExecutionContext cx, ScriptObject loader, Load startingLoad) {
        /* steps 1-2 */
        if (!(loader instanceof LoaderObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 3-4 */
        Deferred deferred = GetDeferred(cx, cx.getIntrinsic(Intrinsics.Promise));
        /* steps 5-10 */
        LinkSet linkSet = new LinkSet((LoaderObject) loader, deferred);
        /* step 11 */
        AddLoadToLinkSet(linkSet, startingLoad);
        /* step 12 */
        return linkSet;
    }

    /**
     * 1.1.2.2 AddLoadToLinkSet(linkSet, load) Abstract Operation
     */
    public static void AddLoadToLinkSet(LinkSet linkSet, Load load) {
        /* step 1 */
        assert load.getStatus() == Load.Status.Loading || load.getStatus() == Load.Status.Loaded;
        /* step 2 */
        LoaderObject loader = linkSet.loader;
        /* step 3 */
        if (!linkSet.loads.contains(load)) {
            /* step 3a */
            linkSet.loads.add(load);
            /* step 3b */
            load.getLinkSets().add(linkSet);
            /* step 3c */
            if (load.getStatus() == Load.Status.Loaded) {
                for (Dependency dependency : load.getDependencies()) {
                    String name = dependency.getModuleName(); // TODO: or normalised name?
                    if (!loader.getModules().containsKey(name)) {
                        if (loader.getLoads().containsKey(name)) {
                            Load depLoad = loader.getLoads().get(name);
                            AddLoadToLinkSet(linkSet, depLoad);
                        }
                    }
                }
            }
        }
    }

    /**
     * 1.1.2.3 UpdateLinkSetOnLoad(linkSet, load) Abstract Operation
     */
    public static void UpdateLinkSetOnLoad(ExecutionContext cx, LinkSet linkSet, Load load) {
        /* step 1 */
        assert linkSet.loads.contains(load);
        /* step 2 */
        assert load.getStatus() == Load.Status.Loaded || load.getStatus() == Load.Status.Linked;
        /* step 3 */
        for (Load depLoad : linkSet.loads) {
            if (depLoad.getStatus() == Load.Status.Loading) {
                return;
            }
        }
        /* step 4 */
        Load startingLoad = linkSet.loads.get(0);
        /* step 5 */
        try {
            Link(cx, linkSet.loads, linkSet.loader);
        } catch (ScriptException e) {
            /* step 6 */
            LinkSetFailed(cx, linkSet, e.getValue());
            return;
        }
        /* step 7 */
        assert linkSet.loads.isEmpty();
        /* step 8 */
        try {
            linkSet.resolve.call(cx, UNDEFINED, startingLoad);
        } catch (ScriptException e) {
            /* step 9 */
            assert false : "unexpected abrupt completion: " + e;
        }
    }

    /**
     * 1.1.2.4 LinkSetFailed(linkSet, exc) Abstract Operation
     */
    public static void LinkSetFailed(ExecutionContext cx, LinkSet linkSet, Object exc) {
        /* step 1 */
        LoaderObject loader = linkSet.loader;
        /* step 2 */
        List<Load> loads = new ArrayList<>(linkSet.loads);
        /* step 3 */
        for (Load load : loads) {
            /* step 3a */
            assert load.getLinkSets().contains(linkSet);
            /* step 3b */
            load.getLinkSets().remove(linkSet);
            /* step 3c */
            String name = load.getName();
            if (name != null) {
                // load can be anonymous at this point
                if (load.getLinkSets().isEmpty() && loader.getLoads().containsKey(name)) {
                    loader.getLoads().remove(name);
                }
            }
        }
        /* step 4 */
        try {
            linkSet.reject.call(cx, UNDEFINED, exc);
        } catch (ScriptException e) {
            /* step 5 */
            assert false : "unexpected abrupt completion: " + e;
        }
    }

    /**
     * 1.1.2.5 FinishLoad(loader, load) Abstract Operation
     */
    public static void FinishLoad(LoaderObject loader, Load load) {
        /* step 1 */
        String name = load.getName();
        /* step 2 */
        if (name != null) {
            assert !loader.getModules().containsKey(name);
            loader.getModules().put(name, load.getModule());
        }
        /* step 3 */
        if (name != null) {
            // load can be anonymous at this point
            if (loader.getLoads().containsKey(name)) {
                loader.getLoads().remove(name);
            }
        }
        /* step 4 */
        for (LinkSet linkSet : load.getLinkSets()) {
            linkSet.loads.remove(load);
        }
        /* step 5 */
        load.getLinkSets().clear();
    }

    /**
     * 1.1.2.6 LoadModule(loader, name, options) Abstract Operation
     */
    public static ScriptObject LoadModule(ExecutionContext cx, LoaderObject loader, Object name,
            Object options) {
        // FIXME: spec bug - source is undefined
        String source = null;
        /* steps 1-2 */
        String sname = ToFlatString(cx, name);
        /* steps 3-4 */
        Object address = GetOption(cx, options, "address");
        /* steps 8-9 */
        AsyncStartLoadPartwayThrough.Step step;
        if (Type.isUndefined(address)) {
            step = AsyncStartLoadPartwayThrough.Step.Locate;
        } else {
            step = AsyncStartLoadPartwayThrough.Step.Fetch;
        }
        /* step 10 */
        ScriptObject metadata = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* steps 5-13 */
        AsyncStartLoadPartwayThrough f = new AsyncStartLoadPartwayThrough(cx.getRealm(), loader,
                sname, step, metadata, address, source);
        /* step 4 */
        return PromiseCreate(cx, f);
    }

    /**
     * 1.1.3 AsyncStartLoadPartwayThrough Functions
     */
    public static final class AsyncStartLoadPartwayThrough extends BuiltinFunction {
        /** [[Loader]] */
        private final LoaderObject loader;
        /** [[ModuleName]] */
        private final String moduleName;
        /** [[Step]] */
        private final Step step;
        /** [[ModuleMetadata]] */
        private final Object moduleMetadata;
        /** [[ModuleAddress]] */
        private final Object moduleAddress;
        /** [[ModuleSource]] */
        private final Object moduleSource;

        public enum Step {
            Locate, Fetch, Translate
        }

        public AsyncStartLoadPartwayThrough(Realm realm, LoaderObject loader, String moduleName,
                Step step, Object moduleMetadata, Object moduleAddress, Object moduleSource) {
            super(realm, ANONYMOUS, 2);
            assert moduleName != null : "anonymous module in async-start-load";
            this.loader = loader;
            this.moduleName = moduleName;
            this.step = step;
            this.moduleMetadata = moduleMetadata;
            this.moduleAddress = moduleAddress;
            this.moduleSource = moduleSource;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolveArg = args.length > 0 ? args[0] : UNDEFINED;
            Object rejectArg = args.length > 1 ? args[1] : UNDEFINED;
            assert IsCallable(resolveArg) && IsCallable(rejectArg);
            Callable resolve = (Callable) resolveArg;
            @SuppressWarnings("unused")
            Callable reject = (Callable) rejectArg;
            /* step 1 */
            LoaderObject loader = this.loader;
            /* step 2 */
            String name = this.moduleName;
            /* step 3 */
            Step step = this.step;
            /* step 4 */
            Object metadata = this.moduleMetadata;
            /* step 5 */
            Object address = this.moduleAddress;
            /* step 6 */
            Object source = this.moduleSource;
            /* step 7 */
            if (loader.getModules().containsKey(name)) {
                throw throwTypeError(calleeContext, Messages.Key.InternalError);
            }
            /* step 8 */
            if (loader.getLoads().containsKey(name)) {
                throw throwTypeError(calleeContext, Messages.Key.InternalError);
            }
            /* steps 9-10 */
            Load load = CreateLoad(calleeContext, name, metadata);
            /* step 11 */
            LinkSet linkSet = CreateLinkSet(calleeContext, loader, load);
            /* step 12 */
            loader.getLoads().put(name, load);
            /* step 13 */
            resolve.call(calleeContext, NULL, linkSet.done);
            /* step 14 */
            if (step == Step.Locate) {
                return ProceedToLocate(calleeContext, loader, load);
            } else if (step == Step.Fetch) {
                // FIXME: spec bug - PromiseOf() -> PromiseResolve()
                ScriptObject addressPromise = PromiseResolve(calleeContext, address);
                return ProceedToFetch(calleeContext, loader, load, addressPromise);
            } else {
                assert step == Step.Translate;
                load.setAddress(address);
                // FIXME: spec bug - PromiseOf() -> PromiseResolve()
                ScriptObject sourcePromise = PromiseResolve(calleeContext, source);
                return ProceedToTranslate(calleeContext, loader, load, sourcePromise);
            }
        }
    }
}

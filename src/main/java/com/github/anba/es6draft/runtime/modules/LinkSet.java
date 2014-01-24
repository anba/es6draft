/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.modules.ModuleLinking.Link;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.modules.Load.Dependency;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.5 Runtime Semantics: Module Linking</h3>
 * <ul>
 * <li>15.2.5.2 LinkSet Records
 * </ul>
 */
public final class LinkSet {
    /** [[Loader]] */
    private final Loader loader;

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

    private LinkSet(Loader loader, PromiseCapability promiseCapability) {
        this.loader = loader;
        this.loads = new ArrayList<>();
        this.done = promiseCapability.getPromise();
        this.resolve = promiseCapability.getResolve();
        this.reject = promiseCapability.getReject();
    }

    /** [[Done]] */
    public ScriptObject getDone() {
        return done;
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

    /**
     * 15.2.5.2.1 CreateLinkSet(loader, startingLoad) Abstract Operation
     */
    public static LinkSet CreateLinkSet(ExecutionContext cx, Loader loader, Load startingLoad) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        PromiseCapability promiseCapability = PromiseBuiltinCapability(cx);
        /* steps 5-10 */
        LinkSet linkSet = new LinkSet(loader, promiseCapability);
        /* step 6 */
        AddLoadToLinkSet(linkSet, startingLoad);
        /* step 7 */
        return linkSet;
    }

    /**
     * 15.2.5.2.2 AddLoadToLinkSet(linkSet, load) Abstract Operation
     */
    public static void AddLoadToLinkSet(LinkSet linkSet, Load load) {
        /* step 1 */
        assert load.getStatus() == Load.Status.Loading || load.getStatus() == Load.Status.Loaded;
        /* step 2 */
        Loader loader = linkSet.loader;
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
     * 15.2.5.2.3 UpdateLinkSetOnLoad(linkSet, load) Abstract Operation
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
        /* step 4 (Assert ?) */
        /* step 5 */
        Load startingLoad = linkSet.loads.get(0);
        /* step 6 */
        try {
            Link(cx, linkSet.loads, linkSet.loader);
        } catch (ScriptException e) {
            /* step 7 */
            LinkSetFailed(cx, linkSet, e.getValue());
            return;
        }
        /* step 8 */
        assert linkSet.loads.isEmpty();
        /* step 9 */
        try {
            linkSet.resolve.call(cx, UNDEFINED, startingLoad);
        } catch (ScriptException e) {
            /* step 10 */
            assert false : "unexpected abrupt completion: " + e;
        }
    }

    /**
     * 15.2.5.2.4 LinkSetFailed(linkSet, exc) Abstract Operation
     */
    public static void LinkSetFailed(ExecutionContext cx, LinkSet linkSet, Object exc) {
        /* step 1 */
        Loader loader = linkSet.loader;
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
     * 15.2.5.2.5 FinishLoad(loader, load) Abstract Operation
     */
    public static void FinishLoad(Loader loader, Load load) {
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
}

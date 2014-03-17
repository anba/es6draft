/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.3 Runtime Semantics: Loader State</h3>
 * <ul>
 * <li>15.2.3.2 Load Records and LoadRequest Objects
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
    private Module body;

    /** [[Execute]] */
    private Callable execute; // sometimes referred to as [[Factory]]

    /** [[Dependencies]] */
    private List<Dependency> dependencies;

    /** [[Exception]] */
    @SuppressWarnings("unused")
    private Object exception;

    /** [[Module]] */
    private ModuleLinkage module;

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
    public Object getAddress() {
        return address;
    }

    /** [[Address]] */
    public void setAddress(Object address) {
        assert this.address == null && address != null;
        this.address = address;
    }

    /** [[Source]] */
    public String getSource() {
        return source;
    }

    /** [[Source]] */
    public void setSource(String source) {
        assert this.source == null && source != null;
        this.source = source;
    }

    /** [[Kind]] */
    public Kind getKind() {
        return kind;
    }

    /** [[Body]] */
    public Module getBody() {
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

    /** [[Dependencies]] */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /** [[Module]] */
    public ModuleLinkage getModule() {
        return module;
    }

    /**
     * Links this {@link Load} record to {@code module}
     */
    void link(ModuleLinkage module) {
        assert status != Status.Linked;
        this.status = Status.Linked;
        this.module = module;
    }

    /**
     * Marks this {@link Load} as loaded
     */
    void loaded() {
        assert status == Status.Loading;
        this.status = Status.Loaded;
    }

    /**
     * Marks this {@link Load} as failed
     */
    void failed(Object exception) {
        assert status == Status.Loading;
        this.status = Status.Failed;
        this.exception = exception;
    }

    /**
     * Changes this Load record to 'declarative'
     */
    void declarative(Module body) {
        assert kind == null;
        this.kind = Kind.Declarative;
        this.body = body;
    }

    /**
     * Changes this Load record to 'dynamic'
     */
    void dynamic(Callable execute) {
        assert kind == null;
        this.kind = Kind.Dynamic;
        this.execute = execute;
    }

    /**
     * Adds a new dependency to this Load record
     */
    void addDependency(String moduleName, String normalisedModuleName) {
        dependencies.add(new Dependency(moduleName, normalisedModuleName));
    }

    /**
     * 15.2.3.2.1 CreateLoad(name) Abstract Operation
     */
    public static Load CreateLoad(ExecutionContext cx, String name) {
        assert name != null : "expected non-anonymous module";
        /* steps 1-13 */
        Load load = new Load(name, ObjectCreate(cx, Intrinsics.ObjectPrototype));
        /* step 14 */
        return load;
    }

    /**
     * 15.2.3.2.1 CreateLoad(name) Abstract Operation
     */
    public static Load CreateLoad(ExecutionContext cx, String name, Object metadata) {
        assert name != null : "expected non-anonymous module";
        /* steps 1-13 */
        Load load = new Load(name, metadata);
        /* step 14 */
        return load;
    }

    /**
     * 15.2.3.2.1 CreateLoad(name) Abstract Operation
     * <p>
     * CreateLoadFromAddress(address) [not in spec]
     */
    public static Load CreateLoadFromAddress(ExecutionContext cx, Object address) {
        // NB: Only loads created by this methods are anonymous
        Load load = new Load(null, ObjectCreate(cx, Intrinsics.ObjectPrototype));
        load.address = address;
        return load;
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     */
    public static ScriptObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata) {
        return CreateLoadRequestObject(cx, name, metadata, null, null);
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     */
    public static ScriptObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata, Object address) {
        return CreateLoadRequestObject(cx, name, metadata, address, null);
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     */
    public static ScriptObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata, Object address, Object source) {
        assert name != null && metadata != null;
        /* steps 1-2 */
        ScriptObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 3 */
        CreateDataProperty(cx, obj, "name", name);
        /* step 4 */
        CreateDataProperty(cx, obj, "metadata", metadata);
        /* step 5 */
        if (address != null) {
            CreateDataProperty(cx, obj, "address", address);
        }
        /* step 6 */
        if (source != null) {
            CreateDataProperty(cx, obj, "source", source);
        }
        /* step 7 */
        return obj;
    }
}

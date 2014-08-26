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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
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
        private final String normalizedModuleName;

        public Dependency(String moduleName, String normalizedModuleName) {
            this.moduleName = moduleName;
            this.normalizedModuleName = normalizedModuleName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getNormalizedModuleName() {
            return normalizedModuleName;
        }
    }

    /** [[Status]] */
    private Status status;

    /** [[Name]] */
    private final String name;

    /** [[LinkSets]] */
    private final ArrayList<LinkSet> linkSets;

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

    /**
     * [[Status]]
     * 
     * @return the load status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * [[Name]]
     * 
     * @return the module name or {@code null} if anonymous
     */
    public String getName() {
        return name;
    }

    /**
     * [[Name]]
     * 
     * @return the module name
     */
    public Object getNameOrNull() {
        return name != null ? name : NULL;
    }

    /**
     * [[LinkSets]]
     * 
     * @return the list of link sets
     */
    public List<LinkSet> getLinkSets() {
        return linkSets;
    }

    /**
     * [[LinkSets]]
     * 
     * @return the sorted list of link sets
     */
    public List<LinkSet> getSortedLinkSets() {
        ArrayList<LinkSet> sorted = new ArrayList<>(linkSets);
        Collections.sort(sorted, LinkSet.comparator());
        return sorted;
    }

    /**
     * [[Metadata]]
     * 
     * @return the user metadata object
     */
    public Object getMetadata() {
        return metadata;
    }

    /**
     * [[Address]]
     * 
     * @return the module address
     */
    public Object getAddress() {
        return address;
    }

    /**
     * [[Address]]
     * 
     * @param address
     *            the new module address
     */
    public void setAddress(Object address) {
        assert this.address == null && address != null;
        this.address = address;
    }

    /**
     * [[Source]]
     * 
     * @return the module source string
     */
    public String getSource() {
        return source;
    }

    /**
     * [[Source]]
     * 
     * @param source
     *            the new module source string
     */
    public void setSource(String source) {
        assert this.source == null && source != null;
        this.source = source;
    }

    /**
     * [[Kind]]
     * 
     * @return the module kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * [[Body]]
     * 
     * @return the parsed module body node
     */
    public Module getBody() {
        return body;
    }

    /**
     * [[Execute]]
     * 
     * @return the execute callback
     */
    public Callable getExecute() {
        return execute;
    }

    /**
     * [[Dependencies]]
     * 
     * @return the list of module dependencies
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * [[Dependencies]]
     * 
     * @param dependencies
     *            the new list of module dependencies
     */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * [[Module]]
     * 
     * @return the module linkage
     */
    public ModuleLinkage getModule() {
        return module;
    }

    /**
     * Links this {@link Load} record to {@code module}.
     * 
     * @param module
     *            new module linkage record
     */
    void link(ModuleLinkage module) {
        assert status != Status.Linked;
        this.status = Status.Linked;
        this.module = module;
    }

    /**
     * Marks this {@link Load} as loaded.
     */
    void loaded() {
        assert status == Status.Loading;
        this.status = Status.Loaded;
    }

    /**
     * Marks this {@link Load} as failed.
     * 
     * @param exception
     *            the module load exception
     */
    void failed(Object exception) {
        assert status == Status.Loading;
        this.status = Status.Failed;
        this.exception = exception;
    }

    /**
     * Changes this Load record to 'declarative'.
     * 
     * @param body
     *            the parsed module body
     */
    void declarative(Module body) {
        assert kind == null;
        this.kind = Kind.Declarative;
        this.body = body;
    }

    /**
     * Changes this Load record to 'dynamic'.
     * 
     * @param execute
     *            the execute callback
     */
    void dynamic(Callable execute) {
        assert kind == null;
        this.kind = Kind.Dynamic;
        this.execute = execute;
    }

    /**
     * Adds a new dependency to this Load record.
     * 
     * @param moduleName
     *            the dependency name
     * @param normalizedModuleName
     *            the normalized dependency name
     */
    void addDependency(String moduleName, String normalizedModuleName) {
        dependencies.add(new Dependency(moduleName, normalizedModuleName));
    }

    /**
     * 15.2.3.2.1 CreateLoad(name) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the module name
     * @return the new load record
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
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the module name
     * @param metadata
     *            the user metadata object
     * @return the new load record
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
     * 
     * @param cx
     *            the execution context
     * @param address
     *            the module address
     * @return the new load record
     */
    public static Load CreateLoadFromAddress(ExecutionContext cx, Object address) {
        // NB: Only loads created by this methods are anonymous
        Load load = new Load(null, ObjectCreate(cx, Intrinsics.ObjectPrototype));
        load.address = address;
        return load;
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the module name
     * @param metadata
     *            the user metadata object
     * @return the new load request script object
     */
    public static OrdinaryObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata) {
        return CreateLoadRequestObject(cx, name, metadata, null, null);
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the module name
     * @param metadata
     *            the user metadata object
     * @param address
     *            the module address
     * @return the new load request script object
     */
    public static OrdinaryObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata, Object address) {
        return CreateLoadRequestObject(cx, name, metadata, address, null);
    }

    /**
     * 15.2.3.2.2 CreateLoadRequestObject(name, metadata, address, source) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the module name
     * @param metadata
     *            the user metadata object
     * @param address
     *            the module address
     * @param source
     *            the module source
     * @return the new load request script object
     */
    public static OrdinaryObject CreateLoadRequestObject(ExecutionContext cx, Object name,
            Object metadata, Object address, Object source) {
        assert name != null && metadata != null;
        /* steps 1-2 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
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

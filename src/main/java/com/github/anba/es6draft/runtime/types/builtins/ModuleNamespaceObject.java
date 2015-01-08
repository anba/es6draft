/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.ResolveExport;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.PropertyMap;
import com.github.anba.es6draft.runtime.modules.ModuleExport;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.6 Module Namespace Exotic Objects
 * </ul>
 */
public final class ModuleNamespaceObject extends OrdinaryObject {
    /** [[Module]] */
    private final ModuleRecord module;

    /** [[Realm]] */
    private final Realm realm;

    /** [[Exports]] */
    private final Set<String> exports;

    private List<String> sortedExports;

    /**
     * Constructs a new Module object.
     * 
     * @param realm
     *            the realm object
     * @param module
     *            the module record
     * @param moduleRealm
     *            the module realm object
     * @param exports
     *            the list of exported bindings
     */
    public ModuleNamespaceObject(Realm realm, ModuleRecord module, Realm moduleRealm,
            Set<String> exports) {
        super(realm);
        this.module = module;
        this.realm = moduleRealm;
        this.exports = exports;
    }

    /**
     * Returns the module record.
     * 
     * @return the module record
     */
    public ModuleRecord getModule() {
        return module;
    }

    /**
     * Returns the list of exported bindings.
     * 
     * @return the list of exported bindings
     */
    public Set<String> getExports() {
        return exports;
    }

    private List<String> getSortedExports() {
        if (sortedExports == null) {
            ArrayList<String> sorted = new ArrayList<>(exports);
            Collections.sort(sorted);
            sortedExports = Collections.unmodifiableList(sorted);
        }
        return sortedExports;
    }

    @Override
    public boolean hasSpecialIndexedProperties() {
        return true;
    }

    /** 9.4.6.1 [[GetPrototypeOf]] ( ) */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        return null;
    }

    /** 9.4.6.2 [[SetPrototypeOf]] (V) */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        return false;
    }

    /** 9.4.6.3 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return false;
    }

    /** 9.4.6.4 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return true;
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        throw newTypeError(cx, Messages.Key.ModulesOwnProperty);
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        throw newTypeError(cx, Messages.Key.ModulesOwnProperty);
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return ordinaryHasOwnProperty(propertyKey);
    }

    /** 9.4.6.5 [[GetOwnProperty]] (P) */
    @Override
    protected Property getProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        throw newTypeError(cx, Messages.Key.ModulesOwnProperty);
    }

    /** 9.4.6.5 [[GetOwnProperty]] (P) */
    @Override
    protected Property getProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        throw newTypeError(cx, Messages.Key.ModulesOwnProperty);
    }

    /** 9.4.6.5 [[GetOwnProperty]] (P) */
    @Override
    protected Property getProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        /* step 2 (not applicable) */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 9.4.6.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    protected boolean defineProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    protected boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    protected boolean defineProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.7 [[HasProperty]] (P) */
    @Override
    protected boolean has(ExecutionContext cx, long propertyKey) {
        return has(cx, ToString(propertyKey));
    }

    /** 9.4.6.7 [[HasProperty]] (P) */
    @Override
    protected boolean has(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-4 */
        return exports.contains(propertyKey);
    }

    /** 9.4.6.7 [[HasProperty]] (P) */
    @Override
    protected boolean has(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        /* steps 2-4 (not applicable) */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /** 9.4.6.8 [[Get]] (P, Receiver) */
    @Override
    protected Object getValue(ExecutionContext cx, long propertyKey, Object receiver) {
        return getValue(cx, ToString(propertyKey), receiver);
    }

    /** 9.4.6.8 [[Get]] (P, Receiver) */
    @Override
    protected Object getValue(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 (not applicable) */
        /* step 3 */
        Set<String> exports = this.exports;
        /* step 4 */
        if (!exports.contains(propertyKey)) {
            return UNDEFINED;
        }
        /* step 5 */
        ModuleRecord m = this.module;
        /* step 6 */
        Realm realm = this.realm;
        /* step 7 */
        Map<String, ModuleRecord> modules = realm.getModules();
        /* step 8 */
        String moduleName = m.getModuleId();
        /* steps 9-10 */
        ModuleExport binding;
        try {
            /* steps 9, 11 */
            binding = ResolveExport(modules, moduleName, propertyKey,
                    new HashMap<String, Set<String>>());
        } catch (ResolutionException e) {
            /* step 10 */
            throw Errors.newReferenceError(cx, Messages.Key.ModulesAmbiguousExport, propertyKey);
        }
        /* step 12 */
        assert binding != null;
        /* step 13 */
        ModuleRecord targetModule = binding.getModule();
        /* step 14 */
        assert targetModule != null;
        /* step 15 */
        ModuleEnvironmentRecord targetEnvRec = targetModule.getEnvironment().getEnvRec();
        /* step 16 */
        return targetEnvRec.getBindingValue(binding.getBindingName(), true);
    }

    /** 9.4.6.8 [[Get]] (P, Receiver) */
    @Override
    protected Object getValue(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        /* steps 3-16 (not applicable) */
        return super.getValue(cx, propertyKey, receiver);
    }

    /** 9.4.6.9 [[Set]] ( P, V, Receiver) */
    @Override
    protected boolean setValue(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.9 [[Set]] ( P, V, Receiver) */
    @Override
    protected boolean setValue(ExecutionContext cx, String propertyKey, Object value,
            Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.9 [[Set]] ( P, V, Receiver) */
    @Override
    protected boolean setValue(ExecutionContext cx, Symbol propertyKey, Object value,
            Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.10 [[Delete]] (P) */
    @Override
    protected boolean deleteProperty(ExecutionContext cx, long propertyKey) {
        return deleteProperty(cx, ToString(propertyKey));
    }

    /** 9.4.6.10 [[Delete]] (P) */
    @Override
    protected boolean deleteProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-4 */
        return !exports.contains(propertyKey);
    }

    /** 9.4.6.10 [[Delete]] (P) */
    @Override
    protected boolean deleteProperty(ExecutionContext cx, Symbol propertyKey) {
        return true;
    }

    /** 9.4.6.11 [[Enumerate]] () */
    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        return getSortedExports();
    }

    @Override
    protected Enumerability isEnumerableOwnProperty(String propertyKey) {
        assert exports.contains(propertyKey) : String.format("'%s' is not an exported binding",
                propertyKey);
        return Enumerability.Enumerable;
    }

    /** 9.4.6.12 [[OwnPropertyKeys]] ( ) */
    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* step 1 */
        @SuppressWarnings("unchecked")
        List<Object> exports = (List<Object>) (List<?>) getSortedExports();
        /* step 2 */
        PropertyMap<Symbol, Property> symbolProperties = symbolProperties();
        /* step 3 */
        if (!symbolProperties.isEmpty()) {
            exports = new ArrayList<>(exports);
            exports.addAll(symbolProperties.keySet());
        }
        /* step 4 */
        return exports;
    }

    /**
     * 9.4.6.13 ModuleNamespaceCreate (module, realm, exports)
     *
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     * @param exports
     *            the exported bindings
     * @return the new module namespace object
     */
    public static ModuleNamespaceObject ModuleNamespaceCreate(ExecutionContext cx,
            ModuleRecord module, Realm realm, Set<String> exports) {
        /* step 1 (not applicable) */
        /* step 2 */
        assert module.getNamespace() == null;
        /* step 3 (not applicable) */
        /* steps 5-9 */
        ModuleNamespaceObject m = new ModuleNamespaceObject(cx.getRealm(), module, realm, exports);
        /* step 10 */
        PropertyMap<Symbol, Property> symbolProperties = m.symbolProperties();
        // 26.3.1 @@toStringTag
        symbolProperties.put(BuiltinSymbol.toStringTag.get(), new Property("Module", false, false,
                true));
        // 26.3.2 [ @@iterator ] ( )
        symbolProperties.put(BuiltinSymbol.iterator.get(), new Property(new ModuleIteratorFunction(
                cx.getRealm()), true, false, true));
        /* step 11 */
        module.setNamespace(m);
        /* step 12 */
        return m;
    }

    /**
     * 26.3.2 [ @@iterator ] ( )
     */
    private static final class ModuleIteratorFunction extends BuiltinFunction {
        public ModuleIteratorFunction(Realm realm) {
            super(realm, "[Symbol.iterator]", 0);
            createDefaultFunctionProperties();
        }

        private ModuleIteratorFunction(Realm realm, Void ignore) {
            super(realm, "[Symbol.iterator]", 0);
        }

        @Override
        public ModuleIteratorFunction clone() {
            return new ModuleIteratorFunction(getRealm(), null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 (not applicable) */
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw Errors.newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            /* step 3 */
            return Type.objectValue(thisValue).enumerate(calleeContext);
        }
    }
}

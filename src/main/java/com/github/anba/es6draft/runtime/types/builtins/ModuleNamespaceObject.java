/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ImmutablePrototypeObject.SetImmutablePrototype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.6 Module Namespace Exotic Objects
 * </ul>
 */
public final class ModuleNamespaceObject extends OrdinaryObject {
    /** [[Module]] */
    private final ModuleRecord module;

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
     * @param exports
     *            the list of exported bindings
     */
    public ModuleNamespaceObject(Realm realm, ModuleRecord module, Set<String> exports) {
        super(realm);
        this.module = module;
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
    public String toString() {
        return String.format("%s, module=%s", super.toString(), module.getSourceCodeId());
    }

    @Override
    public boolean hasSpecialIndexedProperties() {
        return true;
    }

    /** 9.4.6.1 [[SetPrototypeOf]] (V) */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        return SetImmutablePrototype(this, prototype);
    }

    /** 9.4.6.2 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return false;
    }

    /** 9.4.6.3 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return true;
    }

    @Override
    public boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        return hasOwnProperty(cx, ToString(propertyKey));
    }

    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if (!exports.contains(propertyKey)) {
            return false;
        }
        get(cx, propertyKey, this);
        return true;
    }

    @Override
    public boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return ordinaryHasOwnProperty(propertyKey);
    }

    /** 9.4.6.4 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-5 */
        return getOwnProperty(cx, ToString(propertyKey));
    }

    /** 9.4.6.4 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        if (!exports.contains(propertyKey)) {
            return null;
        }
        /* step 4 */
        Object value = get(cx, propertyKey, this);
        /* step 5 */
        return new Property(value, true, true, false);
    }

    /** 9.4.6.4 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        /* steps 2-5 (not applicable) */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 9.4.6.5 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 (not applicable) */
        /* steps 2-9 */
        return defineOwnProperty(cx, ToString(propertyKey), desc);
    }

    /** 9.4.6.5 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property current = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (current == null) {
            return false;
        }
        /* step 4 */
        if (desc.isAccessorDescriptor()) {
            return false;
        }
        /* step 5 */
        if (desc.hasWritable() && !desc.isWritable()) {
            return false;
        }
        /* step 6 */
        if (desc.hasEnumerable() && !desc.isEnumerable()) {
            return false;
        }
        /* step 7 */
        if (desc.hasConfigurable() && desc.isConfigurable()) {
            return false;
        }
        /* step 8 */
        if (desc.hasValue() && !SameValue(desc.getValue(), current.getValue())) {
            return false;
        }
        /* step 9 */
        return true;
    }

    /** 9.4.6.5 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        /* steps 2-9 (not applicable) */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.4.6.6 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, long propertyKey) {
        /* steps 1-4 */
        return hasProperty(cx, ToString(propertyKey));
    }

    /** 9.4.6.6 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-4 */
        return exports.contains(propertyKey);
    }

    /** 9.4.6.6 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        /* steps 2-4 (not applicable) */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /** 9.4.6.7 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        /* steps 1-13 */
        return get(cx, ToString(propertyKey), receiver);
    }

    /** 9.4.6.7 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Set<String> exports = this.exports;
        /* step 4 */
        if (!exports.contains(propertyKey)) {
            return UNDEFINED;
        }
        /* step 5 */
        ModuleRecord m = this.module;
        /* step 6 */
        ResolvedBinding binding;
        try {
            binding = m.resolveExport(propertyKey, new HashMap<>());
        } catch (IOException e) {
            throw Errors.newInternalError(cx, e, Messages.Key.ModulesIOException, e.getMessage());
        } catch (ResolutionException | MalformedNameException | ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }
        /* step 7 */
        assert binding != null && !binding.isAmbiguous();
        /* step 8 */
        ModuleRecord targetModule = binding.getModule();
        /* step 9 */
        assert targetModule != null;
        /* step 10 */
        LexicalEnvironment<?> targetEnv = targetModule.getEnvironment();
        /* step 11 */
        if (targetEnv == null) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, binding.getBindingName());
        }
        /* step ? (Extension: Export From) */
        if (binding.isNameSpaceExport()) {
            try {
                return GetModuleNamespace(cx, targetModule);
            } catch (IOException e) {
                throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
            } catch (MalformedNameException | ResolutionException e) {
                throw e.toScriptException(cx);
            }
        }
        /* step 12 */
        EnvironmentRecord targetEnvRec = targetEnv.getEnvRec();
        /* step 13 */
        return targetEnvRec.getBindingValue(binding.getBindingName(), true);
    }

    /** 9.4.6.7 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* steps 1, 3-13 (not applicable) */
        /* step 2 */
        return ordinaryGet(cx, propertyKey, receiver);
    }

    /** 9.4.6.8 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.8 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.8 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 1 */
        return false;
    }

    /** 9.4.6.9 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        /* steps 1-5 */
        return delete(cx, ToString(propertyKey));
    }

    /** 9.4.6.9 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* steps 3-5 */
        return !exports.contains(propertyKey);
    }

    /** 9.4.6.9 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        /* steps 1, 3-5 (not applicable) */
        /* step 2 */
        return ordinaryDelete(cx, propertyKey);
    }

    /** 9.4.6.10 [[OwnPropertyKeys]] ( ) */
    @Override
    protected void ownPropertyNames(List<? super String> list) {
        list.addAll(getSortedExports());
    }

    /**
     * 9.4.6.11 ModuleNamespaceCreate (module, exports)
     *
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param exports
     *            the exported bindings
     * @return the new module namespace object
     */
    public static ModuleNamespaceObject ModuleNamespaceCreate(ExecutionContext cx, ModuleRecord module,
            Set<String> exports) {
        /* step 1 (not applicable) */
        /* step 2 */
        assert module.getNamespace() == null;
        /* step 3 (not applicable) */
        /* steps 4-8 */
        ModuleNamespaceObject m = new ModuleNamespaceObject(cx.getRealm(), module, exports);
        /* step 9 */
        // 26.3.1 @@toStringTag
        m.infallibleDefineOwnProperty(BuiltinSymbol.toStringTag.get(), new Property("Module", false, false, false));
        // TODO: spec issue - add [[Extensible]] and set to false.
        m.setExtensible(false);
        /* step 10 */
        module.setNamespace(m);
        /* step 11 */
        return m;
    }
}

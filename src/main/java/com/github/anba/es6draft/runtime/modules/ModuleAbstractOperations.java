/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.modules.LinkSet.FinishLoad;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.modules.Load.Dependency;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * <h1>Modules and Module Loaders</h1>
 * <ul>
 * <li>Module Instance Objects
 * <li>Module Linking
 * <li>Module Linking Groups
 * </ul>
 */
public final class ModuleAbstractOperations {
    private ModuleAbstractOperations() {
    }

    public static void CreateImportBinding(EnvironmentRecord envRec, String name,
            ExportBinding binding) {
        // FIXME: not yet specified
    }

    public static ModuleObject LoaderRegistryLookup(LoaderObject loader, String normalizedName) {
        // FIXME: not yet specified
        return loader.getModules().get(normalizedName);
    }

    public static ModuleObject CreateLinkedModuleInstance(ExecutionContext cx) {
        // TODO: not yet specified
        ModuleObject module = new ModuleObject(cx.getRealm(), null, null);
        module.setDependencies(new LinkedHashMap<String, ModuleObject>());
        return module;
    }

    /**
     * <h2>Module Instance Objects</h2>
     * <p>
     * Abstract operation: CreateUnlinkedModuleInstance ( body, boundNames, knownExports,
     * unknownExports, imports )
     */
    public static ModuleObject CreateUnlinkedModuleInstance(ExecutionContext cx, ModuleBody body,
            List<String> boundNames, List<ExportEntry> knownExports,
            List<ExportEntry> unknownExports, List<ImportEntry> imports) {
        /* step 13 */
        Realm realm = cx.getRealm();
        /* step 14 */
        LexicalEnvironment globalEnv = realm.getGlobalEnv();
        /* step 15 */
        LexicalEnvironment env = LexicalEnvironment.newModuleEnvironment(globalEnv);
        /* step 1-12, 16 */
        ModuleObject m = new ModuleObject(cx.getRealm(), body, env);
        /* steps 2-12 */
        // m.body = body;
        // m.boundNames = boundNames;
        // m.knownExportEntries = knownExports;
        // m.unknownExportEntries = unknownExports;
        // m.exportDefinitions = null;
        // m.exports = null;
        // m.dependencies = null;
        // m.unlinkedDependencies = null;
        // m.importEntries = imports;
        // m.importDefinitions = null;
        // m.linkErrors = new ArrayList<>();
        // m.environment = env;
        /* step 17 */
        return m;
    }

    /**
     * <h2>Module Instance Objects</h2>
     * <p>
     * Abstract operation: LookupModuleDependency ( M, requestName )
     */
    public static ModuleObject LookupModuleDependency(ModuleObject module, String requestName) {
        /* step 1 */
        if (requestName == null) {
            return module;
        }
        assert module.getDependencies() != null;
        /* steps 2-3 */
        return module.getDependencies().get(requestName);
    }

    /**
     * <h2>Module Instance Objects</h2>
     * <p>
     * Abstract operation: LookupExport ( M, exportName )
     */
    public static ExportBinding LookupExport(ModuleObject module, String exportName) {
        assert module.getExports() != null;
        /* step 1 */
        if (!module.getExports().containsKey(exportName)) {
            return null;
        }
        /* steps 2-3 */
        return module.getExports().get(exportName);
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: ResolveExportEntries ( M, visited )
     */
    public static List<ExportDefinition> ResolveExportEntries(ExecutionContext cx,
            ModuleObject module, Set<ModuleObject> visited) {
        /* step 1 */
        if (module.getExportDefinitions() != null) {
            return module.getExportDefinitions();
        }
        /* step 2 */
        List<ExportDefinition> defs = new ArrayList<>();
        /* step 3 */
        List<String> boundNames = module.getBoundNames();
        /* step 4 */
        for (ExportEntry entry : module.getKnownExportEntries()) {
            String modReq = entry.moduleRequest;
            ModuleObject otherMod = LookupModuleDependency(module, modReq);
            // FIXME: spec bug - entry.[[Module]] does not exist
            if (entry.moduleRequest == null && entry.localName != null
                    && !boundNames.contains(entry.localName)) {
                ScriptException error = newReferenceError(cx, Messages.Key.ModulesUnresolvedExport,
                        entry.localName);
                module.getLinkErrors().add(error);
            }
            // FIXME: spec bug? - otherwise take these steps?
            else {
                defs.add(new ExportDefinition(otherMod, entry.importName, entry.localName,
                        entry.exportName, true));
            }
        }
        /* step 5 */
        for (ExportEntry entry : module.getUnknownExportEntries()) {
            assert entry.importName == null && entry.localName == null && entry.exportName == null;
            String modReq = entry.moduleRequest;
            ModuleObject otherMod = LookupModuleDependency(module, modReq);
            if (visited.contains(otherMod)) {
                ScriptException error = newSyntaxError(cx, Messages.Key.ModulesCyclicExport);
                module.getLinkErrors().add(error);
            } else {
                visited.add(otherMod);
                List<ExportDefinition> otherDefs = ResolveExportEntries(cx, otherMod, visited);
                for (ExportDefinition def : otherDefs) {
                    defs.add(new ExportDefinition(otherMod, def.exportName, null, def.exportName,
                            false));
                }
            }
        }
        /* step 6 */
        module.setExportDefinitions(defs);
        /* step 7 */
        return defs;
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: ResolveExports ( M )
     */
    public static void ResolveExports(ExecutionContext cx, ModuleObject module) {
        /* step 1 */
        for (ExportDefinition def : module.getExportDefinitions()) {
            ResolveExport(cx, module, def.exportName, new HashMap<ModuleObject, Set<String>>());
        }
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: ResolveExport ( M, exportName, visited )
     */
    public static void ResolveExport(ExecutionContext cx, ModuleObject module, String exportName,
            Map<ModuleObject, Set<String>> visited) {
        /* step 1 */
        Map<String, ExportBinding> exports = module.getExports();
        /* step 2 */
        if (exports.containsKey(exportName)) {
            return /* binding */;
        }
        /* steps 3-4 */
        if (visited.containsKey(module) && visited.get(module).contains(exportName)) {
            ScriptException error = newSyntaxError(cx, Messages.Key.ModulesDuplicateExport,
                    exportName);
            module.getLinkErrors().add(error);
            return /* error */;
        }
        /* step 5 */
        List<ExportDefinition> exportDefinitions = module.getExportDefinitions();
        /* steps 6-8 */
        boolean multipleExplicit = false, multipleImplicit = false;
        ExportDefinition definition = null;
        for (ExportDefinition def : exportDefinitions) {
            if (def.exportName.equals(exportName)) {
                if (definition == null || (!definition.explicit && def.explicit)) {
                    definition = def;
                } else if (def.explicit == definition.explicit) {
                    if (def.explicit) {
                        multipleExplicit = true;
                        break;
                    }
                    multipleImplicit = true;
                }
            }
        }
        /* step 7 */
        if (definition == null) {
            ScriptException error = newReferenceError(cx, Messages.Key.ModulesUnresolvedExport,
                    exportName);
            module.getLinkErrors().add(error);
            return /* error */;
        }
        /* step 8 */
        if (multipleExplicit || (!definition.explicit && multipleImplicit)) {
            ScriptException error = newSyntaxError(cx, Messages.Key.ModulesDuplicateExport,
                    exportName);
            module.getLinkErrors().add(error);
            return /* error */;
        }
        /* step 9 */
        ExportDefinition def = definition;
        /* step 10 */
        if (def.localName != null) {
            ExportBinding binding = new ExportBinding(module, def.localName);
            exports.put(exportName, binding);
            return /* binding */;
        }
        /* step 11 */
        if (!visited.containsKey(module)) {
            visited.put(module, new HashSet<String>());
        }
        visited.get(module).add(exportName);
        /* step 12 */
        ResolveExport(cx, def.module, def.importName, visited);
        /* step 13 */
        return /* binding */;
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: ResolveImportEntries ( M )
     */
    public static List<ImportDefinition> ResolveImportEntries(ModuleObject module) {
        /* step 1 */
        List<ImportEntry> entries = module.getImportEntries();
        /* step 2 */
        List<ImportDefinition> defs = new ArrayList<>();
        /* step 3 */
        for (ImportEntry entry : entries) {
            String modReq = entry.moduleRequest;
            ModuleObject otherMod = LookupModuleDependency(module, modReq);
            defs.add(new ImportDefinition(otherMod, entry.importName, entry.localName));
        }
        /* step 4 */
        return defs;
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: LinkImports ( M )
     */
    public static void LinkImports(ExecutionContext cx, ModuleObject module) {
        /* step 1 */
        EnvironmentRecord envRec = module.getEnvironment().getEnvRec();
        /* step 2 */
        List<ImportDefinition> defs = module.getImportDefinitions();
        /* step 3 */
        for (ImportDefinition def : defs) {
            if (def.importName.equals("module")) {
                /* step 3a */
                envRec.createImmutableBinding(def.localName);
                envRec.initialiseBinding(def.localName, def.module);
            } else {
                /* step 3b */
                // FIXME: spec bug - LookupExport instead of ResolveExport
                ExportBinding binding = LookupExport(def.module, def.importName);
                if (binding == null) {
                    ScriptException error = newReferenceError(cx,
                            Messages.Key.ModulesUnresolvedImport, def.importName);
                    module.getLinkErrors().add(error);
                } else {
                    CreateImportBinding(envRec, def.localName, binding);
                }
            }
        }
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: LinkDeclarativeModules ( loads, loader )
     */
    public static void LinkDeclarativeModules(ExecutionContext cx, List<Load> loads,
            LoaderObject loader) {
        /* step 1 */
        LinkedHashMap<Load, ModuleObject> unlinked = new LinkedHashMap<>();
        /* step 2 */
        Map<String, Load> loadMap = new HashMap<>();
        for (Load load : loads) {
            // TODO: unsafe getName() - maybe null?
            loadMap.put(load.getName(), load);
            if (load.getStatus() != Load.Status.Linked) {
                ModuleBody body = load.getBody();
                List<String> boundNames = body.boundNames();
                List<ExportEntry> knownExports = body.knownExportEntries();
                List<ExportEntry> unknownExports = body.unknownExportEntries();
                List<ImportEntry> imports = body.importEntries();
                ModuleObject module = CreateUnlinkedModuleInstance(cx, body, boundNames,
                        knownExports, unknownExports, imports);
                unlinked.put(load, module);
            }
        }
        /* step 3 */
        for (Entry<Load, ModuleObject> pair : unlinked.entrySet()) {
            LinkedHashMap<String, ModuleObject> resolvedDeps = new LinkedHashMap<>();
            List<Load> unlinkedDeps = new ArrayList<>();
            for (Dependency dep : pair.getKey().getDependencies()) {
                String requestName = dep.getModuleName();
                String normalizedName = dep.getNormalisedModuleName();
                Load load = loadMap.get(normalizedName);
                if (load != null) {
                    if (load.getStatus() == Load.Status.Linked) {
                        resolvedDeps.put(requestName, load.getModule());
                    } else {
                        ModuleObject module = unlinked.get(load);
                        resolvedDeps.put(requestName, module);
                        unlinkedDeps.add(load);
                    }
                } else {
                    ModuleObject module = LoaderRegistryLookup(loader, normalizedName);
                    if (module == null) {
                        ScriptException error = newReferenceError(cx,
                                Messages.Key.ModulesUnresolvedModule, normalizedName);
                        pair.getValue().getLinkErrors().add(error);
                    } else {
                        resolvedDeps.put(requestName, module);
                    }
                }
            }
            pair.getValue().setDependencies(resolvedDeps);
            pair.getValue().setUnlinkedDependencies(unlinkedDeps);
        }
        /* step 4 */
        for (Entry<Load, ModuleObject> pair : unlinked.entrySet()) {
            ModuleObject module = pair.getValue();
            ResolveExportEntries(cx, module, new HashSet<ModuleObject>());
            ResolveExports(cx, module);
        }
        /* step 5 */
        for (Entry<Load, ModuleObject> pair : unlinked.entrySet()) {
            ModuleObject module = pair.getValue();
            ResolveImportEntries(module);
            LinkImports(cx, module);
        }
        /* step 6 */
        for (Entry<Load, ModuleObject> pair : unlinked.entrySet()) {
            ModuleObject module = pair.getValue();
            List<ScriptException> linkErrors = module.getLinkErrors();
            if (!linkErrors.isEmpty()) {
                throw linkErrors.get(0);
            }
        }
        /* step 7 */
        for (Entry<Load, ModuleObject> pair : unlinked.entrySet()) {
            Load load = pair.getKey();
            load.link(pair.getValue());
            FinishLoad(loader, load);
        }
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: LinkDynamicModules ( loads, loader )
     */
    public static void LinkDynamicModules(ExecutionContext cx, List<Load> loads, LoaderObject loader) {
        /* step 1 */
        for (Load load : loads) {
            Callable execute = load.getExecute();
            Object result = execute.call(cx, UNDEFINED);
            if (!(result instanceof ModuleObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ModuleObject module = (ModuleObject) result;
            load.link(module);
            FinishLoad(loader, load);
        }
    }

    private static boolean isModuleLinkSpecComplete() {
        return false;
    }

    /**
     * <h2>Module Linking</h2>
     * <p>
     * Abstract Operation: Link ( start, loader )
     */
    public static void Link(ExecutionContext cx, List<Load> start, LoaderObject loader) {
        // FIXME: module linking spec is incomplete
        if (!isModuleLinkSpecComplete()) {
            for (Load load : start) {
                if (load.getKind() == Load.Kind.Declarative) {
                    throw newInternalError(cx, Messages.Key.InternalError);
                }
            }
            // only dynamic loads
            LinkDynamicModules(cx, start, loader);
            return;
        }

        List<List<Load>> groups = LinkageGroups(start);
        for (List<Load> group : groups) {
            boolean isDeclarative = true;
            for (Load load : group) {
                if (load.getKind() != Load.Kind.Declarative) {
                    isDeclarative = false;
                    break;
                }
            }
            if (isDeclarative) {
                LinkDeclarativeModules(cx, group, loader);
            } else {
                LinkDynamicModules(cx, group, loader);
            }
        }
    }

    /**
     * <h2>Module Linking Groups</h2>
     * <p>
     * Abstract operation: LinkageGroups ( start )
     */
    public static List<List<Load>> LinkageGroups(List<Load> start) {
        // TODO: implement
        /* step 1 */
        /* step 2 */
        /* step 3 */
        /* step 4 */
        int declarativeGroupCount = 0;
        /* step 5 */
        List<List<Load>> declarativeGroups = newList(declarativeGroupCount);
        /* step 6 */
        int dynamicGroupCount = 0;
        /* step 7 */
        List<List<Load>> dynamicGroups = newList(dynamicGroupCount);
        /* step 8 */
        Set<String> visited = new HashSet<>();
        /* step 9 */
        for (Load load : start) {
            BuildLinkageGroups(load, declarativeGroups, dynamicGroups, visited);
        }
        /* step 10 */
        List<List<Load>> groups = new ArrayList<>();
        /* step 11 */
        return groups;
    }

    /**
     * <h2>Module Linking Groups</h2>
     * <p>
     * Abstract operation: BuildLinkageGroups
     */
    public static void BuildLinkageGroups(Load load, Object declarativeGroups,
            Object dynamicGroups, Set<String> visited) {
        // TODO: unsafe getName() - maybe null?
        // TODO: implement
        /* step 1 */
        if (visited.contains(load.getName())) {
            return;
        }
        /* step 2 */
        visited.add(load.getName());
        /* step 3 */
        // FIXME: spec bug: [[UnlinkedDependencies]] -> [[Dependencies]]
    }

    private static <T> List<List<T>> newList(int count) {
        List<List<T>> list = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            list.add(new ArrayList<T>());
        }
        return list;
    }
}

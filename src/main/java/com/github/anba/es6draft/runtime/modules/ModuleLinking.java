/**
 * Copyright (c) 2012-2014 André Bargull
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
import static com.github.anba.es6draft.runtime.modules.ModuleLinkage.CreateModuleLinkageRecord;
import static com.github.anba.es6draft.runtime.modules.ModuleLinkage.LookupExport;
import static com.github.anba.es6draft.runtime.modules.ModuleLinkage.LookupModuleDependency;
import static com.github.anba.es6draft.runtime.modules.ModuleLinkingGroups.LinkageGroups;
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
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.modules.Load.Dependency;
import com.github.anba.es6draft.runtime.objects.modules.ModuleObject;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.5 Runtime Semantics: Module Linking
 * </ul>
 */
public final class ModuleLinking {
    private ModuleLinking() {
    }

    private static boolean isModuleLinkSpecComplete() {
        return false;
    }

    public static void CreateImportBinding(EnvironmentRecord envRec, String name,
            ExportBinding binding) {
        // FIXME: not yet specified
    }

    public static ModuleLinkage LoaderRegistryLookup(Loader loader, String normalizedName) {
        // FIXME: not yet specified
        return loader.getModules().get(normalizedName);
    }

    /**
     * 15.2.5.4 Link ( start, loader )
     */
    public static void Link(ExecutionContext cx, List<Load> start, Loader loader) {
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

        /* step 1 */
        List<List<Load>> groups = LinkageGroups(start);
        /* step 2 */
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
     * 15.2.5.5 LinkDeclarativeModules ( loads, loader )
     */
    public static void LinkDeclarativeModules(ExecutionContext cx, List<Load> loads, Loader loader) {
        /* step 1 */
        LinkedHashMap<Load, ModuleLinkage> unlinked = new LinkedHashMap<>();
        /* step 2 */
        Map<String, Load> loadMap = new HashMap<>();
        for (Load load : loads) {
            // TODO: unsafe getName() - maybe null?
            loadMap.put(load.getName(), load);
            if (load.getStatus() != Load.Status.Linked) {
                ModuleLinkage module = CreateModuleLinkageRecord(loader, load.getBody());
                unlinked.put(load, module);
            }
        }
        /* step 3 */
        for (Entry<Load, ModuleLinkage> pair : unlinked.entrySet()) {
            LinkedHashMap<String, ModuleLinkage> resolvedDeps = new LinkedHashMap<>();
            List<Load> unlinkedDeps = new ArrayList<>();
            for (Dependency dep : pair.getKey().getDependencies()) {
                String requestName = dep.getModuleName();
                String normalizedName = dep.getNormalisedModuleName();
                Load load = loadMap.get(normalizedName);
                if (load != null) {
                    if (load.getStatus() == Load.Status.Linked) {
                        resolvedDeps.put(requestName, load.getModule());
                    } else {
                        ModuleLinkage module = unlinked.get(load);
                        resolvedDeps.put(requestName, module);
                        unlinkedDeps.add(load);
                    }
                } else {
                    ModuleLinkage module = LoaderRegistryLookup(loader, normalizedName);
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
        for (Entry<Load, ModuleLinkage> pair : unlinked.entrySet()) {
            ModuleLinkage module = pair.getValue();
            ResolveExportEntries(cx, module, new HashSet<ModuleLinkage>());
            ResolveExports(cx, module);
        }
        /* step 5 */
        for (Entry<Load, ModuleLinkage> pair : unlinked.entrySet()) {
            ModuleLinkage module = pair.getValue();
            ResolveImportEntries(module);
            LinkImports(cx, module);
        }
        /* step 6 */
        for (Entry<Load, ModuleLinkage> pair : unlinked.entrySet()) {
            ModuleLinkage module = pair.getValue();
            List<ScriptException> linkErrors = module.getLinkErrors();
            if (!linkErrors.isEmpty()) {
                throw linkErrors.get(0);
            }
        }
        /* step 7 */
        for (Entry<Load, ModuleLinkage> pair : unlinked.entrySet()) {
            Load load = pair.getKey();
            load.link(pair.getValue());
            FinishLoad(loader, load);
        }
    }

    /**
     * 15.2.5.5.1 LinkImports ( M )
     */
    public static void LinkImports(ExecutionContext cx, ModuleLinkage module) {
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
     * 15.2.5.6 LinkDynamicModules ( loads, loader )
     */
    public static void LinkDynamicModules(ExecutionContext cx, List<Load> loads, Loader loader) {
        /* step 1 */
        for (Load load : loads) {
            Callable execute = load.getExecute();
            Object result = execute.call(cx, UNDEFINED);
            if (!(result instanceof ModuleObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ModuleObject module = (ModuleObject) result;
            assert module.getModuleLinkage() != null;
            load.link(module.getModuleLinkage());
            FinishLoad(loader, load);
        }
    }

    /**
     * 15.2.5.7 ResolveExportEntries ( M, visited )
     */
    public static List<ExportDefinition> ResolveExportEntries(ExecutionContext cx,
            ModuleLinkage module, Set<ModuleLinkage> visited) {
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
            ModuleLinkage otherMod = LookupModuleDependency(module, modReq);
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
            ModuleLinkage otherMod = LookupModuleDependency(module, modReq);
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
     * 15.2.5.8 ResolveExports ( M )
     */
    public static void ResolveExports(ExecutionContext cx, ModuleLinkage module) {
        /* step 1 */
        for (ExportDefinition def : module.getExportDefinitions()) {
            ResolveExport(cx, module, def.exportName, new HashMap<ModuleLinkage, Set<String>>());
        }
    }

    /**
     * 15.2.5.9 ResolveExport ( M, exportName, visited )
     */
    public static void ResolveExport(ExecutionContext cx, ModuleLinkage module, String exportName,
            Map<ModuleLinkage, Set<String>> visited) {
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
     * 15.2.5.10 ResolveImportEntries ( M )
     */
    public static List<ImportDefinition> ResolveImportEntries(ModuleLinkage module) {
        /* step 1 */
        List<ImportEntry> entries = module.getImportEntries();
        /* step 2 */
        List<ImportDefinition> defs = new ArrayList<>();
        /* step 3 */
        for (ImportEntry entry : entries) {
            String modReq = entry.moduleRequest;
            ModuleLinkage otherMod = LookupModuleDependency(module, modReq);
            defs.add(new ImportDefinition(otherMod, entry.importName, entry.localName));
        }
        /* step 4 */
        return defs;
    }
}

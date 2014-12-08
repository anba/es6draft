/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleDeclarationExecutionContext;
import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newModuleEnvironment;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject.ModuleNamespaceCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.ExportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ImportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ModuleRequests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.ShadowRealm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ModuleLoader;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * <ul>
 * <li>8.6.1 HostNormalizeModuleName
 * <li>8.6.2 HostGetSource
 * <li>15.2.1.15.1 CreateModule
 * <li>15.2.1.15.2 ModuleAt
 * <li>15.2.1.16 ParseModuleAndImports
 * <li>15.2.1.16.1 NormalizeModuleName
 * <li>15.2.1.17 GetExportedNames
 * <li>15.2.1.18 ResolveExport
 * <li>15.2.1.19 ModuleEvaluationJob
 * <li>15.2.1.20 LinkModules
 * <li>15.2.1.21 ModuleDeclarationInstantiation
 * <li>15.2.1.22 ModuleEvaluation
 * </ul>
 */
public final class ModuleSemantics {
    private static final boolean DEBUG = false;

    private ModuleSemantics() {
    }

    private static boolean isHostNormalizedModuleName(String moduleName) {
        // TODO
        return moduleName != null;
    }

    /**
     * 8.6.1 HostNormalizeModuleName ( unnormalizedName) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param parentName
     *            the parent module name
     * @param unnormalizedName
     *            the unnormalized module name
     * @return the normalized module name or {@code null}
     */
    public static String HostNormalizeModuleName(ModuleLoader moduleLoader, String parentName,
            String unnormalizedName) {
        // FIXME: spec bug (https://bugs.ecmascript.org/show_bug.cgi?id=3293)
        assert isHostNormalizedModuleName(parentName);
        return moduleLoader.normalizeName(parentName, unnormalizedName);
    }

    /**
     * 8.6.2 HostGetSource (normalizedName) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param normalizedName
     *            the module name
     * @return the module source code
     * @throws IOException
     *             if there was any I/O error
     */
    public static String HostGetSource(ModuleLoader moduleLoader, String normalizedName)
            throws IOException {
        return moduleLoader.getSource(normalizedName);
    }

    /**
     * HostGetSourceFile (normalizedName) Abstract Operation
     * 
     * @param moduleLoader
     *            the module loader
     * @param normalizedName
     *            the module name
     * @return the module source code file
     */
    public static Path HostGetSourceFile(ModuleLoader moduleLoader, String normalizedName) {
        return moduleLoader.getSourceFile(normalizedName);
    }

    /**
     * 15.2.1.15.1 CreateModule(name) Abstract Operation
     * 
     * @param name
     *            the module name
     * @return a new module record
     */
    public static ModuleRecord CreateModule(String name) {
        /* steps 1-5 */
        return new ModuleRecord(name);
    }

    /**
     * 15.2.1.15.2 ModuleAt( list, name)
     * 
     * @param list
     *            the module list
     * @param name
     *            the module name
     * @return the requested module or {@code null}
     */
    public static ModuleRecord ModuleAt(Map<String, ModuleRecord> list, String name) {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(name);
        /* steps 3-4 */
        return list.get(name);
    }

    /**
     * 15.2.1.16 ParseModuleAndImports ( realm, moduleName. visited )
     * 
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the normalized module name
     * @param visited
     *            the list of previously visited modules
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     */
    public static ModuleRecord ParseModuleAndImports(ShadowRealm realm, String moduleName,
            Map<String, ModuleRecord> visited) throws IOException, MalformedNameException,
            ParserException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleName);
        /* step 3 */
        ModuleRecord m = ModuleAt(visited, moduleName);
        /* step 4 */
        if (m != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in visited%n", moduleName);
            }
            return m;
        }
        /* step 5 */
        Map<String, ModuleRecord> mods = realm.getModules();
        /* step 6 */
        m = ModuleAt(mods, moduleName);
        /* step 7 */
        if (m != null) {
            if (DEBUG) {
                System.out.printf("Module '%s' in realm.[[modules]]%n", moduleName);
            }
            return m;
        }
        /* steps 10-11 */
        String src = HostGetSource(realm.getModuleLoader(), moduleName);
        Path file = HostGetSourceFile(realm.getModuleLoader(), moduleName);
        /* steps 8-9, 12-30 */
        return ParseModuleAndImports(realm, moduleName, src, file, visited);
    }

    /**
     * 15.2.1.16 ParseModuleAndImports ( realm, moduleName. visited )
     * 
     * @param realm
     *            the realm instance
     * @param module
     *            the normalized module name
     * @param src
     *            the module source code
     * @param file
     *            the module source file
     * @param visited
     *            the list of previously visited modules
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     */
    private static ModuleRecord ParseModuleAndImports(ShadowRealm realm, String moduleName,
            String src, Path file, Map<String, ModuleRecord> visited) throws IOException,
            MalformedNameException, ParserException {
        HashMap<String, String> normalizedNames = new HashMap<>();
        /* steps 1-7 (not applicable) */
        /* step 8 */
        ModuleRecord m = CreateModule(moduleName);
        /* step 9 */
        visited.put(moduleName, m);
        /* steps 10-11 (not applicable) */
        /* steps 12-13 */
        Source source = new Source(file, moduleName, 1);
        ScriptLoader scriptLoader = realm.getScriptLoader();
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(source, src);
        /* step 14 (moved) */
        /* step 15 */
        Set<String> requestedModules = ModuleRequests(parsedBody);
        /* step 16 */
        ArrayList<String> importedModules = new ArrayList<>();
        /* step 17 */
        for (String requestedName : requestedModules) {
            String normalizedRequest = NormalizeModuleName(realm, normalizedNames, moduleName,
                    requestedName);
            importedModules.add(normalizedRequest);
            ParseModuleAndImports(realm, normalizedRequest, visited);
        }
        /* step 18 */
        m.setImportedModules(importedModules);
        /* step 19 */
        List<ImportEntry> importEntries = ImportEntries(parsedBody);
        /* step 20 */
        for (ImportEntry importEntry : importEntries) {
            String normalizedRequest = NormalizeModuleName(realm, normalizedNames, moduleName,
                    importEntry.getModuleRequest());
            importEntry.setNormalizedModuleRequest(normalizedRequest);
        }
        /* step 21 */
        m.setImportEntries(importEntries);
        /* step 22 */
        ArrayList<ExportEntry> indirectExportEntries = new ArrayList<>();
        /* step 23 */
        ArrayList<ExportEntry> localExportEntries = new ArrayList<>();
        /* step 24 */
        ArrayList<ExportEntry> starExportEntries = new ArrayList<>();
        /* step 25 */
        List<ExportEntry> exportEntries = ExportEntries(parsedBody);
        /* step 26 */
        for (ExportEntry exportEntry : exportEntries) {
            if (exportEntry.getModuleRequest() == null) {
                localExportEntries.add(exportEntry);
            } else {
                String normalizedReqest = NormalizeModuleName(realm, normalizedNames, moduleName,
                        exportEntry.getModuleRequest());
                exportEntry.setNormalizedModuleRequest(normalizedReqest);
                if (exportEntry.isStarExport()) {
                    starExportEntries.add(exportEntry);
                } else {
                    indirectExportEntries.add(exportEntry);
                }
            }
        }
        /* step 27 */
        m.setLocalExportEntries(localExportEntries);
        /* step 28 */
        m.setIndirectExportEntries(indirectExportEntries);
        /* step 29 */
        m.setStarExportEntries(starExportEntries);
        /* step 14 */
        Module compiledBody = scriptLoader.load(parsedBody, m);
        m.setScriptCode(compiledBody);
        /* step 30 */
        return m;
    }

    /**
     * 15.2.1.16.1 NormalizeModuleName( realm, unnormalizedName )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param parentName
     *            the parent module name
     * @param unnormalizedName
     *            the unnormalized module name
     * @return the normalized module name
     * @throws ScriptException
     *             if the module name cannot be normalized
     */
    public static String NormalizeModuleName(ExecutionContext cx, Realm realm, String parentName,
            String unnormalizedName) throws ScriptException {
        try {
            return NormalizeModuleName(realm, parentName, unnormalizedName);
        } catch (MalformedNameException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * 15.2.1.16.1 NormalizeModuleName( realm, unnormalizedName )
     * 
     * @param realm
     *            the realm instance
     * @param normalizedNames
     *            the map of already normalized names
     * @param parentName
     *            the parent module name
     * @param unnormalizedName
     *            the unnormalized module name
     * @return the normalized module name
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    private static String NormalizeModuleName(ShadowRealm realm,
            Map<String, String> normalizedNames, String parentName, String unnormalizedName)
            throws MalformedNameException {
        String normalized = normalizedNames.get(unnormalizedName);
        if (normalized == null) {
            normalized = NormalizeModuleName(realm, parentName, unnormalizedName);
            normalizedNames.put(unnormalizedName, normalized);
        }
        return normalized;
    }

    /**
     * 15.2.1.16.1 NormalizeModuleName( realm, unnormalizedName )
     * 
     * @param realm
     *            the realm instance
     * @param parentName
     *            the parent module name
     * @param unnormalizedName
     *            the unnormalized module name
     * @return the normalized module name
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    public static String NormalizeModuleName(ShadowRealm realm, String parentName,
            String unnormalizedName) throws MalformedNameException {
        // FIXME: spec bug (https://bugs.ecmascript.org/show_bug.cgi?id=3293)
        /* step 1 */
        // Map<String, String> map = realm.getNameMap();
        /* step 2 */
        // if (map.containsKey(unnormalizedName)) {
        // /* step 2.a */
        // String normalized = map.get(unnormalizedName);
        // /* step 2.a.i */
        // if (normalized == null) {
        // throw new MalformedNameException(unnormalizedName);
        // }
        // /* step 2.a.ii */
        // return normalized;
        // }
        /* step 3 */
        String normalized = HostNormalizeModuleName(realm.getModuleLoader(), parentName,
                unnormalizedName);
        if (DEBUG) {
            System.out.printf("Normalized '%s' -> '%s'%n", unnormalizedName, normalized);
        }
        /* step 4 */
        // map.put(unnormalizedName, normalized);
        /* step 5 */
        if (normalized == null) {
            throw new MalformedNameException(unnormalizedName);
        }
        /* step 6 */
        return normalized;
    }

    /**
     * 15.2.1.17 Static Semantics: GetExportedNames(modules, moduleName, circularitySet)
     * 
     * @param modules
     *            the list of available modules
     * @param moduleName
     *            the normalized module name
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleName}
     */
    public static Set<String> GetExportedNames(Map<String, ModuleRecord> modules,
            String moduleName, Map<String, ModuleRecord> circularitySet) {
        /* step 1 */
        assert modules.containsKey(moduleName);
        /* step 2 */
        if (ModuleAt(circularitySet, moduleName) != null) {
            return Collections.emptySet();
        }
        /* step 3 */
        ModuleRecord m = ModuleAt(modules, moduleName);
        /* step 4 */
        circularitySet.put(moduleName, m);
        /* step 5 */
        HashSet<String> exportedNames = new HashSet<>();
        /* step 6 */
        for (ExportEntry exportEntry : m.getLocalExportEntries()) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 7 */
        for (ExportEntry exportEntry : m.getIndirectExportEntries()) {
            /* step 7.a (not applicable) */
            /* step 7.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 8 */
        for (ExportEntry exportEntry : m.getStarExportEntries()) {
            /* step 8.a */
            HashMap<String, ModuleRecord> circularitySetCopy = new HashMap<>(circularitySet);
            /* step 8.b */
            Set<String> starNames = GetExportedNames(modules,
                    exportEntry.getNormalizedModuleRequest(), circularitySetCopy);
            /* step 8.c */
            for (String n : starNames) {
                if (!exportedNames.contains(n) && !"default".equals(n)) {
                    exportedNames.add(n);
                }
            }
        }
        /* step 9 */
        return exportedNames;
    }

    /**
     * 15.2.1.18 Static Semantics: ResolveExport( modules, moduleName, exportName, circularitySet)
     * 
     * @param modules
     *            the list of available modules
     * @param moduleName
     *            the normalized module name
     * @param exportName
     *            the requested export name
     * @param circularitySet
     *            the list of previously visited modules
     * @return the list of exported names for {@code moduleName}
     * @throws ResolutionException
     *             if the export cannot be resolved
     */
    public static ModuleExport ResolveExport(Map<String, ModuleRecord> modules, String moduleName,
            String exportName, Map<String, Set<String>> circularitySet) throws ResolutionException {
        /* step 1 */
        assert modules.containsKey(moduleName);
        /* step 2 */
        ModuleRecord m = ModuleAt(modules, moduleName);
        /* step 3 */
        Set<String> resolvedExports = circularitySet.get(moduleName);
        if (resolvedExports == null) {
            circularitySet.put(moduleName, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            throw new ResolutionException(Messages.Key.ModulesCyclicExport, exportName);
        }
        /* step 4 */
        resolvedExports.add(exportName);
        /* step 5 */
        for (ExportEntry exportEntry : m.getLocalExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 5.a.i (not applicable) */
                /* step 5.a.ii */
                return new ModuleExport(m, exportEntry.getLocalName());
            }
        }
        /* step 6 */
        for (ExportEntry exportEntry : m.getIndirectExportEntries()) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 6.a.i (not applicable) */
                /* step 6.a.ii */
                return ResolveExport(modules, exportEntry.getNormalizedModuleRequest(),
                        exportEntry.getImportName(), circularitySet);
            }
        }
        /* step 7 */
        if ("default".equals(exportName)) {
            /* step 7.a (not applicable) */
            /* step 7.b */
            throw new ResolutionException(Messages.Key.ModulesMissingDefaultExport, moduleName);
        }
        /* step 8 */
        ModuleExport starResolution = null;
        /* step 9 */
        for (ExportEntry exportEntry : m.getStarExportEntries()) {
            /* step 9.a */
            Map<String, Set<String>> circularitySetCopy = copy(circularitySet);
            /* steps 9.b-9.c */
            ModuleExport resolution = ResolveExport(modules,
                    exportEntry.getNormalizedModuleRequest(), exportName, circularitySetCopy);
            /* step 9.d */
            if (resolution != null) {
                if (starResolution != null) {
                    /* step 9.d.i.1 (not applicable) */
                    /* step 9.d.i.2 */
                    throw new ResolutionException(Messages.Key.ModulesDuplicateStarExport,
                            exportName);
                }
                starResolution = resolution;
            }
        }
        /* step 10 */
        return starResolution;
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @return the module record
     * @throws ScriptException
     *             if the module cannot be loaded
     */
    public static ModuleRecord LoadModule(ExecutionContext cx, Realm realm, String moduleName)
            throws ScriptException {
        try {
            return LoadModule(realm, moduleName);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @return the module record
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static ModuleRecord LoadModule(Realm realm, String moduleName) throws IOException,
            MalformedNameException, ParserException, ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleName);
        /* step 3 (not applicable) */
        /* step 4 */
        Map<String, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, moduleName);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            HashMap<String, ModuleRecord> newModules = new HashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, moduleName, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm.defaultContext(), realm, moduleName, newModules);
        }
        return m;
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm, String moduleName)
            throws ScriptException {
        try {
            ModuleEvaluationJob(realm, moduleName);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleEvaluationJob(Realm realm, String moduleName) throws IOException,
            MalformedNameException, ParserException, ResolutionException {
        /* steps 1-6 */
        ModuleRecord m = LoadModule(realm, moduleName);
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @param sourceCode
     *            the module source code
     * @throws ScriptException
     *             if the module cannot be evaluated
     */
    public static void ModuleEvaluationJob(ExecutionContext cx, Realm realm, String moduleName,
            String sourceCode) throws ScriptException {
        try {
            ModuleEvaluationJob(realm, moduleName, sourceCode);
        } catch (MalformedNameException | ParserException | ResolutionException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * 15.2.1.19 Runtime Semantics: ModuleEvaluationJob ( moduleName )
     * 
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @param sourceCode
     *            the module source code
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleEvaluationJob(Realm realm, String moduleName, String sourceCode)
            throws IOException, MalformedNameException, ParserException, ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleName);
        /* step 3 (not applicable) */
        /* step 4 */
        Map<String, ModuleRecord> modules = realm.getModules();
        /* step 5 */
        ModuleRecord m = ModuleAt(modules, moduleName);
        /* step 6 */
        if (m == null) {
            /* step 6.a */
            HashMap<String, ModuleRecord> newModules = new HashMap<>();
            /* steps 6.b-6.c */
            m = ParseModuleAndImports(realm, moduleName, sourceCode, null, newModules);
            /* steps 6.d-6.e */
            LinkModules(realm.defaultContext(), realm, moduleName, newModules);
        }
        /* steps 7-8 */
        ModuleEvaluation(m, realm);
    }

    /**
     * 15.2.1.20 Runtime Semantics: LinkModules( realm, newModuleSet)
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleName
     *            the module name
     * @param newModuleSet
     *            the list of modules to link
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void LinkModules(ExecutionContext cx, Realm realm, String moduleName,
            Map<String, ModuleRecord> newModuleSet) throws ResolutionException {
        /* step 1 (not applicable) */
        /* step 2 */
        assert isHostNormalizedModuleName(moduleName);
        /* step 3 */
        HashMap<String, ModuleRecord> modules = new HashMap<>(realm.getModules());
        /* step 4 */
        modules.putAll(newModuleSet);
        /* step 5 */
        for (ModuleRecord m : newModuleSet.values()) {
            ModuleDeclarationInstantiation(cx, m, realm, modules);
        }
        /* step 6 (not applicable) */
        /* step 7 */
        realm.getModules().putAll(newModuleSet);
        /* step 8 (return) */
    }

    /**
     * 15.2.1.21 Runtime Semantics: ModuleDeclarationInstantiation( module, realm, moduleSet )
     * 
     * @param cx
     *            the execution context
     * @param module
     *            the module record
     * @param realm
     *            the realm instance
     * @param moduleSet
     *            the list of available modules
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public static void ModuleDeclarationInstantiation(ExecutionContext cx, ModuleRecord module,
            Realm realm, Map<String, ModuleRecord> moduleSet) throws ResolutionException {
        /* steps 1, 3-4, 6 (not applicable) */
        /* step 2 */
        Module code = module.getScriptCode();
        /* step 5 */
        LexicalEnvironment<ModuleEnvironmentRecord> env = newModuleEnvironment(realm.getGlobalEnv());
        /* step 7 */
        module.setEnvironment(env);
        /* steps 8-13 */
        ExecutionContext context = newModuleDeclarationExecutionContext(realm, code);
        code.getModuleBody().moduleDeclarationInstantiation(context, env, realm, moduleSet);
    }

    /**
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param name
     *            the module name
     * @return the module namespace object
     */
    public static ModuleNamespaceObject GetModuleNamespace(ExecutionContext cx, Realm realm,
            String name) {
        return GetModuleNamespace(cx, realm, realm.getModules(), name);
    }

    /**
     * 
     * @param cx
     *            the execution context
     * @param realm
     *            the realm instance
     * @param moduleSet
     *            the list of modules
     * @param name
     *            the module name
     * @return the module namespace object
     */
    public static ModuleNamespaceObject GetModuleNamespace(ExecutionContext cx, Realm realm,
            Map<String, ModuleRecord> moduleSet, String name) {
        ModuleRecord importedModule = ModuleAt(moduleSet, name);
        assert importedModule != null;
        ModuleNamespaceObject namespace = importedModule.getNamespace();
        if (namespace == null) {
            Set<String> exportedNames = GetExportedNames(moduleSet, name,
                    new HashMap<String, ModuleRecord>());
            namespace = ModuleNamespaceCreate(cx, importedModule, realm, exportedNames);
        }
        return namespace;
    }

    /**
     * 15.2.1.22 Runtime Semantics: ModuleEvaluation(module, realm)
     * 
     * @param m
     *            the module record
     * @param realm
     *            the realm instance
     * @return the module evaluation result
     */
    public static Object ModuleEvaluation(ModuleRecord m, Realm realm) {
        /* step 1 */
        if (m.isEvaluated()) {
            return UNDEFINED;
        }
        /* step 2 */
        m.setEvaluated(true);
        /* step 3 */
        for (String name : m.getImportedModules()) {
            assert realm.getModules().containsKey(name);
            ModuleRecord requires = realm.getModules().get(name);
            ModuleEvaluation(requires, realm);
        }
        /* steps 4-9 */
        ExecutionContext moduleContext = newModuleExecutionContext(realm, m);
        /* steps 10-11 */
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(moduleContext);
            /* step 12 */
            Object result = m.getScriptCode().evaluate(moduleContext);
            /* step 15 */
            return result;
        } finally {
            /* steps 13-14 */
            realm.setScriptContext(oldScriptContext);
        }
    }

    private static <K, V> Map<K, Set<V>> copy(Map<K, Set<V>> source) {
        HashMap<K, Set<V>> copy = new HashMap<>(source);
        for (Map.Entry<K, Set<V>> entry : copy.entrySet()) {
            entry.setValue(new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}

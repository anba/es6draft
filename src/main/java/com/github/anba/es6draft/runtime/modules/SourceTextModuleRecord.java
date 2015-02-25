/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleDeclarationExecutionContext;
import static com.github.anba.es6draft.runtime.ExecutionContext.newModuleExecutionContext;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newModuleEnvironment;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.HostResolveImportedModule;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject.ModuleNamespaceCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.ExportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ImportEntries;
import static com.github.anba.es6draft.semantics.StaticSemantics.ModuleRequests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.types.builtins.ModuleNamespaceObject;

/**
 * 15.2.1.16 Source Text Module Records
 */
public final class SourceTextModuleRecord implements ModuleRecord, Cloneable {
    /**
     * [[SourceCodeId]]
     */
    private final SourceIdentifier sourceCodeId;

    /**
     * [[Realm]]
     */
    private Realm realm;

    /**
     * [[Environment]]
     */
    private LexicalEnvironment<ModuleEnvironmentRecord> environment;

    /**
     * [[Namespace]]
     */
    private ModuleNamespaceObject namespace;

    /**
     * [[Evaluated]]
     */
    private boolean evaluated;

    /**
     * [[ECMAScriptCode]]
     */
    private Module scriptCode;

    /**
     * [[RequestedModules]]
     */
    private final Set<String> requestedModules;

    /**
     * [[ImportEntries]]
     */
    private final List<ImportEntry> importEntries;

    /**
     * [[LocalExportEntries]]
     */
    private final List<ExportEntry> localExportEntries;

    /**
     * [[IndirectExportEntries]]
     */
    private final List<ExportEntry> indirectExportEntries;

    /**
     * [[StarExportEntries]]
     */
    private final List<ExportEntry> starExportEntries;

    private boolean instantiated;

    private SourceTextModuleRecord(SourceIdentifier sourceCodeId, Set<String> requestedModules,
            List<ImportEntry> importEntries, List<ExportEntry> localExportEntries,
            List<ExportEntry> indirectExportEntries, List<ExportEntry> starExportEntries) {
        assert sourceCodeId != null;
        this.sourceCodeId = sourceCodeId;
        this.requestedModules = Collections.unmodifiableSet(requestedModules);
        this.importEntries = Collections.unmodifiableList(importEntries);
        this.localExportEntries = Collections.unmodifiableList(localExportEntries);
        this.indirectExportEntries = Collections.unmodifiableList(indirectExportEntries);
        this.starExportEntries = Collections.unmodifiableList(starExportEntries);
    }

    private SourceTextModuleRecord(SourceTextModuleRecord module) {
        this.sourceCodeId = module.sourceCodeId;
        this.scriptCode = module.scriptCode;
        this.requestedModules = module.requestedModules;
        this.importEntries = module.importEntries;
        this.localExportEntries = module.localExportEntries;
        this.indirectExportEntries = module.indirectExportEntries;
        this.starExportEntries = module.starExportEntries;
    }

    @Override
    public SourceTextModuleRecord clone() {
        return new SourceTextModuleRecord(this);
    }

    @Override
    public String toString() {
        return String.format("[Module = %s]", sourceCodeId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return sourceCodeId.hashCode();
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        assert this.realm == null : "module already linked";
        this.realm = Objects.requireNonNull(realm);
    }

    /**
     * [[SourceCodeId]]
     * 
     * @return the module source code identifier
     */
    @Override
    public SourceIdentifier getSourceCodeId() {
        return sourceCodeId;
    }

    /**
     * [[ECMAScriptCode]]
     * 
     * @return the script code for this module
     */
    public Module getScriptCode() {
        return scriptCode;
    }

    /**
     * [[RequestedModules]]
     * 
     * @return the list of requested modules
     */
    public Set<String> getRequestedModules() {
        return requestedModules;
    }

    /**
     * [[ImportEntries]]
     * 
     * @return the list of {@code import} entries
     */
    public List<ImportEntry> getImportEntries() {
        return importEntries;
    }

    /**
     * [[LocalExportEntries]]
     * 
     * @return the list of local {@code export} entries
     */
    public List<ExportEntry> getLocalExportEntries() {
        return localExportEntries;
    }

    /**
     * [[IndirectExportEntries]]
     * 
     * @return the list of indirect {@code export} entries
     */
    public List<ExportEntry> getIndirectExportEntries() {
        return indirectExportEntries;
    }

    /**
     * [[StarExportEntries]]
     * 
     * @return the list of {@code export*} entries
     */
    public List<ExportEntry> getStarExportEntries() {
        return starExportEntries;
    }

    /**
     * [[Environment]]
     * 
     * @return the lexical environment of this module or {@code null} if not instantiated
     */
    @Override
    public LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment() {
        return environment;
    }

    /**
     * [[Namespace]]
     * 
     * @return the module namespace object
     */
    @Override
    public ModuleNamespaceObject getNamespace() {
        return namespace;
    }

    @Override
    public ModuleNamespaceObject createNamespace(ExecutionContext cx, Set<String> exports) {
        assert this.namespace == null;
        ModuleNamespaceObject namespace = ModuleNamespaceCreate(cx, this, exports);
        this.namespace = namespace;
        return namespace;
    }

    /**
     * [[Evaluated]]
     * 
     * @return the evaluated flag
     */
    @Override
    public boolean isEvaluated() {
        return evaluated;
    }

    /**
     * Returns {@code true} if the module is instantiated.
     * 
     * @return {@code true} if the module is instantiated
     */
    public boolean isInstantiated() {
        return instantiated;
    }

    /**
     * 15.2.1.16.1 Runtime Semantics: ParseModule ( sourceText )
     * 
     * @param scriptLoader
     *            the script loader
     * @param sourceCodeId
     *            the source code identifier
     * @param source
     *            the module source code
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the module source contains any syntax errors
     * @throws CompilationException
     *             if the parsed module source cannot be compiled
     */
    public static SourceTextModuleRecord ParseModule(ScriptLoader scriptLoader,
            SourceIdentifier sourceCodeId, ModuleSource source) throws IOException,
            ParserException, CompilationException {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(
                source.toSource(), source.sourceCode());
        /* step 4 */
        Set<String> requestedModules = ModuleRequests(parsedBody);
        /* step 5 */
        List<ImportEntry> importEntries = ImportEntries(parsedBody);
        /* step 6 */
        ArrayList<ExportEntry> indirectExportEntries = new ArrayList<>();
        /* step 7 */
        ArrayList<ExportEntry> localExportEntries = new ArrayList<>();
        /* step 8 */
        ArrayList<ExportEntry> starExportEntries = new ArrayList<>();
        /* step 9 */
        List<ExportEntry> exportEntries = ExportEntries(parsedBody);
        /* step 10 */
        for (ExportEntry exportEntry : exportEntries) {
            if (exportEntry.getModuleRequest() == null) {
                localExportEntries.add(exportEntry);
            } else if (exportEntry.isStarExport()) {
                starExportEntries.add(exportEntry);
            } else {
                indirectExportEntries.add(exportEntry);
            }
        }
        /* step 11 */
        SourceTextModuleRecord m = new SourceTextModuleRecord(sourceCodeId, requestedModules,
                importEntries, localExportEntries, indirectExportEntries, starExportEntries);
        m.scriptCode = scriptLoader.load(parsedBody, m);
        return m;
    }

    /**
     * 15.2.1.16.2 GetExportedNames( exportStarSet ) Concrete Method
     */
    @Override
    public Set<String> getExportedNames(Set<ModuleRecord> exportStarSet) throws IOException,
            MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* steps 2-3 */
        if (!exportStarSet.add(module)) {
            return Collections.emptySet();
        }
        /* step 4 */
        HashSet<String> exportedNames = new HashSet<>();
        /* step 5 */
        for (ExportEntry exportEntry : module.localExportEntries) {
            /* step 5.a (not applicable) */
            /* step 5.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 6 */
        for (ExportEntry exportEntry : module.indirectExportEntries) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            exportedNames.add(exportEntry.getExportName());
        }
        /* step 7 */
        for (ExportEntry exportEntry : module.starExportEntries) {
            /* steps 7.a-b */
            ModuleRecord requestedModule = HostResolveImportedModule(module,
                    exportEntry.getModuleRequest());
            /* step 7.c */
            Set<String> starNames = requestedModule.getExportedNames(exportStarSet);
            /* step 7.d */
            for (String n : starNames) {
                if (!"default".equals(n)) {
                    exportedNames.add(n);
                }
            }
        }
        /* step 8 */
        return exportedNames;
    }

    /**
     * 15.2.1.16.3 ResolveExport(.exportName, resolveSet, exportStarSet) Concrete Method
     */
    @Override
    public ModuleExport resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet,
            Set<ModuleRecord> exportStarSet) throws IOException, MalformedNameException,
            ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        Set<String> resolvedExports = resolveSet.get(module);
        if (resolvedExports == null) {
            resolveSet.put(module, resolvedExports = new HashSet<>());
        } else if (resolvedExports.contains(exportName)) {
            return null;
        }
        /* step 3 */
        resolvedExports.add(exportName);
        /* step 4 */
        for (ExportEntry exportEntry : module.localExportEntries) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 4.a.i (not applicable) */
                /* step 4.a.ii */
                return new ModuleExport(module, exportEntry.getLocalName());
            }
        }
        /* step 5 */
        for (ExportEntry exportEntry : module.indirectExportEntries) {
            if (exportName.equals(exportEntry.getExportName())) {
                /* step 5.a.i (not applicable) */
                /* steps 5.a.ii-iii */
                ModuleRecord importedModule = HostResolveImportedModule(module,
                        exportEntry.getModuleRequest());
                /* steps 5.a.iv-v */
                ModuleExport indirectResolution = importedModule.resolveExport(
                        exportEntry.getImportName(), resolveSet, exportStarSet);
                /* step 5.a.vi */
                if (indirectResolution != null) {
                    return indirectResolution;
                }
            }
        }
        /* step 6 */
        if ("default".equals(exportName)) {
            /* step 6.a (not applicable) */
            /* step 6.b */
            throw new ResolutionException(Messages.Key.ModulesMissingDefaultExport,
                    module.sourceCodeId.toString());
        }
        /* steps 7-8 */
        if (!exportStarSet.add(module)) {
            return null;
        }
        /* step 8 */
        ModuleExport starResolution = null;
        /* step 9 */
        for (ExportEntry exportEntry : module.starExportEntries) {
            /* steps 9.a-b */
            ModuleRecord importedModule = HostResolveImportedModule(module,
                    exportEntry.getModuleRequest());
            /* steps 9.c-d */
            ModuleExport resolution = importedModule.resolveExport(exportName, resolveSet,
                    exportStarSet);
            /* step 9.e */
            if (resolution == ModuleExport.AMBIGUOUS) {
                return ModuleExport.AMBIGUOUS;
            }
            /* step 9.f */
            if (resolution != null) {
                if (starResolution == null) {
                    starResolution = resolution;
                } else {
                    if (resolution.getModule() != starResolution.getModule()) {
                        return ModuleExport.AMBIGUOUS;
                    }
                    if (!resolution.getBindingName().equals(starResolution.getBindingName())) {
                        return ModuleExport.AMBIGUOUS;
                    }
                }
            }
        }
        /* step 11 */
        return starResolution;
    }

    /**
     * 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method
     */
    @Override
    public void instantiate() throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        Realm realm = module.getRealm();
        /* step 3 */
        assert realm != null : "module is not linked";
        /* step 4 */
        Module code = module.scriptCode;
        /* step 5 */
        if (module.environment != null) {
            return;
        }
        /* step 6 */
        LexicalEnvironment<ModuleEnvironmentRecord> env = newModuleEnvironment(realm.getGlobalEnv());
        /* step 7 */
        module.environment = env;
        /* step 8 */
        // TODO: Move to generated code...
        for (String required : module.getRequestedModules()) {
            /* step 8.a (note) */
            /* steps 8.b-c */
            ModuleRecord requiredModule = HostResolveImportedModule(module, required);
            /* steps 8.d-e */
            requiredModule.instantiate();
        }
        /* steps 9-17 */
        ExecutionContext context = newModuleDeclarationExecutionContext(realm, code);
        code.getModuleBody().moduleDeclarationInstantiation(context, this, env);
        module.instantiated = true;
    }

    /**
     * 15.2.1.16.5 ModuleEvaluation() Concrete Method
     */
    @Override
    public Object evaluate() throws IOException, MalformedNameException, ResolutionException {
        /* step 1 */
        SourceTextModuleRecord module = this;
        /* step 2 */
        assert module.instantiated;
        /* step 3 */
        Realm realm = module.getRealm();
        assert realm != null : "module is not linked";
        /* step 4 */
        if (module.evaluated) {
            return UNDEFINED;
        }
        /* step 5 */
        module.evaluated = true;
        /* step 6 */
        for (String required : module.requestedModules) {
            ModuleRecord requiredModule = HostResolveImportedModule(module, required);
            requiredModule.evaluate();
        }
        /* steps 7-12 */
        ExecutionContext moduleContext = newModuleExecutionContext(realm, module);
        /* steps 13-14 */
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(moduleContext);
            /* step 15 */
            Object result = module.scriptCode.evaluate(moduleContext);
            /* step 18 */
            return result;
        } finally {
            /* steps 16-17 */
            realm.setScriptContext(oldScriptContext);
        }
    }
}

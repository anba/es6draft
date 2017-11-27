/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.scope.ModuleScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.ResolvedBinding;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Wraps a source text module record to provide an implicit "require" binding.
 */
public final class NodeSourceTextModuleRecord implements ModuleRecord, Cloneable {
    private final SourceTextModuleRecord module;

    public NodeSourceTextModuleRecord(SourceTextModuleRecord module) {
        this.module = module;
    }

    @Override
    public NodeSourceTextModuleRecord clone() {
        return new NodeSourceTextModuleRecord(module.clone());
    }

    public void setRealm(Realm realm) {
        module.setRealm(realm);
    }

    public Set<String> getRequestedModules() {
        return module.getRequestedModules();
    }

    @Override
    public SourceIdentifier getSourceCodeId() {
        return module.getSourceCodeId();
    }

    @Override
    public Realm getRealm() {
        return module.getRealm();
    }

    @Override
    public LexicalEnvironment<ModuleEnvironmentRecord> getEnvironment() {
        return module.getEnvironment();
    }

    @Override
    public ScriptObject getNamespace() {
        return module.getNamespace();
    }

    @Override
    public void setNamespace(ScriptObject namespace) {
        module.setNamespace(namespace);
    }

    @Override
    public ScriptObject getMeta() {
        return module.getMeta();
    }

    @Override
    public void setMeta(ScriptObject meta) {
        module.setMeta(meta);
    }

    @Override
    public Set<String> getExportedNames(Set<ModuleRecord> exportStarSet)
            throws IOException, MalformedNameException, ResolutionException {
        return module.getExportedNames(exportStarSet);
    }

    @Override
    public ResolvedBinding resolveExport(String exportName, Map<ModuleRecord, Set<String>> resolveSet)
            throws IOException, MalformedNameException, ResolutionException {
        return module.resolveExport(exportName, resolveSet);
    }

    @Override
    public void instantiate() throws IOException, MalformedNameException, ResolutionException {
        SourceTextModuleRecord.Status previousState = module.getStatus();
        module.instantiate();
        SourceTextModuleRecord.Status newState = module.getStatus();

        // Add "require" function when module is instantiated.
        if (newState != previousState && newState == SourceTextModuleRecord.Status.Instantiated) {
            ModuleEnvironmentRecord envRec = module.getEnvironment().getEnvRec();
            if (!envRec.hasBinding("require")) {
                envRec.createImmutableBinding("require", true);
                envRec.initializeBinding("require", NodeFunctions.createRequireFunction(this));
            }
        }
    }

    @Override
    public Object evaluate() throws IOException, MalformedNameException, ResolutionException {
        return module.evaluate();
    }

    /**
     * ParseModule ( sourceText )
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
    public static NodeSourceTextModuleRecord ParseModule(ScriptLoader scriptLoader, SourceIdentifier sourceCodeId,
            ModuleSource source) throws IOException, ParserException, CompilationException {
        // Add an implicit "require" binding to the lexical environment of the module.
        com.github.anba.es6draft.ast.Module parsedBody = scriptLoader.parseModule(source.toSource(),
                source.sourceCode());
        ModuleScope moduleScope = parsedBody.getScope();
        if (!moduleScope.isDeclared(new Name("require"))) {
            moduleScope.addImplicitBinding(new Name("require"));
        }
        SourceTextModuleRecord sourceText = SourceTextModuleRecord.ParseModule(scriptLoader, sourceCodeId, parsedBody);
        return new NodeSourceTextModuleRecord(sourceText);
    }
}

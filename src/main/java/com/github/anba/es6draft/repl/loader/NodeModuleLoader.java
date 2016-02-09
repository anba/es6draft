/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.AbstractFileModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public class NodeModuleLoader extends AbstractFileModuleLoader<ModuleRecord> {
    private final HashMap<SourceIdentifier, ModuleRecord> modules = new HashMap<>();
    private final ScriptLoader scriptLoader;
    private Constructor moduleConstructor;

    public NodeModuleLoader(RuntimeContext context, ScriptLoader scriptLoader) {
        super(context);
        this.scriptLoader = scriptLoader;
    }

    public void setModuleConstructor(Constructor moduleConstructor) {
        this.moduleConstructor = moduleConstructor;
    }

    /**
     * Initializes this module loader.
     * 
     * @param realm
     *            the realm instance
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    public void initialize(Realm realm)
            throws IOException, URISyntaxException, MalformedNameException, ResolutionException {
        ModuleRecord module = NativeCode.loadModule(realm, "module.jsm");
        Constructor moduleConstructor = NativeCode.getModuleExport(module, "default", Constructor.class);
        setModuleConstructor(moduleConstructor);
    }

    private ScriptObject createModuleObject(NodeModuleRecord module, Realm realm) {
        Path file = module.getSource().getFile();
        String fileName = Objects.toString(file, "");
        String dirName = file != null ? Objects.toString(file.getParent(), "") : "";
        return Construct(realm.defaultContext(), moduleConstructor, fileName, dirName);
    }

    @Override
    protected void defineModule(ModuleRecord module) {
        SourceIdentifier identifier = module.getSourceCodeId();
        if (modules.containsKey(identifier)) {
            throw new IllegalArgumentException();
        }
        modules.put(identifier, module);
    }

    @Override
    protected ModuleRecord getModule(SourceIdentifier identifier) {
        return modules.get(identifier);
    }

    @Override
    protected ModuleRecord parseModule(SourceIdentifier identifier, ModuleSource source) throws IOException {
        try {
            return NodeModuleRecord.ParseModule(scriptLoader, identifier, source);
        } catch (ParserException e) {
            // ignore
        }
        return NodeSourceTextModuleRecord.ParseModule(scriptLoader, identifier, source);
    }

    @Override
    protected void linkModule(ModuleRecord module, Realm realm) {
        if (module.getRealm() == null) {
            if (module instanceof NodeSourceTextModuleRecord) {
                ((NodeSourceTextModuleRecord) module).setRealm(realm);
            } else if (module instanceof NodeModuleRecord) {
                ScriptObject moduleObject = createModuleObject((NodeModuleRecord) module, realm);
                ((NodeModuleRecord) module).setRealm(realm, moduleObject);
            }
        }
    }

    @Override
    protected Set<String> getRequestedModules(ModuleRecord module) {
        if (module instanceof NodeSourceTextModuleRecord) {
            return ((NodeSourceTextModuleRecord) module).getRequestedModules();
        }
        return Collections.emptySet();
    }

    @Override
    protected Collection<ModuleRecord> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        return NodeModuleResolution.resolve(getBaseDirectory(), super.normalizeName(unnormalizedName, referrerId),
                unnormalizedName, referrerId);
    }
}

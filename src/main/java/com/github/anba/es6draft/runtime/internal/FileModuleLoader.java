/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord.ParseModule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;

/**
 * 
 */
public class FileModuleLoader implements ModuleLoader {
    private final HashMap<SourceIdentifier, SourceTextModuleRecord> modules = new HashMap<>();
    private final ScriptLoader scriptLoader;
    private final URI baseDirectory;

    public FileModuleLoader(ScriptLoader scriptLoader, Path baseDirectory) {
        this.scriptLoader = scriptLoader;
        this.baseDirectory = baseDirectory.toUri();
    }

    public static final class FileSourceIdentifier implements SourceIdentifier {
        private final String file;

        FileSourceIdentifier(String file) {
            this.file = file;
        }

        public FileSourceIdentifier(Path path) {
            this(Paths.get("").toAbsolutePath().toUri().relativize(path.toUri()).toString());
        }

        public Path getPath() throws InvalidPathException {
            return Paths.get(file);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FileSourceIdentifier) {
                return file.equals(((FileSourceIdentifier) obj).file);
            }
            if (obj instanceof SourceIdentifier) {
                return toUri().equals(((SourceIdentifier) obj).toUri());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public String toString() {
            return file;
        }

        @Override
        public URI toUri() {
            return URI.create(file);
        }
    }

    public static final class FileModuleSource implements ModuleSource {
        private final FileSourceIdentifier sourceId;
        private final Path sourceFile;

        public FileModuleSource(FileSourceIdentifier sourceId, Path sourceFile) {
            this.sourceId = sourceId;
            this.sourceFile = sourceFile;
        }

        @Override
        public String sourceCode() throws IOException {
            return new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        }

        @Override
        public Source toSource() {
            return new Source(sourceFile, sourceId.file, 1);
        }
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        URI moduleName = parse(unnormalizedName);
        if (referrerId != null && isRelative(moduleName)) {
            moduleName = referrerId.toUri().resolve(moduleName);
        }
        return new FileSourceIdentifier(moduleName.normalize().getPath());
    }

    private URI parse(String unnormalizedName) throws MalformedNameException {
        URI moduleName;
        try {
            moduleName = new URI(unnormalizedName);
        } catch (URISyntaxException e) {
            throw new MalformedNameException(unnormalizedName);
        }
        if (hasIllegalComponents(moduleName) || hasEmptyPath(moduleName)) {
            throw new MalformedNameException(unnormalizedName);
        }
        if (isPath(moduleName)) {
            // TODO: Treat as ?
            throw new MalformedNameException(unnormalizedName);
        }
        if (isAbsolute(moduleName)) {
            // TODO: Treat as ?
            throw new MalformedNameException(unnormalizedName);
        }
        return moduleName;
    }

    @Override
    public FileModuleSource getSource(SourceIdentifier identifier) {
        if (!(identifier instanceof FileSourceIdentifier)) {
            throw new IllegalArgumentException();
        }
        FileSourceIdentifier sourceId = (FileSourceIdentifier) identifier;
        Path path = Paths.get(baseDirectory.resolve(sourceId.toUri()));
        return new FileModuleSource(sourceId, path);
    }

    @Override
    public SourceTextModuleRecord resolve(SourceIdentifier identifier) throws IOException,
            ParserException, CompilationException {
        SourceTextModuleRecord module = modules.get(identifier);
        if (module == null) {
            FileModuleSource source = getSource(identifier);
            define(identifier, module = ParseModule(scriptLoader, identifier, source));
        }
        return module;
    }

    @Override
    public SourceTextModuleRecord get(SourceIdentifier identifier) {
        return modules.get(Objects.requireNonNull(identifier));
    }

    @Override
    public void define(SourceIdentifier identifier, ModuleRecord module) {
        if (!(module instanceof SourceTextModuleRecord)) {
            throw new IllegalArgumentException();
        }
        if (modules.containsKey(identifier)) {
            throw new IllegalArgumentException();
        }
        modules.put(Objects.requireNonNull(identifier), (SourceTextModuleRecord) module);
    }

    @Override
    public SourceTextModuleRecord define(SourceIdentifier identifier, ModuleSource source)
            throws ParserException, CompilationException, IOException {
        SourceTextModuleRecord module = ParseModule(scriptLoader, identifier, source);
        define(identifier, module);
        return module;
    }

    @Override
    public void fetch(ModuleRecord module) throws IOException, MalformedNameException {
        if (!(module instanceof SourceTextModuleRecord)) {
            throw new IllegalArgumentException();
        }
        fetch((SourceTextModuleRecord) module, new HashSet<SourceTextModuleRecord>());
    }

    private void fetch(SourceTextModuleRecord module, HashSet<SourceTextModuleRecord> visited)
            throws MalformedNameException, IOException {
        if (visited.add(module)) {
            SourceIdentifier referrerId = module.getSourceCodeId();
            for (String specifier : module.getRequestedModules()) {
                FileSourceIdentifier identifier = normalizeName(specifier, referrerId);
                fetch(resolve(identifier), visited);
            }
        }
    }

    @Override
    public boolean link(ModuleRecord module, Realm realm) {
        if (!(module instanceof SourceTextModuleRecord)) {
            throw new IllegalArgumentException();
        }
        SourceTextModuleRecord mod = (SourceTextModuleRecord) module;
        if (mod.getRealm() == null) {
            mod.setRealm(realm);
            return true;
        }
        return false;
    }

    @Override
    public Collection<SourceTextModuleRecord> getModules() {
        return Collections.<SourceTextModuleRecord> unmodifiableCollection(modules.values());
    }

    private static boolean isAbsolute(URI moduleName) {
        return moduleName.getRawPath().startsWith("/");
    }

    private static boolean isPath(URI moduleName) {
        return moduleName.getRawPath().endsWith("/");
    }

    private static boolean isRelative(URI moduleName) {
        return moduleName.getRawPath().startsWith("./")
                || moduleName.getRawPath().startsWith("../");
    }

    private static boolean hasEmptyPath(URI moduleName) {
        return moduleName.getRawPath() == null || moduleName.getRawPath().isEmpty();
    }

    private static boolean hasIllegalComponents(URI moduleName) {
        // All components except for 'path' must be empty.
        if (moduleName.getScheme() != null) {
            return true;
        }
        if (moduleName.getRawAuthority() != null) {
            return true;
        }
        if (moduleName.getRawUserInfo() != null) {
            return true;
        }
        if (moduleName.getHost() != null) {
            return true;
        }
        if (moduleName.getPort() != -1) {
            return true;
        }
        if (moduleName.getRawQuery() != null) {
            return true;
        }
        if (moduleName.getRawFragment() != null) {
            return true;
        }
        return false;
    }
}

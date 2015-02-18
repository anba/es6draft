/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public class FileModuleLoader implements ModuleLoader {
    private final URI baseDirectory;

    public FileModuleLoader(Path baseDirectory) {
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
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId) {
        try {
            URI moduleName = new URI(unnormalizedName);
            if (hasIllegalComponents(moduleName) || hasEmptyPath(moduleName)) {
                return null;
            }
            if (isPath(moduleName)) {
                // TODO: Treat as ?
                return null;
            }
            if (isAbsolute(moduleName)) {
                // TODO: Treat as ?
                return null;
            }
            if (referrerId != null && isRelative(moduleName)) {
                moduleName = referrerId.toUri().resolve(moduleName);
            }
            return new FileSourceIdentifier(moduleName.normalize().getPath());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public FileModuleSource getSource(SourceIdentifier normalizedName)
            throws IllegalArgumentException {
        if (!(normalizedName instanceof FileSourceIdentifier)) {
            throw new IllegalArgumentException();
        }
        FileSourceIdentifier sourceId = (FileSourceIdentifier) normalizedName;
        Path path = Paths.get(baseDirectory.resolve(sourceId.toUri()));
        return new FileModuleSource(sourceId, path);
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

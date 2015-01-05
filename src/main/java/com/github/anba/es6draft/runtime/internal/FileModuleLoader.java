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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 
 */
public class FileModuleLoader extends ModuleLoader {
    private final URI baseDirectory;

    public FileModuleLoader(Path baseDirectory) {
        this.baseDirectory = baseDirectory.toUri();
    }

    @Override
    public String normalizeName(String unnormalizedName, String referrerId) {
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
                moduleName = new URI(referrerId).resolve(moduleName);
            }
            return moduleName.normalize().getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getSource(String normalizedName) throws IOException {
        Path path = getSourceFile(normalizedName);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Override
    public Path getSourceFile(String normalizedName) {
        URI moduleName = URI.create(normalizedName);
        return Paths.get(baseDirectory.resolve(moduleName));
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

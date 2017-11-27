/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.parser.JSONBuilder;
import com.github.anba.es6draft.parser.JSONParser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;

/**
 * Node module file resolution.
 * 
 * @see https://nodejs.org/api/modules.html#modules_all_together
 */
final class NodeModuleResolution {
    private static final String INDEX_FILE_NAME = "index";
    private static final String PACKAGE_FILE_NAME = "package.json";
    private static final String MODULES_DIR_NAME = "node_modules";
    private static final String EXECUTABLE_NAME = "main";
    private static final String[] FILE_EXTENSIONS = { ".js", ".json" };

    private NodeModuleResolution() {
    }

    /**
     * Resolves a module name.
     * 
     * @param baseDirectory
     *            the base directory
     * @param normalizedName
     *            the normalized module identifier
     * @param unnormalizedName
     *            the unnormalized module identifier
     * @param referrerId
     *            the referrer module identifier or {@code null}
     * @return the resolved and normalized module identifier
     * @throws MalformedNameException
     *             if the name cannot be normalized
     */
    static FileSourceIdentifier resolve(Path baseDirectory, FileSourceIdentifier normalizedName,
            String unnormalizedName, SourceIdentifier referrerId) throws MalformedNameException {
        if (referrerId != null) {
            try {
                Path unnormalizedPath = Paths.get(unnormalizedName);
                Path referrer = Paths.get(baseDirectory.toUri().resolve(referrerId.toUri()));

                if (unnormalizedName.startsWith("./") || unnormalizedName.startsWith("../")) {
                    Path path = referrer.resolveSibling(unnormalizedPath);
                    Path file = loadAsFile(path);
                    if (file != null) {
                        return new FileSourceIdentifier(file);
                    }
                    file = loadAsDirectory(path);
                    if (file != null) {
                        return new FileSourceIdentifier(file);
                    }
                }

                Path file = loadNodeModules(unnormalizedPath, referrer, baseDirectory);
                if (file != null) {
                    return new FileSourceIdentifier(file);
                }
            } catch (InvalidPathException e) {
                throw new MalformedNameException(unnormalizedName);
            }
        }

        // If node module resolution failed, use default module name resolution.
        return normalizedName;
    }

    private static Path loadAsFile(Path path) {
        if (Files.isRegularFile(path)) {
            return path;
        }
        for (String ext : FILE_EXTENSIONS) {
            Path pathWithExt = Paths.get(path + ext);
            if (Files.isRegularFile(pathWithExt)) {
                return pathWithExt;
            }
        }
        return null;
    }

    private static Path loadIndex(Path path) {
        for (String ext : FILE_EXTENSIONS) {
            Path indexFile = path.resolve(INDEX_FILE_NAME + ext);
            if (Files.isRegularFile(indexFile)) {
                return indexFile;
            }
        }
        return null;
    }

    private static Path loadAsDirectory(Path path) {
        Path jsonPackage = path.resolve(PACKAGE_FILE_NAME);
        if (Files.isRegularFile(jsonPackage)) {
            Path executable = readPackage(jsonPackage);
            if (executable != null) {
                executable = path.resolve(executable);
                Path file = loadAsFile(executable);
                if (file != null) {
                    return file;
                }
                file = loadIndex(executable);
                if (file != null) {
                    return file;
                }
            }
        }
        return loadIndex(path);
    }

    private static Path loadNodeModules(Path path, Path referrer, Path baseDirectory) {
        Path start = referrer.getParent();
        if (start != null) {
            start = baseDirectory.relativize(baseDirectory.resolve(start));
        } else {
            start = Paths.get("");
        }

        final Path nodeModules = Paths.get(MODULES_DIR_NAME);
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            Path dirName = dir.getFileName();
            if (dirName == null || dirName.equals(nodeModules)) {
                continue;
            }
            Path p = baseDirectory.resolve(dir.resolve(nodeModules).resolve(path));
            Path file = loadAsFile(p);
            if (file != null) {
                return file;
            }
            file = loadAsDirectory(p);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private static Path readPackage(Path path) {
        String executable;
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            executable = JSONParser.parse(json, new ExecJSONBuilder());
        } catch (IOException | ParserException e) {
            // ignore?
            return null;
        }
        if (executable == null || executable.isEmpty()) {
            return null;
        }
        return Paths.get(executable);
    }

    private static final class ExecJSONBuilder implements JSONBuilder<String, Void, Void, String> {
        private int depth;
        private String executablePath;

        @Override
        public String createDocument(String value) {
            return executablePath;
        }

        @Override
        public Void newObject() {
            depth += 1;
            return null;
        }

        @Override
        public String finishObject(Void object) {
            depth -= 1;
            return null;
        }

        @Override
        public void newProperty(Void object, String name, String rawName, long index) {
            // empty
        }

        @Override
        public void finishProperty(Void object, String name, String rawName, long index, String value) {
            if (depth == 1 && EXECUTABLE_NAME.equals(name)) {
                executablePath = value;
            }
        }

        @Override
        public Void newArray() {
            depth += 1;
            return null;
        }

        @Override
        public String finishArray(Void array) {
            depth -= 1;
            return null;
        }

        @Override
        public void newElement(Void array, long index) {
            // empty
        }

        @Override
        public void finishElement(Void array, long index, String value) {
            // empty
        }

        @Override
        public String newNull() {
            return null;
        }

        @Override
        public String newBoolean(boolean value) {
            return null;
        }

        @Override
        public String newNumber(double value, String rawValue) {
            return null;
        }

        @Override
        public String newString(String value, String rawValue) {
            return value;
        }
    }
}

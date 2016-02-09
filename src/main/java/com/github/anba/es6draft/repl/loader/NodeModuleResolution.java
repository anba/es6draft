/**
 * Copyright (c) 2012-2015 André Bargull
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
import java.util.Iterator;

import com.github.anba.es6draft.parser.JSONBuilder;
import com.github.anba.es6draft.parser.JSONParser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;

/**
 * Node module file resolution.
 * 
 * @see https://iojs.org/api/modules.html#modules_all_together
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
    public static FileSourceIdentifier resolve(Path baseDirectory, FileSourceIdentifier normalizedName,
            String unnormalizedName, SourceIdentifier referrerId) throws MalformedNameException {
        try {
            Path normalizedPath = normalizedName.getPath();
            boolean isRelative = unnormalizedName.startsWith("./") || unnormalizedName.startsWith("../");
            if (isRelative) {
                Path file = findModuleFile(baseDirectory, normalizedPath, true);
                if (file != null) {
                    return new FileSourceIdentifier(baseDirectory, file);
                }
            } else if (referrerId != null) {
                for (Path p : new NodeModulePaths(baseDirectory, referrerId)) {
                    Path file = findModuleFile(baseDirectory, p.resolve(normalizedPath), true);
                    if (file != null) {
                        return new FileSourceIdentifier(baseDirectory, file);
                    }
                }
            }
            return normalizedName;
        } catch (InvalidPathException e) {
            throw new MalformedNameException(unnormalizedName);
        }
    }

    private static Path findModuleFile(Path dir, Path path, boolean searchPackage) {
        path = dir.resolve(path);
        if (Files.exists(path)) {
            if (Files.isRegularFile(path)) {
                return path;
            }
            if (Files.isDirectory(path)) {
                if (searchPackage) {
                    Path executable = readPackage(path);
                    if (executable != null) {
                        Path executablePath = findModuleFile(path, executable, false);
                        if (executablePath != null) {
                            return executablePath;
                        }
                    }
                }
                for (String ext : FILE_EXTENSIONS) {
                    Path indexFile = path.resolve(INDEX_FILE_NAME + ext);
                    if (Files.isRegularFile(indexFile)) {
                        return indexFile;
                    }
                }
            }
        } else {
            for (String ext : FILE_EXTENSIONS) {
                Path pathWithExt = Paths.get(path + ext);
                if (Files.isRegularFile(pathWithExt)) {
                    return pathWithExt;
                }
            }
        }
        return null;
    }

    private static Path readPackage(Path path) {
        Path jsonPackage = path.resolve(PACKAGE_FILE_NAME);
        if (!Files.isRegularFile(jsonPackage)) {
            return null;
        }
        String executable;
        try {
            String json = new String(Files.readAllBytes(jsonPackage), StandardCharsets.UTF_8);
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

    private static final class NodeModulePaths implements Iterable<Path> {
        private final Path referrer;

        NodeModulePaths(Path base, SourceIdentifier referrerId) {
            this.referrer = base.relativize(Paths.get(base.toUri().resolve(referrerId.toUri())));
        }

        @Override
        public Iterator<Path> iterator() {
            return new SimpleIterator<Path>() {
                final Path node_modules = Paths.get(MODULES_DIR_NAME);
                Path p = referrer;

                @Override
                protected Path findNext() {
                    if (p != null) {
                        while ((p = p.getParent()) != null) {
                            Path fileName = p.getFileName();
                            if (fileName != null && !fileName.equals(node_modules)) {
                                return p.resolve(node_modules);
                            }
                        }
                        return node_modules;
                    }
                    return null;
                }
            };
        }
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

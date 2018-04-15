/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import com.github.anba.es6draft.util.TestInfo;

/**
 * Parses and returns test case information from test262 js-doc comments.
 *
 * {@link http://wiki.ecmascript.org/doku.php?id=test262:test_case_format}
 *
 */
final class Test262Info extends TestInfo {
    private static final Pattern yamlContentPattern, yamlMultiContentPattern;

    static {
        String singleComments = "(?:\\s*(?://.*)?\\R)*+";
        String multiComments = "(?:\\s*(?:/\\*(?!---|\\*)(?:[^*]|\\*(?!/))*\\*/)?)*+";
        String singleOrMultiComments = "(?:" + singleComments + "|" + multiComments + ")*";
        String yamlDescriptor = "/\\*---((?s:.*?))---\\*/";
        yamlContentPattern = Pattern.compile(singleComments + yamlDescriptor);
        yamlMultiContentPattern = Pattern.compile(singleOrMultiComments + yamlDescriptor);
    }

    enum ErrorPhase {
        Parse, Resolution, Runtime
    }

    private String description, errorType;
    private ErrorPhase errorPhase;
    private List<String> includes = Collections.emptyList();
    private List<String> features = Collections.emptyList();
    private boolean onlyStrict, noStrict, negative, async, module, raw;

    public Test262Info(Path basedir, Path script) {
        super(basedir, script);
    }

    @Override
    public String toString() {
        // return getTestName();
        return super.toString();
    }

    /**
     * Returns the description for the test case.
     */
    public String getDescription() {
        if (description == null) {
            return "<missing description>";
        }
        return description;
    }

    /**
     * Returns the expected error-type if any.
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns the expected error-phase if any.
     */
    public ErrorPhase getErrorPhase() {
        return errorPhase;
    }

    /**
     * Returns the list of required includes.
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * Returns the list of required features.
     */
    public List<String> getFeatures() {
        return features;
    }

    /**
     * Returns whether the test should only be run in strict-mode.
     */
    public boolean isOnlyStrict() {
        return onlyStrict;
    }

    /**
     * Returns whether the test should not be run in strict-mode.
     */
    public boolean isNoStrict() {
        return noStrict;
    }

    /**
     * Returns {@code true} if the test case is expected to fail.
     */
    public boolean isNegative() {
        return negative;
    }

    /**
     * Returns {@code true} for asynchronous test cases.
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Returns {@code true} for module test cases.
     */
    @Override
    public boolean isModule() {
        return module;
    }

    /**
     * Returns {@code true} if the test case should be run without preamble code.
     */
    public boolean isRaw() {
        return raw;
    }

    /**
     * Returns {@code true} if the test configuration supports the requested strict (or sloppy) mode.
     *
     * @param strictTest
     *            {@code true} if strict-mode test
     * @param unmarkedDefault
     *            the default test mode
     * @return {@code true} if the test should be executed
     */
    public boolean hasMode(boolean strictTest, DefaultMode unmarkedDefault) {
        if (module || raw) {
            // Module or raw tests don't need to run with explicit Use Strict directive.
            return !strictTest;
        }
        if (strictTest) {
            return !isNoStrict() && (isOnlyStrict() || unmarkedDefault != DefaultMode.NonStrict);
        } else {
            return !isOnlyStrict() && (isNoStrict() || unmarkedDefault != DefaultMode.Strict);
        }
    }

    /**
     * Returns {@code true} if the test configuration has the requested features.
     *
     * @param includeFeatures
     *            the set of include features, ignored if empty
     * @param excludeFeatures
     *            the set of exclude features, ignored if empty
     * @return {@code true} if the requested features are present
     */
    public boolean hasFeature(Set<String> includeFeatures, Set<String> excludeFeatures) {
        if (!includeFeatures.isEmpty() && Collections.disjoint(includeFeatures, features)) {
            return false;
        }
        if (!excludeFeatures.isEmpty() && !Collections.disjoint(excludeFeatures, features)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("serial")
    static final class MalformedDataException extends Exception {
        MalformedDataException(String message) {
            super(message);
        }

        MalformedDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private enum ErrorHandler {
        Ignore, Throw
    }

    private enum Location {
        Local, External
    }

    /**
     * Parses the test file information for this test case.
     *
     * @return the file content
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedDataException
     *             if the test file information cannot be parsed
     */
    public String readFile() throws IOException, MalformedDataException {
        return readFile(ErrorHandler.Ignore, Location.External);
    }

    /**
     * Parses the test file information for this test case.
     *
     * @return the file content
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedDataException
     *             if the test file information cannot be parsed
     */
    public String readLocalFile() throws IOException, MalformedDataException {
        return readFile(ErrorHandler.Ignore, Location.Local);
    }

    /**
     * Parses the test file information for this test case.
     *
     * 
     * @return the file content
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedDataException
     *             if the test file information cannot be parsed
     */
    public String readFileStrict() throws IOException, MalformedDataException {
        return readFile(ErrorHandler.Throw, Location.External);
    }

    private String readFile(ErrorHandler error, Location location) throws IOException, MalformedDataException {
        String fileContent = new String(Files.readAllBytes(toFile()), StandardCharsets.UTF_8);

        Matcher m;
        if (location == Location.Local && (m = yamlMultiContentPattern.matcher(fileContent)).lookingAt()) {
            readYaml(m.group(1), error);
        } else if ((m = yamlContentPattern.matcher(fileContent)).lookingAt()) {
            readYaml(m.group(1), error);
        } else {
            throw new MalformedDataException("Invalid test file: " + this);
        }

        boolean containsDone = fileContent.contains("$DONE");
        if (error != ErrorHandler.Ignore) {
            if (this.async && !containsDone && errorPhase != ErrorPhase.Parse) {
                throw new MalformedDataException("'async' flag without $DONE");
            }
            if (!this.async && containsDone) {
                throw new MalformedDataException("Missing 'async' flag");
            }
        }
        this.async |= containsDone;

        return fileContent;
    }

    private static final ConcurrentLinkedQueue<Yaml> yamlQueue = new ConcurrentLinkedQueue<>();

    private void readYaml(String descriptor, ErrorHandler error) throws MalformedDataException {
        assert descriptor != null && !descriptor.isEmpty();
        boolean lenient = error == ErrorHandler.Ignore;
        Yaml yaml = null;
        if (lenient) {
            yaml = yamlQueue.poll();
        }
        if (yaml == null) {
            Constructor constructor = new Constructor(TestDescriptor.class);
            if (lenient) {
                PropertyUtils utils = new PropertyUtils();
                utils.setSkipMissingProperties(true);
                constructor.setPropertyUtils(utils);
            }
            yaml = new Yaml(constructor);
            if (!lenient) {
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setAllowDuplicateKeys(false);
            }
        }
        TestDescriptor desc;
        try {
            desc = yaml.loadAs(descriptor, TestDescriptor.class);
        } catch (YAMLException e) {
            throw new MalformedDataException(e.getMessage(), e);
        }
        if (lenient) {
            yamlQueue.offer(yaml);
        }
        this.description = desc.getDescription();
        this.includes = desc.getIncludes();
        this.features = desc.getFeatures();
        this.errorType = desc.getNegative().getType();
        this.errorPhase = from(desc.getNegative().getPhase(), lenient);
        this.negative = desc.getNegative().getType() != null;
        if (!desc.getFlags().isEmpty()) {
            if (!lenient) {
                for (String flag : desc.getFlags()) {
                    if (!allowedFlags.contains(flag)) {
                        throw new MalformedDataException(String.format("Unknown flag '%s'", flag));
                    }
                }
            }
            this.noStrict = desc.getFlags().contains("noStrict");
            this.onlyStrict = desc.getFlags().contains("onlyStrict");
            this.module = desc.getFlags().contains("module");
            this.raw = desc.getFlags().contains("raw");
            this.async = desc.getFlags().contains("async");
        }
        if (!lenient && errorPhase == ErrorPhase.Resolution && !module) {
            throw new MalformedDataException(String.format("Invalid error phase 'resolution' for non-module test"));
        }
    }

    // FIXME: Not all tests are updated to use parse.
    private static final HashSet<String> allowedErrorPhases = new HashSet<>(
            Arrays.asList("early", "parse", "resolution", "runtime"));

    private static ErrorPhase from(String phase, boolean lenient) throws MalformedDataException {
        if (!lenient && !allowedErrorPhases.contains(phase)) {
            throw new MalformedDataException(String.format("Unknown error phase '%s'", phase));
        }
        switch (phase) {
        case "early":
        case "parse":
            return ErrorPhase.Parse;
        case "resolution":
            return ErrorPhase.Resolution;
        case "runtime":
            return ErrorPhase.Runtime;
        default:
            return ErrorPhase.Runtime;
        }
    }

    private static final HashSet<String> allowedFlags = new HashSet<>(
            Arrays.asList("onlyStrict", "noStrict", "module", "raw", "async", "generated"));

    public static final class TestDescriptor {
        public static final class Negative {
            private String phase = "runtime";
            private String type;

            public String getPhase() {
                return phase;
            }

            public void setPhase(String phase) {
                this.phase = phase;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        private String description;
        private String info;
        private List<String> includes = Collections.emptyList();
        private List<String> flags = Collections.emptyList();
        private List<String> features = Collections.emptyList();
        private Negative negative = new Negative();
        private String es5id;
        private String es6id;
        private String esid;
        private String author;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public List<String> getIncludes() {
            return includes;
        }

        public void setIncludes(List<String> includes) {
            this.includes = includes;
        }

        public List<String> getFlags() {
            return flags;
        }

        public void setFlags(List<String> flags) {
            this.flags = flags;
        }

        public List<String> getFeatures() {
            return features;
        }

        public void setFeatures(List<String> features) {
            this.features = features;
        }

        public Negative getNegative() {
            return negative;
        }

        public void setNegative(Negative negative) {
            this.negative = negative;
        }

        public String getEs5id() {
            return es5id;
        }

        public void setEs5id(String es5id) {
            this.es5id = es5id;
        }

        public String getEs6id() {
            return es6id;
        }

        public void setEs6id(String es6id) {
            this.es6id = es6id;
        }

        public String getEsid() {
            return esid;
        }

        public void setEsid(String esid) {
            this.esid = esid;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }
    }
}

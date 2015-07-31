/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;
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
    private static final Pattern fileNamePattern = Pattern.compile("(.+?)(?:\\.([^.]*)$|$)");
    private static final Pattern tags = Pattern.compile("\\s*\\*\\s*@(\\w+)\\s*(.+)?\\s*");
    private static final Pattern contentPattern, yamlContentPattern;
    static {
        String fileHeader = "(?:\\s*(?://.*)?\\R)*+";
        String descriptor = "/\\*\\*?((?s:.*?))\\*/";
        String yamlDescriptor = "/\\*---((?s:.*?))---\\*/";
        contentPattern = Pattern.compile(fileHeader + descriptor);
        yamlContentPattern = Pattern.compile(fileHeader + yamlDescriptor);
    }

    private String testName, description, errorType;
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
     * Returns the test-name for the test case.
     */
    public String getTestName() {
        if (testName == null) {
            String filename = getScript().getFileName().toString();
            Matcher matcher = fileNamePattern.matcher(filename);
            if (!matcher.matches()) {
                assert false : "regexp failure";
            }
            testName = matcher.group(1);
        }
        return testName;
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
        if (module) {
            // Module tests don't need to run with explicit Use Strict directive.
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

    /**
     * Parses the test file information for this test case.
     * 
     * @return the file content
     * @throws IOException
     *             if there was any I/O error
     */
    public String readFile() throws IOException {
        String fileContent = readFileContent();
        try {
            readFileInformation(fileContent, true);
        } catch (MalformedDataException e) {
            throw new RuntimeException(e);
        }
        return fileContent;
    }

    /**
     * Reads the file content.
     * 
     * @return the file content
     * @throws IOException
     *             if there was any I/O error
     */
    public String readFileContent() throws IOException {
        return new String(Files.readAllBytes(toFile()), StandardCharsets.UTF_8);
    }

    /**
     * Parses the test file information for this test case.
     * 
     * @param content
     *            the file content
     */
    public void readFileInformation(String content) throws MalformedDataException {
        readFileInformation(content, false);
    }

    /**
     * Parses the test file information for this test case.
     * 
     * @param content
     *            the file content
     * @param lenient
     *            if {@code true} ignore unknown test configurations
     * @throws MalformedDataException
     *             if the test file information cannot be parsed
     */
    private void readFileInformation(String content, boolean lenient) throws MalformedDataException {
        Matcher m;
        if ((m = yamlContentPattern.matcher(content)).lookingAt()) {
            readYaml(m.group(1), lenient);
        } else if ((m = contentPattern.matcher(content)).lookingAt()) {
            readTagged(m.group(1), lenient);
        } else {
            throw new MalformedDataException("Invalid test file: " + this);
        }
        this.async = content.contains("$DONE");
    }

    private static final ConcurrentLinkedQueue<Yaml> yamlQueue = new ConcurrentLinkedQueue<>();

    private void readYaml(String descriptor, boolean lenient) throws MalformedDataException {
        assert descriptor != null && !descriptor.isEmpty();
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
        this.errorType = desc.getNegative();
        this.negative = desc.getNegative() != null;
        if (!desc.getFlags().isEmpty()) {
            if (!lenient) {
                for (String flag : desc.getFlags()) {
                    if (!allowedFlags.contains(flag)) {
                        throw new MalformedDataException(String.format("Unknown flag '%s'", flag));
                    }
                }
            }
            this.negative |= desc.getFlags().contains("negative");
            this.noStrict = desc.getFlags().contains("noStrict");
            this.onlyStrict = desc.getFlags().contains("onlyStrict");
            this.module = desc.getFlags().contains("module");
            this.raw = desc.getFlags().contains("raw");
        }
    }

    private static final HashSet<String> allowedFlags = new HashSet<>(Arrays.asList("negative",
            "onlyStrict", "noStrict", "module", "raw"));

    public static final class TestDescriptor {
        private String description;
        private String info;
        private List<String> includes = Collections.emptyList();
        private List<String> flags = Collections.emptyList();
        private List<String> features = Collections.emptyList();
        private String negative;
        private String es5id;
        private String es6id;
        private String bestPractice;
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

        public String getNegative() {
            return negative;
        }

        public void setNegative(String negative) {
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

        public String getBestPractice() {
            return bestPractice;
        }

        public void setBestPractice(String bestPractice) {
            this.bestPractice = bestPractice;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }
    }

    private void readTagged(String descriptor, boolean lenient) throws MalformedDataException {
        assert descriptor != null && !descriptor.isEmpty();
        for (LineIterator lines = new LineIterator(new StringReader(descriptor)); lines.hasNext();) {
            String line = lines.next();
            Matcher m = tags.matcher(line);
            if (m.matches()) {
                String type = m.group(1);
                String val = m.group(2);
                switch (type) {
                case "description":
                    this.description = requireNonNull(val, "description must not be null");
                    break;
                case "noStrict":
                    requireNull(val);
                    this.noStrict = true;
                    break;
                case "onlyStrict":
                    requireNull(val);
                    this.onlyStrict = true;
                    break;
                case "negative":
                    this.negative = true;
                    this.errorType = Objects.toString(val, this.errorType);
                    break;
                case "hostObject":
                case "reviewers":
                case "generator":
                case "verbatim":
                case "noHelpers":
                case "bestPractice":
                case "implDependent":
                case "author":
                    // ignore for now
                    break;
                // legacy
                case "strict_mode_negative":
                    this.negative = true;
                    this.onlyStrict = true;
                    this.errorType = Objects.toString(val, this.errorType);
                    break;
                case "strict_only":
                    requireNull(val);
                    this.onlyStrict = true;
                    break;
                case "errortype":
                    this.errorType = requireNonNull(val, "error-type must not be null");
                    break;
                case "assertion":
                case "section":
                case "path":
                case "comment":
                case "name":
                    // ignore for now
                    break;
                default:
                    // error
                    if (lenient) {
                        break;
                    }
                    throw new MalformedDataException(String.format("unhandled type '%s' (%s)\n",
                            type, this));
                }
            }
        }
    }

    /**
     * Counterpart to {@link Objects#requireNonNull(Object, String)}.
     */
    private static final <T> T requireNull(T t) {
        if (t != null)
            throw new IllegalStateException("object is not null");
        return t;
    }
}

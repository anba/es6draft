/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.input.BOMInputStream;

import com.github.anba.es6draft.util.TestInfo;

/**
 * Parses and returns test case information from test262 js-doc comments
 * 
 * {@link http://wiki.ecmascript.org/doku.php?id=test262:test_case_format}
 * 
 */
class Test262Info extends TestInfo {
    private static final Pattern fileNamePattern = Pattern.compile("(.+?)(?:\\.([^.]*)$|$)");
    private static final Pattern tags = Pattern.compile("\\s*\\*\\s*@(\\w+)\\s*(.+)?\\s*");
    private static final Pattern start = Pattern.compile("\\s*/\\*\\*.*");
    private static final Pattern end = Pattern.compile("\\s*\\*/.*");

    private String testName, description, errorType;
    private boolean onlyStrict, noStrict, negative;

    public Test262Info(Path basedir, Path script) {
        super(basedir, script);
    }

    @Override
    public String toString() {
        return getTestName();
    }

    /**
     * Returns the test-name for the test case
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
     * Returns the description for the test case
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the expected error-type if any
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns whether the test should only be run in strict-mode
     */
    public boolean isOnlyStrict() {
        return onlyStrict;
    }

    /**
     * Returns whether the test should not be run in strict-mode
     */
    public boolean isNoStrict() {
        return noStrict;
    }

    /**
     * Returns {@code true} iff the test case is expected to fail
     */
    public boolean isNegative() {
        return negative;
    }

    /**
     * Counterpart to {@link Objects#requireNonNull(Object, String)}
     */
    private static final <T> T requireNull(T t) {
        if (t != null)
            throw new IllegalStateException("object is not null");
        return t;
    }

    /**
     * Parses the test file information for this test case
     */
    public void readFileInformation() throws IOException {
        Reader reader = newReader(Files.newInputStream(toFile()));
        try ($LineIterator lines = new $LineIterator(reader)) {
            boolean preamble = true;
            while (lines.hasNext()) {
                String line = lines.next();
                if (preamble) {
                    if (start.matcher(line).matches()) {
                        preamble = false;
                    }
                } else if (end.matcher(line).matches()) {
                    break;
                } else {
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
                            System.err.printf("unhandled type '%s' (%s)\n", type, this);
                        }
                    }
                }
            }
        }
    }

    // inject AutoCloseable into LineIterator
    private static final class $LineIterator extends LineIterator implements AutoCloseable {
        public $LineIterator(Reader reader) throws IllegalArgumentException {
            super(reader);
        }
    }

    private static Reader newReader(InputStream stream) throws IOException {
        BOMInputStream bomstream = new BOMInputStream(stream, ByteOrderMark.UTF_8,
                ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
        String charset = defaultIfNull(bomstream.getBOMCharsetName(), StandardCharsets.UTF_8.name());
        return new InputStreamReader(bomstream, charset);
    }
}

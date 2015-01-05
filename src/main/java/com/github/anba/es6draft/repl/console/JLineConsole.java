/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.TerminalFactory;
import jline.TerminalSupport;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * {@link ReplConsole} implementation for JLine based consoles
 */
public final class JLineConsole implements ReplConsole {
    private final ConsoleReader console;
    private final Formatter formatter;

    public JLineConsole(String programName) throws IOException {
        this(newConsoleReader(programName));
    }

    public JLineConsole(ConsoleReader console) {
        this.console = console;
        this.formatter = new Formatter(console.getOutput());
    }

    private static ConsoleReader newConsoleReader(String programName) throws IOException {
        configureTerminalFlavors();
        ConsoleReader consoleReader = new ConsoleReader(programName, new FileInputStream(
                FileDescriptor.in), System.out, TerminalFactory.get(), getDefaultEncoding());
        consoleReader.setExpandEvents(false);
        return consoleReader;
    }

    private static void configureTerminalFlavors() {
        final boolean isWindows = isWindows();
        final String type = System.getProperty(TerminalFactory.JLINE_TERMINAL);
        if (isWindows && type == null) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.WINDOWS,
                    UnsupportedTerminal.class);
        } else if (isWindows && type.equalsIgnoreCase(TerminalFactory.UNIX)) {
            TerminalFactory.registerFlavor(TerminalFactory.Flavor.UNIX, CygwinTerminal.class);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static String getDefaultEncoding() {
        return Charset.defaultCharset().name();
    }

    public static final class CygwinTerminal extends TerminalSupport {
        private final int width, height;

        public CygwinTerminal() {
            super(true);
            String settings = System.getProperty(TerminalFactory.JLINE_TERMINAL + ".settings", "");
            width = getProperty(settings, "columns", DEFAULT_WIDTH);
            height = getProperty(settings, "rows", DEFAULT_HEIGHT);
        }

        private static int getProperty(String settings, String name, int defaultValue) {
            Matcher m = Pattern.compile(name + "\\s+(\\d{1,4})").matcher(settings);
            return m.find() ? Integer.parseInt(m.group(1)) : defaultValue;
        }

        @Override
        public void init() throws Exception {
            super.init();
            setEchoEnabled(false);
            setAnsiSupported(true);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    @Override
    public void addCompletion(Realm realm) {
        console.addCompleter(new ShellCompleter(realm));
    }

    @Override
    public boolean isAnsiSupported() {
        return console.getTerminal().isAnsiSupported();
    }

    @Override
    public void printf(String format, Object... args) {
        formatter.format(format, args).flush();
    }

    @Override
    public String readLine(String prompt) {
        try {
            return console.readLine(prompt);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public String readLine() {
        try {
            return console.readLine();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void putstr(String s) {
        try {
            console.print(s);
            console.flush();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void print(String s) {
        try {
            console.println(s);
            console.flush();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void printErr(String s) {
        System.err.println(s);
    }

    private static final class ShellCompleter implements Completer {
        private static final Pattern hierarchyPattern, namePattern;
        static {
            final String space = "\\s*";
            final String name = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
            final String spacedName = space + name + space;
            final String hierarchy = "(?:" + spacedName + "\\.)*" + spacedName + "\\.?";
            hierarchyPattern = Pattern.compile(space + "(" + hierarchy + ")" + space + "$");
            namePattern = Pattern.compile(name);
        }
        private final Realm realm;

        ShellCompleter(Realm realm) {
            this.realm = realm;
        }

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            ExecutionContext cx = realm.defaultContext();
            ScriptObject object = realm.getGlobalThis();
            String leftContext = Objects.toString(buffer, "").substring(0, cursor);
            if (leftContext.isEmpty()) {
                addCandidates(candidates, getPropertyNames(cx, object), "", "");
                return candidates.isEmpty() ? -1 : 0;
            }
            Matcher m = hierarchyPattern.matcher(leftContext);
            lookupFailure: if (m.find()) {
                ArrayList<String> segments = segments(m.group(1));
                StringBuilder prefix = new StringBuilder();
                List<String> properties = segments.subList(0, segments.size() - 1);
                if (!properties.isEmpty() && "this".equals(properties.get(0))) {
                    // skip leading `this` segment in property traversal
                    properties = properties.subList(1, properties.size());
                    prefix.append("this.");
                }
                for (String property : properties) {
                    if (!HasProperty(cx, object, property)) {
                        break lookupFailure;
                    }
                    Object value = Get(cx, object, property);
                    if (Type.isObject(value)) {
                        object = Type.objectValue(value);
                    } else if (!Type.isUndefinedOrNull(value)) {
                        object = ToObject(cx, value);
                    } else {
                        break lookupFailure;
                    }
                    prefix.append(property).append('.');
                }
                String partial = segments.get(segments.size() - 1);
                addCandidates(candidates, getPropertyNames(cx, object), partial, prefix.toString());
                return candidates.isEmpty() ? -1 : m.start(1);
            }
            return -1;
        }

        private void addCandidates(List<CharSequence> candidates, Iterable<String> names,
                String partial, String prefix) {
            for (String name : names) {
                if (name.startsWith(partial) && namePattern.matcher(name).matches()) {
                    candidates.add(prefix + name);
                }
            }
        }

        private LinkedHashSet<String> getPropertyNames(ExecutionContext cx, ScriptObject object) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            while (object != null) {
                for (Object key : object.ownPropertyKeys(cx)) {
                    if (key instanceof String) {
                        names.add((String) key);
                    }
                }
                object = object.getPrototypeOf(cx);
            }
            return names;
        }

        private ArrayList<String> segments(String hierarchy) {
            ArrayList<String> segments = new ArrayList<>();
            Matcher m = namePattern.matcher(hierarchy);
            while (m.find()) {
                segments.add(m.group());
            }
            if (hierarchy.charAt(hierarchy.length() - 1) == '.') {
                // add empty segment for trailing dot
                segments.add("");
            }
            assert !segments.isEmpty();
            return segments;
        }
    }
}

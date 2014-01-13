/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetOwnPropertyNames;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public JLineConsole(ConsoleReader console) {
        this.console = console;
        this.formatter = new Formatter(console.getOutput());
    }

    @Override
    public boolean addCompletion(Realm realm) {
        return console.addCompleter(new ShellCompleter(realm));
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
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void print(String s) {
        try {
            console.println(s);
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
                List<String> segments = segments(m.group(1));
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
                    if (Type.isUndefinedOrNull(value)) {
                        break lookupFailure;
                    } else if (!Type.isObject(value)) {
                        value = ToObject(cx, value);
                    }
                    object = Type.objectValue(value);
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

        private Set<String> getPropertyNames(ExecutionContext cx, ScriptObject object) {
            Set<String> names = new LinkedHashSet<>();
            while (object != null) {
                names.addAll(GetOwnPropertyNames(cx, object));
                object = object.getPrototypeOf(cx);
            }
            return names;
        }

        private List<String> segments(String hierarchy) {
            List<String> segments = new ArrayList<>();
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

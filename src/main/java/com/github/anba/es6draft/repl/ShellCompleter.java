/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.console.ShellConsole.Completion;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 *
 */
final class ShellCompleter implements ShellConsole.Completer {
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

    public ShellCompleter(Realm realm) {
        this.realm = realm;
    }

    @Override
    public Completion complete(String line, int cursor) {
        ExecutionContext cx = realm.defaultContext();
        ScriptObject object = realm.getGlobalThis();
        String leftContext = line.substring(0, cursor);
        if (leftContext.isEmpty()) {
            ArrayList<String> candidates = createCandidates(getPropertyNames(cx, object), "", "");
            return new Completion(line, 0, cursor, candidates);
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
            ArrayList<String> candidates = createCandidates(getPropertyNames(cx, object), partial, prefix.toString());
            return new Completion(line, m.start(1), cursor, candidates);
        }
        return new Completion(line, 0, 0, Collections.<String> emptyList());
    }

    private ArrayList<String> createCandidates(Iterable<String> names, String partial, String prefix) {
        ArrayList<String> candidates = new ArrayList<>();
        for (String name : names) {
            if (name.startsWith(partial) && namePattern.matcher(name).matches()) {
                candidates.add(prefix + name);
            }
        }
        return candidates;
    }

    private LinkedHashSet<String> getPropertyNames(ExecutionContext cx, ScriptObject object) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (; object != null; object = object.getPrototypeOf(cx)) {
            for (Object key : object.ownPropertyKeys(cx)) {
                if (key instanceof String) {
                    names.add((String) key);
                }
            }
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

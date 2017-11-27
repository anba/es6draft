/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.DToA;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Console;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.StackTraces;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.objects.JSONObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
public final class ConsoleObject {
    private int groups = 0;
    private final HashMap<String, Integer> labels = new HashMap<>();
    private final HashMap<Object, Long> timers = new HashMap<>();
    private final boolean ansiSupported;
    private final boolean colored;

    private ConsoleObject(boolean ansiSupported, boolean colored) {
        this.ansiSupported = ansiSupported;
        this.colored = ansiSupported && colored;
    }

    /**
     * Creates a new {@code console} object.
     * 
     * @param realm
     *            the realm instance
     * @param colored
     *            enabled colored output
     * @return the console object
     */
    static ScriptObject createConsole(Realm realm, boolean colored) {
        boolean ansiSupported = isAnsiSupported(realm.getRuntimeContext().getConsole());
        ConsoleObject consoleObject = new ConsoleObject(ansiSupported, colored);
        OrdinaryObject console = OrdinaryObject.ObjectCreate(realm.defaultContext(), Intrinsics.ObjectPrototype);
        Properties.createProperties(realm.defaultContext(), console, consoleObject, ConsoleObject.class);
        return console;
    }

    private static boolean isAnsiSupported(Console c) {
        return c instanceof ShellConsole && ((ShellConsole) c).isAnsiSupported();
    }

    private enum LogLevel {
        Log, Info, Warn, Error
    }

    private String withLevel(LogLevel level, String message) {
        if (!colored) {
            return message;
        }
        switch (level) {
        case Log:
        case Info:
            return message;
        case Warn:
            return Ansi.Attribute.TextColorRed + message + Ansi.Attribute.Reset;
        case Error:
            return Ansi.Attribute.TextColorRed.and(Ansi.Attribute.Bold) + message + Ansi.Attribute.Reset;
        default:
            throw new AssertionError();
        }
    }

    private String groups() {
        String s = Strings.repeat(' ', groups);
        return s + s;
    }

    private void println(ExecutionContext cx, LogLevel level, String message) {
        PrintWriter w = cx.getRuntimeContext().getConsole().writer();
        w.println(groups() + withLevel(level, message));
    }

    private String inspect(ExecutionContext cx, Object argument) {
        return SourceBuilder.ToSource(cx, argument);
    }

    private String format(ExecutionContext cx, Object... args) {
        assert args.length > 0;
        Object message = args[0];
        Iterator<Object> values = Arrays.asList(args).iterator();
        assert values.hasNext();
        values.next();

        StringBuffer result = new StringBuffer();
        if (!Type.isString(message)) {
            result.append(inspect(cx, message));
            while (values.hasNext()) {
                result.append(' ').append(inspect(cx, values.next()));
            }
        } else {
            Pattern p = Pattern.compile("%(?:\\.(\\d{1,2}))?([%dfijos])");
            Matcher m = p.matcher(Type.stringValue(message));
            java.util.function.Function<java.util.function.Function<Object, String>, String> formatNext = t -> {
                return values.hasNext() ? t.apply(values.next()) : m.group();
            };
            BiFunction<Double, Integer, String> toFixedNumber = (d, f) -> {
                StringBuilder sb = new StringBuilder();
                DToA.JS_dtostr(sb, DToA.DTOSTR_FIXED, f, d);
                return sb.toString();
            };

            while (m.find()) {
                String replacement;
                switch (m.group(2).charAt(0)) {
                case '%':
                    replacement = "%";
                    break;
                case 'd':
                    replacement = formatNext.apply(v -> ToFlatString(cx, ToNumber(cx, v)));
                    break;
                case 'i':
                    replacement = formatNext.apply(v -> toFixedNumber.apply(ToNumber(cx, v), 0));
                    break;
                case 'f':
                    int pad = m.group(1) != null ? Math.min(Integer.parseInt(m.group(1)), 21) : 6;
                    replacement = formatNext.apply(v -> toFixedNumber.apply(ToNumber(cx, v), pad));
                    break;
                case 'j':
                    try {
                        replacement = formatNext.apply(v -> JSONObject.stringify(cx, v));
                    } catch (ScriptException e) {
                        replacement = "[]";
                    }
                    break;
                case 'o':
                    replacement = formatNext.apply(v -> inspect(cx, v));
                    break;
                case 's':
                default:
                    replacement = formatNext.apply(v -> ToFlatString(cx, v));
                    break;
                }
                m.appendReplacement(result, replacement);
            }
            m.appendTail(result);

            while (values.hasNext()) {
                Object v = values.next();
                if (Type.isObject(v)) {
                    result.append(' ').append(inspect(cx, v));
                } else {
                    result.append(' ').append(ToFlatString(cx, v));
                }
            }
        }
        return result.toString();
    }

    @Function(name = "assert", arity = 1)
    public void _assert(ExecutionContext cx, Object expression, Object... args) {
        if (!ToBoolean(expression)) {
            String msg = args.length == 0 ? "" : format(cx, args);
            ErrorObject error = new ErrorObject(cx.getRealm(), Intrinsics.ErrorPrototype, msg);
            CreateDataProperty(cx, error, "name", "AssertionError");
            throw ScriptException.create(error);
        }
    }

    @Function(name = "log", arity = 1)
    @AliasFunction(name = "debug")
    public void log(ExecutionContext cx, Object... args) {
        if (args.length > 0) {
            println(cx, LogLevel.Log, format(cx, args));
        }
    }

    @Function(name = "info", arity = 1)
    public void info(ExecutionContext cx, Object... args) {
        if (args.length > 0) {
            println(cx, LogLevel.Info, format(cx, args));
        }
    }

    @Function(name = "warn", arity = 1)
    public void warn(ExecutionContext cx, Object... args) {
        if (args.length > 0) {
            println(cx, LogLevel.Warn, format(cx, args));
        }
    }

    @Function(name = "error", arity = 1)
    public void error(ExecutionContext cx, Object... args) {
        if (args.length > 0) {
            println(cx, LogLevel.Error, format(cx, args));
        }
    }

    @Function(name = "clear", arity = 0)
    public void clear(ExecutionContext cx) {
        if (ansiSupported) {
            cx.getRuntimeContext().getConsole().writer().print("\u001B[2J\u001B[1;1H");
        }
    }

    @Function(name = "dir", arity = 1)
    public void dir(ExecutionContext cx, Object object) {
        println(cx, LogLevel.Log, inspect(cx, object));
    }

    private static String frameToString(StackTraceElement frame) {
        return String.format("%s (%s:%d)", frame.getMethodName(), frame.getFileName(), frame.getLineNumber());
    }

    @Function(name = "trace", arity = 0)
    public void trace(ExecutionContext cx) {
        final String prefix = String.format("%n%s    at", groups());
        StringBuilder sb = new StringBuilder();
        sb.append("Trace");
        for (StackTraceElement frame : StackTraces.scriptStackTrace(new Throwable())) {
            sb.append(prefix).append(' ').append(frameToString(frame));
        }
        println(cx, LogLevel.Log, sb.toString());
    }

    @Function(name = "group", arity = 1)
    @AliasFunction(name = "groupCollapsed")
    public void group(ExecutionContext cx, Object... args) {
        groups += 1;
        if (args.length > 0) {
            println(cx, LogLevel.Log, format(cx, args));
        }
    }

    @Function(name = "groupEnd", arity = 0)
    public void groupEnd(ExecutionContext cx) {
        groups = Math.max(groups - 1, 0);
    }

    @Function(name = "count", arity = 0)
    public void count(ExecutionContext cx, Object label) {
        String message, key;
        if (Type.isUndefined(label)) {
            StackTraceElement frame = StackTraces.stackTraceStream(new Throwable()).findFirst().get();
            key = frameToString(frame);
            message = "default";
        } else {
            key = ToFlatString(cx, label);
            message = key;
        }
        labels.compute(key, (k, c) -> c != null ? c + 1 : 1);
        println(cx, LogLevel.Info, String.format("%s: %d", message, labels.get(key)));
    }

    @Function(name = "time", arity = 1)
    public void time(ExecutionContext cx, Object name) {
        if (Type.isUndefined(name)) {
            name = "default";
        }
        timers.computeIfAbsent(name, __ -> System.currentTimeMillis());
    }

    @Function(name = "timeEnd", arity = 1)
    public void timeEnd(ExecutionContext cx, Object name) {
        if (Type.isUndefined(name)) {
            name = "default";
        }
        Long start = timers.remove(name);
        if (start != null) {
            long end = System.currentTimeMillis();
            println(cx, LogLevel.Info, String.format("%s: %d ms", ToString(cx, name), end - start));
        }
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.Objects;

import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * Support class to retrieve source code of compiled function objects.
 */
final class FunctionSource {
    private static final String NATIVE_CODE = " [native code] ";
    private static final String NO_SOURCE = " [no source] ";
    private static final int DEFAULT_FLAGS = RuntimeInfo.FunctionFlags.Declaration.getValue();

    private FunctionSource() {
    }

    /**
     * Returns the string:
     * 
     * <pre>
     * function "functionName"() { [native code] }
     * </pre>
     * 
     * @param functionName
     *            the function name
     * @return the function source string
     */
    public static String nativeCode(String functionName) {
        return sourceString(functionName, NATIVE_CODE, DEFAULT_FLAGS);
    }

    /**
     * Returns the string:
     * 
     * <pre>
     * function "functionName"() { [no source] }
     * </pre>
     * 
     * @param functionName
     *            the function name
     * @return the function source string
     */
    public static String noSource(String functionName) {
        return sourceString(functionName, NO_SOURCE, DEFAULT_FLAGS);
    }

    /**
     * Returns the function source string for a user-defined function object.
     * 
     * @param function
     *            the function object
     * @return the function source string
     */
    public static String toSourceString(FunctionObject function) {
        RuntimeInfo.Function code = function.getCode();
        String name = Objects.toString(code.functionName(), "");
        int flags = code.functionFlags();
        if (RuntimeInfo.FunctionFlags.Native.isSet(flags)) {
            return sourceString(name, NATIVE_CODE, flags);
        }
        RuntimeInfo.FunctionSource source = code.source();
        if (source == null) {
            return sourceString(name, NO_SOURCE, flags);
        }
        return sourceString(name, source.parameters(), source.body(), flags,
                function.getRealm().isEnabled(CompatibilityOption.ImplicitStrictDirective));
    }

    private static String sourceString(String name, String body, int flags) {
        return sourceString(name, "() ", body, flags, false);
    }

    private static String sourceString(String name, String parameters, String body, int flags, boolean implicitStrict) {
        boolean async = RuntimeInfo.FunctionFlags.Async.isSet(flags);
        boolean generator = RuntimeInfo.FunctionFlags.Generator.isSet(flags);
        StringBuilder source = new StringBuilder(32 + name.length() + parameters.length() + body.length());
        if (RuntimeInfo.FunctionFlags.Arrow.isSet(flags)) {
            // ArrowFunction, GeneratorComprehension, AsyncArrowFunction
            if (generator) {
                // Display generator comprehension as generator function.
                source.append("function* ").append(name);
            } else if (async) {
                source.append("async ");
            }
            source.append(parameters);
        } else if (RuntimeInfo.FunctionFlags.Declaration.isSet(flags)) {
            // FunctionDeclaration, (Legacy)GeneratorDeclaration, AsyncFunctionDeclaration
            if (async) {
                source.append("async function ");
            } else if (generator && !RuntimeInfo.FunctionFlags.LegacyGenerator.isSet(flags)) {
                source.append("function* ");
            } else {
                source.append("function ");
            }
            source.append(name).append(parameters);
        } else if (RuntimeInfo.FunctionFlags.Expression.isSet(flags)) {
            // FunctionExpression, (Legacy)GeneratorExpression, AsyncFunctionExpression
            if (async) {
                source.append("async function ");
            } else if (generator && !RuntimeInfo.FunctionFlags.LegacyGenerator.isSet(flags)) {
                source.append("function* ");
            } else {
                source.append("function ");
            }
            if (RuntimeInfo.FunctionFlags.ScopedName.isSet(flags)) {
                source.append(name);
            }
            source.append(parameters);
        } else if (RuntimeInfo.FunctionFlags.Method.isSet(flags)) {
            // MethodDefinition
            if (RuntimeInfo.FunctionFlags.Static.isSet(flags)) {
                source.append("static ");
            }
            if (async) {
                source.append("async ");
            } else if (generator) {
                source.append('*');
            }
            source.append(name).append(parameters);
        } else {
            // ClassDefinition
            assert RuntimeInfo.FunctionFlags.Class.isSet(flags);
            // parameters = constructor method, body = optional call constructor method
            return source.append(parameters).append(body).toString();
        }
        if (RuntimeInfo.FunctionFlags.ConciseBody.isSet(flags)) {
            source.append(body);
        } else {
            source.append('{');
            if (RuntimeInfo.FunctionFlags.ImplicitStrict.isSet(flags) && implicitStrict) {
                source.append("\n\"use strict\";\n");
            }
            source.append(body).append('}');
        }
        return source.toString();
    }
}

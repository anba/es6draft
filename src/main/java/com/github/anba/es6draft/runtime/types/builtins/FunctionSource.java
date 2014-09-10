/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.Callable.SourceSelector;

/**
 * Support class to retrieve source code of compiled function objects.
 */
final class FunctionSource {
    private FunctionSource() {
    }

    /**
     * Returns the function source string for an user-defined function object.
     * 
     * @param selector
     *            the source selector
     * @param function
     *            the function object
     * @return the function source string
     */
    public static String toSourceString(SourceSelector selector, FunctionObject function) {
        RuntimeInfo.Function code = function.getCode();
        String src = code.source();
        if (src == null) {
            return FunctionSource.noSource(selector);
        }
        String source;
        try {
            source = SourceCompressor.decompress(src).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String parameters = source.substring(0, code.bodySourceStart());
        String body = source.substring(code.bodySourceStart());
        if (selector == SourceSelector.Body) {
            return body;
        }
        return sourceString(function, parameters, body);
    }

    private static String sourceString(FunctionObject function, String parameters, String body) {
        RuntimeInfo.Function code = function.getCode();
        boolean async = code.isAsync(), generator = code.isGenerator();
        String name = code.functionName();
        int flags = code.functionFlags();
        StringBuilder source = new StringBuilder();
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
            if (code.hasScopedName()) {
                source.append(name);
            }
            source.append(parameters);
        } else {
            // MethodDefinition
            assert RuntimeInfo.FunctionFlags.Method.isSet(flags);
            if (RuntimeInfo.FunctionFlags.Static.isSet(flags)) {
                source.append("static ");
            }
            if (async) {
                source.append("async ");
            } else if (generator) {
                source.append('*');
            }
            source.append(name).append(parameters);
        }
        if (RuntimeInfo.FunctionFlags.ConciseBody.isSet(flags)) {
            source.append(body);
        } else {
            source.append('{');
            if (RuntimeInfo.FunctionFlags.ImplicitStrict.isSet(flags)
                    && function.getRealm().isEnabled(CompatibilityOption.ImplicitStrictDirective)) {
                source.append("\n\"use strict\";\n");
            }
            source.append(body).append('}');
        }
        return source.toString();
    }

    /**
     * Returns the string:
     * 
     * <pre>
     * function "functionName"() { [native code] }
     * </pre>
     * 
     * @param selector
     *            the source selector
     * @param functionName
     *            the function name
     * @return the function source string
     */
    public static String nativeCode(SourceSelector selector, String functionName) {
        return sourceString(selector, functionName, " [native code] ");
    }

    /**
     * Returns the string:
     * 
     * <pre>
     * function F() { [no source] }
     * </pre>
     * 
     * @param selector
     *            the source selector
     * @return the function source string
     */
    public static String noSource(SourceSelector selector) {
        return sourceString(selector, "F", " [no source] ");
    }

    private static String sourceString(SourceSelector selector, String functionName, String body) {
        if (selector == SourceSelector.Body) {
            return body;
        }
        int length = functionName.length() + body.length() + 9 + 3 + 2;
        return new StringBuilder(length).append("function ").append(functionName).append("() ")
                .append('{').append(body).append('}').toString();
    }
}

/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.Objects;

import com.github.anba.es6draft.parser.Characters;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * Support class to retrieve source code of compiled function objects.
 */
final class FunctionSource {
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
        return "function " + functionName + "() { [native code] }";
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
    public static String nativeCodeWithBindingIdentifier(String functionName) {
        int beginIndex = 0, endIndex = functionName.length();
        if (functionName.startsWith("get ") || functionName.startsWith("set ")) {
            beginIndex = 4;
        }
        if (functionName.startsWith("[Symbol.", beginIndex)) {
            beginIndex += 8;
            endIndex -= 1;
        }
        String name = functionName.substring(beginIndex, endIndex);
        if (isValidBindingIdentifier(name)) {
            return "function " + name + "() { [native code] }";
        }
        return "function () { [native code] }";
    }

    private static boolean isValidBindingIdentifier(String name) {
        if (name.isEmpty()) {
            return false;
        }
        int cp = name.codePointAt(0);
        if (!Characters.isIdentifierStart(cp)) {
            return false;
        }
        for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
            cp = name.codePointAt(i);
            if (!Characters.isIdentifierPart(cp)) {
                return false;
            }
        }
        return true;
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
            return "function " + name + "() { [native code] }";
        }
        RuntimeInfo.FunctionSource source = code.source();
        if (source == null) {
            return "function " + name + "() { [no source] }";
        }
        if (function.getRealm().getRuntimeContext().isEnabled(CompatibilityOption.FunctionToString)) {
            return source.toString();
        }
        return sourceString(name, source.toString(), flags);
    }

    private static String sourceString(String name, String sourceString, int flags) {
        boolean async = RuntimeInfo.FunctionFlags.Async.isSet(flags);
        boolean generator = RuntimeInfo.FunctionFlags.Generator.isSet(flags);
        StringBuilder source = new StringBuilder(16 + name.length() + sourceString.length());
        if (RuntimeInfo.FunctionFlags.Arrow.isSet(flags)) {
            // ArrowFunction, GeneratorComprehension, AsyncArrowFunction
            if (generator) {
                // Display generator comprehension as generator function.
                source.append("function* ").append(name);
            } else if (async) {
                source.append("async ");
            }
        } else if (RuntimeInfo.FunctionFlags.Declaration.isSet(flags)) {
            // FunctionDeclaration, GeneratorDeclaration, AsyncFunctionDeclaration
            if (async && generator) {
                source.append("async function* ");
            } else if (async) {
                source.append("async function ");
            } else if (generator) {
                source.append("function* ");
            } else {
                source.append("function ");
            }
            source.append(name);
        } else if (RuntimeInfo.FunctionFlags.Expression.isSet(flags)) {
            // FunctionExpression, GeneratorExpression, AsyncFunctionExpression
            if (async && generator) {
                source.append("async function* ");
            } else if (async) {
                source.append("async function ");
            } else if (generator) {
                source.append("function* ");
            } else {
                source.append("function ");
            }
            if (RuntimeInfo.FunctionFlags.ScopedName.isSet(flags)) {
                source.append(name);
            }
        } else if (RuntimeInfo.FunctionFlags.Method.isSet(flags)) {
            // MethodDefinition
            if (async && generator) {
                source.append("async* ");
            } else if (async) {
                source.append("async ");
            } else if (generator) {
                source.append('*');
            } else if (RuntimeInfo.FunctionFlags.Getter.isSet(flags)) {
                source.append("get ");
            } else if (RuntimeInfo.FunctionFlags.Setter.isSet(flags)) {
                source.append("set ");
            }
            source.append(name);
        } else {
            // ClassDefinition
            assert RuntimeInfo.FunctionFlags.Class.isSet(flags);
        }
        return source.append(sourceString).toString();
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.analyzer.CodeSize;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakSetObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Default runtime functions.
 */
public final class RuntimeFunctions {
    private RuntimeFunctions() {
    }

    private static Intrinsics getIntrinsicByName(ExecutionContext cx, String name) {
        try {
            return Intrinsics.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid intrinsic: " + name);
        }
    }

    private static CompatibilityOption getCompatibilityOptionByName(ExecutionContext cx, String name) {
        try {
            return CompatibilityOption.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid compatibility option: " + name);
        }
    }

    /**
     * Native function: {@code %Include(<file>)}.
     * <p>
     * Loads and evaluates the script file.
     * 
     * @param cx
     *            the execution context
     * @param file
     *            the file path
     * @return the script evaluation result
     */
    public static Object Include(ExecutionContext cx, CharSequence file) {
        Realm realm = cx.getRealm();
        Source base = realm.sourceInfo(cx);
        if (base == null || base.getFile() == null) {
            throw newInternalError(cx, Messages.Key.InternalError, "No source: " + Objects.toString(base));
        }
        Path path = Objects.requireNonNull(base.getFile().getParent()).resolve(file.toString());
        Source source = new Source(path, Objects.requireNonNull(path.getFileName()).toString(), 1);
        Script script;
        try {
            script = realm.getScriptLoader().script(source, path);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        } catch (IOException e) {
            throw newInternalError(cx, e, Messages.Key.InternalError, e.toString());
        }
        return script.evaluate(cx);
    }

    /**
     * Native function: {@code %Intrinsic(<name>)}.
     * <p>
     * Returns the intrinsic by name.
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the intrinsic name
     * @return the intrinsic
     */
    public static OrdinaryObject Intrinsic(ExecutionContext cx, String name) {
        Intrinsics id = getIntrinsicByName(cx, name);
        return cx.getRealm().getIntrinsic(id);
    }

    /**
     * Native function: {@code %SetIntrinsic(<name>, <realm>)}.
     * <p>
     * Sets the intrinsic to a new value.
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the intrinsic name
     * @param intrinsic
     *            the new intrinsic object
     * @return the intrinsic
     */
    public static OrdinaryObject SetIntrinsic(ExecutionContext cx, String name, OrdinaryObject intrinsic) {
        Intrinsics id = getIntrinsicByName(cx, name);
        cx.getRealm().setIntrinsic(id, intrinsic);
        return intrinsic;
    }

    /**
     * Native function: {@code %CallFunction(<function>, <thisValue>, ...<arguments>)}.
     * <p>
     * Calls the function object.
     * 
     * @param cx
     *            the execution context
     * @param fn
     *            the function
     * @param thisValue
     *            the this-value
     * @param args
     *            the function arguments
     * @return the function return value
     */
    public static Object CallFunction(ExecutionContext cx, Callable fn, Object thisValue, Object... args) {
        return fn.call(cx, thisValue, args);
    }

    /**
     * Native function: {@code %GlobalTemplate()}.
     * <p>
     * Returns the global object template.
     * 
     * @param cx
     *            the execution context
     * @return the global object template
     */
    public static GlobalObject GlobalTemplate(ExecutionContext cx) {
        return cx.getRealm().getGlobalObjectTemplate();
    }

    /**
     * Native function: {@code %GlobalObject()}.
     * <p>
     * Returns the global object.
     * 
     * @param cx
     *            the execution context
     * @return the global object
     */
    public static ScriptObject GlobalObject(ExecutionContext cx) {
        return cx.getRealm().getGlobalObject();
    }

    /**
     * Native function: {@code %GlobalThis()}.
     * <p>
     * Returns the global this.
     * 
     * @param cx
     *            the execution context
     * @return the global this
     */
    public static ScriptObject GlobalThis(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
    }

    /**
     * Native function: {@code %IsArrayBuffer(<value>)}.
     * <p>
     * Tests whether the input argument is an ArrayBuffer object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an ArrayBuffer
     */
    public static boolean IsArrayBuffer(Object value) {
        return value instanceof ArrayBufferObject;
    }

    /**
     * Native function: {@code %IsCompatibilityOptionEnabled(<name>)}.
     * <p>
     * Tests whether or not a compatibility option is enabled
     * 
     * @param cx
     *            the execution context
     * @param name
     *            compatibility option name
     * @return {@code true} if the compatibility option is enabled
     */
    public static boolean IsCompatibilityOptionEnabled(ExecutionContext cx, String name) {
        return cx.getRealm().isEnabled(getCompatibilityOptionByName(cx, name));
    }

    /**
     * Native function: {@code %IsDetachedBuffer(<arrayBuffer>)}.
     * <p>
     * Tests whether or not the array buffer is detached.
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @return {@code true} if the array buffer is detached
     */
    public static boolean IsDetachedBuffer(ArrayBufferObject arrayBuffer) {
        return ArrayBufferConstructor.IsDetachedBuffer(arrayBuffer);
    }

    /**
     * Native function: {@code %IsFunctionExpression(<function>)}.
     * <p>
     * Returns {@code true} if <var>function</var> is a function expression.
     * 
     * @param function
     *            the function object
     * @return {@code true} if <var>function</var> is a function expression
     */
    public static boolean IsFunctionExpression(Callable function) {
        if (!(function instanceof FunctionObject)) {
            return false;
        }
        FunctionObject funObj = (FunctionObject) function;
        RuntimeInfo.Function code = funObj.getCode();
        if (code == null) {
            return false;
        }
        return code.is(RuntimeInfo.FunctionFlags.Expression) && !code.is(RuntimeInfo.FunctionFlags.Arrow);
    }

    /**
     * Native function: {@code %IsGenerator(<value>)}.
     * <p>
     * Tests whether the input argument is a generator object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is a generator
     */
    public static boolean IsGenerator(Object value) {
        return value instanceof GeneratorObject;
    }

    /**
     * Native function: {@code %IsTypedArrayObject(<elementType>, <value>)}.
     * <p>
     * Tests whether the input argument is a typed array object.
     * 
     * @param elementType
     *            the element type name
     * @param value
     *            the input argument
     * @return {@code true} if the object is a typed array object
     */
    public static boolean IsTypedArrayObject(CharSequence elementType, Object value) {
        if (!(value instanceof TypedArrayObject)) {
            return false;
        }
        return ((TypedArrayObject) value).getElementType().getConstructorName().equals(elementType.toString());
    }

    /**
     * Native function: {@code %RegExpReplace(<regexp>, <string>, <replacement>)}.
     * <p>
     * Replaces every occurrence of <var>regexp</var> in <var>string</var> with <var>replacement</var>.
     * 
     * @param cx
     *            the execution context
     * @param regexp
     *            the regular expression object
     * @param string
     *            the input string
     * @param replacement
     *            the replacement string
     * @return the result string
     */
    public static String RegExpReplace(ExecutionContext cx, RegExpObject regexp, CharSequence string,
            CharSequence replacement) {
        return RegExpPrototype.RegExpReplace(cx, regexp, string.toString(), replacement.toString());
    }

    /**
     * Native function: {@code %RegExpTest(<regexp>, <string>)}.
     * <p>
     * Returns {@code true} if <var>string</var> matches <var>regexp</var>.
     * 
     * @param cx
     *            the execution context
     * @param regexp
     *            the regular expression object
     * @param string
     *            the input string
     * @return {@code true} if <var>string</var> matches <var>regexp</var>
     */
    public static boolean RegExpTest(ExecutionContext cx, RegExpObject regexp, CharSequence string) {
        return RegExpPrototype.RegExpTest(cx, regexp, string.toString());
    }

    /**
     * Native function: {@code %SymbolDescription(<symbol>)}.
     * <p>
     * Returns the symbol's description or {@link Undefined#UNDEFINED}.
     * 
     * @param symbol
     *            the symbol object
     * @return the symbol's description or {@link Undefined#UNDEFINED}
     */
    public static Object SymbolDescription(Symbol symbol) {
        return symbol.getDescription() != null ? symbol.getDescription() : UNDEFINED;
    }

    /**
     * Native function: {@code %ToPropertyKey(<value>)}.
     * <p>
     * Converts the input argument to a property key.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the input argument
     * @return the property key
     */
    public static Object ToPropertyKey(ExecutionContext cx, Object value) {
        return AbstractOperations.ToPropertyKey(cx, value);
    }

    /**
     * Native function: {@code %ToString(<value>)}.
     * <p>
     * Converts the input argument to a string.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the input argument
     * @return the string
     */
    public static Object ToString(ExecutionContext cx, Object value) {
        return AbstractOperations.ToString(cx, value);
    }

    /**
     * Native function: {@code %WeakMapClear(<weakMap>)}.
     * <p>
     * Clears the weak map object.
     * 
     * @param cx
     *            the execution context
     * @param weakMap
     *            the weak map object
     */
    public static void WeakMapClear(ExecutionContext cx, Object weakMap) {
        if (!(weakMap instanceof WeakMapObject)) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        ((WeakMapObject) weakMap).getWeakMapData().clear();
    }

    /**
     * Native function: {@code %WeakSetClear(<weakSet>)}.
     * <p>
     * Clears the weak set object.
     * 
     * @param cx
     *            the execution context
     * @param weakSet
     *            the weak set object
     */
    public static void WeakSetClear(ExecutionContext cx, Object weakSet) {
        if (!(weakSet instanceof WeakSetObject)) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        ((WeakSetObject) weakSet).getWeakSetData().clear();
    }

    /**
     * Creates a new data property.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @param key
     *            the property key
     * @param value
     *            the property value
     */
    public static void CreateDataPropertyOrThrow(ExecutionContext cx, ScriptObject object, Object key, Object value) {
        AbstractOperations.CreateDataPropertyOrThrow(cx, object, ToPropertyKey(cx, key), value);
    }

    /**
     * Returns the array buffer's byte order
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     * @return the byte order
     */
    public static String ByteOrder(ExecutionContext cx, ArrayBuffer arrayBuffer) {
        if (arrayBuffer.isDetached()) {
            return "";
        }
        return arrayBuffer.getData().order().toString();
    }

    /**
     * Returns the estimated code size for the given source code.
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the source code
     * @return the estimated code size
     */
    public static int CodeSize(ExecutionContext cx, CharSequence sourceCode) {
        try {
            com.github.anba.es6draft.ast.Script parsedScript = cx.getRealm().getScriptLoader()
                    .parseScript(new Source("<script>", 1), sourceCode.toString());
            return CodeSize.calculate(parsedScript);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
    }
}

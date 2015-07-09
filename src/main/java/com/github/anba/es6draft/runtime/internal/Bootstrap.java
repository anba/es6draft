/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.EqualityComparison;
import static com.github.anba.es6draft.runtime.AbstractOperations.RelationalComparison;
import static com.github.anba.es6draft.runtime.AbstractOperations.StrictEqualityComparison;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckCallable;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckConstructor;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Arrays;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.compiler.assembler.Handle;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 *
 */
public final class Bootstrap {
    private Bootstrap() {
    }

    private static final class CallNames {
        static final String CALL = "expression::call";
        static final String CONSTRUCT = "expression::construct";
        static final String SUPER = "expression::super";
        static final String CONCAT = "expression::concat";
        static final String ADD = "expression::add";
        static final String EQ = "expression::equals";
        static final String SHEQ = "expression::strictEquals";
        static final String LT = "expression::lessThan";
        static final String GT = "expression::greaterThan";
        static final String LE = "expression::lessThanEquals";
        static final String GE = "expression::greaterThanEquals";
    }

    private static final class Descriptors {
        static final MethodTypeDescriptor ADD = MethodTypeDescriptor.methodType(Object.class,
                Object.class, Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor CMP = MethodTypeDescriptor.methodType(boolean.class,
                Object.class, Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor EQ = MethodTypeDescriptor.methodType(boolean.class,
                Object.class, Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor STRICT_EQ = MethodTypeDescriptor.methodType(
                boolean.class, Object.class, Object.class);
        static final MethodTypeDescriptor CALL = MethodTypeDescriptor.methodType(Object.class,
                Object.class, ExecutionContext.class, Object.class, Object[].class);
        static final MethodTypeDescriptor CONSTRUCT = MethodTypeDescriptor.methodType(
                ScriptObject.class, Object.class, ExecutionContext.class, Object[].class);
        static final MethodTypeDescriptor SUPER = MethodTypeDescriptor.methodType(
                ScriptObject.class, Constructor.class, ExecutionContext.class, Constructor.class,
                Object[].class);
    }

    private static final Handle BOOTSTRAP;
    static {
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class,
                String.class, MethodType.class);
        BOOTSTRAP = MethodName.findStatic(Bootstrap.class, "bootstrapDynamic", mt).toHandle();
    }

    /**
     * Returns the invokedynamic instruction name for call expressions.
     * 
     * @return the invokedynamic instruction name
     */
    public static String getCallName() {
        return CallNames.CALL;
    }

    /**
     * Returns the method descriptor for call expressions.
     * 
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getCallMethodDescriptor() {
        return Descriptors.CALL;
    }

    /**
     * Returns the bootstrapping handle for call expressions.
     * 
     * @return the bootstrapping handle
     */
    public static Handle getCallBootstrap() {
        return BOOTSTRAP;
    }

    private static final MethodHandle callSetupMH;
    private static final MethodHandle callGenericMH;
    private static final MethodHandle testFunctionObjectMH, testBuiltinFunctionMH;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        testFunctionObjectMH = lookup.findStatic("testFunctionObject",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testBuiltinFunctionMH = lookup.findStatic("testBuiltinFunction",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        callGenericMH = lookup.findStatic("callGeneric", MethodType.methodType(Object.class,
                Object.class, ExecutionContext.class, Object.class, Object[].class));
        callSetupMH = lookup.findStatic("callSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Object.class, ExecutionContext.class, Object.class,
                Object[].class));
    }

    @SuppressWarnings("unused")
    private static MethodHandle callSetup(MutableCallSite callsite, Object function,
            ExecutionContext cx, Object thisValue, Object[] arguments) {
        MethodHandle target, test;
        if (function instanceof FunctionObject) {
            FunctionObject fn = (FunctionObject) function;
            test = MethodHandles.insertArguments(testFunctionObjectMH, 1, fn.getMethodInfo());
            target = fn.getCallMethod();
        } else if (function instanceof BuiltinFunction) {
            BuiltinFunction fn = (BuiltinFunction) function;
            test = MethodHandles.insertArguments(testBuiltinFunctionMH, 1, fn.getMethodInfo());
            target = fn.getCallMethod();
        } else {
            target = test = null;
        }
        return setCallSiteTarget(callsite, target, test, callGenericMH);
    }

    @SuppressWarnings("unused")
    private static boolean testFunctionObject(Object function, Object methodInfo) {
        return function instanceof FunctionObject
                && ((FunctionObject) function).getMethodInfo() == methodInfo;
    }

    @SuppressWarnings("unused")
    private static boolean testBuiltinFunction(Object function, Object methodInfo) {
        return function instanceof BuiltinFunction
                && ((BuiltinFunction) function).getMethodInfo() == methodInfo;
    }

    @SuppressWarnings("unused")
    private static Object callGeneric(Object function, ExecutionContext callerContext,
            Object thisValue, Object[] arguments) {
        return CheckCallable(function, callerContext).call(callerContext, thisValue, arguments);
    }

    /**
     * Returns the invokedynamic instruction name for construct expressions.
     * 
     * @return the invokedynamic instruction name
     */
    public static String getConstructName() {
        return CallNames.CONSTRUCT;
    }

    /**
     * Returns the method descriptor for construct expressions.
     * 
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getConstructMethodDescriptor() {
        return Descriptors.CONSTRUCT;
    }

    /**
     * Returns the bootstrapping handle for construct expressions.
     * 
     * @return the bootstrapping handle
     */
    public static Handle getConstructBootstrap() {
        return BOOTSTRAP;
    }

    private static final MethodHandle constructSetupMH;
    private static final MethodHandle constructGenericMH;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        constructGenericMH = lookup.findStatic("constructGeneric", MethodType.methodType(
                ScriptObject.class, Object.class, ExecutionContext.class, Object[].class));
        constructSetupMH = lookup.findStatic("constructSetup", MethodType.methodType(
                MethodHandle.class, MutableCallSite.class, Object.class, ExecutionContext.class,
                Object[].class));
    }

    @SuppressWarnings("unused")
    private static MethodHandle constructSetup(MutableCallSite callsite, Object constructor,
            ExecutionContext cx, Object[] arguments) {
        MethodHandle target, test;
        if (constructor instanceof FunctionObject && constructor instanceof Constructor) {
            FunctionObject fn = (FunctionObject) constructor;
            test = MethodHandles.insertArguments(testFunctionObjectMH, 1, fn.getMethodInfo());
            target = fn.getConstructMethod();
        } else if (constructor instanceof BuiltinConstructor) {
            BuiltinConstructor fn = (BuiltinConstructor) constructor;
            test = MethodHandles.insertArguments(testBuiltinFunctionMH, 1, fn.getMethodInfo());
            target = fn.getConstructMethod();
        } else {
            target = test = null;
        }
        if (target != null) {
            // Insert constructor as newTarget argument.
            target = target.asType(target.type().changeParameterType(2, constructor.getClass()));
            target = MethodHandles.permuteArguments(target, target.type().dropParameterTypes(2, 3),
                    0, 1, 0, 2);
        }
        return setCallSiteTarget(callsite, target, test, constructGenericMH);
    }

    @SuppressWarnings("unused")
    private static ScriptObject constructGeneric(Object constructor,
            ExecutionContext callerContext, Object[] arguments) {
        return CheckConstructor(constructor, callerContext).construct(callerContext,
                (Constructor) constructor, arguments);
    }

    /**
     * Returns the invokedynamic instruction name for super() expressions.
     * 
     * @return the invokedynamic instruction name
     */
    public static String getSuperName() {
        return CallNames.SUPER;
    }

    /**
     * Returns the method descriptor for super() expressions.
     * 
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getSuperMethodDescriptor() {
        return Descriptors.SUPER;
    }

    /**
     * Returns the bootstrapping handle for super() expressions.
     * 
     * @return the bootstrapping handle
     */
    public static Handle getSuperBootstrap() {
        return BOOTSTRAP;
    }

    private static final MethodHandle superSetupMH;
    private static final MethodHandle superGenericMH;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        superGenericMH = lookup.findStatic("superGeneric", MethodType.methodType(
                ScriptObject.class, Constructor.class, ExecutionContext.class, Constructor.class,
                Object[].class));
        superSetupMH = lookup.findStatic("superSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Constructor.class, ExecutionContext.class,
                Constructor.class, Object[].class));
    }

    @SuppressWarnings("unused")
    private static MethodHandle superSetup(MutableCallSite callsite, Constructor constructor,
            ExecutionContext cx, Constructor newTarget, Object[] arguments) {
        MethodHandle target, test;
        if (constructor instanceof FunctionObject && constructor instanceof Constructor) {
            FunctionObject fn = (FunctionObject) constructor;
            test = MethodHandles.insertArguments(testFunctionObjectMH, 1, fn.getMethodInfo());
            target = fn.getConstructMethod();
        } else if (constructor instanceof BuiltinConstructor) {
            BuiltinConstructor fn = (BuiltinConstructor) constructor;
            test = MethodHandles.insertArguments(testBuiltinFunctionMH, 1, fn.getMethodInfo());
            target = fn.getConstructMethod();
        } else {
            target = test = null;
        }
        if (test != null) {
            test = test.asType(test.type().changeParameterType(0, Constructor.class));
        }
        return setCallSiteTarget(callsite, target, test, superGenericMH);
    }

    @SuppressWarnings("unused")
    private static ScriptObject superGeneric(Constructor constructor,
            ExecutionContext callerContext, Constructor newTarget, Object[] arguments) {
        return constructor.construct(callerContext, newTarget, arguments);
    }

    /**
     * Returns the invokedynamic instruction name for concat expressions.
     * 
     * @return the invokedynamic instruction name
     */
    public static String getConcatName() {
        return CallNames.CONCAT;
    }

    /**
     * Returns the method descriptor for concat expressions.
     * 
     * @param numberOfStrings
     *            the number of strings
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getConcatMethodDescriptor(int numberOfStrings) {
        Class<?>[] parameters = new Class<?>[numberOfStrings + 1];
        parameters[0] = ExecutionContext.class;
        Arrays.fill(parameters, 1, parameters.length, CharSequence.class);
        return MethodTypeDescriptor.methodType(CharSequence.class, parameters);
    }

    /**
     * Returns the bootstrapping handle for concat expressions.
     * 
     * @return the bootstrapping handle
     */
    public static Handle getConcatBootstrap() {
        return BOOTSTRAP;
    }

    private static final int MAX_STRING_SEGMENT_SIZE = 5;
    private static final int CONCAT_MIN_PARAMS = 2;
    private static final int CONCAT_MAX_SPECIALIZATION = 10;
    private static final MethodHandle[] testConcatMH;
    private static final MethodHandle[] concatConsMH;
    private static final MethodHandle[] concatMH;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        final int methods = CONCAT_MAX_SPECIALIZATION - CONCAT_MIN_PARAMS + 1;

        MethodHandle[] test = new MethodHandle[methods + 1];
        ArrayList<Class<?>> testParams = new ArrayList<>();
        testParams.add(CharSequence.class);
        for (int i = CONCAT_MIN_PARAMS; i <= CONCAT_MAX_SPECIALIZATION; ++i) {
            testParams.add(CharSequence.class);
            test[i - CONCAT_MIN_PARAMS] = dropContext(lookup.findStatic("testConcat",
                    MethodType.methodType(boolean.class, testParams)));
        }
        test[methods] = dropContext(lookup.findStatic("testConcat",
                MethodType.methodType(boolean.class, CharSequence[].class)));
        testConcatMH = test;

        MethodHandle[] cons = new MethodHandle[methods + 1];
        ArrayList<Class<?>> consParams = new ArrayList<>();
        consParams.add(ExecutionContext.class);
        consParams.add(CharSequence.class);
        for (int i = CONCAT_MIN_PARAMS; i <= CONCAT_MAX_SPECIALIZATION; ++i) {
            consParams.add(CharSequence.class);
            cons[i - CONCAT_MIN_PARAMS] = lookup.findStatic("concatCons",
                    MethodType.methodType(CharSequence.class, consParams));
        }
        cons[methods] = lookup.findStatic("concatCons", MethodType.methodType(CharSequence.class,
                ExecutionContext.class, CharSequence[].class));
        concatConsMH = cons;

        MethodHandle[] concat = new MethodHandle[methods + 1];
        ArrayList<Class<?>> concatParams = new ArrayList<>();
        concatParams.add(CharSequence.class);
        for (int i = CONCAT_MIN_PARAMS; i <= CONCAT_MAX_SPECIALIZATION; ++i) {
            concatParams.add(CharSequence.class);
            concat[i - CONCAT_MIN_PARAMS] = dropContext(lookup.findStatic("concat",
                    MethodType.methodType(CharSequence.class, concatParams)));
        }
        concat[methods] = dropContext(lookup.findStatic("concat",
                MethodType.methodType(CharSequence.class, CharSequence[].class)));
        concatMH = concat;
    }

    private static MethodHandle dropContext(MethodHandle mh) {
        return MethodHandles.dropArguments(mh, 0, ExecutionContext.class);
    }

    private static void concatSetup(MutableCallSite callsite, MethodType type) {
        MethodHandle target, test, generic;
        int numberOfStrings = type.parameterCount() - 1; // CharSequence..., ExecutionContext
        if (numberOfStrings <= CONCAT_MAX_SPECIALIZATION) {
            assert numberOfStrings >= CONCAT_MIN_PARAMS;
            int index = numberOfStrings - CONCAT_MIN_PARAMS;
            target = concatMH[index];
            test = testConcatMH[index];
            generic = concatConsMH[index];
            setCallSiteTarget(callsite, target, test, generic);
        } else {
            final int index = CONCAT_MAX_SPECIALIZATION - CONCAT_MIN_PARAMS + 1;
            target = concatMH[index].asCollector(CharSequence[].class, numberOfStrings);
            test = testConcatMH[index].asCollector(CharSequence[].class, numberOfStrings);
            generic = concatConsMH[index].asCollector(CharSequence[].class, numberOfStrings);
        }
        setCallSiteTarget(callsite, target, test, generic);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2) {
        return (s1.length() + s2.length()) <= (2 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3) {
        return (s1.length() + s2.length() + s3.length()) <= (3 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4) {
        return (s1.length() + s2.length() + s3.length() + s4.length()) <= (4 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        return n <= (5 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        n += s6.length();
        return n <= (6 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        n += s6.length();
        n += s7.length();
        return n <= (7 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        n += s6.length();
        n += s7.length();
        n += s8.length();
        return n <= (8 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8,
            CharSequence s9) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        n += s6.length();
        n += s7.length();
        n += s8.length();
        n += s9.length();
        return n <= (9 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8,
            CharSequence s9, CharSequence s10) {
        int n = s1.length();
        n += s2.length();
        n += s3.length();
        n += s4.length();
        n += s5.length();
        n += s6.length();
        n += s7.length();
        n += s8.length();
        n += s9.length();
        n += s10.length();
        return n <= (10 * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static boolean testConcat(CharSequence[] strings) {
        int n = 0;
        for (CharSequence charSequence : strings) {
            n += charSequence.length();
        }
        return n <= (strings.length * MAX_STRING_SEGMENT_SIZE);
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2) {
        return ScriptRuntime.add(s1, s2, cx);
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3) {
        return ScriptRuntime.add(ScriptRuntime.add(s1, s2, cx), s3, cx);
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4) {
        return ScriptRuntime.add(ScriptRuntime.add(ScriptRuntime.add(s1, s2, cx), s3, cx), s4, cx);
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5, CharSequence s6) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        s = ScriptRuntime.add(s, s6, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        s = ScriptRuntime.add(s, s6, cx);
        s = ScriptRuntime.add(s, s7, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7,
            CharSequence s8) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        s = ScriptRuntime.add(s, s6, cx);
        s = ScriptRuntime.add(s, s7, cx);
        s = ScriptRuntime.add(s, s8, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7,
            CharSequence s8, CharSequence s9) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        s = ScriptRuntime.add(s, s6, cx);
        s = ScriptRuntime.add(s, s7, cx);
        s = ScriptRuntime.add(s, s8, cx);
        s = ScriptRuntime.add(s, s9, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2,
            CharSequence s3, CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7,
            CharSequence s8, CharSequence s9, CharSequence s10) {
        CharSequence s = ScriptRuntime.add(s1, s2, cx);
        s = ScriptRuntime.add(s, s3, cx);
        s = ScriptRuntime.add(s, s4, cx);
        s = ScriptRuntime.add(s, s5, cx);
        s = ScriptRuntime.add(s, s6, cx);
        s = ScriptRuntime.add(s, s7, cx);
        s = ScriptRuntime.add(s, s8, cx);
        s = ScriptRuntime.add(s, s9, cx);
        s = ScriptRuntime.add(s, s10, cx);
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concatCons(ExecutionContext cx, CharSequence[] strings) {
        CharSequence s = "";
        for (CharSequence cs : strings) {
            s = ScriptRuntime.add(s, cs, cx);
        }
        return s;
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2) {
        int s1len = s1.length(), s2len = s2.length();
        if (s1len == 0) {
            return s2;
        }
        if (s2len == 0) {
            return s1;
        }
        char[] ca = new char[s1len + s2len];
        s1.toString().getChars(0, s1len, ca, 0);
        s2.toString().getChars(0, s2len, ca, s1len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length();
        char[] ca = new char[s1len + s2len + s3len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        char[] ca = new char[s1len + s2len + s3len + s4len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length(), s6len = s6.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        if (s6len != 0)
            s6.toString().getChars(0, s6len, ca, s1len + s2len + s3len + s4len + s5len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length(), s6len = s6.length(), s7len = s7.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        if (s6len != 0)
            s6.toString().getChars(0, s6len, ca, s1len + s2len + s3len + s4len + s5len);
        if (s7len != 0)
            s7.toString().getChars(0, s7len, ca, s1len + s2len + s3len + s4len + s5len + s6len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length(), s6len = s6.length(), s7len = s7.length(), s8len = s8.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        if (s6len != 0)
            s6.toString().getChars(0, s6len, ca, s1len + s2len + s3len + s4len + s5len);
        if (s7len != 0)
            s7.toString().getChars(0, s7len, ca, s1len + s2len + s3len + s4len + s5len + s6len);
        if (s8len != 0)
            s8.toString().getChars(0, s8len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8,
            CharSequence s9) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length(), s6len = s6.length(), s7len = s7.length(), s8len = s8.length();
        int s9len = s9.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len + s9len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        if (s6len != 0)
            s6.toString().getChars(0, s6len, ca, s1len + s2len + s3len + s4len + s5len);
        if (s7len != 0)
            s7.toString().getChars(0, s7len, ca, s1len + s2len + s3len + s4len + s5len + s6len);
        if (s8len != 0)
            s8.toString().getChars(0, s8len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len);
        if (s9len != 0)
            s9.toString().getChars(0, s9len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8,
            CharSequence s9, CharSequence s10) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int s5len = s5.length(), s6len = s6.length(), s7len = s7.length(), s8len = s8.length();
        int s9len = s9.length(), s10len = s10.length();
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len + s9len
                + s10len];
        if (s1len != 0)
            s1.toString().getChars(0, s1len, ca, 0);
        if (s2len != 0)
            s2.toString().getChars(0, s2len, ca, s1len);
        if (s3len != 0)
            s3.toString().getChars(0, s3len, ca, s1len + s2len);
        if (s4len != 0)
            s4.toString().getChars(0, s4len, ca, s1len + s2len + s3len);
        if (s5len != 0)
            s5.toString().getChars(0, s5len, ca, s1len + s2len + s3len + s4len);
        if (s6len != 0)
            s6.toString().getChars(0, s6len, ca, s1len + s2len + s3len + s4len + s5len);
        if (s7len != 0)
            s7.toString().getChars(0, s7len, ca, s1len + s2len + s3len + s4len + s5len + s6len);
        if (s8len != 0)
            s8.toString().getChars(0, s8len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len);
        if (s9len != 0)
            s9.toString().getChars(0, s9len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len);
        if (s10len != 0)
            s10.toString().getChars(0, s10len, ca,
                    s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len + s9len);
        return new String(ca);
    }

    @SuppressWarnings("unused")
    private static CharSequence concat(CharSequence[] strings) {
        StringBuilder sb = new StringBuilder(MAX_STRING_SEGMENT_SIZE * CONCAT_MAX_SPECIALIZATION);
        for (CharSequence s : strings) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Returns the invokedynamic instruction name for the given binary operator.
     * 
     * @param binary
     *            the binary operator
     * @return the invokedynamic instruction name
     */
    public static String getName(BinaryExpression.Operator binary) {
        switch (binary) {
        case ADD:
            return CallNames.ADD;
        case EQ:
            return CallNames.EQ;
        case SHEQ:
            return CallNames.SHEQ;
        case LT:
            return CallNames.LT;
        case GT:
            return CallNames.GT;
        case LE:
            return CallNames.LE;
        case GE:
            return CallNames.GE;
        case NE:
        case SHNE:
        default:
            throw new UnsupportedOperationException(binary.toString());
        }
    }

    /**
     * Returns the method descriptor for the given binary operator.
     * 
     * @param binary
     *            the binary operator
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getMethodDescriptor(BinaryExpression.Operator binary) {
        switch (binary) {
        case ADD:
            return Descriptors.ADD;
        case EQ:
            return Descriptors.EQ;
        case SHEQ:
            return Descriptors.STRICT_EQ;
        case LT:
        case GT:
        case LE:
        case GE:
            return Descriptors.CMP;
        case NE:
        case SHNE:
        default:
            throw new UnsupportedOperationException(binary.toString());
        }
    }

    /**
     * Returns the bootstrapping handle for the given binary operator.
     * 
     * @param binary
     *            the binary operator
     * @return the bootstrapping handle
     */
    public static Handle getBootstrap(BinaryExpression.Operator binary) {
        switch (binary) {
        case ADD:
        case EQ:
        case SHEQ:
        case LT:
        case GT:
        case LE:
        case GE:
            return BOOTSTRAP;
        case NE:
        case SHNE:
        default:
            throw new UnsupportedOperationException(binary.toString());
        }
    }

    private static final MethodHandle addSetupMH, relCmpSetupMH, eqCmpSetupMH, strictEqCmpSetupMH;
    private static final MethodHandle addStringMH, addNumberMH, addGenericMH;
    private static final MethodHandle relCmpStringMH, relCmpNumberMH, relCmpGenericMH;
    private static final MethodHandle lessThanMH, greaterThanMH, lessThanEqualsMH,
            greaterThanEqualsMH;
    private static final MethodHandle eqCmpStringMH, eqCmpNumberMH, eqCmpBooleanMH, eqCmpGenericMH;
    private static final MethodHandle strictEqCmpStringMH, strictEqCmpNumberMH,
            strictEqCmpBooleanMH, strictEqCmpGenericMH;
    private static final MethodHandle testStringMH, testNumberMH, testBooleanMH;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        testStringMH = lookup.findStatic("testString",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testNumberMH = lookup.findStatic("testNumber",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testBooleanMH = lookup.findStatic("testBoolean",
                MethodType.methodType(boolean.class, Object.class, Object.class));

        addStringMH = lookup.findStatic("addString", MethodType.methodType(CharSequence.class,
                CharSequence.class, CharSequence.class, ExecutionContext.class));
        MethodHandle addNumber = lookup.findStatic("addNumber",
                MethodType.methodType(Double.class, Number.class, Number.class));
        addNumberMH = MethodHandles.dropArguments(addNumber, 2, ExecutionContext.class);
        addGenericMH = lookup.findStatic("addGeneric", MethodType.methodType(Object.class,
                Object.class, Object.class, ExecutionContext.class));

        MethodHandle relCmpString = lookup.findStatic("relCmpString",
                MethodType.methodType(int.class, CharSequence.class, CharSequence.class));
        relCmpStringMH = MethodHandles.dropArguments(relCmpString, 2, ExecutionContext.class);
        MethodHandle relCmpNumber = lookup.findStatic("relCmpNumber",
                MethodType.methodType(int.class, Number.class, Number.class));
        relCmpNumberMH = MethodHandles.dropArguments(relCmpNumber, 2, ExecutionContext.class);
        relCmpGenericMH = lookup.findStatic("relCmpGeneric", MethodType.methodType(int.class,
                Object.class, Object.class, RelationalOperator.class, ExecutionContext.class));

        lessThanMH = lookup.findStatic("lessThan", MethodType.methodType(boolean.class, int.class));
        greaterThanMH = lookup.findStatic("greaterThan",
                MethodType.methodType(boolean.class, int.class));
        lessThanEqualsMH = lookup.findStatic("lessThanEquals",
                MethodType.methodType(boolean.class, int.class));
        greaterThanEqualsMH = lookup.findStatic("greaterThanEquals",
                MethodType.methodType(boolean.class, int.class));

        MethodHandle eqCmpString = lookup.findStatic("eqCmpString",
                MethodType.methodType(boolean.class, CharSequence.class, CharSequence.class));
        MethodHandle eqCmpNumber = lookup.findStatic("eqCmpNumber",
                MethodType.methodType(boolean.class, Number.class, Number.class));
        MethodHandle eqCmpBoolean = lookup.findStatic("eqCmpBoolean",
                MethodType.methodType(boolean.class, Boolean.class, Boolean.class));

        eqCmpStringMH = MethodHandles.dropArguments(eqCmpString, 2, ExecutionContext.class);
        eqCmpNumberMH = MethodHandles.dropArguments(eqCmpNumber, 2, ExecutionContext.class);
        eqCmpBooleanMH = MethodHandles.dropArguments(eqCmpBoolean, 2, ExecutionContext.class);
        eqCmpGenericMH = lookup.findStatic("eqCmpGeneric", MethodType.methodType(boolean.class,
                Object.class, Object.class, ExecutionContext.class));

        strictEqCmpStringMH = eqCmpString;
        strictEqCmpNumberMH = eqCmpNumber;
        strictEqCmpBooleanMH = eqCmpBoolean;
        strictEqCmpGenericMH = lookup.findStatic("strictEqCmpGeneric",
                MethodType.methodType(boolean.class, Object.class, Object.class));

        addSetupMH = lookup.findStatic("addSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Object.class, Object.class, ExecutionContext.class));
        relCmpSetupMH = lookup.findStatic("relCmpSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, RelationalOperator.class, Object.class, Object.class,
                ExecutionContext.class));
        eqCmpSetupMH = lookup.findStatic("eqCmpSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Object.class, Object.class, ExecutionContext.class));
        strictEqCmpSetupMH = lookup.findStatic("strictEqCmpSetup", MethodType.methodType(
                MethodHandle.class, MutableCallSite.class, Object.class, Object.class));
    }

    @SuppressWarnings("unused")
    private static CharSequence addString(CharSequence arg1, CharSequence arg2, ExecutionContext cx) {
        return ScriptRuntime.add(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static Double addNumber(Number arg1, Number arg2) {
        return arg1.doubleValue() + arg2.doubleValue();
    }

    @SuppressWarnings("unused")
    private static Object addGeneric(Object arg1, Object arg2, ExecutionContext cx) {
        return ScriptRuntime.add(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static int relCmpString(CharSequence arg1, CharSequence arg2) {
        int c = arg1.toString().compareTo(arg2.toString());
        return c < 0 ? 1 : 0;
    }

    @SuppressWarnings("unused")
    private static int relCmpNumber(Number arg1, Number arg2) {
        double nx = arg1.doubleValue();
        double ny = arg2.doubleValue();
        return Double.isNaN(nx) || Double.isNaN(ny) ? -1 : nx < ny ? 1 : 0;
    }

    @SuppressWarnings("unused")
    private static int relCmpGeneric(Object arg1, Object arg2, RelationalOperator op,
            ExecutionContext cx) {
        return RelationalComparison(cx, arg1, arg2, op.leftFirst());
    }

    enum RelationalOperator {
        LessThan, GreaterThan, LessThanEquals, GreaterThanEquals;

        boolean leftFirst() {
            switch (this) {
            case LessThan:
                return true;
            case GreaterThan:
                return false;
            case LessThanEquals:
                return false;
            case GreaterThanEquals:
                return true;
            default:
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    private static boolean lessThan(int result) {
        return result > 0;
    }

    @SuppressWarnings("unused")
    private static boolean greaterThan(int result) {
        return result > 0;
    }

    @SuppressWarnings("unused")
    private static boolean lessThanEquals(int result) {
        return result == 0;
    }

    @SuppressWarnings("unused")
    private static boolean greaterThanEquals(int result) {
        return result == 0;
    }

    @SuppressWarnings("unused")
    private static boolean eqCmpString(CharSequence arg1, CharSequence arg2) {
        return arg1.toString().equals(arg2.toString());
    }

    @SuppressWarnings("unused")
    private static boolean eqCmpNumber(Number arg1, Number arg2) {
        return arg1.doubleValue() == arg2.doubleValue();
    }

    @SuppressWarnings("unused")
    private static boolean eqCmpBoolean(Boolean arg1, Boolean arg2) {
        return arg1.booleanValue() == arg2.booleanValue();
    }

    @SuppressWarnings("unused")
    private static boolean eqCmpGeneric(Object arg1, Object arg2, ExecutionContext cx) {
        return EqualityComparison(cx, arg1, arg2);
    }

    @SuppressWarnings("unused")
    private static boolean strictEqCmpGeneric(Object arg1, Object arg2) {
        return StrictEqualityComparison(arg1, arg2);
    }

    private static boolean testString(Object arg1, Object arg2) {
        return Type.isString(arg1) && Type.isString(arg2);
    }

    private static boolean testNumber(Object arg1, Object arg2) {
        return Type.isNumber(arg1) && Type.isNumber(arg2);
    }

    private static boolean testBoolean(Object arg1, Object arg2) {
        return Type.isBoolean(arg1) && Type.isBoolean(arg2);
    }

    private static Type getType(Object arg1, Object arg2) {
        if (testString(arg1, arg2)) {
            return Type.String;
        }
        if (testNumber(arg1, arg2)) {
            return Type.Number;
        }
        if (testBoolean(arg1, arg2)) {
            return Type.Boolean;
        }
        return Type.Object;
    }

    private static MethodHandle getTestFor(Type type) {
        switch (type) {
        case Boolean:
            return testBooleanMH;
        case Number:
            return testNumberMH;
        case String:
            return testStringMH;
        default:
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static MethodHandle addSetup(MutableCallSite callsite, Object arg1, Object arg2,
            ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = addStringMH;
        } else if (type == Type.Number) {
            target = addNumberMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestFor(type), addGenericMH);
    }

    @SuppressWarnings("unused")
    private static MethodHandle relCmpSetup(MutableCallSite callsite, RelationalOperator op,
            Object arg1, Object arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = filterReturnValue(relCmpStringMH, op);
        } else if (type == Type.Number) {
            target = filterReturnValue(relCmpNumberMH, op);
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestFor(type),
                filterReturnValue(MethodHandles.insertArguments(relCmpGenericMH, 2, op), op));
    }

    private static MethodHandle filterReturnValue(MethodHandle mh, RelationalOperator op) {
        return MethodHandles.filterReturnValue(mh, returnFilter(op));
    }

    private static MethodHandle returnFilter(RelationalOperator op) {
        switch (op) {
        case LessThan:
            return lessThanMH;
        case GreaterThan:
            return greaterThanMH;
        case LessThanEquals:
            return lessThanEqualsMH;
        case GreaterThanEquals:
            return greaterThanEqualsMH;
        default:
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    private static MethodHandle eqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2,
            ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = eqCmpStringMH;
        } else if (type == Type.Number) {
            target = eqCmpNumberMH;
        } else if (type == Type.Boolean) {
            target = eqCmpBooleanMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestFor(type), eqCmpGenericMH);
    }

    @SuppressWarnings("unused")
    private static MethodHandle strictEqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = strictEqCmpStringMH;
        } else if (type == Type.Number) {
            target = strictEqCmpNumberMH;
        } else if (type == Type.Boolean) {
            target = strictEqCmpBooleanMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestFor(type), strictEqCmpGenericMH);
    }

    private static MethodHandle setCallSiteTarget(MutableCallSite callsite, MethodHandle target,
            MethodHandle test, MethodHandle generic) {
        MethodHandle callSiteTarget;
        if (target != null) {
            target = target.asType(callsite.type());
            if (test != null) {
                MethodHandle fallback = createFallback(callsite, generic);
                callSiteTarget = MethodHandles.guardWithTest(test, target, fallback);
            } else {
                callSiteTarget = target;
            }
        } else {
            callSiteTarget = target = generic;
        }
        callsite.setTarget(callSiteTarget);
        return target;
    }

    private static MethodHandle createFallback(MutableCallSite callsite, MethodHandle generic) {
        // only perform fallback to generic for now
        MethodHandle fallback = MethodHandles.insertArguments(switchToGenericMH, 0, callsite,
                generic);
        return setupCallSiteTarget(callsite.type(), fallback);
    }

    private static MethodHandle setupCallSiteTarget(MethodType type, MethodHandle target) {
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type), target);
    }

    private static final MethodHandle switchToGenericMH;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        switchToGenericMH = lookup.findStatic("switchToGeneric", MethodType.methodType(
                MethodHandle.class, MutableCallSite.class, MethodHandle.class));
    }

    @SuppressWarnings("unused")
    private static MethodHandle switchToGeneric(MutableCallSite callsite, MethodHandle generic) {
        callsite.setTarget(generic);
        return generic;
    }

    private static final ConstantCallSite stackOverFlow_Add;
    private static final ConstantCallSite stackOverFlow_Cmp;
    private static final ConstantCallSite stackOverFlow_Eq;
    private static final ConstantCallSite stackOverFlow_StrictEq;
    private static final ConstantCallSite stackOverFlow_Call;
    private static final ConstantCallSite stackOverFlow_Construct;
    private static final ConstantCallSite stackOverFlow_Super;
    private static final MethodHandle stackOverFlow_Concat;
    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        stackOverFlow_Add = new ConstantCallSite(lookup.findStatic("stackOverFlow_Add", MethodType
                .methodType(Object.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_Cmp = new ConstantCallSite(lookup.findStatic("stackOverFlow_Cmp", MethodType
                .methodType(int.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_Eq = new ConstantCallSite(lookup.findStatic("stackOverFlow_Eq", MethodType
                .methodType(boolean.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_StrictEq = new ConstantCallSite(lookup.findStatic("stackOverFlow_StrictEq",
                MethodType.methodType(boolean.class, Object.class, Object.class)));
        stackOverFlow_Call = new ConstantCallSite(lookup.findStatic("stackOverFlow_Call",
                MethodType.methodType(Object.class, Object.class, ExecutionContext.class,
                        Object.class, Object[].class)));
        stackOverFlow_Construct = new ConstantCallSite(lookup.findStatic("stackOverFlow_Construct",
                MethodType.methodType(ScriptObject.class, Object.class, ExecutionContext.class,
                        Object[].class)));
        stackOverFlow_Super = new ConstantCallSite(lookup.findStatic("stackOverFlow_Super",
                MethodType.methodType(ScriptObject.class, Constructor.class,
                        ExecutionContext.class, Constructor.class, Object[].class)));
        stackOverFlow_Concat = lookup.findStatic("stackOverFlow_Concat",
                MethodType.methodType(CharSequence.class));
    }

    @SuppressWarnings("unused")
    private static Object stackOverFlow_Add(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static int stackOverFlow_Cmp(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static boolean stackOverFlow_Eq(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static boolean stackOverFlow_StrictEq(Object arg1, Object arg2) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static Object stackOverFlow_Call(Object fun, ExecutionContext cx, Object thisValue,
            Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static ScriptObject stackOverFlow_Construct(Object constructor, ExecutionContext cx,
            Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static ScriptObject stackOverFlow_Super(Constructor constructor, ExecutionContext cx,
            Constructor newTarget, Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    @SuppressWarnings("unused")
    private static CharSequence stackOverFlow_Concat() {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    /**
     * The invokedynamic bootstrapping method.
     * 
     * @param caller
     *            the caller lookup
     * @param name
     *            the instruction name
     * @param type
     *            the expected method type
     * @return the invokedynamic call-site object
     */
    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name,
            MethodType type) {
        // System.out.printf("caller: %s\n", caller);
        // System.out.printf("name: %s\n", name);
        // System.out.printf("type: %s\n", type);
        try {
            MutableCallSite callsite = new MutableCallSite(type);

            MethodHandle setup;
            switch (name) {
            case CallNames.CALL:
                setup = MethodHandles.insertArguments(callSetupMH, 0, callsite);
                break;
            case CallNames.CONSTRUCT:
                setup = MethodHandles.insertArguments(constructSetupMH, 0, callsite);
                break;
            case CallNames.SUPER:
                setup = MethodHandles.insertArguments(superSetupMH, 0, callsite);
                break;
            case CallNames.ADD:
                setup = MethodHandles.insertArguments(addSetupMH, 0, callsite);
                break;
            case CallNames.EQ:
                setup = MethodHandles.insertArguments(eqCmpSetupMH, 0, callsite);
                break;
            case CallNames.SHEQ:
                setup = MethodHandles.insertArguments(strictEqCmpSetupMH, 0, callsite);
                break;
            case CallNames.LT:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite,
                        RelationalOperator.LessThan);
                break;
            case CallNames.GT:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite,
                        RelationalOperator.GreaterThan);
                break;
            case CallNames.LE:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite,
                        RelationalOperator.LessThanEquals);
                break;
            case CallNames.GE:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite,
                        RelationalOperator.GreaterThanEquals);
                break;
            case CallNames.CONCAT:
                concatSetup(callsite, type);
                return callsite;
            default:
                throw new IllegalArgumentException(name);
            }

            callsite.setTarget(setupCallSiteTarget(type, setup));
            return callsite;
        } catch (StackOverflowError e) {
            switch (name) {
            case CallNames.CALL:
                return stackOverFlow_Call;
            case CallNames.CONSTRUCT:
                return stackOverFlow_Construct;
            case CallNames.SUPER:
                return stackOverFlow_Super;
            case CallNames.CONCAT:
                return new ConstantCallSite(MethodHandles.dropArguments(stackOverFlow_Concat, 0,
                        type.parameterArray()));
            case CallNames.ADD:
                return stackOverFlow_Add;
            case CallNames.EQ:
                return stackOverFlow_Eq;
            case CallNames.SHEQ:
                return stackOverFlow_StrictEq;
            case CallNames.LT:
            case CallNames.GT:
            case CallNames.LE:
            case CallNames.GE:
                return stackOverFlow_Cmp;
            default:
                throw new IllegalArgumentException(name);
            }
        }
    }
}

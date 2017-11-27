/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.EqualityComparison;
import static com.github.anba.es6draft.runtime.AbstractOperations.RelationalComparison;
import static com.github.anba.es6draft.runtime.AbstractOperations.StrictEqualityComparison;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.language.CallOperations.CheckCallable;
import static com.github.anba.es6draft.runtime.language.CallOperations.CheckConstructor;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.ast.UnaryExpression;
import com.github.anba.es6draft.ast.UpdateExpression;
import com.github.anba.es6draft.compiler.assembler.Handle;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.language.Operators;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 *
 */
@SuppressWarnings("unused")
public final class Bootstrap {
    private Bootstrap() {
    }

    private static final class CallNames {
        static final String CALL = "expression::call";
        static final String CONSTRUCT = "expression::construct";
        static final String SUPER = "expression::super";
        static final String CONCAT = "expression::concat";
        static final String ADD = "expression::add";
        static final String SUB = "expression::sub";
        static final String EXP = "expression::exp";
        static final String MUL = "expression::mul";
        static final String DIV = "expression::div";
        static final String MOD = "expression::mod";
        static final String SHL = "expression::shl";
        static final String SHR = "expression::shr";
        static final String USHR = "expression::ushr";
        static final String EQ = "expression::equals";
        static final String SHEQ = "expression::strictEquals";
        static final String LT = "expression::lessThan";
        static final String GT = "expression::greaterThan";
        static final String LE = "expression::lessThanEquals";
        static final String GE = "expression::greaterThanEquals";
        static final String BITAND = "expression::bitand";
        static final String BITOR = "expression::bitor";
        static final String BITXOR = "expression::bitxor";
        static final String BITNOT = "expression::bitnot";
        static final String NEG = "expression::neg";
        static final String INC = "expression::inc";
        static final String DEC = "expression::dec";
    }

    private static final class Descriptors {
        static final MethodTypeDescriptor ADD = MethodTypeDescriptor.methodType(Object.class, Object.class,
                Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor BINARYNUMBER = MethodTypeDescriptor.methodType(Number.class, Number.class,
                Number.class, ExecutionContext.class);
        static final MethodTypeDescriptor UNARYNUMBER = MethodTypeDescriptor.methodType(Number.class, Number.class);
        static final MethodTypeDescriptor CMP = MethodTypeDescriptor.methodType(boolean.class, Object.class,
                Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor EQ = MethodTypeDescriptor.methodType(boolean.class, Object.class,
                Object.class, ExecutionContext.class);
        static final MethodTypeDescriptor STRICT_EQ = MethodTypeDescriptor.methodType(boolean.class, Object.class,
                Object.class);
        static final MethodTypeDescriptor CALL = MethodTypeDescriptor.methodType(Object.class, Object.class,
                ExecutionContext.class, Object.class, Object[].class);
        static final MethodTypeDescriptor CONSTRUCT = MethodTypeDescriptor.methodType(ScriptObject.class, Object.class,
                ExecutionContext.class, Object[].class);
        static final MethodTypeDescriptor SUPER = MethodTypeDescriptor.methodType(ScriptObject.class, Constructor.class,
                ExecutionContext.class, Constructor.class, Object[].class);
    }

    private static final Handle BOOTSTRAP;

    static {
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                MethodType.class);
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
        callGenericMH = lookup.findStatic("callGeneric", MethodType.methodType(Object.class, Object.class,
                ExecutionContext.class, Object.class, Object[].class));
        callSetupMH = lookup.findStatic("callSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Object.class, ExecutionContext.class, Object.class, Object[].class));
    }

    private static MethodHandle callSetup(MutableCallSite callsite, Object function, ExecutionContext cx,
            Object thisValue, Object[] arguments) {
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

    private static boolean testFunctionObject(Object function, Object methodInfo) {
        return function instanceof FunctionObject && ((FunctionObject) function).getMethodInfo() == methodInfo;
    }

    private static boolean testBuiltinFunction(Object function, Object methodInfo) {
        return function instanceof BuiltinFunction && ((BuiltinFunction) function).getMethodInfo() == methodInfo;
    }

    private static Object callGeneric(Object function, ExecutionContext callerContext, Object thisValue,
            Object[] arguments) {
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
        constructGenericMH = lookup.findStatic("constructGeneric",
                MethodType.methodType(ScriptObject.class, Object.class, ExecutionContext.class, Object[].class));
        constructSetupMH = lookup.findStatic("constructSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Object.class, ExecutionContext.class, Object[].class));
    }

    private static MethodHandle constructSetup(MutableCallSite callsite, Object constructor, ExecutionContext cx,
            Object[] arguments) {
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
            target = MethodHandles.permuteArguments(target, target.type().dropParameterTypes(2, 3), 0, 1, 0, 2);
        }
        return setCallSiteTarget(callsite, target, test, constructGenericMH);
    }

    private static ScriptObject constructGeneric(Object constructor, ExecutionContext callerContext,
            Object[] arguments) {
        return CheckConstructor(constructor, callerContext).construct(callerContext, arguments);
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
        superGenericMH = lookup.findStatic("superGeneric", MethodType.methodType(ScriptObject.class, Constructor.class,
                ExecutionContext.class, Constructor.class, Object[].class));
        superSetupMH = lookup.findStatic("superSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Constructor.class, ExecutionContext.class, Constructor.class, Object[].class));
    }

    private static MethodHandle superSetup(MutableCallSite callsite, Constructor constructor, ExecutionContext cx,
            Constructor newTarget, Object[] arguments) {
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

    private static ScriptObject superGeneric(Constructor constructor, ExecutionContext callerContext,
            Constructor newTarget, Object[] arguments) {
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
            test[i - CONCAT_MIN_PARAMS] = dropContext(
                    lookup.findStatic("testConcat", MethodType.methodType(boolean.class, testParams)));
        }
        test[methods] = dropContext(
                lookup.findStatic("testConcat", MethodType.methodType(boolean.class, CharSequence[].class)));
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
        cons[methods] = lookup.findStatic("concatCons",
                MethodType.methodType(CharSequence.class, ExecutionContext.class, CharSequence[].class));
        concatConsMH = cons;

        MethodHandle[] concat = new MethodHandle[methods + 1];
        ArrayList<Class<?>> concatParams = new ArrayList<>();
        concatParams.add(CharSequence.class);
        for (int i = CONCAT_MIN_PARAMS; i <= CONCAT_MAX_SPECIALIZATION; ++i) {
            concatParams.add(CharSequence.class);
            concat[i - CONCAT_MIN_PARAMS] = dropContext(
                    lookup.findStatic("concat", MethodType.methodType(CharSequence.class, concatParams)));
        }
        concat[methods] = dropContext(
                lookup.findStatic("concat", MethodType.methodType(CharSequence.class, CharSequence[].class)));
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
        } else {
            final int index = CONCAT_MAX_SPECIALIZATION - CONCAT_MIN_PARAMS + 1;
            target = concatMH[index].asCollector(CharSequence[].class, numberOfStrings);
            test = testConcatMH[index].asCollector(CharSequence[].class, numberOfStrings);
            generic = concatConsMH[index].asCollector(CharSequence[].class, numberOfStrings);
        }
        setCallSiteTarget(callsite, target, test, generic);
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2) {
        /* @formatter:off */
        int n = s1.length() + s2.length();
        if (n < 0) return false;
        return n <= (2 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        return n <= (3 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        return n <= (4 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        return n <= (5 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        n += s6.length();
        if (n < 0) return false;
        return n <= (6 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        n += s6.length();
        if (n < 0) return false;
        n += s7.length();
        if (n < 0) return false;
        return n <= (7 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        n += s6.length();
        if (n < 0) return false;
        n += s7.length();
        if (n < 0) return false;
        n += s8.length();
        if (n < 0) return false;
        return n <= (8 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        n += s6.length();
        if (n < 0) return false;
        n += s7.length();
        if (n < 0) return false;
        n += s8.length();
        if (n < 0) return false;
        n += s9.length();
        if (n < 0) return false;
        return n <= (9 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9, CharSequence s10) {
        /* @formatter:off */
        int n = s1.length();
        n += s2.length();
        if (n < 0) return false;
        n += s3.length();
        if (n < 0) return false;
        n += s4.length();
        if (n < 0) return false;
        n += s5.length();
        if (n < 0) return false;
        n += s6.length();
        if (n < 0) return false;
        n += s7.length();
        if (n < 0) return false;
        n += s8.length();
        if (n < 0) return false;
        n += s9.length();
        if (n < 0) return false;
        n += s10.length();
        if (n < 0) return false;
        return n <= (10 * MAX_STRING_SEGMENT_SIZE);
        /* @formatter:on */
    }

    private static boolean testConcat(CharSequence[] strings) {
        int n = 0;
        for (CharSequence charSequence : strings) {
            n += charSequence.length();
            if (n < 0) {
                return false;
            }
        }
        return n <= (strings.length * MAX_STRING_SEGMENT_SIZE);
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2) {
        return Operators.add(s1, s2, cx);
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        s = Operators.add(s, s6, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        s = Operators.add(s, s6, cx);
        s = Operators.add(s, s7, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        s = Operators.add(s, s6, cx);
        s = Operators.add(s, s7, cx);
        s = Operators.add(s, s8, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        s = Operators.add(s, s6, cx);
        s = Operators.add(s, s7, cx);
        s = Operators.add(s, s8, cx);
        s = Operators.add(s, s9, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence s1, CharSequence s2, CharSequence s3,
            CharSequence s4, CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9,
            CharSequence s10) {
        CharSequence s = Operators.add(s1, s2, cx);
        s = Operators.add(s, s3, cx);
        s = Operators.add(s, s4, cx);
        s = Operators.add(s, s5, cx);
        s = Operators.add(s, s6, cx);
        s = Operators.add(s, s7, cx);
        s = Operators.add(s, s8, cx);
        s = Operators.add(s, s9, cx);
        s = Operators.add(s, s10, cx);
        return s;
    }

    private static CharSequence concatCons(ExecutionContext cx, CharSequence[] strings) {
        CharSequence s = "";
        for (CharSequence cs : strings) {
            s = Operators.add(s, cs, cx);
        }
        return s;
    }

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

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int s6len = s6.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        if (s6len != 0) { s6.toString().getChars(0, s6len, ca, offset); offset += s6len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int s6len = s6.length(), s7len = s7.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        if (s6len != 0) { s6.toString().getChars(0, s6len, ca, offset); offset += s6len; }
        if (s7len != 0) { s7.toString().getChars(0, s7len, ca, offset); offset += s7len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int s6len = s6.length(), s7len = s7.length(), s8len = s8.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        if (s6len != 0) { s6.toString().getChars(0, s6len, ca, offset); offset += s6len; }
        if (s7len != 0) { s7.toString().getChars(0, s7len, ca, offset); offset += s7len; }
        if (s8len != 0) { s8.toString().getChars(0, s8len, ca, offset); offset += s8len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int s6len = s6.length(), s7len = s7.length(), s8len = s8.length(), s9len = s9.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len + s9len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        if (s6len != 0) { s6.toString().getChars(0, s6len, ca, offset); offset += s6len; }
        if (s7len != 0) { s7.toString().getChars(0, s7len, ca, offset); offset += s7len; }
        if (s8len != 0) { s8.toString().getChars(0, s8len, ca, offset); offset += s8len; }
        if (s9len != 0) { s9.toString().getChars(0, s9len, ca, offset); offset += s9len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence s1, CharSequence s2, CharSequence s3, CharSequence s4,
            CharSequence s5, CharSequence s6, CharSequence s7, CharSequence s8, CharSequence s9, CharSequence s10) {
        int s1len = s1.length(), s2len = s2.length(), s3len = s3.length(), s4len = s4.length(), s5len = s5.length();
        int s6len = s6.length(), s7len = s7.length(), s8len = s8.length(), s9len = s9.length(), s10len = s10.length();
        int offset = 0;
        char[] ca = new char[s1len + s2len + s3len + s4len + s5len + s6len + s7len + s8len + s9len + s10len];
        /* @formatter:off */
        if (s1len != 0) { s1.toString().getChars(0, s1len, ca, offset); offset += s1len; }
        if (s2len != 0) { s2.toString().getChars(0, s2len, ca, offset); offset += s2len; }
        if (s3len != 0) { s3.toString().getChars(0, s3len, ca, offset); offset += s3len; }
        if (s4len != 0) { s4.toString().getChars(0, s4len, ca, offset); offset += s4len; }
        if (s5len != 0) { s5.toString().getChars(0, s5len, ca, offset); offset += s5len; }
        if (s6len != 0) { s6.toString().getChars(0, s6len, ca, offset); offset += s6len; }
        if (s7len != 0) { s7.toString().getChars(0, s7len, ca, offset); offset += s7len; }
        if (s8len != 0) { s8.toString().getChars(0, s8len, ca, offset); offset += s8len; }
        if (s9len != 0) { s9.toString().getChars(0, s9len, ca, offset); offset += s9len; }
        if (s10len != 0) { s10.toString().getChars(0, s10len, ca, offset); offset += s10len; }
        /* @formatter:on */
        return new String(ca);
    }

    private static CharSequence concat(CharSequence[] strings) {
        StringBuilder sb = new StringBuilder(MAX_STRING_SEGMENT_SIZE * CONCAT_MAX_SPECIALIZATION);
        for (CharSequence s : strings) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Returns the invokedynamic instruction name for the given update operator.
     * 
     * @param update
     *            the update operator
     * @return the invokedynamic instruction name
     */
    public static String getName(UpdateExpression.Operator update) {
        switch (update) {
        case POST_DEC:
        case PRE_DEC:
            return CallNames.DEC;
        case POST_INC:
        case PRE_INC:
            return CallNames.INC;
        default:
            throw new UnsupportedOperationException(update.toString());
        }
    }

    /**
     * Returns the method descriptor for the given update operator.
     * 
     * @param update
     *            the update operator
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getMethodDescriptor(UpdateExpression.Operator update) {
        switch (update) {
        case POST_DEC:
        case PRE_DEC:
        case POST_INC:
        case PRE_INC:
            return Descriptors.UNARYNUMBER;
        default:
            throw new UnsupportedOperationException(update.toString());
        }
    }

    /**
     * Returns the bootstrapping handle for the given update operator.
     * 
     * @param update
     *            the update operator
     * @return the bootstrapping handle
     */
    public static Handle getBootstrap(UpdateExpression.Operator update) {
        switch (update) {
        case POST_DEC:
        case PRE_DEC:
        case POST_INC:
        case PRE_INC:
            return BOOTSTRAP;
        default:
            throw new UnsupportedOperationException(update.toString());
        }
    }

    /**
     * Returns the invokedynamic instruction name for the given unary operator.
     * 
     * @param unary
     *            the unary operator
     * @return the invokedynamic instruction name
     */
    public static String getName(UnaryExpression.Operator unary) {
        switch (unary) {
        case BITNOT:
            return CallNames.BITNOT;
        case NEG:
            return CallNames.NEG;
        default:
            throw new UnsupportedOperationException(unary.toString());
        }
    }

    /**
     * Returns the method descriptor for the given unary operator.
     * 
     * @param unary
     *            the unary operator
     * @return the method descriptor
     */
    public static MethodTypeDescriptor getMethodDescriptor(UnaryExpression.Operator unary) {
        switch (unary) {
        case BITNOT:
        case NEG:
            return Descriptors.UNARYNUMBER;
        default:
            throw new UnsupportedOperationException(unary.toString());
        }
    }

    /**
     * Returns the bootstrapping handle for the given unary operator.
     * 
     * @param unary
     *            the unary operator
     * @return the bootstrapping handle
     */
    public static Handle getBootstrap(UnaryExpression.Operator unary) {
        switch (unary) {
        case NEG:
        case BITNOT:
            return BOOTSTRAP;
        default:
            throw new UnsupportedOperationException(unary.toString());
        }
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
        case SUB:
            return CallNames.SUB;
        case EXP:
            return CallNames.EXP;
        case MUL:
            return CallNames.MUL;
        case DIV:
            return CallNames.DIV;
        case MOD:
            return CallNames.MOD;
        case SHL:
            return CallNames.SHL;
        case SHR:
            return CallNames.SHR;
        case USHR:
            return CallNames.USHR;
        case BITAND:
            return CallNames.BITAND;
        case BITOR:
            return CallNames.BITOR;
        case BITXOR:
            return CallNames.BITXOR;
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
        case SUB:
        case EXP:
        case MUL:
        case DIV:
        case MOD:
        case SHL:
        case SHR:
        case USHR:
        case BITAND:
        case BITOR:
        case BITXOR:
            return Descriptors.BINARYNUMBER;
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
        case SUB:
        case EXP:
        case MUL:
        case DIV:
        case MOD:
        case SHL:
        case SHR:
        case USHR:
        case BITAND:
        case BITOR:
        case BITXOR:
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

    private static final MethodHandle relCmpSetupMH, relCmpStringMH, relCmpNumberMH, relCmpGenericMH;
    private static final MethodHandle lessThanMH, greaterThanMH, lessThanEqualsMH, greaterThanEqualsMH;
    private static final MethodHandle eqCmpSetupMH, eqCmpStringMH, eqCmpNumberMH, eqCmpBooleanMH, eqCmpGenericMH;
    private static final MethodHandle strictEqCmpSetupMH, strictEqCmpStringMH, strictEqCmpNumberMH,
            strictEqCmpBooleanMH, strictEqCmpGenericMH;

    private static final MethodHandle addSetupMH, addStringMH, addNumberMH, addGenericMH;
    private static final MethodHandle subSetupMH, subNumberMH, subBigIntMH, subGenericMH;
    private static final MethodHandle expSetupMH, expNumberMH, expBigIntMH, expGenericMH;
    private static final MethodHandle mulSetupMH, mulNumberMH, mulBigIntMH, mulGenericMH;
    private static final MethodHandle divSetupMH, divNumberMH, divBigIntMH, divGenericMH;
    private static final MethodHandle modSetupMH, modNumberMH, modBigIntMH, modGenericMH;

    private static final MethodHandle shlSetupMH, shlNumberMH, shlBigIntMH, shlGenericMH;
    private static final MethodHandle shrSetupMH, shrNumberMH, shrBigIntMH, shrGenericMH;
    private static final MethodHandle ushrSetupMH, ushrNumberMH, ushrBigIntMH, ushrGenericMH;

    private static final MethodHandle bitAndSetupMH, bitAndNumberMH, bitAndBigIntMH, bitAndGenericMH;
    private static final MethodHandle bitOrSetupMH, bitOrNumberMH, bitOrBigIntMH, bitOrGenericMH;
    private static final MethodHandle bitXorSetupMH, bitXorNumberMH, bitXorBigIntMH, bitXorGenericMH;

    private static final MethodHandle bitNotSetupMH, bitNotNumberMH, bitNotBigIntMH, bitNotGenericMH;
    private static final MethodHandle negSetupMH, negNumberMH, negBigIntMH, negGenericMH;

    private static final MethodHandle incSetupMH, incNumberMH, incBigIntMH, incGenericMH;
    private static final MethodHandle decSetupMH, decNumberMH, decBigIntMH, decGenericMH;

    private static final MethodHandle testStringMH, testNumberMH, testBooleanMH, testBigIntMH;
    private static final MethodHandle testBinaryNumberMH, testBinaryBigIntMH;
    private static final MethodHandle testUnaryNumberMH, testUnaryBigIntMH;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        testStringMH = lookup.findStatic("testString",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testNumberMH = lookup.findStatic("testNumber",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testBooleanMH = lookup.findStatic("testBoolean",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        testBigIntMH = lookup.findStatic("testBigInt",
                MethodType.methodType(boolean.class, Object.class, Object.class));

        testBinaryNumberMH = lookup.findStatic("testNumber",
                MethodType.methodType(boolean.class, Number.class, Number.class));
        testBinaryBigIntMH = lookup.findStatic("testBigInt",
                MethodType.methodType(boolean.class, Number.class, Number.class));

        testUnaryNumberMH = lookup.findStatic("testNumber", MethodType.methodType(boolean.class, Number.class));
        testUnaryBigIntMH = lookup.findStatic("testBigInt", MethodType.methodType(boolean.class, Number.class));

        // Relational comparison operator.
        relCmpSetupMH = lookup.findStatic("relCmpSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, RelationalOperator.class, Object.class, Object.class, ExecutionContext.class));
        MethodHandle relCmpString = lookup.findStatic("relCmpString",
                MethodType.methodType(int.class, CharSequence.class, CharSequence.class));
        relCmpStringMH = MethodHandles.dropArguments(relCmpString, 2, ExecutionContext.class);
        MethodHandle relCmpNumber = lookup.findStatic("relCmpNumber",
                MethodType.methodType(int.class, Number.class, Number.class));
        relCmpNumberMH = MethodHandles.dropArguments(relCmpNumber, 2, ExecutionContext.class);
        relCmpGenericMH = lookup.findStatic("relCmpGeneric", MethodType.methodType(int.class, Object.class,
                Object.class, RelationalOperator.class, ExecutionContext.class));

        lessThanMH = lookup.findStatic("lessThan", MethodType.methodType(boolean.class, int.class));
        greaterThanMH = lookup.findStatic("greaterThan", MethodType.methodType(boolean.class, int.class));
        lessThanEqualsMH = lookup.findStatic("lessThanEquals", MethodType.methodType(boolean.class, int.class));
        greaterThanEqualsMH = lookup.findStatic("greaterThanEquals", MethodType.methodType(boolean.class, int.class));

        // Shared equality comparison operators.
        MethodHandle eqCmpString = lookup.findStatic("eqCmpString",
                MethodType.methodType(boolean.class, CharSequence.class, CharSequence.class));
        MethodHandle eqCmpNumber = lookup.findStatic("eqCmpNumber",
                MethodType.methodType(boolean.class, Number.class, Number.class));
        MethodHandle eqCmpBoolean = lookup.findStatic("eqCmpBoolean",
                MethodType.methodType(boolean.class, Boolean.class, Boolean.class));

        // Equality operator.
        eqCmpSetupMH = lookup.findStatic("eqCmpSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Object.class, Object.class, ExecutionContext.class));
        eqCmpStringMH = MethodHandles.dropArguments(eqCmpString, 2, ExecutionContext.class);
        eqCmpNumberMH = MethodHandles.dropArguments(eqCmpNumber, 2, ExecutionContext.class);
        eqCmpBooleanMH = MethodHandles.dropArguments(eqCmpBoolean, 2, ExecutionContext.class);
        eqCmpGenericMH = lookup.findStatic("eqCmpGeneric",
                MethodType.methodType(boolean.class, Object.class, Object.class, ExecutionContext.class));

        // Strict-Equality operator.
        strictEqCmpSetupMH = lookup.findStatic("strictEqCmpSetup",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, Object.class, Object.class));
        strictEqCmpStringMH = eqCmpString;
        strictEqCmpNumberMH = eqCmpNumber;
        strictEqCmpBooleanMH = eqCmpBoolean;
        strictEqCmpGenericMH = lookup.findStatic("strictEqCmpGeneric",
                MethodType.methodType(boolean.class, Object.class, Object.class));

        // Addition operator.
        addSetupMH = lookup.findStatic("addSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Object.class, Object.class, ExecutionContext.class));
        addStringMH = lookup.findStatic("addString", MethodType.methodType(CharSequence.class, CharSequence.class,
                CharSequence.class, ExecutionContext.class));
        MethodHandle addNumber = lookup.findStatic("addNumber",
                MethodType.methodType(Double.class, Number.class, Number.class));
        addNumberMH = MethodHandles.dropArguments(addNumber, 2, ExecutionContext.class);
        addGenericMH = lookup.findStatic("addGeneric",
                MethodType.methodType(Object.class, Object.class, Object.class, ExecutionContext.class));

        // Subtraction operator.
        subSetupMH = lookup.findStatic("subSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        subNumberMH = lookup.findStatic("subNumber",
                MethodType.methodType(Double.class, Double.class, Double.class, ExecutionContext.class));
        subBigIntMH = lookup.findStatic("subBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        subGenericMH = lookup.findStatic("subGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Exponentiation operator.
        expSetupMH = lookup.findStatic("expSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        expNumberMH = lookup.findStatic("expNumber",
                MethodType.methodType(Double.class, Double.class, Double.class, ExecutionContext.class));
        expBigIntMH = lookup.findStatic("expBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        expGenericMH = lookup.findStatic("expGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Multiplication operator.
        mulSetupMH = lookup.findStatic("mulSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        mulNumberMH = lookup.findStatic("mulNumber",
                MethodType.methodType(Double.class, Double.class, Double.class, ExecutionContext.class));
        mulBigIntMH = lookup.findStatic("mulBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        mulGenericMH = lookup.findStatic("mulGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Division operator.
        divSetupMH = lookup.findStatic("divSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        divNumberMH = lookup.findStatic("divNumber",
                MethodType.methodType(Double.class, Double.class, Double.class, ExecutionContext.class));
        divBigIntMH = lookup.findStatic("divBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        divGenericMH = lookup.findStatic("divGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Modulus operator.
        modSetupMH = lookup.findStatic("modSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        modNumberMH = lookup.findStatic("modNumber",
                MethodType.methodType(Double.class, Double.class, Double.class, ExecutionContext.class));
        modBigIntMH = lookup.findStatic("modBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        modGenericMH = lookup.findStatic("modGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Signed shift-left operator.
        shlSetupMH = lookup.findStatic("shlSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        shlNumberMH = lookup.findStatic("shlNumber",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, ExecutionContext.class));
        shlBigIntMH = lookup.findStatic("shlBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        shlGenericMH = lookup.findStatic("shlGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Signed shift-right operator.
        shrSetupMH = lookup.findStatic("shrSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        shrNumberMH = lookup.findStatic("shrNumber",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, ExecutionContext.class));
        shrBigIntMH = lookup.findStatic("shrBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        shrGenericMH = lookup.findStatic("shrGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // Unsigned shift-right operator.
        ushrSetupMH = lookup.findStatic("ushrSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        ushrNumberMH = lookup.findStatic("ushrNumber",
                MethodType.methodType(Long.class, Integer.class, Integer.class, ExecutionContext.class));
        ushrBigIntMH = lookup.findStatic("ushrBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        ushrGenericMH = lookup.findStatic("ushrGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // BitAnd operator.
        bitAndSetupMH = lookup.findStatic("bitAndSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Number.class, Number.class, ExecutionContext.class));
        bitAndNumberMH = lookup.findStatic("bitAndNumber",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, ExecutionContext.class));
        bitAndBigIntMH = lookup.findStatic("bitAndBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        bitAndGenericMH = lookup.findStatic("bitAndGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // BitOr operator.
        bitOrSetupMH = lookup.findStatic("bitOrSetup", MethodType.methodType(MethodHandle.class, MutableCallSite.class,
                Number.class, Number.class, ExecutionContext.class));
        bitOrNumberMH = lookup.findStatic("bitOrNumber",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, ExecutionContext.class));
        bitOrBigIntMH = lookup.findStatic("bitOrBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        bitOrGenericMH = lookup.findStatic("bitOrGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // BitXor operator.
        bitXorSetupMH = lookup.findStatic("bitXorSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Number.class, Number.class, ExecutionContext.class));
        bitXorNumberMH = lookup.findStatic("bitXorNumber",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, ExecutionContext.class));
        bitXorBigIntMH = lookup.findStatic("bitXorBigInt",
                MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class, ExecutionContext.class));
        bitXorGenericMH = lookup.findStatic("bitXorGeneric",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class));

        // BitNot operator.
        bitNotSetupMH = lookup.findStatic("bitNotSetup",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, Number.class));
        bitNotNumberMH = lookup.findStatic("bitNotNumber", MethodType.methodType(Integer.class, Integer.class));
        bitNotBigIntMH = lookup.findStatic("bitNotBigInt", MethodType.methodType(BigInteger.class, BigInteger.class));
        bitNotGenericMH = lookup.findStatic("bitNotGeneric", MethodType.methodType(Number.class, Number.class));

        // Unary negation operator.
        negSetupMH = lookup.findStatic("negSetup",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, Number.class));
        negNumberMH = lookup.findStatic("negNumber", MethodType.methodType(Double.class, Double.class));
        negBigIntMH = lookup.findStatic("negBigInt", MethodType.methodType(BigInteger.class, BigInteger.class));
        negGenericMH = lookup.findStatic("negGeneric", MethodType.methodType(Number.class, Number.class));

        // Increment operator.
        incSetupMH = lookup.findStatic("incSetup",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, Number.class));
        incNumberMH = lookup.findStatic("incNumber", MethodType.methodType(Double.class, Double.class));
        incBigIntMH = lookup.findStatic("incBigInt", MethodType.methodType(BigInteger.class, BigInteger.class));
        incGenericMH = lookup.findStatic("incGeneric", MethodType.methodType(Number.class, Number.class));

        // Decrement operator.
        decSetupMH = lookup.findStatic("decSetup",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, Number.class));
        decNumberMH = lookup.findStatic("decNumber", MethodType.methodType(Double.class, Double.class));
        decBigIntMH = lookup.findStatic("decBigInt", MethodType.methodType(BigInteger.class, BigInteger.class));
        decGenericMH = lookup.findStatic("decGeneric", MethodType.methodType(Number.class, Number.class));
    }

    private static CharSequence addString(CharSequence arg1, CharSequence arg2, ExecutionContext cx) {
        return Operators.add(arg1, arg2, cx);
    }

    private static Double addNumber(Number arg1, Number arg2) {
        return arg1.doubleValue() + arg2.doubleValue();
    }

    private static Object addGeneric(Object arg1, Object arg2, ExecutionContext cx) {
        return Operators.add(arg1, arg2, cx);
    }

    private static int relCmpString(CharSequence arg1, CharSequence arg2) {
        int c = arg1.toString().compareTo(arg2.toString());
        return c < 0 ? 1 : 0;
    }

    private static int relCmpNumber(Number arg1, Number arg2) {
        double nx = arg1.doubleValue();
        double ny = arg2.doubleValue();
        return Double.isNaN(nx) || Double.isNaN(ny) ? -1 : nx < ny ? 1 : 0;
    }

    private static int relCmpGeneric(Object arg1, Object arg2, RelationalOperator op, ExecutionContext cx) {
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

    private static boolean lessThan(int result) {
        return result > 0;
    }

    private static boolean greaterThan(int result) {
        return result > 0;
    }

    private static boolean lessThanEquals(int result) {
        return result == 0;
    }

    private static boolean greaterThanEquals(int result) {
        return result == 0;
    }

    private static boolean eqCmpString(CharSequence arg1, CharSequence arg2) {
        return arg1.length() == arg2.length() && arg1.toString().equals(arg2.toString());
    }

    private static boolean eqCmpNumber(Number arg1, Number arg2) {
        return arg1.doubleValue() == arg2.doubleValue();
    }

    private static boolean eqCmpBoolean(Boolean arg1, Boolean arg2) {
        return arg1.booleanValue() == arg2.booleanValue();
    }

    private static boolean eqCmpGeneric(Object arg1, Object arg2, ExecutionContext cx) {
        return EqualityComparison(cx, arg1, arg2);
    }

    private static boolean strictEqCmpGeneric(Object arg1, Object arg2) {
        return StrictEqualityComparison(arg1, arg2);
    }

    private static Double subNumber(Double arg1, Double arg2, ExecutionContext cx) {
        return arg1 - arg2;
    }

    private static BigInteger subBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.subtract(arg1, arg2);
    }

    private static Number subGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Double.class) {
            return arg1.doubleValue() - arg2.doubleValue();
        }
        return BigIntType.subtract((BigInteger) arg1, (BigInteger) arg2);
    }

    private static Double expNumber(Double arg1, Double arg2, ExecutionContext cx) {
        return Math.pow(arg1, arg2);
    }

    private static BigInteger expBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.exponentiate(cx, arg1, arg2);
    }

    private static Number expGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Double.class) {
            return Math.pow((double) arg1, (double) arg2);
        }
        return BigIntType.exponentiate(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Double mulNumber(Double arg1, Double arg2, ExecutionContext cx) {
        return arg1 * arg2;
    }

    private static BigInteger mulBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.multiply(arg1, arg2);
    }

    private static Number mulGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Double.class) {
            return arg1.doubleValue() * arg2.doubleValue();
        }
        return BigIntType.multiply((BigInteger) arg1, (BigInteger) arg2);
    }

    private static Double divNumber(Double arg1, Double arg2, ExecutionContext cx) {
        return arg1 / arg2;
    }

    private static BigInteger divBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.divide(cx, arg1, arg2);
    }

    private static Number divGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Double.class) {
            return arg1.doubleValue() / arg2.doubleValue();
        }
        return BigIntType.divide(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Double modNumber(Double arg1, Double arg2, ExecutionContext cx) {
        return arg1 % arg2;
    }

    private static BigInteger modBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.remainder(cx, arg1, arg2);
    }

    private static Number modGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Double.class) {
            return arg1.doubleValue() % arg2.doubleValue();
        }
        return BigIntType.remainder(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer shlNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return arg1 << arg2;
    }

    private static BigInteger shlBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.leftShift(cx, arg1, arg2);
    }

    private static Number shlGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return arg1.intValue() << arg2.intValue();
        }
        return BigIntType.leftShift(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer shrNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return arg1 >> arg2;
    }

    private static BigInteger shrBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.signedRightShift(cx, arg1, arg2);
    }

    private static Number shrGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return arg1.intValue() >> arg2.intValue();
        }
        return BigIntType.signedRightShift(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Long ushrNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return (arg1 >>> arg2) & 0xffff_ffffL;
    }

    private static BigInteger ushrBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.unsignedRightShift(cx, arg1, arg2);
    }

    private static Number ushrGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return (arg1.intValue() >>> arg2.intValue()) & 0xffff_ffffL;
        }
        return BigIntType.unsignedRightShift(cx, (BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer bitAndNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return arg1 & arg2;
    }

    private static BigInteger bitAndBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.bitwiseAND(arg1, arg2);
    }

    private static Number bitAndGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return arg1.intValue() & arg2.intValue();
        }
        return BigIntType.bitwiseAND((BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer bitOrNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return arg1 | arg2;
    }

    private static BigInteger bitOrBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.bitwiseOR(arg1, arg2);
    }

    private static Number bitOrGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return arg1.intValue() | arg2.intValue();
        }
        return BigIntType.bitwiseOR((BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer bitXorNumber(Integer arg1, Integer arg2, ExecutionContext cx) {
        return arg1 ^ arg2;
    }

    private static BigInteger bitXorBigInt(BigInteger arg1, BigInteger arg2, ExecutionContext cx) {
        return BigIntType.bitwiseXOR(arg1, arg2);
    }

    private static Number bitXorGeneric(Number arg1, Number arg2, ExecutionContext cx) {
        if (arg1.getClass() != arg2.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (arg1.getClass() == Integer.class) {
            return arg1.intValue() ^ arg2.intValue();
        }
        return BigIntType.bitwiseXOR((BigInteger) arg1, (BigInteger) arg2);
    }

    private static Integer bitNotNumber(Integer arg) {
        return ~arg.intValue();
    }

    private static BigInteger bitNotBigInt(BigInteger arg) {
        return BigIntType.bitwiseNOT(arg);
    }

    private static Number bitNotGeneric(Number arg) {
        if (arg.getClass() == Integer.class) {
            return ~arg.intValue();
        }
        return BigIntType.bitwiseNOT((BigInteger) arg);
    }

    private static Double negNumber(Double arg) {
        return -arg.doubleValue();
    }

    private static BigInteger negBigInt(BigInteger arg) {
        return BigIntType.unaryMinus(arg);
    }

    private static Number negGeneric(Number arg) {
        if (arg.getClass() == Double.class) {
            return -arg.doubleValue();
        }
        return BigIntType.unaryMinus((BigInteger) arg);
    }

    private static Double incNumber(Double arg) {
        return arg.doubleValue() + 1;
    }

    private static BigInteger incBigInt(BigInteger arg) {
        return BigIntType.add(arg, BigIntType.UNIT);
    }

    private static Number incGeneric(Number arg) {
        if (arg.getClass() == Double.class) {
            return arg.doubleValue() + 1;
        }
        return BigIntType.add((BigInteger) arg, BigIntType.UNIT);
    }

    private static Double decNumber(Double arg) {
        return arg.doubleValue() - 1;
    }

    private static BigInteger decBigInt(BigInteger arg) {
        return BigIntType.subtract(arg, BigIntType.UNIT);
    }

    private static Number decGeneric(Number arg) {
        if (arg.getClass() == Double.class) {
            return arg.doubleValue() - 1;
        }
        return BigIntType.subtract((BigInteger) arg, BigIntType.UNIT);
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

    private static boolean testBigInt(Object arg1, Object arg2) {
        return Type.isBigInt(arg1) && Type.isBigInt(arg2);
    }

    private static boolean testNumber(Number arg1, Number arg2) {
        return Type.isNumber(arg1) && Type.isNumber(arg2);
    }

    private static boolean testBigInt(Number arg1, Number arg2) {
        return Type.isBigInt(arg1) && Type.isBigInt(arg2);
    }

    private static boolean testNumber(Number arg) {
        return Type.isNumber(arg);
    }

    private static boolean testBigInt(Number arg) {
        return Type.isBigInt(arg);
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
        if (testBigInt(arg1, arg2)) {
            return Type.BigInt;
        }
        return Type.Object;
    }

    private static Type getType(Number arg1, Number arg2) {
        if (testNumber(arg1, arg2)) {
            return Type.Number;
        }
        if (testBigInt(arg1, arg2)) {
            return Type.BigInt;
        }
        return Type.Object;
    }

    private static Type getType(Number arg) {
        if (testNumber(arg)) {
            return Type.Number;
        }
        assert testBigInt(arg);
        return Type.BigInt;
    }

    private static MethodHandle getTestForUnaryNumber(Type type) {
        switch (type) {
        case Number:
            return testUnaryNumberMH;
        case BigInt:
            return testUnaryBigIntMH;
        default:
            return null;
        }
    }

    private static MethodHandle getTestForBinaryNumber(Type type) {
        switch (type) {
        case Number:
            return testBinaryNumberMH;
        case BigInt:
            return testBinaryBigIntMH;
        default:
            return null;
        }
    }

    private static MethodHandle getTestForBinary(Type type) {
        switch (type) {
        case Boolean:
            return testBooleanMH;
        case Number:
            return testNumberMH;
        case String:
            return testStringMH;
        case BigInt:
            return testBigIntMH;
        default:
            return null;
        }
    }

    private static MethodHandle addSetup(MutableCallSite callsite, Object arg1, Object arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = addStringMH;
        } else if (type == Type.Number) {
            target = addNumberMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinary(type), addGenericMH);
    }

    private static MethodHandle relCmpSetup(MutableCallSite callsite, RelationalOperator op, Object arg1, Object arg2,
            ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = filterReturnValue(relCmpStringMH, op);
        } else if (type == Type.Number) {
            target = filterReturnValue(relCmpNumberMH, op);
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinary(type),
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

    private static MethodHandle eqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2, ExecutionContext cx) {
        // TODO(BigInt): Implement BigInt specializations.
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
        return setCallSiteTarget(callsite, target, getTestForBinary(type), eqCmpGenericMH);
    }

    private static MethodHandle strictEqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2) {
        // TODO(BigInt): Implement BigInt specializations.
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
        return setCallSiteTarget(callsite, target, getTestForBinary(type), strictEqCmpGenericMH);
    }

    private static MethodHandle subSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = subNumberMH;
        } else if (type == Type.BigInt) {
            target = subBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), subGenericMH);
    }

    private static MethodHandle expSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = expNumberMH;
        } else if (type == Type.BigInt) {
            target = expBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), expGenericMH);
    }

    private static MethodHandle mulSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = mulNumberMH;
        } else if (type == Type.BigInt) {
            target = mulBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), mulGenericMH);
    }

    private static MethodHandle divSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = divNumberMH;
        } else if (type == Type.BigInt) {
            target = divBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), divGenericMH);
    }

    private static MethodHandle modSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = modNumberMH;
        } else if (type == Type.BigInt) {
            target = modBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), modGenericMH);
    }

    private static MethodHandle shlSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = shlNumberMH;
        } else if (type == Type.BigInt) {
            target = shlBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), shlGenericMH);
    }

    private static MethodHandle shrSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = shrNumberMH;
        } else if (type == Type.BigInt) {
            target = shrBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), shrGenericMH);
    }

    private static MethodHandle ushrSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = ushrNumberMH;
        } else if (type == Type.BigInt) {
            target = ushrBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), ushrGenericMH);
    }

    private static MethodHandle bitAndSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = bitAndNumberMH;
        } else if (type == Type.BigInt) {
            target = bitAndBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), bitAndGenericMH);
    }

    private static MethodHandle bitOrSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = bitOrNumberMH;
        } else if (type == Type.BigInt) {
            target = bitOrBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), bitOrGenericMH);
    }

    private static MethodHandle bitXorSetup(MutableCallSite callsite, Number arg1, Number arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.Number) {
            target = bitXorNumberMH;
        } else if (type == Type.BigInt) {
            target = bitXorBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForBinaryNumber(type), bitXorGenericMH);
    }

    private static MethodHandle bitNotSetup(MutableCallSite callsite, Number arg) {
        Type type = getType(arg);
        MethodHandle target;
        if (type == Type.Number) {
            target = bitNotNumberMH;
        } else if (type == Type.BigInt) {
            target = bitNotBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForUnaryNumber(type), bitNotGenericMH);
    }

    private static MethodHandle negSetup(MutableCallSite callsite, Number arg) {
        Type type = getType(arg);
        MethodHandle target;
        if (type == Type.Number) {
            target = negNumberMH;
        } else if (type == Type.BigInt) {
            target = negBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForUnaryNumber(type), negGenericMH);
    }

    private static MethodHandle incSetup(MutableCallSite callsite, Number arg) {
        Type type = getType(arg);
        MethodHandle target;
        if (type == Type.Number) {
            target = incNumberMH;
        } else if (type == Type.BigInt) {
            target = incBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForUnaryNumber(type), incGenericMH);
    }

    private static MethodHandle decSetup(MutableCallSite callsite, Number arg) {
        Type type = getType(arg);
        MethodHandle target;
        if (type == Type.Number) {
            target = decNumberMH;
        } else if (type == Type.BigInt) {
            target = decBigIntMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestForUnaryNumber(type), decGenericMH);
    }

    private static MethodHandle setCallSiteTarget(MutableCallSite callsite, MethodHandle target, MethodHandle test,
            MethodHandle generic) {
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
        MethodHandle fallback = MethodHandles.insertArguments(switchToGenericMH, 0, callsite, generic);
        return setupCallSiteTarget(callsite.type(), fallback);
    }

    private static MethodHandle setupCallSiteTarget(MethodType type, MethodHandle target) {
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type), target);
    }

    private static final MethodHandle switchToGenericMH;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        switchToGenericMH = lookup.findStatic("switchToGeneric",
                MethodType.methodType(MethodHandle.class, MutableCallSite.class, MethodHandle.class));
    }

    private static MethodHandle switchToGeneric(MutableCallSite callsite, MethodHandle generic) {
        callsite.setTarget(generic);
        return generic;
    }

    private static final ConstantCallSite stackOverFlow_Add;
    private static final ConstantCallSite stackOverFlow_Cmp;
    private static final ConstantCallSite stackOverFlow_Eq;
    private static final ConstantCallSite stackOverFlow_StrictEq;
    private static final ConstantCallSite stackOverFlow_BinaryNumber;
    private static final ConstantCallSite stackOverFlow_UnaryNumber;
    private static final ConstantCallSite stackOverFlow_Call;
    private static final ConstantCallSite stackOverFlow_Construct;
    private static final ConstantCallSite stackOverFlow_Super;
    private static final MethodHandle stackOverFlow_Concat;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        stackOverFlow_Add = new ConstantCallSite(lookup.findStatic("stackOverFlow_Add",
                MethodType.methodType(Object.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_Cmp = new ConstantCallSite(lookup.findStatic("stackOverFlow_Cmp",
                MethodType.methodType(int.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_Eq = new ConstantCallSite(lookup.findStatic("stackOverFlow_Eq",
                MethodType.methodType(boolean.class, Object.class, Object.class, ExecutionContext.class)));
        stackOverFlow_StrictEq = new ConstantCallSite(lookup.findStatic("stackOverFlow_StrictEq",
                MethodType.methodType(boolean.class, Object.class, Object.class)));
        stackOverFlow_BinaryNumber = new ConstantCallSite(lookup.findStatic("stackOverFlow_BinaryNumber",
                MethodType.methodType(Number.class, Number.class, Number.class, ExecutionContext.class)));
        stackOverFlow_UnaryNumber = new ConstantCallSite(
                lookup.findStatic("stackOverFlow_UnaryNumber", MethodType.methodType(Number.class, Number.class)));
        stackOverFlow_Call = new ConstantCallSite(lookup.findStatic("stackOverFlow_Call", MethodType
                .methodType(Object.class, Object.class, ExecutionContext.class, Object.class, Object[].class)));
        stackOverFlow_Construct = new ConstantCallSite(lookup.findStatic("stackOverFlow_Construct",
                MethodType.methodType(ScriptObject.class, Object.class, ExecutionContext.class, Object[].class)));
        stackOverFlow_Super = new ConstantCallSite(lookup.findStatic("stackOverFlow_Super", MethodType.methodType(
                ScriptObject.class, Constructor.class, ExecutionContext.class, Constructor.class, Object[].class)));
        stackOverFlow_Concat = lookup.findStatic("stackOverFlow_Concat", MethodType.methodType(CharSequence.class));
    }

    private static Object stackOverFlow_Add(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static int stackOverFlow_Cmp(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static boolean stackOverFlow_Eq(Object arg1, Object arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static boolean stackOverFlow_StrictEq(Object arg1, Object arg2) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static Number stackOverFlow_BinaryNumber(Number arg1, Number arg2, ExecutionContext cx) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static Number stackOverFlow_UnaryNumber(Number arg) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static Object stackOverFlow_Call(Object fun, ExecutionContext cx, Object thisValue, Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static ScriptObject stackOverFlow_Construct(Object constructor, ExecutionContext cx, Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

    private static ScriptObject stackOverFlow_Super(Constructor constructor, ExecutionContext cx, Constructor newTarget,
            Object[] arguments) {
        throw new StackOverflowError("bootstrap stack overflow");
    }

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
    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) {
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
            case CallNames.SUB:
                setup = MethodHandles.insertArguments(subSetupMH, 0, callsite);
                break;
            case CallNames.EXP:
                setup = MethodHandles.insertArguments(expSetupMH, 0, callsite);
                break;
            case CallNames.MUL:
                setup = MethodHandles.insertArguments(mulSetupMH, 0, callsite);
                break;
            case CallNames.DIV:
                setup = MethodHandles.insertArguments(divSetupMH, 0, callsite);
                break;
            case CallNames.MOD:
                setup = MethodHandles.insertArguments(modSetupMH, 0, callsite);
                break;
            case CallNames.SHL:
                setup = MethodHandles.insertArguments(shlSetupMH, 0, callsite);
                break;
            case CallNames.SHR:
                setup = MethodHandles.insertArguments(shrSetupMH, 0, callsite);
                break;
            case CallNames.USHR:
                setup = MethodHandles.insertArguments(ushrSetupMH, 0, callsite);
                break;
            case CallNames.BITAND:
                setup = MethodHandles.insertArguments(bitAndSetupMH, 0, callsite);
                break;
            case CallNames.BITOR:
                setup = MethodHandles.insertArguments(bitOrSetupMH, 0, callsite);
                break;
            case CallNames.BITXOR:
                setup = MethodHandles.insertArguments(bitXorSetupMH, 0, callsite);
                break;
            case CallNames.EQ:
                setup = MethodHandles.insertArguments(eqCmpSetupMH, 0, callsite);
                break;
            case CallNames.SHEQ:
                setup = MethodHandles.insertArguments(strictEqCmpSetupMH, 0, callsite);
                break;
            case CallNames.LT:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite, RelationalOperator.LessThan);
                break;
            case CallNames.GT:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite, RelationalOperator.GreaterThan);
                break;
            case CallNames.LE:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite, RelationalOperator.LessThanEquals);
                break;
            case CallNames.GE:
                setup = MethodHandles.insertArguments(relCmpSetupMH, 0, callsite, RelationalOperator.GreaterThanEquals);
                break;
            case CallNames.CONCAT:
                concatSetup(callsite, type);
                return callsite;
            case CallNames.BITNOT:
                setup = MethodHandles.insertArguments(bitNotSetupMH, 0, callsite);
                break;
            case CallNames.NEG:
                setup = MethodHandles.insertArguments(negSetupMH, 0, callsite);
                break;
            case CallNames.INC:
                setup = MethodHandles.insertArguments(incSetupMH, 0, callsite);
                break;
            case CallNames.DEC:
                setup = MethodHandles.insertArguments(decSetupMH, 0, callsite);
                break;
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
                return new ConstantCallSite(
                        MethodHandles.dropArguments(stackOverFlow_Concat, 0, type.parameterArray()));
            case CallNames.ADD:
                return stackOverFlow_Add;
            case CallNames.SUB:
            case CallNames.EXP:
            case CallNames.MUL:
            case CallNames.DIV:
            case CallNames.MOD:
            case CallNames.SHL:
            case CallNames.SHR:
            case CallNames.USHR:
            case CallNames.BITAND:
            case CallNames.BITOR:
            case CallNames.BITXOR:
                return stackOverFlow_BinaryNumber;
            case CallNames.EQ:
                return stackOverFlow_Eq;
            case CallNames.SHEQ:
                return stackOverFlow_StrictEq;
            case CallNames.LT:
            case CallNames.GT:
            case CallNames.LE:
            case CallNames.GE:
                return stackOverFlow_Cmp;
            case CallNames.BITNOT:
            case CallNames.NEG:
            case CallNames.INC:
            case CallNames.DEC:
                return stackOverFlow_UnaryNumber;
            default:
                throw new IllegalArgumentException(name);
            }
        }
    }
}

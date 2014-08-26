/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeConstructor;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.NativeTailCallFunction;

/**
 *
 */
public final class Bootstrap {
    private Bootstrap() {
    }

    private static final class Types {
        static final org.objectweb.asm.Type Object = org.objectweb.asm.Type.getType(Object.class);
        static final org.objectweb.asm.Type Object_ = org.objectweb.asm.Type
                .getType(Object[].class);
        static final org.objectweb.asm.Type ExecutionContext = org.objectweb.asm.Type
                .getType(ExecutionContext.class);
        static final org.objectweb.asm.Type Callable = org.objectweb.asm.Type
                .getType(Callable.class);
    }

    private static final class CallNames {
        static final String CALL = "expression::call";
        static final String ADD = "expression::add";
        static final String EQ = "expression::equals";
        static final String SHEQ = "expression::strictEquals";
        static final String LT = "expression::lessThan";
        static final String GT = "expression::greaterThan";
        static final String LE = "expression::lessThanEquals";
        static final String GE = "expression::greaterThanEquals";
    }

    private static final String OP_ADD = org.objectweb.asm.Type.getMethodDescriptor(Types.Object,
            Types.Object, Types.Object, Types.ExecutionContext);
    private static final String OP_CMP = org.objectweb.asm.Type.getMethodDescriptor(
            org.objectweb.asm.Type.INT_TYPE, Types.Object, Types.Object, Types.ExecutionContext);
    private static final String OP_EQ = org.objectweb.asm.Type
            .getMethodDescriptor(org.objectweb.asm.Type.BOOLEAN_TYPE, Types.Object, Types.Object,
                    Types.ExecutionContext);
    private static final String OP_STRICT_EQ = org.objectweb.asm.Type.getMethodDescriptor(
            org.objectweb.asm.Type.BOOLEAN_TYPE, Types.Object, Types.Object);
    private static final String OP_CALL = org.objectweb.asm.Type.getMethodDescriptor(Types.Object,
            Types.Callable, Types.ExecutionContext, Types.Object, Types.Object_);

    private static final Handle BOOTSTRAP;
    static {
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class,
                String.class, MethodType.class);
        BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(Bootstrap.class), "bootstrapDynamic",
                mt.toMethodDescriptorString());
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
    public static String getCallMethodDescriptor() {
        return OP_CALL;
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
    private static final MethodHandle testFunctionObjectMH, testNativeFunctionMH,
            testNativeTailCallFunctionMH, testNativeConstructorMH;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        testFunctionObjectMH = lookup.findStatic("testFunctionObject",
                MethodType.methodType(boolean.class, Callable.class, MethodHandle.class));
        testNativeFunctionMH = lookup.findStatic("testNativeFunction",
                MethodType.methodType(boolean.class, Callable.class, MethodHandle.class));
        testNativeTailCallFunctionMH = lookup.findStatic("testNativeTailCallFunction",
                MethodType.methodType(boolean.class, Callable.class, MethodHandle.class));
        testNativeConstructorMH = lookup.findStatic("testNativeConstructor",
                MethodType.methodType(boolean.class, Callable.class, MethodHandle.class));
        callGenericMH = lookup.findStatic("callGeneric", MethodType.methodType(Object.class,
                Callable.class, ExecutionContext.class, Object.class, Object[].class));
        callSetupMH = lookup.findStatic("callSetup", MethodType.methodType(MethodHandle.class,
                MutableCallSite.class, Callable.class, ExecutionContext.class, Object.class,
                Object[].class));
    }

    @SuppressWarnings("unused")
    private static MethodHandle callSetup(MutableCallSite callsite, Callable function,
            ExecutionContext cx, Object thisValue, Object[] arguments) {
        MethodHandle target, test;
        if (function instanceof FunctionObject) {
            MethodHandle mh = ((FunctionObject) function).getCallMethod();
            test = MethodHandles.insertArguments(testFunctionObjectMH, 1, mh);
            target = mh;
        } else if (function instanceof NativeFunction) {
            MethodHandle mh = ((NativeFunction) function).getCallMethod();
            test = MethodHandles.insertArguments(testNativeFunctionMH, 1, mh);
            target = MethodHandles.dropArguments(mh, 0, NativeFunction.class);
        } else if (function instanceof NativeTailCallFunction) {
            MethodHandle mh = ((NativeTailCallFunction) function).getCallMethod();
            test = MethodHandles.insertArguments(testNativeTailCallFunctionMH, 1, mh);
            target = MethodHandles.dropArguments(mh, 0, NativeTailCallFunction.class);
        } else if (function instanceof NativeConstructor) {
            MethodHandle mh = ((NativeConstructor) function).getCallMethod();
            test = MethodHandles.insertArguments(testNativeConstructorMH, 1, mh);
            target = MethodHandles.dropArguments(mh, 0, NativeConstructor.class);
        } else {
            target = test = null;
        }
        return setCallSiteTarget(callsite, target, test, callGenericMH);
    }

    @SuppressWarnings("unused")
    private static boolean testFunctionObject(Callable function, MethodHandle callMethod) {
        return function instanceof FunctionObject
                && ((FunctionObject) function).getCallMethod() == callMethod;
    }

    @SuppressWarnings("unused")
    private static boolean testNativeFunction(Callable function, MethodHandle callMethod) {
        return function instanceof NativeFunction
                && ((NativeFunction) function).getCallMethod() == callMethod;
    }

    @SuppressWarnings("unused")
    private static boolean testNativeTailCallFunction(Callable function, MethodHandle callMethod) {
        return function instanceof NativeTailCallFunction
                && ((NativeTailCallFunction) function).getCallMethod() == callMethod;
    }

    @SuppressWarnings("unused")
    private static boolean testNativeConstructor(Callable function, MethodHandle callMethod) {
        return function instanceof NativeConstructor
                && ((NativeConstructor) function).getCallMethod() == callMethod;
    }

    @SuppressWarnings("unused")
    private static Object callGeneric(Callable function, ExecutionContext callerContext,
            Object thisValue, Object[] arguments) {
        return function.call(callerContext, thisValue, arguments);
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
        case NE:
            return CallNames.EQ;
        case SHEQ:
        case SHNE:
            return CallNames.SHEQ;
        case LT:
            return CallNames.LT;
        case GT:
            return CallNames.GT;
        case LE:
            return CallNames.LE;
        case GE:
            return CallNames.GE;
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
    public static String getMethodDescriptor(BinaryExpression.Operator binary) {
        switch (binary) {
        case ADD:
            return OP_ADD;
        case EQ:
        case NE:
            return OP_EQ;
        case SHEQ:
        case SHNE:
            return OP_STRICT_EQ;
        case LT:
        case GT:
        case LE:
        case GE:
            return OP_CMP;
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
        case NE:
        case SHEQ:
        case SHNE:
        case LT:
        case GT:
        case LE:
        case GE:
            return BOOTSTRAP;
        default:
            throw new UnsupportedOperationException(binary.toString());
        }
    }

    private static final MethodHandle addSetupMH, relCmpSetupMH, eqCmpSetupMH, strictEqCmpSetupMH;
    private static final MethodHandle addStringMH, addNumberMH, addGenericMH;
    private static final MethodHandle relCmpStringMH, relCmpNumberMH, relCmpGenericMH;
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
                Object.class, Object.class, boolean.class, ExecutionContext.class));

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
                MutableCallSite.class, boolean.class, Object.class, Object.class,
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
        return (Double.isNaN(nx) || Double.isNaN(ny) ? -1 : nx < ny ? 1 : 0);
    }

    @SuppressWarnings("unused")
    private static int relCmpGeneric(Object arg1, Object arg2, boolean leftFirst,
            ExecutionContext cx) {
        return ScriptRuntime.relationalComparison(arg1, arg2, leftFirst, cx);
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
        return ScriptRuntime.equalityComparison(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static boolean strictEqCmpGeneric(Object arg1, Object arg2) {
        return ScriptRuntime.strictEqualityComparison(arg1, arg2);
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
    private static MethodHandle relCmpSetup(MutableCallSite callsite, boolean leftFirst,
            Object arg1, Object arg2, ExecutionContext cx) {
        Type type = getType(arg1, arg2);
        MethodHandle target;
        if (type == Type.String) {
            target = relCmpStringMH;
        } else if (type == Type.Number) {
            target = relCmpNumberMH;
        } else {
            target = null;
        }
        return setCallSiteTarget(callsite, target, getTestFor(type),
                MethodHandles.insertArguments(relCmpGenericMH, 2, leftFirst));
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
                MethodHandle fallback = getFallback(callsite, generic);
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

    private static MethodHandle getFallback(MutableCallSite callsite, MethodHandle generic) {
        // only perform fallback to generic for now
        MethodHandle fallback = MethodHandles.insertArguments(switchToGenericMH, 0, callsite,
                generic);
        return getSetupCallSiteTarget(callsite.type(), fallback);
    }

    private static MethodHandle getSetupCallSiteTarget(MethodType type, MethodHandle target) {
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
                MethodType.methodType(Object.class, Callable.class, ExecutionContext.class,
                        Object.class, Object[].class)));
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
    private static Object stackOverFlow_Call(Callable fun, ExecutionContext cx, Object thisValue,
            Object[] arguments) {
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
                setup = MethodHandles
                        .insertArguments(relCmpSetupMH, 0, callsite, true /* leftFirst */);
                break;
            case CallNames.GT:
                setup = MethodHandles
                        .insertArguments(relCmpSetupMH, 0, callsite, false /* leftFirst */);
                break;
            case CallNames.LE:
                setup = MethodHandles
                        .insertArguments(relCmpSetupMH, 0, callsite, false /* leftFirst */);
                break;
            case CallNames.GE:
                setup = MethodHandles
                        .insertArguments(relCmpSetupMH, 0, callsite, true /* leftFirst */);
                break;
            default:
                throw new IllegalArgumentException(name);
            }

            callsite.setTarget(getSetupCallSiteTarget(type, setup));
            return callsite;
        } catch (StackOverflowError e) {
            switch (name) {
            case CallNames.CALL:
                return stackOverFlow_Call;
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

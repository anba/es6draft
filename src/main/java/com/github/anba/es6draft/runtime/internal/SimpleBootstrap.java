/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import com.github.anba.es6draft.ast.BinaryExpression;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Type;

/**
 *
 */
public class SimpleBootstrap {
    private static class Types {
        static final org.objectweb.asm.Type Object = org.objectweb.asm.Type.getType(Object.class);
        static final org.objectweb.asm.Type ExecutionContext = org.objectweb.asm.Type
                .getType(ExecutionContext.class);
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

    private static final Handle BINARY_BOOTSTRAP;
    static {
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class,
                String.class, MethodType.class);
        BINARY_BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(SimpleBootstrap.class), "bootstrapDynamic",
                mt.toMethodDescriptorString());
    }

    public static String getName(BinaryExpression.Operator binary) {
        switch (binary) {
        case ADD:
            return "expression::add";
        case EQ:
        case NE:
            return "expression::equals";
        case SHEQ:
        case SHNE:
            return "expression::strictEquals";
        case LT:
            return "expression::lessThan";
        case GT:
            return "expression::greaterThan";
        case LE:
            return "expression::lessThanEquals";
        case GE:
            return "expression::greaterThanEquals";
        default:
            throw new UnsupportedOperationException(binary.toString());
        }
    }

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
            return BINARY_BOOTSTRAP;
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
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> thisClass = lookup.lookupClass();

        try {
            testStringMH = lookup.findStatic(thisClass, "testString",
                    MethodType.methodType(boolean.class, Object.class, Object.class));
            testNumberMH = lookup.findStatic(thisClass, "testNumber",
                    MethodType.methodType(boolean.class, Object.class, Object.class));
            testBooleanMH = lookup.findStatic(thisClass, "testBoolean",
                    MethodType.methodType(boolean.class, Object.class, Object.class));

            addStringMH = lookup.findStatic(thisClass, "addString", MethodType.methodType(
                    CharSequence.class, CharSequence.class, CharSequence.class,
                    ExecutionContext.class));
            addNumberMH = lookup.findStatic(thisClass, "addNumber",
                    MethodType.methodType(Double.class, Number.class, Number.class));
            addGenericMH = lookup.findStatic(thisClass, "addGeneric", MethodType.methodType(
                    Object.class, Object.class, Object.class, ExecutionContext.class));

            relCmpStringMH = lookup.findStatic(thisClass, "relCmpString",
                    MethodType.methodType(int.class, CharSequence.class, CharSequence.class));
            relCmpNumberMH = lookup.findStatic(thisClass, "relCmpNumber",
                    MethodType.methodType(int.class, Number.class, Number.class));
            relCmpGenericMH = lookup.findStatic(thisClass, "relCmpGeneric", MethodType.methodType(
                    int.class, Object.class, Object.class, boolean.class, ExecutionContext.class));

            eqCmpStringMH = lookup.findStatic(thisClass, "eqCmpString",
                    MethodType.methodType(boolean.class, CharSequence.class, CharSequence.class));
            eqCmpNumberMH = lookup.findStatic(thisClass, "eqCmpNumber",
                    MethodType.methodType(boolean.class, Number.class, Number.class));
            eqCmpBooleanMH = lookup.findStatic(thisClass, "eqCmpBoolean",
                    MethodType.methodType(boolean.class, Boolean.class, Boolean.class));
            eqCmpGenericMH = lookup.findStatic(thisClass, "eqCmpGeneric", MethodType.methodType(
                    boolean.class, Object.class, Object.class, ExecutionContext.class));

            strictEqCmpStringMH = eqCmpStringMH;
            strictEqCmpNumberMH = eqCmpNumberMH;
            strictEqCmpBooleanMH = eqCmpBooleanMH;
            strictEqCmpGenericMH = lookup.findStatic(thisClass, "strictEqCmpGeneric",
                    MethodType.methodType(boolean.class, Object.class, Object.class));

            addSetupMH = lookup.findStatic(thisClass, "addSetup", MethodType.methodType(
                    Object.class, MutableCallSite.class, Object.class, Object.class,
                    ExecutionContext.class));
            relCmpSetupMH = lookup.findStatic(thisClass, "relCmpSetup", MethodType.methodType(
                    int.class, MutableCallSite.class, boolean.class, Object.class, Object.class,
                    ExecutionContext.class));
            eqCmpSetupMH = lookup.findStatic(thisClass, "eqCmpSetup", MethodType.methodType(
                    boolean.class, MutableCallSite.class, Object.class, Object.class,
                    ExecutionContext.class));
            strictEqCmpSetupMH = lookup.findStatic(thisClass, "strictEqCmpSetup", MethodType
                    .methodType(boolean.class, MutableCallSite.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
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

    @SuppressWarnings("unused")
    private static Object addSetup(MutableCallSite callsite, Object arg1, Object arg2,
            ExecutionContext cx) throws Throwable {
        MethodHandle callSiteTarget, target;
        if (testString(arg1, arg2)) {
            target = addStringMH.asType(callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testStringMH, target, addGenericMH);
        } else if (testNumber(arg1, arg2)) {
            target = MethodHandles.dropArguments(addNumberMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testNumberMH, target, addGenericMH);
        } else {
            callSiteTarget = target = addGenericMH;
        }

        callsite.setTarget(callSiteTarget);
        return target.invokeExact(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static int relCmpSetup(MutableCallSite callsite, boolean leftFirst, Object arg1,
            Object arg2, ExecutionContext cx) throws Throwable {
        MethodHandle callSiteTarget, target;
        if (testString(arg1, arg2)) {
            target = MethodHandles.dropArguments(relCmpStringMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testStringMH, target,
                    MethodHandles.insertArguments(relCmpGenericMH, 2, leftFirst));
        } else if (testNumber(arg1, arg2)) {
            target = MethodHandles.dropArguments(relCmpNumberMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testNumberMH, target,
                    MethodHandles.insertArguments(relCmpGenericMH, 2, leftFirst));
        } else {
            callSiteTarget = target = MethodHandles.insertArguments(relCmpGenericMH, 2, leftFirst);
        }

        callsite.setTarget(callSiteTarget);
        return (int) target.invokeExact(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static boolean eqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2,
            ExecutionContext cx) throws Throwable {
        MethodHandle callSiteTarget, target;
        if (testString(arg1, arg2)) {
            target = MethodHandles.dropArguments(eqCmpStringMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testStringMH, target, eqCmpGenericMH);
        } else if (testNumber(arg1, arg2)) {
            target = MethodHandles.dropArguments(eqCmpNumberMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testNumberMH, target, eqCmpGenericMH);
        } else if (testBoolean(arg1, arg2)) {
            target = MethodHandles.dropArguments(eqCmpBooleanMH, 2, ExecutionContext.class).asType(
                    callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testBooleanMH, target, eqCmpGenericMH);
        } else {
            callSiteTarget = target = eqCmpGenericMH;
        }

        callsite.setTarget(callSiteTarget);
        return (boolean) target.invokeExact(arg1, arg2, cx);
    }

    @SuppressWarnings("unused")
    private static boolean strictEqCmpSetup(MutableCallSite callsite, Object arg1, Object arg2)
            throws Throwable {
        MethodHandle callSiteTarget, target;
        if (testString(arg1, arg2)) {
            target = strictEqCmpStringMH.asType(callsite.type());
            callSiteTarget = MethodHandles
                    .guardWithTest(testStringMH, target, strictEqCmpGenericMH);
        } else if (testNumber(arg1, arg2)) {
            target = strictEqCmpNumberMH.asType(callsite.type());
            callSiteTarget = MethodHandles
                    .guardWithTest(testNumberMH, target, strictEqCmpGenericMH);
        } else if (testBoolean(arg1, arg2)) {
            target = strictEqCmpBooleanMH.asType(callsite.type());
            callSiteTarget = MethodHandles.guardWithTest(testBooleanMH, target,
                    strictEqCmpGenericMH);
        } else {
            callSiteTarget = target = strictEqCmpGenericMH;
        }

        callsite.setTarget(callSiteTarget);
        return (boolean) target.invokeExact(arg1, arg2);
    }

    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name,
            MethodType type) {
        // System.out.printf("caller: %s\n", caller);
        // System.out.printf("name: %s\n", name);
        // System.out.printf("type: %s\n", type);
        MutableCallSite callsite = new MutableCallSite(type);

        MethodHandle target;
        switch (name) {
        case "expression::add":
            target = MethodHandles.insertArguments(addSetupMH, 0, callsite);
            break;
        case "expression::equals":
            target = MethodHandles.insertArguments(eqCmpSetupMH, 0, callsite);
            break;
        case "expression::strictEquals":
            target = MethodHandles.insertArguments(strictEqCmpSetupMH, 0, callsite);
            break;
        case "expression::lessThan":
            target = MethodHandles
                    .insertArguments(relCmpSetupMH, 0, callsite, true /* leftFirst */);
            break;
        case "expression::greaterThan":
            target = MethodHandles
                    .insertArguments(relCmpSetupMH, 0, callsite, false /* leftFirst */);
            break;
        case "expression::lessThanEquals":
            target = MethodHandles
                    .insertArguments(relCmpSetupMH, 0, callsite, false /* leftFirst */);
            break;
        case "expression::greaterThanEquals":
            target = MethodHandles
                    .insertArguments(relCmpSetupMH, 0, callsite, true /* leftFirst */);
            break;
        default:
            throw new IllegalArgumentException(name);
        }

        callsite.setTarget(target);
        return callsite;
    }
}
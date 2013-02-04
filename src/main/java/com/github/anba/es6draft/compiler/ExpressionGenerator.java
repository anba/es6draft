/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.Substitutions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;
import com.github.anba.es6draft.runtime.internal.SimpleBootstrap;
import com.github.anba.es6draft.semantics.ObjectLiteralStaticSemantics;

/**
 *
 */
class ExpressionGenerator extends DefaultCodeGenerator<ValType, MethodGenerator> {

    /* ----------------------------------------------------------------------------------------- */

    /**
     * ref = `eval` {@code node}<br>
     * GetValue(ref)<br>
     */
    private ValType evalAndGetValue(Expression node, MethodGenerator mv) {
        if (node instanceof Identifier) {
            mv.load(Register.ExecutionContext);
            mv.aconst(((Identifier) node).getName());
            if (mv.isStrict()) {
                mv.invoke(Methods.ExecutionContext_strictIdentifierValue);
            } else {
                mv.invoke(Methods.ExecutionContext_nonstrictIdentifierValue);
            }
            return ValType.Any;
        } else {
            ValType type = node.accept(this, mv);
            GetValue(node, type, mv);
            return type;
        }
    }

    private void GetValue(Expression node, ValType type, MethodGenerator mv) {
        assert !node.accept(IsReference.INSTANCE, null)
                || (type == ValType.Any || type == ValType.Reference) : type;
        assert !(type == ValType.Reference) || node.accept(IsReference.INSTANCE, null) : type;

        if (node.accept(IsReference.INSTANCE, null)) {
            if (type == ValType.Reference) {
                mv.load(Register.Realm);
                mv.invoke(Methods.Reference_GetValue_);
            } else {
                GetValue(mv);
            }
        }
    }

    private void PutValue(Expression node, ValType type, MethodGenerator mv) {
        assert !node.accept(IsReference.INSTANCE, null)
                || (type == ValType.Any || type == ValType.Reference) : type;
        assert !(type == ValType.Reference) || node.accept(IsReference.INSTANCE, null) : type;

        if (node.accept(IsReference.INSTANCE, null)) {
            if (type == ValType.Reference) {
                mv.load(Register.Realm);
                mv.invoke(Methods.Reference_PutValue_);
            } else {
                PutValue(mv);
            }
        }
    }

    private static final Object[] EMPTY_BSM_ARGS = new Object[] {};

    private void invokeDynamicOperator(BinaryExpression.Operator operator, MethodGenerator mv) {
        mv.invokedynamic(SimpleBootstrap.getName(operator),
                SimpleBootstrap.getMethodDescriptor(operator),
                SimpleBootstrap.getBootstrap(operator), EMPTY_BSM_ARGS);
    }

    /**
     * [11.2.3 EvaluateCall Abstract Operation]
     */
    private void EvaluateCall(Expression call, Expression base, ValType type,
            List<Expression> arguments, boolean directEval, MethodGenerator mv) {
        // stack: [ref] -> [ref, ref]
        mv.dup();
        /* step 1-2 */
        GetValue(base, type, mv);
        /* step 3-4 */
        boolean hasSpread = false;
        if (arguments.isEmpty()) {
            mv.get(Fields.ScriptRuntime_EMPTY_ARRAY);
        } else {
            mv.newarray(arguments.size(), Types.Object);
            for (int i = 0, size = arguments.size(); i < size; ++i) {
                mv.dup();
                mv.iconst(i);
                /* [11.2.5 Argument Lists] ArgumentListEvaluation */
                Expression argument = arguments.get(i);
                hasSpread |= (argument instanceof SpreadElement);
                ValType argtype = evalAndGetValue(argument, mv);
                mv.toBoxed(argtype);
                mv.astore(Types.Object);
            }
            if (hasSpread) {
                mv.invoke(Methods.ScriptRuntime_toFlatArray);
            }
        }
        // stack: ref func args
        mv.lineInfo(call);

        // stack: [ref, func, args] -> [args, ref, func]
        mv.dupX2();
        mv.pop();

        // stack: [args, ref, func] -> [args, ref, func(Callable)]
        mv.load(Register.Realm);
        mv.invoke(Methods.ScriptRuntime_CheckCallable);

        Label notEval = null, afterCall = null;
        if (directEval) {
            // test for possible direct-eval call
            notEval = new Label();
            afterCall = new Label();

            // stack: [args, ref, func(Callable)] -> [args, ref, func(Callable), isDirectEval]
            mv.dup2();
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_IsBuiltinEval);
            mv.ifeq(notEval);

            // stack: [args, ref, func(Callable)] -> [args]
            mv.pop2();

            // stack: [args] -> [arg0]
            if (hasSpread) {
                Label isEmpty = new Label(), after = new Label();
                mv.dup();
                mv.arraylength();
                mv.ifeq(isEmpty);
                mv.iconst(0);
                mv.aload(Types.Object);
                mv.goTo(after);
                mv.mark(isEmpty);
                mv.pop();
                mv.get(Fields.Undefined_UNDEFINED);
                mv.mark(after);
            } else if (arguments.isEmpty()) {
                mv.pop();
                mv.get(Fields.Undefined_UNDEFINED);
            } else {
                mv.iconst(0);
                mv.aload(Types.Object);
            }

            // stack: [args0] -> [result]
            mv.load(Register.ExecutionContext);
            mv.iconst(mv.isStrict());
            mv.iconst(mv.isGlobal());
            mv.invoke(Methods.Eval_directEval);

            mv.goTo(afterCall);
            mv.mark(notEval);
        }

        // TODO: tail-call for SuperExpression(call) or TemplateCallExpression?
        if (call instanceof CallExpression && mv.isTailCall((CallExpression) call)) {
            Label noTailCall = new Label();
            mv.dup();
            mv.instanceOf(Types.OrdinaryFunction);
            mv.ifeq(noTailCall);

            // stack: [args, ref, func(Callable)] -> [args, thisValue, func(Callable)]
            mv.swap();
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_GetCallThisValue);
            mv.swap();

            mv.newarray(3, Types.Object);

            // store func(Callable)
            mv.dupX1();
            mv.swap();
            mv.iconst(0);
            mv.swap();
            mv.astore(Types.Object);

            // store thisValue
            mv.dupX1();
            mv.swap();
            mv.iconst(1);
            mv.swap();
            mv.astore(Types.Object);

            // store args
            mv.dupX1();
            mv.swap();
            mv.iconst(2);
            mv.swap();
            mv.astore(Types.Object);

            // stack: [<func(Callable), thisValue, args>]
            mv.areturn(Types.Object_);

            mv.mark(noTailCall);
        }

        // stack: [args, ref, func(Callable)] -> [args, func(Callable), thisValue]
        mv.swap();
        mv.load(Register.Realm);
        mv.invoke(Methods.ScriptRuntime_GetCallThisValue);

        // stack: [args, func(Callable), thisValue] -> [func(Callable), thisValue, args]
        mv.dup2X1();
        mv.pop2();

        // stack: [func(Callable), thisValue, args] -> [result]
        mv.invoke(Methods.Callable_call);

        if (afterCall != null) {
            mv.mark(afterCall);
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    private final ArrayComprehensionGenerator arraycomprgen;

    public ExpressionGenerator(CodeGenerator codegen) {
        super(codegen);
        this.arraycomprgen = new ArrayComprehensionGenerator(codegen);
    }

    @Override
    protected ValType visit(Node node, MethodGenerator mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /* ----------------------------------------------------------------------------------------- */

    @Override
    public ValType visit(ArrayComprehension node, MethodGenerator mv) {
        return node.accept(arraycomprgen, mv);
    }

    @Override
    public ValType visit(ArrayLiteral node, MethodGenerator mv) {
        boolean hasSpread = false;
        for (Expression element : node.getElements()) {
            if (element instanceof SpreadElement) {
                hasSpread = true;
                break;
            }
        }

        mv.load(Register.Realm);
        mv.lconst(0);
        mv.invoke(Methods.ExoticArray_ArrayCreate);
        if (!hasSpread) {
            int nextIndex = 0;
            for (Expression element : node.getElements()) {
                if (element instanceof Elision) {
                    // Elision
                } else {
                    mv.dup();
                    mv.iconst(nextIndex);
                    ValType type = evalAndGetValue(element, mv);
                    mv.toBoxed(type);
                    mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                }
                nextIndex += 1;
            }
            mv.dup();
            mv.load(Register.Realm);
            mv.swap();
            mv.aconst("length");
            mv.iconst(nextIndex);
            mv.toBoxed(Type.INT_TYPE);
            mv.iconst(0);
            mv.invoke(Methods.AbstractOperations_Put);
        } else {
            // stack: [array, nextIndex]
            mv.iconst(0); // nextIndex
            int elisionWidth = 0;
            for (Expression element : node.getElements()) {
                if (element instanceof Elision) {
                    // Elision
                    elisionWidth += 1;
                    continue;
                }
                if (elisionWidth != 0) {
                    mv.iconst(elisionWidth);
                    mv.add(Type.INT_TYPE);
                    elisionWidth = 0;
                }
                if (element instanceof SpreadElement) {
                    // stack: [array, nextIndex] -> [array, nextIndex, array, nextIndex]
                    mv.dup2();
                    Expression spread = ((SpreadElement) element).getExpression();
                    ValType type = evalAndGetValue(spread, mv);
                    mv.toBoxed(type);
                    mv.load(Register.Realm);
                    mv.invoke(Methods.ScriptRuntime_ArrayAccumulationSpreadElement);
                    // stack: ... -> [array, nextIndex]
                    mv.swap();
                    mv.pop();
                } else {
                    // stack: [array, nextIndex] -> [array, nextIndex, array, nextIndex]
                    mv.dup2();
                    ValType type = evalAndGetValue(element, mv);
                    mv.toBoxed(type);
                    // stack: [array, nextIndex, array, nextIndex, obj] -> [array, nextIndex]
                    mv.invoke(Methods.ScriptRuntime_defineProperty__int);
                    elisionWidth += 1;
                }
            }
            if (elisionWidth != 0) {
                mv.iconst(elisionWidth);
                mv.add(Type.INT_TYPE);
                elisionWidth = 0;
            }

            // stack: [array, nextIndex] -> [array, 'length', (nextIndex), false, cx]
            mv.toBoxed(Type.INT_TYPE);
            mv.swap();
            mv.dup();
            mv.load(Register.Realm);
            mv.dup2X2();
            mv.pop2();
            mv.swap();
            // stack: [array, realm, array, (nextIndex)]
            mv.aconst("length");
            mv.swap();
            mv.iconst(0);
            mv.invoke(Methods.AbstractOperations_Put);
        }

        return ValType.Object;
    }

    @Override
    public ValType visit(ArrowFunction node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> ArrowFunction
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_EvaluateArrowFunction);

        return ValType.Object;
    }

    @Override
    public ValType visit(AssignmentExpression node, MethodGenerator mv) {
        LeftHandSideExpression left = node.getLeft();
        Expression right = node.getRight();
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            if (left instanceof AssignmentPattern) {
                ValType rtype = evalAndGetValue(right, mv);

                // FIXME: spec bug(?) return GetValue(rref) instead of ToObject(GetValue(rref))
                // rval rval
                ToObject(rtype, mv);

                mv.dup();
                DestructuringAssignment((AssignmentPattern) left, mv);

                return ValType.Object;
            } else {
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                ValType rtype = evalAndGetValue(right, mv);

                // lref rval
                mv.dupX(ltype, rtype);
                mv.toBoxed(rtype);
                PutValue(left, ltype, mv);

                return (rtype != ValType.Reference ? rtype : ValType.Object);
            }
        } else {
            switch (node.getOperator()) {
            case ASSIGN_MUL: {
                // 11.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
                ToNumber(rtype, mv);
                mv.mul(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_DIV: {
                // 11.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
                ToNumber(rtype, mv);
                mv.div(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_MOD: {
                // 11.5 Multiplicative Operators
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
                ToNumber(rtype, mv);
                mv.rem(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_ADD: {
                // 11.6.1 The Addition operator ( + )
                if (right instanceof StringLiteral) {
                    // x += ""
                    ValType ltype = left.accept(this, mv);
                    assert !ltype.isPrimitive() : "invalid lhs for assignment";
                    mv.dup();
                    GetValue(left, ltype, mv);
                    ToPrimitive(ltype, null, mv);
                    ToString(ltype, mv);
                    if (!((StringLiteral) right).getValue().isEmpty()) {
                        right.accept(this, mv);
                        mv.invoke(Methods.ScriptRuntime_add_str);
                    }
                    // r lref r
                    mv.dupX1();
                    PutValue(left, ltype, mv);
                    return ValType.String;
                }

                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                mv.toBoxed(rtype);
                // lref lval rval
                mv.load(Register.Realm);
                mv.invoke(Methods.ScriptRuntime_add);
                // r lref r
                mv.dupX1();
                PutValue(left, ltype, mv);
                return ValType.Any;
            }
            case ASSIGN_SUB: {
                // 1.6.2 The Subtraction Operator ( - )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
                ToNumber(rtype, mv);
                mv.sub(Type.DOUBLE_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number);
                mv.toBoxed(ValType.Number);
                PutValue(left, ltype, mv);
                return ValType.Number;
            }
            case ASSIGN_SHL: {
                // 11.7.1 The Left Shift Operator ( << )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
                ToInt32(rtype, mv); // ToUint32()
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.shl(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_SHR: {
                // 11.7.2 The Signed Right Shift Operator ( >> )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
                ToInt32(rtype, mv); // ToUint32()
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.shr(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_USHR: {
                // 11.7.3 The Unsigned Right Shift Operator ( >>> )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToUint32(ltype, mv);
                mv.swap(rtype, ValType.Number_uint);
                ToInt32(rtype, mv); // ToUint32()
                mv.iconst(0x1F);
                mv.and(Type.INT_TYPE);
                mv.ushr(Type.LONG_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_uint);
                mv.toBoxed(ValType.Number_uint);
                PutValue(left, ltype, mv);
                return ValType.Number_uint;
            }
            case ASSIGN_BITAND: {
                // 11.10 Binary Bitwise Operators ( & )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
                ToInt32(rtype, mv);
                mv.and(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_BITXOR: {
                // 11.10 Binary Bitwise Operators ( ^ )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
                ToInt32(rtype, mv);
                mv.xor(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN_BITOR: {
                // 11.10 Binary Bitwise Operators ( | )
                ValType ltype = left.accept(this, mv);
                assert !ltype.isPrimitive() : "invalid lhs for assignment";
                mv.dup();
                GetValue(left, ltype, mv);
                ValType rtype = evalAndGetValue(right, mv);
                // lref lval rval
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
                ToInt32(rtype, mv);
                mv.or(Type.INT_TYPE);
                // r lref r
                mv.dupX(ltype, ValType.Number_int);
                mv.toBoxed(ValType.Number_int);
                PutValue(left, ltype, mv);
                return ValType.Number_int;
            }
            case ASSIGN:
            default:
                throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
            }
        }
    }

    @Override
    public ValType visit(BinaryExpression node, MethodGenerator mv) {
        Expression left = node.getLeft();
        Expression right = node.getRight();

        switch (node.getOperator()) {
        case MUL: {
            // 11.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.mul(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case DIV: {
            // 11.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.div(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case MOD: {
            // 11.5 Multiplicative Operators
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.rem(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case ADD: {
            // 11.6.1 The Addition operator ( + )
            if (left instanceof StringLiteral) {
                if (((StringLiteral) left).getValue().isEmpty()) {
                    // "" + x
                    ValType rtype = evalAndGetValue(right, mv);
                    rtype = ToPrimitive(rtype, null, mv);
                    ToString(rtype, mv);
                } else {
                    left.accept(this, mv);
                    ValType rtype = evalAndGetValue(right, mv);
                    rtype = ToPrimitive(rtype, null, mv);
                    ToString(rtype, mv);
                    mv.invoke(Methods.ScriptRuntime_add_str);
                }
                return ValType.String;
            } else if (right instanceof StringLiteral) {
                if (((StringLiteral) right).getValue().isEmpty()) {
                    // x + ""
                    ValType ltype = evalAndGetValue(left, mv);
                    ltype = ToPrimitive(ltype, null, mv);
                    ToString(ltype, mv);
                } else {
                    ValType ltype = evalAndGetValue(left, mv);
                    ltype = ToPrimitive(ltype, null, mv);
                    ToString(ltype, mv);
                    right.accept(this, mv);
                    mv.invoke(Methods.ScriptRuntime_add_str);
                }
                return ValType.String;
            }

            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Any;
        }
        case SUB: {
            // 1.6.2 The Subtraction Operator ( - )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToNumber(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToNumber(ltype, mv);
                mv.swap(rtype, ValType.Number);
            }
            ToNumber(rtype, mv);
            mv.sub(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case SHL: {
            // 11.7.1 The Left Shift Operator ( << )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.shl(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case SHR: {
            // 11.7.2 The Signed Right Shift Operator ( >> )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.shr(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case USHR: {
            // 11.7.3 The Unsigned Right Shift Operator ( >>> )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToUint32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToUint32(ltype, mv);
                mv.swap(rtype, ValType.Number_uint);
            }
            ToInt32(rtype, mv); // ToUint32()
            mv.iconst(0x1F);
            mv.and(Type.INT_TYPE);
            mv.ushr(Type.LONG_TYPE);
            return ValType.Number_uint;
        }
        case LT: {
            // 11.8 Relational Operators ( < )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifgt(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }

            return ValType.Boolean;
        }
        case GT: {
            // 11.8 Relational Operators ( > )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);
            mv.swap();

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifgt(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case LE: {
            // 11.8 Relational Operators ( <= )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);
            mv.swap();

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifeq(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case GE: {
            // 11.8 Relational Operators ( >= )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            {
                Label lbl1 = new Label(), lbl2 = new Label();
                mv.ifeq(lbl1);
                mv.iconst(false);
                mv.goTo(lbl2);
                mv.mark(lbl1);
                mv.iconst(true);
                mv.mark(lbl2);
            }
            return ValType.Boolean;
        }
        case INSTANCEOF: {
            // 11.8 Relational Operators ( instanceof )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_instanceOfOperator);
            return ValType.Boolean;
        }
        case IN: {
            // 11.8 Relational Operators ( in )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);

            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_in);
            return ValType.Boolean;
        }
        case EQ: {
            // 11.9 Equality Operators ( == )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);

            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case NE: {
            // 11.9 Equality Operators ( != )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);

            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            mv.load(Register.Realm);
            invokeDynamicOperator(node.getOperator(), mv);

            mv.iconst(1);
            mv.xor(Type.INT_TYPE);
            return ValType.Boolean;
        }
        case SHEQ: {
            // 11.9 Equality Operators ( === )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);

            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            invokeDynamicOperator(node.getOperator(), mv);

            return ValType.Boolean;
        }
        case SHNE: {
            // 11.9 Equality Operators ( !== )
            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);

            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);

            invokeDynamicOperator(node.getOperator(), mv);

            mv.iconst(1);
            mv.xor(Type.INT_TYPE);
            return ValType.Boolean;
        }
        case BITAND: {
            // 11.10 Binary Bitwise Operators ( & )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.and(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case BITXOR: {
            // 11.10 Binary Bitwise Operators ( ^ )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.xor(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case BITOR: {
            // 11.10 Binary Bitwise Operators ( | )
            ValType ltype = evalAndGetValue(left, mv);
            if (ltype.isPrimitive() || right instanceof ValueLiteral) {
                ToInt32(ltype, mv);
            }
            ValType rtype = evalAndGetValue(right, mv);
            if (!(ltype.isPrimitive() || right instanceof ValueLiteral)) {
                mv.swap(ltype, rtype);
                ToInt32(ltype, mv);
                mv.swap(rtype, ValType.Number_int);
            }
            ToInt32(rtype, mv);
            mv.or(Type.INT_TYPE);
            return ValType.Number_int;
        }

        case AND:
        case OR: {
            // 11.11 Binary Logical Operators
            Label after = new Label();

            ValType ltype = evalAndGetValue(left, mv);
            mv.toBoxed(ltype);
            mv.dup();
            ToBoolean(ValType.Any, mv);
            if (node.getOperator() == BinaryExpression.Operator.AND) {
                mv.ifeq(after);
            } else {
                mv.ifne(after);
            }
            mv.pop();
            ValType rtype = evalAndGetValue(right, mv);
            mv.toBoxed(rtype);
            mv.mark(after);

            return ValType.Any;
        }
        default:
            throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    @Override
    public ValType visit(BooleanLiteral node, MethodGenerator mv) {
        mv.iconst(node.getValue());

        return ValType.Boolean;
    }

    @Override
    public ValType visit(CallExpression node, MethodGenerator mv) {
        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);

        // direct call to eval?
        boolean directEval = (node.getBase() instanceof Identifier && "eval"
                .equals(((Identifier) node.getBase()).getName()));
        EvaluateCall(node, node.getBase(), type, node.getArguments(), directEval, mv);

        return ValType.Any;
    }

    @Override
    public ValType visit(ClassExpression node, MethodGenerator mv) {
        String className = (node.getName() != null ? node.getName().getName() : null);
        ClassDefinitionEvaluation(node, className, mv);

        return ValType.Object;
    }

    @Override
    public ValType visit(CommaExpression node, MethodGenerator mv) {
        ValType type = null;
        List<Expression> list = node.getOperands();
        for (Expression e : list) {
            if (type != null) {
                mv.pop(type);
            }
            type = evalAndGetValue(e, mv);
        }
        assert type != null;
        return (type != ValType.Reference ? type : ValType.Any);
    }

    @Override
    public ValType visit(ConditionalExpression node, MethodGenerator mv) {
        Label l0 = new Label(), l1 = new Label();

        ValType typeTest = evalAndGetValue(node.getTest(), mv);
        ToBoolean(typeTest, mv);
        mv.ifeq(l0);
        ValType typeThen = evalAndGetValue(node.getThen(), mv);
        mv.toBoxed(typeThen);
        mv.goTo(l1);
        mv.mark(l0);
        ValType typeOtherwise = evalAndGetValue(node.getOtherwise(), mv);
        mv.toBoxed(typeOtherwise);
        mv.mark(l1);

        return ValType.Any;
    }

    @Override
    public ValType visit(ElementAccessor node, MethodGenerator mv) {
        ValType baseType = evalAndGetValue(node.getBase(), mv);
        mv.toBoxed(baseType);
        ValType elementType = evalAndGetValue(node.getElement(), mv);
        mv.toBoxed(elementType);
        mv.load(Register.Realm);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_getElement);

        return ValType.Reference;
    }

    @Override
    public ValType visit(FunctionExpression node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionExpression
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_EvaluateFunctionExpression);

        return ValType.Object;
    }

    @Override
    public ValType visit(GeneratorComprehension node, MethodGenerator mv) {
        codegen.compile(node, mv);

        String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext);
        mv.invokeStaticMH(codegen.getClassName(), codegen.methodName(node), desc);
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorComprehension);

        return ValType.Object;
    }

    @Override
    public ValType visit(GeneratorExpression node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionExpression
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_EvaluateGeneratorExpression);

        return ValType.Object;
    }

    @Override
    public ValType visit(Identifier node, MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.aconst(node.getName());
        if (mv.isStrict()) {
            mv.invoke(Methods.ExecutionContext_strictIdentifierResolution);
        } else {
            mv.invoke(Methods.ExecutionContext_nonstrictIdentifierResolution);
        }

        return ValType.Reference;
    }

    @Override
    public ValType visit(NewExpression node, MethodGenerator mv) {
        ValType type = evalAndGetValue(node.getExpression(), mv);
        mv.toBoxed(type);
        List<Expression> arguments = node.getArguments();
        if (arguments.isEmpty()) {
            mv.get(Fields.ScriptRuntime_EMPTY_ARRAY);
        } else {
            boolean hasSpread = false;
            mv.newarray(arguments.size(), Types.Object);
            for (int i = 0, size = arguments.size(); i < size; ++i) {
                mv.dup();
                mv.iconst(i);
                /* [11.2.5 Argument Lists] ArgumentListEvaluation */
                Expression argument = arguments.get(i);
                hasSpread |= (argument instanceof SpreadElement);
                ValType argtype = evalAndGetValue(argument, mv);
                mv.toBoxed(argtype);
                mv.astore(Types.Object);
            }
            if (hasSpread) {
                mv.invoke(Methods.ScriptRuntime_toFlatArray);
            }
        }
        mv.lineInfo(node);
        mv.load(Register.Realm);
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorCall);

        return ValType.Any;
    }

    @Override
    public ValType visit(NullLiteral node, MethodGenerator mv) {
        mv.get(Fields.Null_NULL);

        return ValType.Null;
    }

    @Override
    public ValType visit(NumericLiteral node, MethodGenerator mv) {
        double v = node.getValue();
        if ((int) v == v) {
            mv.iconst((int) v);
            return ValType.Number_int;
        } else {
            mv.dconst(v);
            return ValType.Number;
        }
    }

    @Override
    public ValType visit(ObjectLiteral node, MethodGenerator mv) {
        // deferred static semantics validation :(
        ObjectLiteralStaticSemantics.validate(node, mv.isStrict());

        mv.load(Register.Realm);
        mv.invoke(Methods.OrdinaryObject_ObjectCreate);
        for (PropertyDefinition property : node.getProperties()) {
            mv.dup();
            codegen.propertyDefinition(property, mv);
        }

        return ValType.Object;
    }

    @Override
    public ValType visit(PropertyAccessor node, MethodGenerator mv) {
        ValType type = evalAndGetValue(node.getBase(), mv);
        mv.toBoxed(type);
        mv.aconst(node.getName());
        mv.load(Register.Realm);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ScriptRuntime_getProperty);

        return ValType.Reference;
    }

    @Override
    public ValType visit(RegularExpressionLiteral node, MethodGenerator mv) {
        mv.load(Register.Realm);
        mv.aconst(node.getRegexp());
        mv.aconst(node.getFlags());
        mv.invoke(Methods.ScriptRuntime_RegExp);

        return ValType.Object;
    }

    @Override
    public ValType visit(SpreadElement node, MethodGenerator mv) {
        ValType type = evalAndGetValue(node.getExpression(), mv);
        mv.toBoxed(type);
        mv.load(Register.Realm);
        mv.invoke(Methods.ScriptRuntime_SpreadArray);

        return ValType.Any; // actually Object[]
    }

    @Override
    public ValType visit(StringLiteral node, MethodGenerator mv) {
        mv.aconst(node.getValue());

        return ValType.String;
    }

    @Override
    public ValType visit(SuperExpression node, MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ScriptRuntime_GetThisEnvironmentOrThrow);
        if (node.getName() != null) {
            mv.aconst(node.getName());
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getSuperProperty);
            return ValType.Reference;
        } else if (node.getExpression() != null) {
            ValType type = evalAndGetValue(node.getExpression(), mv);
            mv.toBoxed(type);
            mv.load(Register.Realm);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getSuperElement);
            return ValType.Reference;
        } else {
            assert node.getArguments() != null;
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.ScriptRuntime_getSuperMethod);

            EvaluateCall(node, node, ValType.Reference, node.getArguments(), false, mv);

            return ValType.Any;
        }
    }

    @Override
    public ValType visit(TemplateCallExpression node, MethodGenerator mv) {
        codegen.compile(node.getTemplate());

        TemplateLiteral template = node.getTemplate();
        List<Expression> substitutions = Substitutions(template);
        List<Expression> arguments = new ArrayList<>(substitutions.size() + 1);
        arguments.add(template);
        arguments.addAll(substitutions);

        ValType type = node.getBase().accept(this, mv);
        mv.toBoxed(type);
        EvaluateCall(node, node.getBase(), type, arguments, false, mv);

        return ValType.Any;
    }

    @Override
    public ValType visit(TemplateLiteral node, MethodGenerator mv) {
        if (node.isTagged()) {
            codegen.GetTemplateCallSite(node, mv);
            return ValType.Object;
        }

        mv.anew(Types.StringBuilder);
        mv.dup();
        mv.invoke(Methods.StringBuilder_init);

        boolean chars = true;
        for (Expression expr : node.getElements()) {
            assert chars == (expr instanceof TemplateCharacters);
            if (chars) {
                mv.aconst(((TemplateCharacters) expr).getValue());
                mv.invoke(Methods.StringBuilder_append_String);
            } else {
                ValType type = evalAndGetValue(expr, mv);
                ToString(type, mv);
                mv.invoke(Methods.StringBuilder_append_Charsequence);
            }
            chars = !chars;
        }

        mv.invoke(Methods.StringBuilder_toString);

        return ValType.String;
    }

    @Override
    public ValType visit(ThisExpression node, MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ExecutionContext_thisResolution);

        return ValType.Any;
    }

    @Override
    public ValType visit(UnaryExpression node, MethodGenerator mv) {
        switch (node.getOperator()) {
        case POST_INC: {
            // 11.3.1 Postfix Increment Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            assert !type.isPrimitive() : "reference lhs for inc/dec";
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dupX(type, ValType.Number);
            mv.dconst(1d);
            mv.add(Type.DOUBLE_TYPE);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case POST_DEC: {
            // 11.3.2 Postfix Decrement Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            assert !type.isPrimitive() : "reference lhs for inc/dec";
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dupX(type, ValType.Number);
            mv.dconst(1d);
            mv.sub(Type.DOUBLE_TYPE);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case DELETE: {
            // 11.4.1 The delete Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_delete);
            return ValType.Boolean;
        }
        case VOID: {
            // 11.4.2 The void Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            mv.pop(type);
            mv.get(Fields.Undefined_UNDEFINED);
            return ValType.Undefined;
        }
        case TYPEOF: {
            // 11.4.3 The typeof Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            mv.toBoxed(type);
            mv.load(Register.Realm);
            mv.invoke(Methods.ScriptRuntime_typeof);
            return ValType.String;
        }
        case PRE_INC: {
            // 11.4.4 Prefix Increment Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            assert !type.isPrimitive() : "reference lhs for inc/dec";
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dconst(1d);
            mv.add(Type.DOUBLE_TYPE);
            mv.dupX(type, ValType.Number);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case PRE_DEC: {
            // 11.4.5 Prefix Decrement Operator
            Expression expr = node.getOperand();
            ValType type = expr.accept(this, mv);
            assert !type.isPrimitive() : "reference lhs for inc/dec";
            mv.dup();
            GetValue(expr, type, mv);
            ToNumber(type, mv);
            mv.dconst(1d);
            mv.sub(Type.DOUBLE_TYPE);
            mv.dupX(type, ValType.Number);
            mv.toBoxed(ValType.Number);
            PutValue(expr, type, mv);
            return ValType.Number;
        }
        case POS: {
            // 11.4.6 Unary + Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            return ValType.Number;
        }
        case NEG: {
            // 11.4.7 Unary - Operator
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToNumber(type, mv);
            mv.neg(Type.DOUBLE_TYPE);
            return ValType.Number;
        }
        case BITNOT: {
            // 11.4.8 Bitwise NOT Operator ( ~ )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToInt32(type, mv);
            mv.iconst(-1);
            mv.xor(Type.INT_TYPE);
            return ValType.Number_int;
        }
        case NOT: {
            // 11.4.9 Logical NOT Operator ( ! )
            Expression expr = node.getOperand();
            ValType type = evalAndGetValue(expr, mv);
            ToBoolean(type, mv);
            mv.iconst(1);
            mv.xor(Type.INT_TYPE);
            return ValType.Boolean;
        }
        default:
            throw new IllegalStateException(Objects.toString(node.getOperator(), "<null>"));
        }
    }

    @Override
    public ValType visit(YieldExpression node, MethodGenerator mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            ValType type = evalAndGetValue(expr, mv);
            mv.toBoxed(type);
        } else {
            mv.get(Fields.Undefined_UNDEFINED);
        }

        mv.load(Register.ExecutionContext);
        if (node.isDelegatedYield()) {
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_delegatedYield);
        } else {
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_yield);
        }

        return ValType.Any;
    }
}

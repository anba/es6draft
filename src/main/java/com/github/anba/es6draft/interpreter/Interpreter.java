/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckCallable;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckConstructor;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.IsBuiltinEval;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Reference.GetValue;
import static com.github.anba.es6draft.runtime.types.Reference.PutValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BinaryExpression.Operator;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Simple interpreter to speed-up `eval` evaluation
 */
public final class Interpreter extends DefaultNodeVisitor<Object, ExecutionContext> {
    /**
     * Returns a new {@link InterpretedScript} if {@code parsedScript} can be interpreted, otherwise
     * returns {@code null}.
     * 
     * @param parsedScript
     *            the script node
     * @return the interpreted script or {@code null}
     */
    public static InterpretedScript script(Script parsedScript) {
        if (!parsedScript.accept(InterpreterTest.INSTANCE, null)) {
            return null;
        }
        return new InterpretedScript(parsedScript);
    }

    private final boolean strict;
    private final boolean globalCode;
    private final boolean globalScope;
    private final boolean globalThis;
    private final boolean enclosedByWithStatement;
    private final boolean enclosedByLexicalDeclaration;

    public Interpreter(Script parsedScript) {
        this.strict = parsedScript.isStrict();
        this.globalCode = parsedScript.isGlobalCode();
        this.globalScope = parsedScript.isGlobalScope();
        this.globalThis = parsedScript.isGlobalThis();
        this.enclosedByWithStatement = parsedScript.isEnclosedByWithStatement();
        this.enclosedByLexicalDeclaration = parsedScript.isEnclosedByLexicalDeclaration();
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * 12.4.4 Postfix Increment Operator.
     * 
     * @param lhs
     *            the left-hand side expression
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double postIncrement(Object lhs, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(lhs, cx));
        double newValue = oldValue + 1;
        PutValue(lhs, newValue, cx);
        return oldValue;
    }

    /**
     * 12.4.5 Postfix Decrement Operator
     * 
     * @param lhs
     *            the left-hand side expression
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double postDecrement(Object lhs, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(lhs, cx));
        double newValue = oldValue - 1;
        PutValue(lhs, newValue, cx);
        return oldValue;
    }

    /**
     * 12.5.4 The delete Operator
     * 
     * @param expr
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static boolean delete(Object expr, ExecutionContext cx) {
        if (!(expr instanceof Reference)) {
            return true;
        }
        return ((Reference<?, ?>) expr).delete(cx);
    }

    /**
     * 12.5.5 The void Operator
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Undefined _void(Object value, ExecutionContext cx) {
        return UNDEFINED;
    }

    /**
     * 12.5.7 Prefix Increment Operator
     * 
     * @param expr
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double preIncrement(Object expr, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(expr, cx));
        double newValue = oldValue + 1;
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.5.8 Prefix Decrement Operator
     * 
     * @param expr
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double preDecrement(Object expr, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(expr, cx));
        double newValue = oldValue - 1;
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.5.9 Unary + Operator
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double pos(Object value, ExecutionContext cx) {
        return ToNumber(cx, value);
    }

    /**
     * 12.5.10 Unary - Operator
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double neg(Object value, ExecutionContext cx) {
        double oldValue = ToNumber(cx, value);
        return -oldValue;
    }

    /**
     * 12.5.11 Bitwise NOT Operator ( ~ )
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer bitnot(Object value, ExecutionContext cx) {
        int oldValue = ToInt32(cx, value);
        return ~oldValue;
    }

    /**
     * 12.5.12 Logical NOT Operator ( ! )
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean not(Object value, ExecutionContext cx) {
        boolean oldValue = ToBoolean(value);
        return !oldValue;
    }

    /**
     * Extension: Exponentiation Operator
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double exp(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return Math.pow(lnum, rnum);
    }

    /**
     * 12.6 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double mul(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum * rnum;
    }

    /**
     * 12.6 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double div(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum / rnum;
    }

    /**
     * 12.6 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double mod(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum % rnum;
    }

    /**
     * 12.7.2 The Subtraction Operator ( - )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Double sub(Object lval, Object rval, ExecutionContext cx) {
        double lnum = ToNumber(cx, lval);
        double rnum = ToNumber(cx, rval);
        return lnum - rnum;
    }

    /**
     * 12.8.1 The Left Shift Operator ( {@literal <<} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer leftShift(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum << shiftCount;
    }

    /**
     * 12.8.2 The Signed Right Shift Operator ( {@literal >>} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer rightShift(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >> shiftCount;
    }

    /**
     * 12.8.3 The Unsigned Right Shift Operator ( {@literal >>>} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Long unsignedRightShift(Object lval, Object rval, ExecutionContext cx) {
        long lnum = ToUint32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >>> shiftCount;
    }

    /**
     * 12.9 Relational Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean _instanceof(Object lval, Object rval, ExecutionContext cx) {
        return ScriptRuntime.InstanceofOperator(lval, rval, cx);
    }

    /**
     * 12.9 Relational Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean lessThan(Object lval, Object rval, ExecutionContext cx) {
        return RelationalComparison(cx, lval, rval, true) == 1;
    }

    /**
     * 12.9 Relational Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean lessThanEqual(Object lval, Object rval, ExecutionContext cx) {
        return RelationalComparison(cx, rval, lval, false) == 0;
    }

    /**
     * 12.9 Relational Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean greaterThan(Object lval, Object rval, ExecutionContext cx) {
        return RelationalComparison(cx, rval, lval, false) == 1;
    }

    /**
     * 12.9 Relational Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean greaterThanEqual(Object lval, Object rval, ExecutionContext cx) {
        return RelationalComparison(cx, lval, rval, true) == 0;
    }

    /**
     * 12.10 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean equals(Object lval, Object rval, ExecutionContext cx) {
        return EqualityComparison(cx, rval, lval);
    }

    /**
     * 12.10 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean notEquals(Object lval, Object rval, ExecutionContext cx) {
        return !EqualityComparison(cx, rval, lval);
    }

    /**
     * 12.10 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean strictEquals(Object lval, Object rval, ExecutionContext cx) {
        return StrictEqualityComparison(rval, lval);
    }

    /**
     * 12.10 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Boolean strictNotEquals(Object lval, Object rval, ExecutionContext cx) {
        return !StrictEqualityComparison(rval, lval);
    }

    /**
     * 12.11 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer bitand(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        int rnum = ToInt32(cx, rval);
        return lnum & rnum;
    }

    /**
     * 12.11 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer bitxor(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        int rnum = ToInt32(cx, rval);
        return lnum ^ rnum;
    }

    /**
     * 12.11 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Integer bitor(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        int rnum = ToInt32(cx, rval);
        return lnum | rnum;
    }

    /* ----------------------------------------------------------------------------------------- */

    @Override
    protected Object visit(Node node, ExecutionContext cx) {
        throw new IllegalStateException();
    }

    @Override
    public Object visit(Script node, ExecutionContext cx) {
        Object completionValue = UNDEFINED;
        for (StatementListItem stmt : node.getStatements()) {
            Object val = stmt.accept(this, cx);
            if (val != null) {
                completionValue = val;
            }
        }
        return completionValue;
    }

    @Override
    public Object visit(VariableStatement node, ExecutionContext cx) {
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, cx);
        }
        return null;
    }

    @Override
    public Object visit(VariableDeclaration node, ExecutionContext cx) {
        BindingIdentifier binding = (BindingIdentifier) node.getBinding();
        Expression initializer = node.getInitializer();
        if (initializer != null) {
            Object val = initializer.accept(this, cx);
            val = GetValue(val, cx);
            cx.resolveBinding(binding.getName().getIdentifier(), strict).putValue(val, cx);
        }
        return null;
    }

    @Override
    public Object visit(ExpressionStatement node, ExecutionContext cx) {
        Object value = node.getExpression().accept(this, cx);
        value = GetValue(value, cx);
        return value;
    }

    @Override
    public Object visit(AssignmentExpression node, ExecutionContext cx) {
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            Object lref = node.getLeft().accept(this, cx);
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, cx);
            PutValue(lref, rval, cx);
            return rval;
        } else {
            Object lref = node.getLeft().accept(this, cx);
            Object lval = GetValue(lref, cx);
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, cx);

            switch (node.getOperator()) {
            case ASSIGN_ADD:
                rval = ScriptRuntime.add(lval, rval, cx);
                break;
            case ASSIGN_BITAND:
                rval = bitand(lval, rval, cx);
                break;
            case ASSIGN_BITOR:
                rval = bitor(lval, rval, cx);
                break;
            case ASSIGN_BITXOR:
                rval = bitxor(lval, rval, cx);
                break;
            case ASSIGN_DIV:
                rval = div(lval, rval, cx);
                break;
            case ASSIGN_EXP:
                rval = exp(lval, rval, cx);
                break;
            case ASSIGN_MOD:
                rval = mod(lval, rval, cx);
                break;
            case ASSIGN_MUL:
                rval = mul(lval, rval, cx);
                break;
            case ASSIGN_SHL:
                rval = leftShift(lval, rval, cx);
                break;
            case ASSIGN_SHR:
                rval = rightShift(lval, rval, cx);
                break;
            case ASSIGN_SUB:
                rval = sub(lval, rval, cx);
                break;
            case ASSIGN_USHR:
                rval = unsignedRightShift(lval, rval, cx);
                break;
            case ASSIGN:
            default:
                throw new AssertionError();
            }
            PutValue(lref, rval, cx);
            return rval;
        }
    }

    @Override
    public Object visit(BinaryExpression node, ExecutionContext cx) {
        if (node.getOperator() == BinaryExpression.Operator.AND
                || node.getOperator() == BinaryExpression.Operator.OR) {
            return visitAndOr(node, cx);
        }

        // evaluate lhs/rhs and call GetValue()
        /* steps 1-6 */
        Object lval = node.getLeft().accept(this, cx);
        lval = GetValue(lval, cx);
        Object rval = node.getRight().accept(this, cx);
        rval = GetValue(rval, cx);

        // call binary operator specific code
        switch (node.getOperator()) {
        case ADD:
            return ScriptRuntime.add(lval, rval, cx);
        case BITAND:
            return bitand(lval, rval, cx);
        case BITOR:
            return bitor(lval, rval, cx);
        case BITXOR:
            return bitxor(lval, rval, cx);
        case DIV:
            return div(lval, rval, cx);
        case EQ:
            return equals(lval, rval, cx);
        case EXP:
            return exp(lval, rval, cx);
        case GE:
            return greaterThanEqual(lval, rval, cx);
        case GT:
            return greaterThan(lval, rval, cx);
        case IN:
            return ScriptRuntime.in(lval, rval, cx);
        case INSTANCEOF:
            return _instanceof(lval, rval, cx);
        case LE:
            return lessThanEqual(lval, rval, cx);
        case LT:
            return lessThan(lval, rval, cx);
        case MOD:
            return mod(lval, rval, cx);
        case MUL:
            return mul(lval, rval, cx);
        case NE:
            return notEquals(lval, rval, cx);
        case SHEQ:
            return strictEquals(lval, rval, cx);
        case SHL:
            return leftShift(lval, rval, cx);
        case SHNE:
            return strictNotEquals(lval, rval, cx);
        case SHR:
            return rightShift(lval, rval, cx);
        case SUB:
            return sub(lval, rval, cx);
        case USHR:
            return unsignedRightShift(lval, rval, cx);
        case AND:
        case OR:
        default:
            throw new AssertionError();
        }
    }

    private Object visitAndOr(BinaryExpression node, ExecutionContext cx) {
        Object lval = node.getLeft().accept(this, cx);
        lval = GetValue(lval, cx);
        if (ToBoolean(lval) ^ node.getOperator() == Operator.AND) {
            return lval;
        } else {
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, cx);
            return rval;
        }
    }

    @Override
    public Object visit(UnaryExpression node, ExecutionContext cx) {
        Object val = node.getOperand().accept(this, cx);
        switch (node.getOperator()) {
        case BITNOT:
            return bitnot(GetValue(val, cx), cx);
        case DELETE:
            return delete(val, cx);
        case NEG:
            return neg(GetValue(val, cx), cx);
        case NOT:
            return not(GetValue(val, cx), cx);
        case POS:
            return pos(GetValue(val, cx), cx);
        case POST_DEC:
            return postDecrement(val, cx);
        case POST_INC:
            return postIncrement(val, cx);
        case PRE_DEC:
            return preDecrement(val, cx);
        case PRE_INC:
            return preIncrement(val, cx);
        case TYPEOF:
            return ScriptRuntime.typeof(val, cx);
        case VOID:
            return _void(GetValue(val, cx), cx);
        default:
            throw new AssertionError();
        }
    }

    @Override
    public Object visit(CommaExpression node, ExecutionContext cx) {
        assert !node.getOperands().isEmpty();
        Object val = null;
        for (Expression expression : node.getOperands()) {
            val = GetValue(expression.accept(this, cx), cx);
        }
        return val;
    }

    @Override
    public Object visit(ConditionalExpression node, ExecutionContext cx) {
        Object test = node.getTest().accept(this, cx);
        test = GetValue(test, cx);
        Object val;
        if (ToBoolean(test)) {
            val = node.getThen().accept(this, cx);
        } else {
            val = node.getOtherwise().accept(this, cx);
        }
        return GetValue(val, cx);
    }

    @Override
    public Object visit(NullLiteral node, ExecutionContext cx) {
        return NULL;
    }

    @Override
    public Object visit(BooleanLiteral node, ExecutionContext cx) {
        return node.getValue();
    }

    @Override
    public Object visit(NumericLiteral node, ExecutionContext cx) {
        return node.getValue();
    }

    @Override
    public Object visit(StringLiteral node, ExecutionContext cx) {
        return node.getValue();
    }

    @Override
    public Object visit(RegularExpressionLiteral node, ExecutionContext cx) {
        return RegExpConstructor.RegExpCreate(cx, node.getRegexp(), node.getFlags());
    }

    @Override
    public Object visit(ObjectLiteral node, ExecutionContext cx) {
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        for (PropertyDefinition propertyDefinition : node.getProperties()) {
            assert propertyDefinition instanceof PropertyValueDefinition;
            PropertyValueDefinition propValDef = (PropertyValueDefinition) propertyDefinition;
            PropertyName propertyName = propValDef.getPropertyName();
            Expression propertyValue = propValDef.getPropertyValue();

            String propName = PropName(propertyName);
            long propIndex = propName != null ? IndexedMap.toIndex(propName) : -1;
            assert propName != null && !(propertyName instanceof ComputedPropertyName);
            Object value = propertyValue.accept(this, cx);
            value = GetValue(value, cx);

            if ("__proto__".equals(propName)
                    && cx.getRealm().isEnabled(CompatibilityOption.ProtoInitializer)) {
                ScriptRuntime.defineProtoProperty(obj, value, cx);
            } else if (IndexedMap.isIndex(propIndex)) {
                ScriptRuntime.defineProperty(obj, propIndex, value, cx);
            } else {
                ScriptRuntime.defineProperty(obj, propName, value, cx);
            }
        }
        return obj;
    }

    @Override
    public Object visit(ArrayLiteral node, ExecutionContext cx) {
        ArrayObject array = ArrayCreate(cx, 0);
        int nextIndex = 0;
        for (Expression element : node.getElements()) {
            if (element instanceof Elision) {
                // Elision
            } else {
                Object value = element.accept(this, cx);
                value = GetValue(value, cx);
                ScriptRuntime.defineProperty(array, nextIndex, value, cx);
            }
            nextIndex += 1;
        }
        Set(cx, array, "length", nextIndex, false);
        return array;
    }

    @Override
    public Object visit(CallExpression node, ExecutionContext cx) {
        Object ref = node.getBase().accept(this, cx);
        return EvaluateCall(ref, node.getArguments(), directEval(node), cx);
    }

    /**
     * 12.3.4.2 Runtime Semantics: EvaluateCall( ref, arguments, tailPosition )<br>
     * 12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )
     * 
     * @param ref
     *            the call base reference
     * @param arguments
     *            the function call arguments
     * @param directEval
     *            the direct eval flag
     * @param cx
     *            the execution context
     * @return the return value after applying the call operation
     */
    private Object EvaluateCall(Object ref, List<Expression> arguments, boolean directEval,
            ExecutionContext cx) {
        /* steps 1-2 (EvaluateCall) */
        Object func = GetValue(ref, cx);
        /* steps 3-4 (EvaluateCall) */
        Object thisValue = UNDEFINED;
        if (ref instanceof Reference) {
            Reference<?, ?> rref = (Reference<?, ?>) ref;
            if (rref.isPropertyReference()) {
                thisValue = rref.getThisValue();
            } else if (!(rref instanceof Reference.BindingReference)) {
                assert rref instanceof Reference.IdentifierReference;
                Reference.IdentifierReference<?> idref = (Reference.IdentifierReference<?>) rref;
                ScriptObject newThisValue = idref.getBase().withBaseObject();
                if (newThisValue != null) {
                    thisValue = newThisValue;
                }
            }
        }
        /* steps 1-2 (EvaluateDirectCall) */
        Object[] argList = ArgumentListEvaluation(arguments, cx);
        /* steps 3-4 (EvaluateDirectCall) */
        Callable f = CheckCallable(func, cx);
        /* [12.3.4.1 Runtime Semantics: Evaluation - step 3] */
        if (directEval && IsBuiltinEval(ref, f, cx)) {
            int evalFlags = EvalFlags.Direct.getValue();
            if (strict) {
                evalFlags |= EvalFlags.Strict.getValue();
            }
            if (globalCode) {
                evalFlags |= EvalFlags.GlobalCode.getValue();
            }
            if (globalScope) {
                evalFlags |= EvalFlags.GlobalScope.getValue();
            }
            if (globalThis) {
                evalFlags |= EvalFlags.GlobalThis.getValue();
            }
            if (enclosedByWithStatement) {
                evalFlags |= EvalFlags.EnclosedByWithStatement.getValue();
            }
            if (enclosedByLexicalDeclaration) {
                evalFlags |= EvalFlags.EnclosedByLexicalDeclaration.getValue();
            }
            return Eval.directEval(argList, cx, evalFlags);
        }
        if (directEval && ScriptRuntime.directEvalFallbackHook(cx) != null) {
            argList = ScriptRuntime.directEvalFallbackArguments(f, cx, thisValue, argList);
            thisValue = ScriptRuntime.directEvalFallbackThisArgument(cx);
            f = ScriptRuntime.directEvalFallbackHook(cx);
        }
        /* steps 5, 7-8 (EvaluateDirectCall) (not applicable) */
        /* steps 6, 9 (EvaluateDirectCall) */
        return f.call(cx, thisValue, argList);
    }

    private Object[] ArgumentListEvaluation(List<Expression> arguments, ExecutionContext cx) {
        int size = arguments.size();
        Object[] args = new Object[size];
        for (int i = 0; i < size; ++i) {
            Object arg = arguments.get(i).accept(this, cx);
            args[i] = GetValue(arg, cx);
        }
        return args;
    }

    private static boolean directEval(CallExpression node) {
        Expression base = node.getBase();
        if (base instanceof IdentifierReference
                && "eval".equals(((IdentifierReference) base).getName())) {
            return true;
        }
        return false;
    }

    @Override
    public Object visit(NewExpression node, ExecutionContext cx) {
        Object constructor = node.getExpression().accept(this, cx);
        constructor = GetValue(constructor, cx);
        Object[] args = ArgumentListEvaluation(node.getArguments(), cx);
        return CheckConstructor(constructor, cx).construct(cx, (Constructor) constructor, args);
    }

    @Override
    public Object visit(ElementAccessor node, ExecutionContext cx) {
        Object base = node.getBase().accept(this, cx);
        base = GetValue(base, cx);
        Object element = node.getElement().accept(this, cx);
        element = GetValue(element, cx);
        return ScriptRuntime.getElement(base, element, cx, strict);
    }

    @Override
    public Object visit(PropertyAccessor node, ExecutionContext cx) {
        Object base = node.getBase().accept(this, cx);
        base = GetValue(base, cx);
        return ScriptRuntime.getProperty(base, node.getName(), cx, strict);
    }

    @Override
    public Object visit(IdentifierReference node, ExecutionContext cx) {
        return cx.resolveBinding(node.getName(), strict);
    }

    @Override
    public Object visit(ThisExpression node, ExecutionContext cx) {
        return cx.resolveThisBinding();
    }

    /**
     * {@link NodeVisitor} to test whether or not the given nodes can be executed by the interpreter
     */
    private static final class InterpreterTest extends DefaultNodeVisitor<Boolean, Void> {
        static final DefaultNodeVisitor<Boolean, Void> INSTANCE = new InterpreterTest();

        @Override
        protected Boolean visit(Node node, Void value) {
            return false;
        }

        @Override
        public Boolean visit(Script node, Void value) {
            for (StatementListItem stmt : node.getStatements()) {
                if (!stmt.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(VariableStatement node, Void value) {
            for (VariableDeclaration decl : node.getElements()) {
                if (!decl.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(VariableDeclaration node, Void value) {
            Binding binding = node.getBinding();
            if (!(binding instanceof BindingIdentifier)) {
                return false;
            }
            Expression initializer = node.getInitializer();
            return initializer == null || initializer.accept(this, value);
        }

        @Override
        public Boolean visit(ExpressionStatement node, Void value) {
            return node.getExpression().accept(this, value);
        }

        @Override
        public Boolean visit(CallExpression node, Void value) {
            if (!node.getBase().accept(this, value)) {
                return false;
            }
            for (Expression expression : node.getArguments()) {
                if (!expression.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(NewExpression node, Void value) {
            if (!node.getExpression().accept(this, value)) {
                return false;
            }
            for (Expression expression : node.getArguments()) {
                if (!expression.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(ConditionalExpression node, Void value) {
            return node.getTest().accept(this, value) && node.getThen().accept(this, value)
                    && node.getOtherwise().accept(this, value);
        }

        @Override
        public Boolean visit(CommaExpression node, Void value) {
            for (Expression expression : node.getOperands()) {
                if (!expression.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(ElementAccessor node, Void value) {
            return node.getBase().accept(this, value) && node.getElement().accept(this, value);
        }

        @Override
        public Boolean visit(PropertyAccessor node, Void value) {
            return node.getBase().accept(this, value);
        }

        @Override
        public Boolean visit(IdentifierName node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(IdentifierReference node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(ThisExpression node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(RegularExpressionLiteral node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(ObjectLiteral node, Void value) {
            for (PropertyDefinition propertyDefinition : node.getProperties()) {
                if (!propertyDefinition.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Boolean visit(PropertyValueDefinition node, Void value) {
            return node.getPropertyName().accept(this, value)
                    && node.getPropertyValue().accept(this, value);
        }

        @Override
        public Boolean visit(ArrayLiteral node, Void value) {
            for (Expression expression : node.getElements()) {
                if (!expression.accept(this, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected Boolean visit(Literal node, Void value) {
            return true;
        }

        @Override
        public Boolean visit(AssignmentExpression node, Void value) {
            return node.getLeft().accept(this, value) && node.getRight().accept(this, value);
        }

        @Override
        public Boolean visit(BinaryExpression node, Void value) {
            return node.getLeft().accept(this, value) && node.getRight().accept(this, value);
        }

        @Override
        public Boolean visit(UnaryExpression node, Void value) {
            return node.getOperand().accept(this, value);
        }
    }
}

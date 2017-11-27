/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.language.CallOperations.CheckCallable;
import static com.github.anba.es6draft.runtime.language.CallOperations.CheckConstructor;
import static com.github.anba.es6draft.runtime.language.CallOperations.IsBuiltinEval;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Reference.GetValue;
import static com.github.anba.es6draft.runtime.types.Reference.PutValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BinaryExpression.Operator;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.language.ArrayOperations;
import com.github.anba.es6draft.runtime.language.CallOperations;
import com.github.anba.es6draft.runtime.language.ObjectOperations;
import com.github.anba.es6draft.runtime.language.Operators;
import com.github.anba.es6draft.runtime.language.PropertyOperations;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.objects.text.RegExpConstructor;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Basic interpreter to speed-up evaluation of simple eval-scripts.
 */
public final class Interpreter extends DefaultNodeVisitor<Object, ExecutionContext> {
    /**
     * Returns a new {@link InterpretedScript} if {@code parsedScript} can be interpreted, otherwise returns
     * {@code null}.
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

    private final EnumSet<Parser.Option> parserOptions;
    private final boolean strict;
    private int currentLine;

    Interpreter(Script parsedScript) {
        this.parserOptions = EnumSet.copyOf(parsedScript.getParserOptions());
        this.strict = parsedScript.isStrict();
        this.currentLine = parsedScript.getBeginLine();
    }

    int getCurrentLine() {
        return currentLine;
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * 12.4.4 Postfix Increment Operator
     * 
     * @param lhs
     *            the left-hand side expression
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number postIncrement(Reference<?, ?> lhs, ExecutionContext cx) {
        Number oldValue = ToNumeric(cx, GetValue(lhs, cx));
        if (oldValue.getClass() == Double.class) {
            double newValue = (double) oldValue + 1;
            PutValue(lhs, newValue, cx);
            return oldValue;
        }
        BigInteger newValue = BigIntType.add((BigInteger) oldValue, BigIntType.UNIT);
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
    private static Number postDecrement(Reference<?, ?> lhs, ExecutionContext cx) {
        Number oldValue = ToNumeric(cx, GetValue(lhs, cx));
        if (oldValue.getClass() == Double.class) {
            double newValue = (double) oldValue - 1;
            PutValue(lhs, newValue, cx);
            return oldValue;
        }
        BigInteger newValue = BigIntType.subtract((BigInteger) oldValue, BigIntType.UNIT);
        PutValue(lhs, newValue, cx);
        return oldValue;
    }

    /**
     * 12.4.6 Prefix Increment Operator
     * 
     * @param expr
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number preIncrement(Reference<?, ?> expr, ExecutionContext cx) {
        Number oldValue = ToNumeric(cx, GetValue(expr, cx));
        if (oldValue.getClass() == Double.class) {
            double newValue = (double) oldValue + 1;
            PutValue(expr, newValue, cx);
            return newValue;
        }
        BigInteger newValue = BigIntType.add((BigInteger) oldValue, BigIntType.UNIT);
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.4.7 Prefix Decrement Operator
     * 
     * @param expr
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number preDecrement(Reference<?, ?> expr, ExecutionContext cx) {
        Number oldValue = ToNumeric(cx, GetValue(expr, cx));
        if (oldValue.getClass() == Double.class) {
            double newValue = (double) oldValue - 1;
            PutValue(expr, newValue, cx);
            return newValue;
        }
        BigInteger newValue = BigIntType.subtract((BigInteger) oldValue, BigIntType.UNIT);
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.5.3 The delete Operator
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
     * 12.5.4 The void Operator
     * 
     * @param value
     *            the expression value
     * @return the return value after applying the operation
     */
    private static Undefined _void(Object value) {
        assert !(value instanceof Reference);
        return UNDEFINED;
    }

    /**
     * 12.5.6 Unary + Operator
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
     * 12.5.7 Unary - Operator
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number neg(Object value, ExecutionContext cx) {
        Number num = ToNumeric(cx, value);
        if (num.getClass() == Double.class) {
            return -(double) num;
        }
        return BigIntType.unaryMinus((BigInteger) num);
    }

    /**
     * 12.5.8 Bitwise NOT Operator ( ~ )
     * 
     * @param value
     *            the expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number bitnot(Object value, ExecutionContext cx) {
        Number num = ToNumericInt32(cx, value);
        if (num.getClass() == Integer.class) {
            return ~(int) num;
        }
        return BigIntType.bitwiseNOT((BigInteger) num);
    }

    /**
     * 12.5.9 Logical NOT Operator ( ! )
     * 
     * @param value
     *            the expression value
     * @return the return value after applying the operation
     */
    private static Boolean not(Object value) {
        return !ToBoolean(value);
    }

    /**
     * 12.6 Exponentiation Operator
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number exp(Object leftValue, Object rightValue, ExecutionContext cx) {
        Number lnum = ToNumeric(cx, leftValue);
        Number rnum = ToNumeric(cx, rightValue);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return Math.pow((double) lnum, (double) rnum);
        }
        return BigIntType.exponentiate(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.7 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number mul(Object leftValue, Object rightValue, ExecutionContext cx) {
        Number lnum = ToNumeric(cx, leftValue);
        Number rnum = ToNumeric(cx, rightValue);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return (double) lnum * (double) rnum;
        }
        return BigIntType.multiply((BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.7 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number div(Object leftValue, Object rightValue, ExecutionContext cx) {
        Number lnum = ToNumeric(cx, leftValue);
        Number rnum = ToNumeric(cx, rightValue);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return (double) lnum / (double) rnum;
        }
        return BigIntType.divide(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.7 Multiplicative Operators
     * 
     * @param leftValue
     *            the left-hand side expression value
     * @param rightValue
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number mod(Object leftValue, Object rightValue, ExecutionContext cx) {
        Number lnum = ToNumeric(cx, leftValue);
        Number rnum = ToNumeric(cx, rightValue);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return (double) lnum % (double) rnum;
        }
        return BigIntType.remainder(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.8.2 The Subtraction Operator ( - )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number sub(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumeric(cx, lval);
        Number rnum = ToNumeric(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Double.class) {
            return (double) lnum - (double) rnum;
        }
        return BigIntType.subtract((BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.9.1 The Left Shift Operator ( {@literal <<} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number leftShift(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return (int) lnum << ((int) rnum & 0x1f);
        }
        return BigIntType.leftShift(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.9.2 The Signed Right Shift Operator ( {@literal >>} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number rightShift(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return (int) lnum >> ((int) rnum & 0x1f);
        }
        return BigIntType.signedRightShift(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.9.3 The Unsigned Right Shift Operator ( {@literal >>>} )
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number unsignedRightShift(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return ((int) lnum & 0xffffffffL) >>> ((int) rnum & 0x1f);
        }
        return BigIntType.unsignedRightShift(cx, (BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.10 Relational Operators
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
        return Operators.InstanceofOperator(lval, rval, cx);
    }

    /**
     * 12.10 Relational Operators
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
     * 12.10 Relational Operators
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
     * 12.10 Relational Operators
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
     * 12.10 Relational Operators
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
     * 12.11 Equality Operators
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
     * 12.11 Equality Operators
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
     * 12.11 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @return the return value after applying the operation
     */
    private static Boolean strictEquals(Object lval, Object rval) {
        return StrictEqualityComparison(rval, lval);
    }

    /**
     * 12.11 Equality Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @return the return value after applying the operation
     */
    private static Boolean strictNotEquals(Object lval, Object rval) {
        return !StrictEqualityComparison(rval, lval);
    }

    /**
     * 12.12 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number bitand(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return (int) lnum & (int) rnum;
        }
        return BigIntType.bitwiseAND((BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.12 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number bitxor(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return (int) lnum ^ (int) rnum;
        }
        return BigIntType.bitwiseXOR((BigInteger) lnum, (BigInteger) rnum);
    }

    /**
     * 12.12 Binary Bitwise Operators
     * 
     * @param lval
     *            the left-hand side expression value
     * @param rval
     *            the right-hand side expression value
     * @param cx
     *            the execution context
     * @return the return value after applying the operation
     */
    private static Number bitor(Object lval, Object rval, ExecutionContext cx) {
        Number lnum = ToNumericInt32(cx, lval);
        Number rnum = ToNumericInt32(cx, rval);
        if (lnum.getClass() != rnum.getClass()) {
            throw newTypeError(cx, Messages.Key.BigIntNumber);
        }
        if (lnum.getClass() == Integer.class) {
            return (int) lnum | (int) rnum;
        }
        return BigIntType.bitwiseOR((BigInteger) lnum, (BigInteger) rnum);
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
            currentLine = binding.getBeginLine();
            Reference<?, String> lhs = cx.resolveBinding(binding.getName().getIdentifier(), strict);
            Object val = GetValue(initializer.accept(this, cx), cx);
            currentLine = node.getBeginLine();
            lhs.putValue(val, cx);
        }
        return null;
    }

    @Override
    public Object visit(ExpressionStatement node, ExecutionContext cx) {
        return GetValue(node.getExpression().accept(this, cx), cx);
    }

    @Override
    public Object visit(AssignmentExpression node, ExecutionContext cx) {
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            Reference<?, ?> lref = (Reference<?, ?>) node.getLeft().accept(this, cx);
            Object rval = GetValue(node.getRight().accept(this, cx), cx);
            currentLine = node.getBeginLine();
            PutValue(lref, rval, cx);
            return rval;
        } else {
            Reference<?, ?> lref = (Reference<?, ?>) node.getLeft().accept(this, cx);
            Object lval = GetValue(lref, cx);
            Object rval = GetValue(node.getRight().accept(this, cx), cx);
            currentLine = node.getBeginLine();
            Object r;
            switch (node.getOperator()) {
            case ASSIGN_ADD:
                r = Operators.add(lval, rval, cx);
                break;
            case ASSIGN_BITAND:
                r = bitand(lval, rval, cx);
                break;
            case ASSIGN_BITOR:
                r = bitor(lval, rval, cx);
                break;
            case ASSIGN_BITXOR:
                r = bitxor(lval, rval, cx);
                break;
            case ASSIGN_DIV:
                r = div(lval, rval, cx);
                break;
            case ASSIGN_EXP:
                r = exp(lval, rval, cx);
                break;
            case ASSIGN_MOD:
                r = mod(lval, rval, cx);
                break;
            case ASSIGN_MUL:
                r = mul(lval, rval, cx);
                break;
            case ASSIGN_SHL:
                r = leftShift(lval, rval, cx);
                break;
            case ASSIGN_SHR:
                r = rightShift(lval, rval, cx);
                break;
            case ASSIGN_SUB:
                r = sub(lval, rval, cx);
                break;
            case ASSIGN_USHR:
                r = unsignedRightShift(lval, rval, cx);
                break;
            case ASSIGN:
            default:
                throw new AssertionError();
            }
            PutValue(lref, r, cx);
            return r;
        }
    }

    @Override
    public Object visit(BinaryExpression node, ExecutionContext cx) {
        if (node.getOperator() == BinaryExpression.Operator.AND || node.getOperator() == BinaryExpression.Operator.OR) {
            return visitAndOr(node, cx);
        }
        /* steps 1-? */
        Object lval = GetValue(node.getLeft().accept(this, cx), cx);
        Object rval = GetValue(node.getRight().accept(this, cx), cx);
        currentLine = node.getBeginLine();
        switch (node.getOperator()) {
        case ADD:
            return Operators.add(lval, rval, cx);
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
            return Operators.in(lval, rval, cx);
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
            return strictEquals(lval, rval);
        case SHL:
            return leftShift(lval, rval, cx);
        case SHNE:
            return strictNotEquals(lval, rval);
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
        Object lval = GetValue(node.getLeft().accept(this, cx), cx);
        if (ToBoolean(lval) ^ node.getOperator() == Operator.AND) {
            return lval;
        }
        return GetValue(node.getRight().accept(this, cx), cx);
    }

    @Override
    public Object visit(UnaryExpression node, ExecutionContext cx) {
        Object val = node.getOperand().accept(this, cx);
        currentLine = node.getBeginLine();
        switch (node.getOperator()) {
        case BITNOT:
            return bitnot(GetValue(val, cx), cx);
        case DELETE:
            return delete(val, cx);
        case NEG:
            return neg(GetValue(val, cx), cx);
        case NOT:
            return not(GetValue(val, cx));
        case POS:
            return pos(GetValue(val, cx), cx);
        case TYPEOF:
            return Operators.typeof(val, cx);
        case VOID:
            return _void(GetValue(val, cx));
        default:
            throw new AssertionError();
        }
    }

    @Override
    public Object visit(UpdateExpression node, ExecutionContext cx) {
        Reference<?, ?> val = (Reference<?, ?>) node.getOperand().accept(this, cx);
        currentLine = node.getBeginLine();
        switch (node.getOperator()) {
        case POST_DEC:
            return postDecrement(val, cx);
        case POST_INC:
            return postIncrement(val, cx);
        case PRE_DEC:
            return preDecrement(val, cx);
        case PRE_INC:
            return preIncrement(val, cx);
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
        Object test = GetValue(node.getTest().accept(this, cx), cx);
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
    public Object visit(BigIntegerLiteral node, ExecutionContext value) {
        return node.getValue();
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
        currentLine = node.getBeginLine();
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
            Object value = GetValue(propertyValue.accept(this, cx), cx);

            if ("__proto__".equals(propName)
                    && cx.getRuntimeContext().isEnabled(CompatibilityOption.ProtoInitializer)) {
                ObjectOperations.defineProtoProperty(obj, value, cx);
            } else if (IndexedMap.isIndex(propIndex)) {
                ObjectOperations.defineProperty(obj, propIndex, value, cx);
            } else {
                ObjectOperations.defineProperty(obj, propName, value, cx);
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
                Object value = GetValue(element.accept(this, cx), cx);
                ArrayOperations.defineProperty(array, nextIndex, value);
            }
            nextIndex += 1;
        }
        Set(cx, array, "length", nextIndex, false);
        return array;
    }

    @Override
    public Object visit(CallExpression node, ExecutionContext cx) {
        Object ref = node.getBase().accept(this, cx);
        return EvaluateCall(node, ref, node.getArguments(), directEval(node), cx);
    }

    /**
     * 12.3.4.2 Runtime Semantics: EvaluateCall( ref, arguments, tailPosition )<br>
     * 12.3.4.3 Runtime Semantics: EvaluateDirectCall( func, thisValue, arguments, tailPosition )
     * 
     * @param node
     *            the function call node
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
    private Object EvaluateCall(CallExpression node, Object ref, List<Expression> arguments, boolean directEval,
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
        currentLine = node.getBeginLine();
        Callable f = CheckCallable(func, cx);
        /* [12.3.4.1 Runtime Semantics: Evaluation - step 3] */
        if (directEval && IsBuiltinEval(ref, f, cx)) {
            int evalFlags = EvalFlags.toFlags(node.getEvalFlags());
            if (strict) {
                // TODO: Remove this case when StrictDirectiveSimpleParameterList is the default.
                evalFlags |= EvalFlags.Strict.getValue();
            }
            if (parserOptions.contains(Parser.Option.EnclosedByWithStatement)) {
                evalFlags |= EvalFlags.EnclosedByWithStatement.getValue();
            }
            if (parserOptions.contains(Parser.Option.EnclosedByCatchStatement)) {
                evalFlags |= EvalFlags.EnclosedByCatchStatement.getValue();
            }
            if (parserOptions.contains(Parser.Option.EnclosedByLexicalDeclaration)) {
                evalFlags |= EvalFlags.EnclosedByLexicalDeclaration.getValue();
            }
            return Eval.directEval(argList, cx, evalFlags);
        }
        if (directEval && CallOperations.directEvalFallbackHook(cx) != null) {
            argList = CallOperations.directEvalFallbackArguments(f, cx, thisValue, argList);
            thisValue = CallOperations.directEvalFallbackThisArgument(cx);
            f = CallOperations.directEvalFallbackHook(cx);
        }
        /* steps 5, 7-8 (EvaluateDirectCall) (not applicable) */
        /* steps 6, 9 (EvaluateDirectCall) */
        return f.call(cx, thisValue, argList);
    }

    private Object[] ArgumentListEvaluation(List<Expression> arguments, ExecutionContext cx) {
        int size = arguments.size();
        Object[] args = new Object[size];
        for (int i = 0; i < size; ++i) {
            args[i] = GetValue(arguments.get(i).accept(this, cx), cx);
        }
        return args;
    }

    private static boolean directEval(CallExpression node) {
        Expression base = node.getBase();
        if (base instanceof IdentifierReference && "eval".equals(((IdentifierReference) base).getName())) {
            return true;
        }
        return false;
    }

    @Override
    public Object visit(NewExpression node, ExecutionContext cx) {
        Object constructor = node.getExpression().accept(this, cx);
        constructor = GetValue(constructor, cx);
        Object[] args = ArgumentListEvaluation(node.getArguments(), cx);
        currentLine = node.getBeginLine();
        return CheckConstructor(constructor, cx).construct(cx, args);
    }

    @Override
    public Object visit(NewTarget node, ExecutionContext cx) {
        return Operators.GetNewTargetOrUndefined(cx);
    }

    @Override
    public Object visit(ElementAccessor node, ExecutionContext cx) {
        Object base = GetValue(node.getBase().accept(this, cx), cx);
        Object element = GetValue(node.getElement().accept(this, cx), cx);
        currentLine = node.getBeginLine();
        return PropertyOperations.getElement(base, element, cx, strict);
    }

    @Override
    public Object visit(PropertyAccessor node, ExecutionContext cx) {
        Object base = GetValue(node.getBase().accept(this, cx), cx);
        currentLine = node.getBeginLine();
        return PropertyOperations.getProperty(base, node.getName(), cx, strict);
    }

    @Override
    public Object visit(IdentifierReference node, ExecutionContext cx) {
        currentLine = node.getBeginLine();
        return cx.resolveBinding(node.getName(), strict);
    }

    @Override
    public Object visit(ThisExpression node, ExecutionContext cx) {
        currentLine = node.getBeginLine();
        return cx.resolveThisBinding();
    }

    /**
     * {@link NodeVisitor} to test whether or not the script can be executed by the interpreter.
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
        public Boolean visit(NewTarget node, Void value) {
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
            return node.getPropertyName().accept(this, value) && node.getPropertyValue().accept(this, value);
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

        @Override
        public Boolean visit(UpdateExpression node, Void value) {
            return node.getOperand().accept(this, value);
        }
    }
}

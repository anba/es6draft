/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckCallable;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.GetCallThisValue;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.IsBuiltinEval;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Reference.GetValue;
import static com.github.anba.es6draft.runtime.types.Reference.PutValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BinaryExpression.Operator;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * Simple interpreter to speed-up `eval` evaluation
 */
public class Interpreter extends DefaultNodeVisitor<Object, ExecutionContext> {
    /**
     * Returns a new {@link InterpretedScript} if the supplied {@code script} can be interpreted,
     * otherwise returns {@code null}
     */
    public static InterpretedScript script(Script parsedScript) {
        if (!parsedScript.accept(InterpreterTest.INSTANCE, null)) {
            return null;
        }
        return new InterpretedScript(new ScriptBodyImpl(parsedScript));
    }

    private boolean strict;
    private boolean globalCode;

    public Interpreter(Script parsedScript) {
        this.strict = parsedScript.isStrict();
        this.globalCode = parsedScript.isGlobalCode();
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * 11.3.1 Postfix Increment Operator
     */
    private static Double postIncrement(Object lhs, Realm realm) {
        double oldValue = ToNumber(realm, GetValue(lhs, realm));
        double newValue = oldValue + 1;
        PutValue(lhs, newValue, realm);
        return oldValue;
    }

    /**
     * 11.3.2 Postfix Decrement Operator
     */
    private static Double postDecrement(Object lhs, Realm realm) {
        double oldValue = ToNumber(realm, GetValue(lhs, realm));
        double newValue = oldValue - 1;
        PutValue(lhs, newValue, realm);
        return oldValue;
    }

    /**
     * 11.4.2 The void Operator
     */
    private static Undefined _void(Object value, Realm realm) {
        return Undefined.UNDEFINED;
    }

    /**
     * 11.4.4 Prefix Increment Operator
     */
    private static Double preIncrement(Object expr, Realm realm) {
        double oldValue = ToNumber(realm, GetValue(expr, realm));
        double newValue = oldValue + 1;
        PutValue(expr, newValue, realm);
        return newValue;
    }

    /**
     * 11.4.5 Prefix Decrement Operator
     */
    private static Double preDecrement(Object expr, Realm realm) {
        double oldValue = ToNumber(realm, GetValue(expr, realm));
        double newValue = oldValue - 1;
        PutValue(expr, newValue, realm);
        return newValue;
    }

    /**
     * 11.4.6 Unary + Operator
     */
    private static Double pos(Object value, Realm realm) {
        return ToNumber(realm, value);
    }

    /**
     * 11.4.7 Unary - Operator
     */
    private static Double neg(Object value, Realm realm) {
        double oldValue = ToNumber(realm, value);
        return -oldValue;
    }

    /**
     * 11.4.8 Bitwise NOT Operator ( ~ )
     */
    private static Integer bitnot(Object value, Realm realm) {
        int oldValue = ToInt32(realm, value);
        return ~oldValue;
    }

    /**
     * 11.4.9 Logical NOT Operator ( ! )
     */
    private static Boolean not(Object value, Realm realm) {
        boolean oldValue = ToBoolean(value);
        return !oldValue;
    }

    /**
     * 11.5 Multiplicative Operators
     */
    private static Double mul(Object leftValue, Object rightValue, Realm realm) {
        double lnum = ToNumber(realm, leftValue);
        double rnum = ToNumber(realm, rightValue);
        return lnum * rnum;
    }

    /**
     * 11.5 Multiplicative Operators
     */
    private static Double div(Object leftValue, Object rightValue, Realm realm) {
        double lnum = ToNumber(realm, leftValue);
        double rnum = ToNumber(realm, rightValue);
        return lnum / rnum;
    }

    /**
     * 11.5 Multiplicative Operators
     */
    private static Double mod(Object leftValue, Object rightValue, Realm realm) {
        double lnum = ToNumber(realm, leftValue);
        double rnum = ToNumber(realm, rightValue);
        return lnum % rnum;
    }

    /**
     * 11.6.2 The Subtraction Operator ( - )
     */
    private static Double sub(Object lval, Object rval, Realm realm) {
        double lnum = ToNumber(realm, lval);
        double rnum = ToNumber(realm, rval);
        return lnum - rnum;
    }

    /**
     * 11.7.1 The Left Shift Operator ( << )
     */
    private static Integer leftShift(Object lval, Object rval, Realm realm) {
        int lnum = ToInt32(realm, lval);
        long rnum = ToUint32(realm, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum << shiftCount;
    }

    /**
     * 11.7.2 The Signed Right Shift Operator ( >> )
     */
    private static Integer rightShift(Object lval, Object rval, Realm realm) {
        int lnum = ToInt32(realm, lval);
        long rnum = ToUint32(realm, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >> shiftCount;
    }

    /**
     * 11.7.3 The Unsigned Right Shift Operator ( >>> )
     */
    private static Long unsignedRightShift(Object lval, Object rval, Realm realm) {
        long lnum = ToUint32(realm, lval);
        long rnum = ToUint32(realm, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >>> shiftCount;
    }

    /**
     * 11.8 Relational Operators
     */
    private static Boolean _instanceof(Object lval, Object rval, Realm realm) {
        return ScriptRuntime.instanceOfOperator(lval, rval, realm);
    }

    /**
     * 11.8 Relational Operators
     */
    private static Boolean lessThan(Object lval, Object rval, Realm realm) {
        int c = ScriptRuntime.relationalComparison(lval, rval, true, realm);
        return (c == 1);
    }

    /**
     * 11.8 Relational Operators
     */
    private static Boolean lessThanEqual(Object lval, Object rval, Realm realm) {
        int c = ScriptRuntime.relationalComparison(rval, lval, false, realm);
        return (c == 0);
    }

    /**
     * 11.8 Relational Operators
     */
    private static Boolean greaterThan(Object lval, Object rval, Realm realm) {
        int c = ScriptRuntime.relationalComparison(rval, lval, false, realm);
        return (c == 1);
    }

    /**
     * 11.8 Relational Operators
     */
    private static Boolean greaterThanEqual(Object lval, Object rval, Realm realm) {
        int c = ScriptRuntime.relationalComparison(lval, rval, true, realm);
        return (c == 0);
    }

    /**
     * 11.9 Equality Operators
     */
    private static Boolean equals(Object lval, Object rval, Realm realm) {
        return ScriptRuntime.equalityComparison(rval, lval, realm);
    }

    /**
     * 11.9 Equality Operators
     */
    private static Boolean notEquals(Object lval, Object rval, Realm realm) {
        return !ScriptRuntime.equalityComparison(rval, lval, realm);
    }

    /**
     * 11.9 Equality Operators
     */
    private static Boolean strictEquals(Object lval, Object rval, Realm realm) {
        return ScriptRuntime.strictEqualityComparison(rval, lval);
    }

    /**
     * 11.9 Equality Operators
     */
    private static Boolean strictNotEquals(Object lval, Object rval, Realm realm) {
        return !ScriptRuntime.strictEqualityComparison(rval, lval);
    }

    /**
     * 11.10 Binary Bitwise Operators
     */
    private static Integer bitand(Object lval, Object rval, Realm realm) {
        int lnum = ToInt32(realm, lval);
        int rnum = ToInt32(realm, rval);
        return lnum & rnum;
    }

    /**
     * 11.10 Binary Bitwise Operators
     */
    private static Integer bitxor(Object lval, Object rval, Realm realm) {
        int lnum = ToInt32(realm, lval);
        int rnum = ToInt32(realm, rval);
        return lnum ^ rnum;
    }

    /**
     * 11.10 Binary Bitwise Operators
     */
    private static Integer bitor(Object lval, Object rval, Realm realm) {
        int lnum = ToInt32(realm, lval);
        int rnum = ToInt32(realm, rval);
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
        Realm realm = cx.getRealm();
        BindingIdentifier binding = (BindingIdentifier) node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            Object val = initialiser.accept(this, cx);
            val = GetValue(val, realm);
            cx.identifierResolution(binding.getName(), strict).PutValue(val, realm);
        }
        return null;
    }

    @Override
    public Object visit(ExpressionStatement node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object value = node.getExpression().accept(this, cx);
        value = GetValue(value, realm);
        return value;
    }

    @Override
    public Object visit(AssignmentExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        if (node.getOperator() == AssignmentExpression.Operator.ASSIGN) {
            Object lref = node.getLeft().accept(this, cx);
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, realm);
            PutValue(lref, rval, realm);
            return rval;
        } else {
            Object lref = node.getLeft().accept(this, cx);
            Object lval = GetValue(lref, realm);
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, realm);

            switch (node.getOperator()) {
            case ASSIGN_ADD:
                rval = ScriptRuntime.add(lval, rval, realm);
                break;
            case ASSIGN_BITAND:
                rval = bitand(lval, rval, realm);
                break;
            case ASSIGN_BITOR:
                rval = bitor(lval, rval, realm);
                break;
            case ASSIGN_BITXOR:
                rval = bitxor(lval, rval, realm);
                break;
            case ASSIGN_DIV:
                rval = div(lval, rval, realm);
                break;
            case ASSIGN_MOD:
                rval = mod(lval, rval, realm);
                break;
            case ASSIGN_MUL:
                rval = mul(lval, rval, realm);
                break;
            case ASSIGN_SHL:
                rval = leftShift(lval, rval, realm);
                break;
            case ASSIGN_SHR:
                rval = rightShift(lval, rval, realm);
                break;
            case ASSIGN_SUB:
                rval = sub(lval, rval, realm);
                break;
            case ASSIGN_USHR:
                rval = unsignedRightShift(lval, rval, realm);
                break;
            case ASSIGN:
            default:
                throw new IllegalStateException();
            }
            PutValue(lref, rval, realm);
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
        Realm realm = cx.getRealm();
        /* step 1-6 */
        Object lval = node.getLeft().accept(this, cx);
        lval = GetValue(lval, realm);
        Object rval = node.getRight().accept(this, cx);
        rval = GetValue(rval, realm);

        // call binary operator specific code
        switch (node.getOperator()) {
        case ADD:
            return ScriptRuntime.add(lval, rval, realm);
        case BITAND:
            return bitand(lval, rval, realm);
        case BITOR:
            return bitor(lval, rval, realm);
        case BITXOR:
            return bitxor(lval, rval, realm);
        case DIV:
            return div(lval, rval, realm);
        case EQ:
            return equals(lval, rval, realm);
        case GE:
            return greaterThanEqual(lval, rval, realm);
        case GT:
            return greaterThan(lval, rval, realm);
        case IN:
            return ScriptRuntime.in(lval, rval, realm);
        case INSTANCEOF:
            return _instanceof(lval, rval, realm);
        case LE:
            return lessThanEqual(lval, rval, realm);
        case LT:
            return lessThan(lval, rval, realm);
        case MOD:
            return mod(lval, rval, realm);
        case MUL:
            return mul(lval, rval, realm);
        case NE:
            return notEquals(lval, rval, realm);
        case SHEQ:
            return strictEquals(lval, rval, realm);
        case SHL:
            return leftShift(lval, rval, realm);
        case SHNE:
            return strictNotEquals(lval, rval, realm);
        case SHR:
            return rightShift(lval, rval, realm);
        case SUB:
            return sub(lval, rval, realm);
        case USHR:
            return unsignedRightShift(lval, rval, realm);
        case AND:
        case OR:
        default:
            throw new IllegalStateException();
        }
    }

    private Object visitAndOr(BinaryExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object lval = node.getLeft().accept(this, cx);
        lval = GetValue(lval, realm);
        if (ToBoolean(lval) ^ node.getOperator() == Operator.AND) {
            return lval;
        } else {
            Object rval = node.getRight().accept(this, cx);
            rval = GetValue(rval, realm);
            return rval;
        }
    }

    @Override
    public Object visit(UnaryExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object val = node.getOperand().accept(this, cx);
        switch (node.getOperator()) {
        case BITNOT:
            return bitnot(GetValue(val, realm), realm);
        case DELETE:
            return ScriptRuntime.delete(val, realm);
        case NEG:
            return neg(GetValue(val, realm), realm);
        case NOT:
            return not(GetValue(val, realm), realm);
        case POS:
            return pos(GetValue(val, realm), realm);
        case POST_DEC:
            return postDecrement(val, realm);
        case POST_INC:
            return postIncrement(val, realm);
        case PRE_DEC:
            return preDecrement(val, realm);
        case PRE_INC:
            return preIncrement(val, realm);
        case TYPEOF:
            return ScriptRuntime.typeof(val, realm);
        case VOID:
            return _void(GetValue(val, realm), realm);
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public Object visit(CommaExpression node, ExecutionContext cx) {
        assert !node.getOperands().isEmpty();
        Realm realm = cx.getRealm();
        Object val = null;
        for (Expression expression : node.getOperands()) {
            val = GetValue(expression.accept(this, cx), realm);
        }
        return val;
    }

    @Override
    public Object visit(ConditionalExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object test = node.getTest().accept(this, cx);
        test = GetValue(test, realm);
        Object val;
        if (ToBoolean(test)) {
            val = node.getThen().accept(this, cx);
        } else {
            val = node.getOtherwise().accept(this, cx);
        }
        return GetValue(val, realm);
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
        return ScriptRuntime.RegExp(cx.getRealm(), node.getRegexp(), node.getFlags());
    }

    @Override
    public Object visit(CallExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object ref = node.getBase().accept(this, cx);
        Object func = GetValue(ref, realm);
        List<Expression> arguments = node.getArguments();
        int size = arguments.size();
        Object[] args = new Object[size];
        for (int i = 0; i < size; ++i) {
            Object arg = arguments.get(i).accept(this, cx);
            args[i] = GetValue(arg, realm);
        }
        Callable f = CheckCallable(func, realm);
        if (directEval(node) && IsBuiltinEval(ref, f, realm)) {
            Object x = args.length > 0 ? args[0] : Undefined.UNDEFINED;
            return Eval.directEval(x, cx, strict, globalCode);
        }
        Object thisValue = GetCallThisValue(ref, realm);
        Object result = f.call(thisValue, args);
        return result;
    }

    private static boolean directEval(CallExpression node) {
        Expression base = node.getBase();
        if (base instanceof Identifier && "eval".equals(((Identifier) base).getName())) {
            return true;
        }
        return false;
    }

    @Override
    public Object visit(NewExpression node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object constructor = node.getExpression().accept(this, cx);
        constructor = GetValue(constructor, realm);
        List<Expression> arguments = node.getArguments();
        int size = arguments.size();
        Object[] args = new Object[size];
        for (int i = 0; i < size; ++i) {
            Object arg = arguments.get(i).accept(this, cx);
            args[i] = GetValue(arg, realm);
        }
        return ScriptRuntime.EvaluateConstructorCall(constructor, args, realm);
    }

    @Override
    public Object visit(ElementAccessor node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object base = node.getBase().accept(this, cx);
        base = GetValue(base, realm);
        Object element = node.getElement().accept(this, cx);
        element = GetValue(element, realm);
        return ScriptRuntime.getElement(base, element, realm, strict);
    }

    @Override
    public Object visit(PropertyAccessor node, ExecutionContext cx) {
        Realm realm = cx.getRealm();
        Object base = node.getBase().accept(this, cx);
        base = GetValue(base, realm);
        return ScriptRuntime.getProperty(base, node.getName(), realm, strict);
    }

    @Override
    public Object visit(Identifier node, ExecutionContext cx) {
        return cx.identifierResolution(node.getName(), strict);
    }

    @Override
    public Object visit(ThisExpression node, ExecutionContext cx) {
        return cx.thisResolution();
    }

    static class ScriptBodyImpl implements RuntimeInfo.ScriptBody {
        private Script parsedScript;

        ScriptBodyImpl(Script parsedScript) {
            this.parsedScript = parsedScript;
        }

        @Override
        public boolean isStrict() {
            return parsedScript.isStrict();
        }

        @Override
        public void globalDeclarationInstantiation(Realm realm, LexicalEnvironment globalEnv,
                boolean deletableBindings) {
            DeclarationBindingInstantiation.GlobalDeclarationInstantiation(realm, globalEnv,
                    parsedScript, deletableBindings);
        }

        @Override
        public void evalDeclarationInstantiation(Realm realm, LexicalEnvironment lexEnv,
                LexicalEnvironment varEnv, boolean deletableBindings) {
            DeclarationBindingInstantiation.EvalDeclarationInstantiation(realm, lexEnv, varEnv,
                    parsedScript, deletableBindings);
        }

        @Override
        public Object evaluate(ExecutionContext cx) {
            return parsedScript.accept(new Interpreter(parsedScript), cx);
        }
    }

    private static class InterpreterTest extends DefaultNodeVisitor<Boolean, Void> {
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
            Expression initialiser = node.getInitialiser();
            return initialiser == null || initialiser.accept(this, value);
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
        public Boolean visit(Identifier node, Void value) {
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

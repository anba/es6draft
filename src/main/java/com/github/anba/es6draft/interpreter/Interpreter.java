/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.EvalDeclarationInstantiation;
import static com.github.anba.es6draft.interpreter.DeclarationBindingInstantiation.GlobalDeclarationInstantiation;
import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.CheckCallable;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.IsBuiltinEval;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Reference.GetValue;
import static com.github.anba.es6draft.runtime.types.Reference.PutValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BinaryExpression.Operator;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.Eval.EvalFlags;
import com.github.anba.es6draft.runtime.objects.RegExpConstructor;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

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
    private boolean globalScope;
    private boolean enclosedByWithStatement;

    public Interpreter(Script parsedScript) {
        this.strict = parsedScript.isStrict();
        this.globalCode = parsedScript.isGlobalCode();
        this.globalScope = parsedScript.isGlobalScope();
        this.enclosedByWithStatement = parsedScript.isEnclosedByWithStatement();
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * 12.3.4 Postfix Increment Operator
     */
    private static Double postIncrement(Object lhs, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(lhs, cx));
        double newValue = oldValue + 1;
        PutValue(lhs, newValue, cx);
        return oldValue;
    }

    /**
     * 12.3.5 Postfix Decrement Operator
     */
    private static Double postDecrement(Object lhs, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(lhs, cx));
        double newValue = oldValue - 1;
        PutValue(lhs, newValue, cx);
        return oldValue;
    }

    /**
     * 12.4.5 The void Operator
     */
    private static Undefined _void(Object value, ExecutionContext cx) {
        return UNDEFINED;
    }

    /**
     * 12.4.7 Prefix Increment Operator
     */
    private static Double preIncrement(Object expr, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(expr, cx));
        double newValue = oldValue + 1;
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.4.8 Prefix Decrement Operator
     */
    private static Double preDecrement(Object expr, ExecutionContext cx) {
        double oldValue = ToNumber(cx, GetValue(expr, cx));
        double newValue = oldValue - 1;
        PutValue(expr, newValue, cx);
        return newValue;
    }

    /**
     * 12.4.9 Unary + Operator
     */
    private static Double pos(Object value, ExecutionContext cx) {
        return ToNumber(cx, value);
    }

    /**
     * 12.4.10 Unary - Operator
     */
    private static Double neg(Object value, ExecutionContext cx) {
        double oldValue = ToNumber(cx, value);
        return -oldValue;
    }

    /**
     * 12.4.11 Bitwise NOT Operator ( ~ )
     */
    private static Integer bitnot(Object value, ExecutionContext cx) {
        int oldValue = ToInt32(cx, value);
        return ~oldValue;
    }

    /**
     * 12.4.12 Logical NOT Operator ( ! )
     */
    private static Boolean not(Object value, ExecutionContext cx) {
        boolean oldValue = ToBoolean(value);
        return !oldValue;
    }

    /**
     * 12.5 Multiplicative Operators
     */
    private static Double mul(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum * rnum;
    }

    /**
     * 12.5 Multiplicative Operators
     */
    private static Double div(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum / rnum;
    }

    /**
     * 12.5 Multiplicative Operators
     */
    private static Double mod(Object leftValue, Object rightValue, ExecutionContext cx) {
        double lnum = ToNumber(cx, leftValue);
        double rnum = ToNumber(cx, rightValue);
        return lnum % rnum;
    }

    /**
     * 12.6.2 The Subtraction Operator ( - )
     */
    private static Double sub(Object lval, Object rval, ExecutionContext cx) {
        double lnum = ToNumber(cx, lval);
        double rnum = ToNumber(cx, rval);
        return lnum - rnum;
    }

    /**
     * 12.7.1 The Left Shift Operator ( << )
     */
    private static Integer leftShift(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum << shiftCount;
    }

    /**
     * 12.7.2 The Signed Right Shift Operator ( >> )
     */
    private static Integer rightShift(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >> shiftCount;
    }

    /**
     * 12.7.3 The Unsigned Right Shift Operator ( >>> )
     */
    private static Long unsignedRightShift(Object lval, Object rval, ExecutionContext cx) {
        long lnum = ToUint32(cx, lval);
        long rnum = ToUint32(cx, rval);
        int shiftCount = (int) (rnum & 0x1F);
        return lnum >>> shiftCount;
    }

    /**
     * 12.8 Relational Operators
     */
    private static Boolean _instanceof(Object lval, Object rval, ExecutionContext cx) {
        return ScriptRuntime.InstanceofOperator(lval, rval, cx);
    }

    /**
     * 12.8 Relational Operators
     */
    private static Boolean lessThan(Object lval, Object rval, ExecutionContext cx) {
        int c = ScriptRuntime.relationalComparison(lval, rval, true, cx);
        return (c == 1);
    }

    /**
     * 12.8 Relational Operators
     */
    private static Boolean lessThanEqual(Object lval, Object rval, ExecutionContext cx) {
        int c = ScriptRuntime.relationalComparison(rval, lval, false, cx);
        return (c == 0);
    }

    /**
     * 12.8 Relational Operators
     */
    private static Boolean greaterThan(Object lval, Object rval, ExecutionContext cx) {
        int c = ScriptRuntime.relationalComparison(rval, lval, false, cx);
        return (c == 1);
    }

    /**
     * 12.8 Relational Operators
     */
    private static Boolean greaterThanEqual(Object lval, Object rval, ExecutionContext cx) {
        int c = ScriptRuntime.relationalComparison(lval, rval, true, cx);
        return (c == 0);
    }

    /**
     * 12.9 Equality Operators
     */
    private static Boolean equals(Object lval, Object rval, ExecutionContext cx) {
        return ScriptRuntime.equalityComparison(rval, lval, cx);
    }

    /**
     * 12.9 Equality Operators
     */
    private static Boolean notEquals(Object lval, Object rval, ExecutionContext cx) {
        return !ScriptRuntime.equalityComparison(rval, lval, cx);
    }

    /**
     * 12.9 Equality Operators
     */
    private static Boolean strictEquals(Object lval, Object rval, ExecutionContext cx) {
        return ScriptRuntime.strictEqualityComparison(rval, lval);
    }

    /**
     * 12.9 Equality Operators
     */
    private static Boolean strictNotEquals(Object lval, Object rval, ExecutionContext cx) {
        return !ScriptRuntime.strictEqualityComparison(rval, lval);
    }

    /**
     * 12.10 Binary Bitwise Operators
     */
    private static Integer bitand(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        int rnum = ToInt32(cx, rval);
        return lnum & rnum;
    }

    /**
     * 12.10 Binary Bitwise Operators
     */
    private static Integer bitxor(Object lval, Object rval, ExecutionContext cx) {
        int lnum = ToInt32(cx, lval);
        int rnum = ToInt32(cx, rval);
        return lnum ^ rnum;
    }

    /**
     * 12.10 Binary Bitwise Operators
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
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            Object val = initialiser.accept(this, cx);
            val = GetValue(val, cx);
            cx.resolveBinding(binding.getName(), strict).putValue(val, cx);
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
                throw new IllegalStateException();
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
            throw new IllegalStateException();
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
            return ScriptRuntime.delete(val, cx);
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
            throw new IllegalStateException();
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
        ScriptObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        for (PropertyDefinition propertyDefinition : node.getProperties()) {
            assert propertyDefinition instanceof PropertyValueDefinition;
            PropertyValueDefinition propValDef = (PropertyValueDefinition) propertyDefinition;
            PropertyName propertyName = propValDef.getPropertyName();
            Expression propertyValue = propValDef.getPropertyValue();

            String propName = PropName(propertyName);
            assert propName != null && !(propertyName instanceof ComputedPropertyName);
            Object value = propertyValue.accept(this, cx);
            value = GetValue(value, cx);

            if ("__proto__".equals(propName)
                    && cx.getRealm().isEnabled(CompatibilityOption.ProtoInitialiser)) {
                ScriptRuntime.defineProtoProperty(obj, value, cx);
            } else {
                ScriptRuntime.defineProperty(obj, propName, value, cx);
            }
        }
        return obj;
    }

    @Override
    public Object visit(ArrayLiteral node, ExecutionContext cx) {
        ExoticArray array = ArrayCreate(cx, 0);
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
        Put(cx, array, "length", nextIndex, false);
        return array;
    }

    @Override
    public Object visit(CallExpression node, ExecutionContext cx) {
        Object ref = node.getBase().accept(this, cx);
        return EvaluateCall(ref, node.getArguments(), directEval(node), cx);
    }

    /**
     * 12.2.4.2 Runtime Semantics: EvaluateCall
     */
    private Object EvaluateCall(Object ref, List<Expression> arguments, boolean directEval,
            ExecutionContext cx) {
        /* steps 1-2 */
        Object func = GetValue(ref, cx);
        /* steps 3-4 */
        Object[] argList = ArgumentListEvaluation(arguments, cx);
        /* steps 5-6 */
        Callable f = CheckCallable(func, cx);
        /* steps 7-8 */
        Object thisValue = UNDEFINED;
        if (ref instanceof Reference) {
            Reference<?, ?> rref = (Reference<?, ?>) ref;
            if (rref.isPropertyReference()) {
                thisValue = rref.getThisValue(cx);
            } else {
                assert rref instanceof Reference.IdentifierReference;
                Reference<EnvironmentRecord, String> idref = (Reference.IdentifierReference) rref;
                ScriptObject newThisValue = idref.getBase().withBaseObject();
                if (newThisValue != null) {
                    thisValue = newThisValue;
                }
            }
        }
        /* [18.2.1.1] Direct Call to Eval */
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
            if (enclosedByWithStatement) {
                evalFlags |= EvalFlags.EnclosedByWithStatement.getValue();
            }
            return Eval.directEval(argList, cx, evalFlags);
        }
        if (directEval && ScriptRuntime.directEvalFallbackHook(cx) != null) {
            argList = ScriptRuntime.directEvalFallbackArguments(argList, thisValue, f);
            thisValue = UNDEFINED; // FIXME: unspecified
            f = ScriptRuntime.directEvalFallbackHook(cx);
        }
        /* steps 9, 11, 12 (not applicable) */
        /* steps 10, 13 */
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
        if (base instanceof Identifier && "eval".equals(((Identifier) base).getName())) {
            return true;
        }
        return false;
    }

    @Override
    public Object visit(NewExpression node, ExecutionContext cx) {
        Object constructor = node.getExpression().accept(this, cx);
        constructor = GetValue(constructor, cx);
        Object[] args = ArgumentListEvaluation(node.getArguments(), cx);
        return ScriptRuntime.EvaluateConstructorCall(constructor, args, cx);
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
    public Object visit(Identifier node, ExecutionContext cx) {
        return cx.resolveBinding(node.getName(), strict);
    }

    @Override
    public Object visit(ThisExpression node, ExecutionContext cx) {
        return cx.resolveThisBinding();
    }

    private static final class ScriptBodyImpl implements RuntimeInfo.ScriptBody {
        private Script parsedScript;

        ScriptBodyImpl(Script parsedScript) {
            this.parsedScript = parsedScript;
        }

        @Override
        public String sourceFile() {
            return parsedScript.getSourceFile();
        }

        @Override
        public boolean isStrict() {
            return parsedScript.isStrict();
        }

        @Override
        public void globalDeclarationInstantiation(ExecutionContext cx,
                LexicalEnvironment globalEnv, LexicalEnvironment lexicalEnv,
                boolean deletableBindings) {
            GlobalDeclarationInstantiation(cx, parsedScript, globalEnv, lexicalEnv,
                    deletableBindings);
        }

        @Override
        public void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment varEnv,
                LexicalEnvironment lexEnv, boolean deletableBindings) {
            EvalDeclarationInstantiation(cx, parsedScript, varEnv, lexEnv, deletableBindings);
        }

        @Override
        public Object evaluate(ExecutionContext cx) {
            return parsedScript.accept(new Interpreter(parsedScript), cx);
        }
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

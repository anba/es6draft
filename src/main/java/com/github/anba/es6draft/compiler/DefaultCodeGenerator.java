/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 *
 */
abstract class DefaultCodeGenerator<R, V extends MethodGenerator> extends DefaultNodeVisitor<R, V> {
    protected final CodeGenerator codegen;

    protected DefaultCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    protected static final void tailCall(Expression expr, MethodGenerator mv) {
        while (expr instanceof CommaExpression) {
            List<Expression> list = ((CommaExpression) expr).getOperands();
            expr = list.get(list.size() - 1);
        }
        if (expr instanceof ConditionalExpression) {
            tailCall(((ConditionalExpression) expr).getThen(), mv);
            tailCall(((ConditionalExpression) expr).getOtherwise(), mv);
        } else if (expr instanceof CallExpression) {
            mv.setTailCall((CallExpression) expr);
        }
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void getLexicalEnvironment(MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getLexicalEnvironment);
    }

    /**
     * stack: [obj] -> [lexEnv]
     */
    protected final void newObjectEnvironment(MethodGenerator mv, boolean withEnvironment) {
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getLexicalEnvironment);
        mv.iconst(withEnvironment);
        mv.invokestatic(Methods.LexicalEnvironment_newObjectEnvironment);
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void newDeclarativeEnvironment(MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invokestatic(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    /**
     * stack: [lexEnv] -> []
     */
    protected final void pushLexicalEnvironment(MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.swap();
        mv.invokevirtual(Methods.ExecutionContext_pushLexicalEnvironment);
    }

    /**
     * stack: [] -> []
     */
    protected final void popLexicalEnvironment(MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_popLexicalEnvironment);
    }

    /**
     * Calls <code>GetValue(o)</code> if the expression could possibly be a reference
     */
    protected final void invokeGetValue(Expression node, MethodGenerator mv) {
        if (node.accept(IsReference.INSTANCE, null)) {
            GetValue(mv);
        }
    }

    /**
     * stack: [object] -> [boolean]
     */
    protected final void isUndefinedOrNull(MethodGenerator mv) {
        mv.invokestatic(Methods.Type_isUndefinedOrNull);
    }

    enum ValType {
        Undefined, Null, Boolean, Number, Number_int, Number_uint, String, Object, Reference, Any;

        public int size() {
            switch (this) {
            case Number:
            case Number_uint:
                return 2;
            case Number_int:
            case Undefined:
            case Null:
            case Boolean:
            case String:
            case Object:
            case Reference:
            case Any:
            default:
                return 1;
            }
        }

        public boolean isNumeric() {
            switch (this) {
            case Number:
            case Number_int:
            case Number_uint:
                return true;
            case Undefined:
            case Null:
            case Boolean:
            case String:
            case Object:
            case Reference:
            case Any:
            default:
                return false;
            }
        }

        public boolean isPrimitive() {
            switch (this) {
            case Undefined:
            case Null:
            case Boolean:
            case Number:
            case Number_int:
            case Number_uint:
            case String:
                return true;
            case Object:
            case Reference:
            case Any:
            default:
                return false;
            }
        }
    }

    /**
     * stack: [Object] -> [Object]
     */
    protected final void GetValue(MethodGenerator mv) {
        mv.load(Register.Realm);
        mv.invokestatic(Methods.Reference_GetValue);
    }

    /**
     * stack: [Object, Object] -> []
     */
    protected final void PutValue(MethodGenerator mv) {
        mv.load(Register.Realm);
        mv.invokestatic(Methods.Reference_PutValue);
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final ValType ToPrimitive(ValType from,
            com.github.anba.es6draft.runtime.types.Type preferredType, MethodGenerator mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Undefined:
        case Null:
        case Boolean:
        case String:
            return from;
        case Object:
        case Any:
        default:
            mv.load(Register.Realm);
            mv.swap();
            assert preferredType == null : "NYI"; // TODO: load enum value?
            mv.aconst(null);
            mv.invokestatic(Methods.AbstractOperations_ToPrimitive);
            return ValType.Any;
        }
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final void ToBoolean(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
            mv.invokestatic(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invokestatic(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invokestatic(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.iconst(false);
            return;
        case Boolean:
            return;
        case String: {
            Label l0 = new Label(), l1 = new Label();
            mv.invokeinterface(Methods.CharSequence_length);
            mv.ifeq(l0);
            mv.iconst(true);
            mv.goTo(l1);
            mv.mark(l0);
            mv.iconst(false);
            mv.mark(l1);
            return;
        }
        case Object:
        case Any:
        default:
            mv.invokestatic(Methods.AbstractOperations_ToBoolean);
            return;
        }
    }

    /**
     * stack: [Object] -> [double]
     */
    protected final void ToNumber(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            return;
        case Undefined:
            mv.pop();
            mv.getstatic(Fields.Double_NaN);
            return;
        case Null:
            mv.pop();
            mv.dconst(0);
            return;
        case Boolean:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            return;
        case String:
            mv.invokestatic(Methods.AbstractOperations_ToNumber_CharSequence);
            return;
        case Object:
        case Any:
        default:
            mv.load(Register.Realm);
            mv.swap();
            mv.invokestatic(Methods.AbstractOperations_ToNumber);
            return;
        }
    }

    /**
     * stack: [Object] -> [int]
     */
    protected final void ToInt32(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
            mv.invokestatic(Methods.AbstractOperations_ToInt32_double);
            return;
        case Number_int:
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.INT_TYPE);
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.iconst(0);
            return;
        case Boolean:
            return;
        case String:
            mv.invokestatic(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invokestatic(Methods.AbstractOperations_ToInt32_double);
            return;
        case Object:
        case Any:
        default:
            mv.load(Register.Realm);
            mv.swap();
            mv.invokestatic(Methods.AbstractOperations_ToInt32);
            return;
        }
    }

    /**
     * stack: [Object] -> [long]
     */
    protected final void ToUint32(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
            mv.invokestatic(Methods.AbstractOperations_ToUint32_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            return;
        case Number_uint:
            return;
        case Undefined:
        case Null:
            mv.pop();
            mv.lconst(0);
            return;
        case Boolean:
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            return;
        case String:
            mv.invokestatic(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invokestatic(Methods.AbstractOperations_ToUint32_double);
            return;
        case Object:
        case Any:
        default:
            mv.load(Register.Realm);
            mv.swap();
            mv.invokestatic(Methods.AbstractOperations_ToUint32);
            return;
        }
    }

    /**
     * stack: [Object] -> [CharSequence]
     */
    protected final void ToString(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
            mv.invokestatic(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invokestatic(Methods.AbstractOperations_ToString_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invokestatic(Methods.AbstractOperations_ToString_double);
            return;
        case Undefined:
            mv.pop();
            mv.aconst("undefined");
            return;
        case Null:
            mv.pop();
            mv.aconst("null");
            return;
        case Boolean:
            mv.invokestatic(Methods.Boolean_toString);
            return;
        case String:
            return;
        case Object:
        case Any:
        default:
            mv.load(Register.Realm);
            mv.swap();
            mv.invokestatic(Methods.AbstractOperations_ToString);
            return;
        }
    }

    /**
     * stack: [Object] -> [Scriptable]
     */
    protected final void ToObject(ValType from, MethodGenerator mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Boolean:
            mv.toBoxed(from);
            break;
        case Undefined:
        case Null:
        case String:
        case Object:
        case Any:
        default:
            break;
        }

        mv.load(Register.Realm);
        mv.swap();
        mv.invokestatic(Methods.AbstractOperations_ToObject);
    }

    protected void BindingInitialisation(Binding node, MethodGenerator mv) {
        new BindingInitialisationGenerator(codegen).generate(node, mv);
    }

    protected void BindingInitialisationWithEnvironment(Binding node, MethodGenerator mv) {
        new BindingInitialisationGenerator(codegen).generateWithEnvironment(node, mv);
    }

    protected void DestructuringAssignment(AssignmentPattern node, MethodGenerator mv) {
        new DestructuringAssignmentGenerator(codegen).generate(node, mv);
    }

    protected void ClassDefinitionEvaluation(ClassDefinition def, String className,
            MethodGenerator mv) {
        // stack: [] -> [<proto,ctor>]
        if (def.getHeritage() == null) {
            mv.load(Register.Realm);
            mv.invokestatic(Methods.ScriptRuntime_getDefaultClassProto);
        } else {
            // FIXME: spec bug (ClassHeritage runtime evaluation not defined)
            ValType type = codegen.expression(def.getHeritage(), mv);
            mv.toBoxed(type);
            invokeGetValue(def.getHeritage(), mv);
            mv.load(Register.Realm);
            mv.invokestatic(Methods.ScriptRuntime_getClassProto);
        }

        // stack: [<proto,ctor>] -> [ctor, proto]
        mv.dup();
        mv.iconst(1);
        mv.aload(Types.Scriptable_);
        mv.swap();
        mv.iconst(0);
        mv.aload(Types.Scriptable_);

        // steps 4-5
        if (className != null) {
            // stack: [ctor, proto] -> [ctor, proto, scope]
            newDeclarativeEnvironment(mv);

            // stack: [ctor, proto, scope] -> [ctor, proto, scope, proto, scope]
            mv.dup2();

            // stack: [ctor, proto, scope, proto, scope] -> [ctor, proto, scope, proto, envRec]
            mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

            // stack: [ctor, proto, scope, proto, envRec] -> [ctor, proto, scope, proto, envRec]
            mv.dup();
            mv.aconst(className);
            mv.invokeinterface(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [ctor, proto, scope, proto, envRec] -> [ctor, proto, scope]
            mv.swap();
            mv.aconst(className);
            mv.swap();
            mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);

            // stack: [ctor, proto, scope] -> [ctor, proto]
            pushLexicalEnvironment(mv);
        }

        // steps 6-12
        MethodDefinition constructor = ConstructorMethod(def);
        if (constructor != null) {
            codegen.compile(constructor);

            // Runtime Semantics: Evaluation -> MethodDefinition
            // stack: [ctor, proto] -> [proto, F]
            mv.dupX1();
            mv.invokestatic(codegen.getClassName(), codegen.methodName(constructor) + "_rti",
                    Type.getMethodDescriptor(Types.RuntimeInfo$Function));
            mv.load(Register.ExecutionContext);
            mv.invokestatic(Methods.ScriptRuntime_EvaluateConstructorMethod);
        } else {
            // default constructor
            // stack: [ctor, proto] -> [proto, F]
            mv.dupX1();
            mv.invokestatic(Methods.ScriptRuntime_CreateDefaultConstructor);
            mv.load(Register.ExecutionContext);
            mv.invokestatic(Methods.ScriptRuntime_EvaluateConstructorMethod);
        }

        // stack: [proto, F] -> [F, proto]
        mv.swap();

        // steps 13-14
        List<MethodDefinition> methods = def.getBody();
        for (MethodDefinition method : methods) {
            if (method == constructor) {
                // FIXME: spec bug? (not handled in draft)
                continue;
            }
            mv.dup();
            codegen.propertyDefinition(method, mv);
        }

        // step 15
        if (className != null) {
            // restore previous lexical environment
            popLexicalEnvironment(mv);
        }

        // stack: [F, proto] -> [F]
        mv.pop();
    }
}

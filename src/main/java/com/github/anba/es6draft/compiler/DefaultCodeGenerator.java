/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.ConstructorMethod;
import static com.github.anba.es6draft.semantics.StaticSemantics.PrototypeMethodDefinitions;
import static com.github.anba.es6draft.semantics.StaticSemantics.StaticMethodDefinitions;

import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 *
 */
abstract class DefaultCodeGenerator<R, V extends ExpressionVisitor> extends
        DefaultNodeVisitor<R, V> {
    private static class Fields {
        static final FieldDesc Double_NaN = FieldDesc.create(FieldType.Static, Types.Double, "NaN",
                Type.DOUBLE_TYPE);
    }

    private static class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_ToPrimitive = MethodDesc
                .create(MethodType.Static, Types.AbstractOperations, "ToPrimitive", Type
                        .getMethodType(Types.Object, Types.ExecutionContext, Types.Object,
                                Types._Type));

        static final MethodDesc AbstractOperations_ToBoolean = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));

        static final MethodDesc AbstractOperations_ToBoolean_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToBoolean",
                Type.getMethodType(Type.BOOLEAN_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToFlatString = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToFlatString",
                Type.getMethodType(Types.String, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToNumber_CharSequence = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToNumber",
                Type.getMethodType(Type.DOUBLE_TYPE, Types.CharSequence));

        static final MethodDesc AbstractOperations_ToInt32 = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToInt32_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToInt32",
                Type.getMethodType(Type.INT_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToUint32 = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToUint32_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToUint32",
                Type.getMethodType(Type.LONG_TYPE, Type.DOUBLE_TYPE));

        static final MethodDesc AbstractOperations_ToObject = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToObject",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.CharSequence, Types.ExecutionContext, Types.Object));

        static final MethodDesc AbstractOperations_ToString_double = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "ToString",
                Type.getMethodType(Types.String, Type.DOUBLE_TYPE));

        // class: Boolean
        static final MethodDesc Boolean_toString = MethodDesc.create(MethodType.Static,
                Types.Boolean, "toString", Type.getMethodType(Types.String, Type.BOOLEAN_TYPE));

        // class: CharSequence
        static final MethodDesc CharSequence_length = MethodDesc.create(MethodType.Interface,
                Types.CharSequence, "length", Type.getMethodType(Type.INT_TYPE));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createImmutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String));

        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getLexicalEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_pushLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "pushLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_popLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "popLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ExecutionContext_restoreLexicalEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "restoreLexicalEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        static final MethodDesc LexicalEnvironment_newObjectEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newObjectEnvironment", Type
                        .getMethodType(Types.LexicalEnvironment, Types.ScriptObject,
                                Types.LexicalEnvironment, Type.BOOLEAN_TYPE));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_CreateDefaultConstructor = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "CreateDefaultConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_CreateDefaultEmptyConstructor = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "CreateDefaultEmptyConstructor",
                Type.getMethodType(Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_EvaluateConstructorMethod = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "EvaluateConstructorMethod", Type
                        .getMethodType(Types.OrdinaryFunction, Types.ScriptObject,
                                Types.ScriptObject, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getClassProto = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getDefaultClassProto = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "getDefaultClassProto",
                Type.getMethodType(Types.ScriptObject_, Types.ExecutionContext));

        // class: Type
        static final MethodDesc Type_isUndefinedOrNull = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefinedOrNull",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    protected final CodeGenerator codegen;

    protected DefaultCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    protected static final void tailCall(Expression expr, ExpressionVisitor mv) {
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

    protected final ValType expression(Expression node, ExpressionVisitor mv) {
        return codegen.expression(node, mv);
    }

    protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
        return codegen.expressionValue(node, mv);
    }

    /**
     * stack: [] -> []
     */
    protected final Variable<LexicalEnvironment> saveEnvironment(AbruptNode node,
            StatementVisitor mv) {
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            return saveEnvironment(mv);
        }
        return null;
    }

    /**
     * stack: [] -> []
     */
    protected final Variable<LexicalEnvironment> saveEnvironment(StatementVisitor mv) {
        Variable<LexicalEnvironment> savedEnv = mv
                .newVariable("savedEnv", LexicalEnvironment.class);
        getLexicalEnvironment(mv);
        mv.store(savedEnv);
        return savedEnv;
    }

    /**
     * stack: [] -> []
     */
    protected final void restoreEnvironment(AbruptNode node, Abrupt abrupt,
            Variable<LexicalEnvironment> savedEnv, StatementVisitor mv) {
        if (node.getAbrupt().contains(abrupt)) {
            assert savedEnv != null;
            restoreEnvironment(mv, savedEnv);
        }
    }

    /**
     * stack: [] -> []
     */
    protected final void restoreEnvironment(StatementVisitor mv,
            Variable<LexicalEnvironment> savedEnv) {
        mv.loadExecutionContext();
        mv.load(savedEnv);
        mv.invoke(Methods.ExecutionContext_restoreLexicalEnvironment);
    }

    protected final void freeVariable(Variable<?> var, StatementVisitor mv) {
        if (var != null) {
            mv.freeVariable(var);
        }
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void getLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
    }

    /**
     * stack: [] -> [envRec]
     */
    protected final void getEnvironmentRecord(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
     * stack: [obj] -> [lexEnv]
     */
    protected final void newObjectEnvironment(ExpressionVisitor mv, boolean withEnvironment) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.iconst(withEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newObjectEnvironment);
    }

    /**
     * stack: [] -> [lexEnv]
     */
    protected final void newDeclarativeEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    /**
     * stack: [lexEnv] -> []
     */
    protected final void pushLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.ExecutionContext_pushLexicalEnvironment);
    }

    /**
     * stack: [] -> []
     */
    protected final void popLexicalEnvironment(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_popLexicalEnvironment);
    }

    /**
     * stack: [object] -> [boolean]
     */
    protected final void isUndefinedOrNull(ExpressionVisitor mv) {
        mv.invoke(Methods.Type_isUndefinedOrNull);
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

        public Type toType() {
            switch (this) {
            case Boolean:
                return Type.BOOLEAN_TYPE;
            case String:
                return Types.CharSequence;
            case Number:
                return Type.DOUBLE_TYPE;
            case Number_int:
                return Type.INT_TYPE;
            case Number_uint:
                return Type.LONG_TYPE;
            case Object:
                return Types.ScriptObject;
            case Reference:
                return Types.Reference;
            case Null:
                return Types.Null;
            case Undefined:
                return Types.Undefined;
            case Any:
            default:
                return Types.Object;
            }
        }

        public Type toBoxedType() {
            switch (this) {
            case Boolean:
                return Types.Boolean;
            case String:
                return Types.CharSequence;
            case Number:
                return Types.Double;
            case Number_int:
                return Types.Integer;
            case Number_uint:
                return Types.Long;
            case Object:
                return Types.ScriptObject;
            case Reference:
                return Types.Reference;
            case Null:
                return Types.Null;
            case Undefined:
                return Types.Undefined;
            case Any:
            default:
                return Types.Object;
            }
        }
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final ValType ToPrimitive(ValType from, ExpressionVisitor mv) {
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
            mv.loadExecutionContext();
            mv.swap();
            mv.aconst(null);
            mv.invoke(Methods.AbstractOperations_ToPrimitive);
            return ValType.Any;
        }
    }

    /**
     * stack: [Object] -> [boolean]
     */
    protected final void ToBoolean(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToBoolean_double);
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
            mv.invoke(Methods.CharSequence_length);
            mv.ifeq(l0);
            mv.iconst(true);
            mv.goTo(l1);
            mv.mark(l0);
            mv.iconst(false);
            mv.mark(l1);
            return;
        }
        case Object:
            mv.pop();
            mv.iconst(true);
            return;
        case Any:
        default:
            mv.invoke(Methods.AbstractOperations_ToBoolean);
            return;
        }
    }

    /**
     * stack: [Object] -> [double]
     */
    protected final void ToNumber(ValType from, ExpressionVisitor mv) {
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
            mv.get(Fields.Double_NaN);
            return;
        case Null:
            mv.pop();
            mv.dconst(0);
            return;
        case Boolean:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            return;
        case String:
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToNumber);
            return;
        }
    }

    /**
     * stack: [Object] -> [int]
     */
    protected final void ToInt32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToInt32_double);
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
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invoke(Methods.AbstractOperations_ToInt32_double);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToInt32);
            return;
        }
    }

    /**
     * stack: [Object] -> [long]
     */
    protected final void ToUint32(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.LONG_TYPE);
            mv.lconst(0xffff_ffffL);
            mv.and(Type.LONG_TYPE);
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
            mv.invoke(Methods.AbstractOperations_ToNumber_CharSequence);
            mv.invoke(Methods.AbstractOperations_ToUint32_double);
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToUint32);
            return;
        }
    }

    /**
     * stack: [Object] -> [CharSequence]
     */
    protected final void ToString(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToString_double);
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
            mv.invoke(Methods.Boolean_toString);
            return;
        case String:
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToString);
            return;
        }
    }

    /**
     * stack: [Object] -> [String]
     */
    protected final void ToFlatString(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_int:
            mv.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToString_double);
            return;
        case Number_uint:
            mv.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            mv.invoke(Methods.AbstractOperations_ToString_double);
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
            mv.invoke(Methods.Boolean_toString);
            return;
        case String:
            return;
        case Object:
        case Any:
        default:
            mv.loadExecutionContext();
            mv.swap();
            mv.invoke(Methods.AbstractOperations_ToFlatString);
            return;
        }
    }

    /**
     * stack: [Object] -> [ScriptObject]
     */
    protected final void ToObject(ValType from, ExpressionVisitor mv) {
        switch (from) {
        case Number:
        case Number_int:
        case Number_uint:
        case Boolean:
            mv.toBoxed(from);
            break;
        case Object:
            return;
        case Undefined:
        case Null:
        case String:
        case Any:
        default:
            break;
        }

        mv.loadExecutionContext();
        mv.swap();
        mv.invoke(Methods.AbstractOperations_ToObject);
    }

    protected void BindingInitialisation(Binding node, ExpressionVisitor mv) {
        new BindingInitialisationGenerator(codegen).generate(node, mv);
    }

    /**
     * stack: [envRec, value] -> []
     */
    protected void BindingInitialisationWithEnvironment(Binding node, ExpressionVisitor mv) {
        new BindingInitialisationGenerator(codegen).generateWithEnvironment(node, mv);
    }

    /**
     * stack: [value] -> []
     */
    protected void DestructuringAssignment(AssignmentPattern node, ExpressionVisitor mv) {
        new DestructuringAssignmentGenerator(codegen).generate(node, mv);
    }

    protected void ClassDefinitionEvaluation(ClassDefinition def, String className,
            ExpressionVisitor mv) {
        // stack: [] -> [<proto,ctor>]
        if (def.getHeritage() == null) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getDefaultClassProto);
        } else {
            ValType type = expressionValue(def.getHeritage(), mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getClassProto);
        }

        // stack: [<proto,ctor>] -> [ctor, proto]
        mv.dup();
        mv.iconst(1);
        mv.aload(Types.ScriptObject_);
        mv.swap();
        mv.iconst(0);
        mv.aload(Types.ScriptObject_);

        // steps 4-5
        if (className != null) {
            // stack: [ctor, proto] -> [ctor, proto, scope]
            // TODO: make explicit...
            // implicit: mv.enterScope(def)
            newDeclarativeEnvironment(mv);

            // stack: [ctor, proto, scope] -> [ctor, proto, scope]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);
            mv.aconst(className);
            mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);

            // stack: [ctor, proto, scope] -> [ctor, proto]
            pushLexicalEnvironment(mv);
        }

        // stack: [ctor, proto] -> [proto, ctor, proto]
        mv.dupX1();

        // steps 6
        MethodDefinition constructor = ConstructorMethod(def);
        if (constructor != null) {
            codegen.compile(constructor);
            // Runtime Semantics: Evaluation -> MethodDefinition
            mv.invokestatic(codegen.getClassName(),
                    codegen.methodName(constructor, FunctionName.RTI),
                    Type.getMethodDescriptor(Types.RuntimeInfo$Function));
        } else {
            // step 7
            if (def.getHeritage() != null) {
                // FIXME: spec bug - `new (class extends null {})` throws TypeError
                mv.invoke(Methods.ScriptRuntime_CreateDefaultConstructor);
            } else {
                mv.invoke(Methods.ScriptRuntime_CreateDefaultEmptyConstructor);
            }
        }

        // step 9-10, step 12-13
        // stack: [proto, ctor, proto, <rti>] -> [proto, F]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_EvaluateConstructorMethod);

        // step 11
        if (className != null) {
            // stack: [proto, F] -> [proto, F, F, envRec]
            mv.dup();
            getLexicalEnvironment(mv);
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [proto, F, F, envRec] -> [proto, F, envRec, name, F]
            mv.swap();
            mv.aconst(className);
            mv.swap();

            // stack: [proto, F, envRec, name, F] -> [proto, F]
            mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
        }

        // stack: [proto, F] -> [F, proto]
        mv.swap();

        // steps 14-15
        List<MethodDefinition> protoMethods = PrototypeMethodDefinitions(def);
        for (MethodDefinition method : protoMethods) {
            if (method == constructor) {
                continue;
            }
            mv.dup();
            codegen.propertyDefinition(method, mv);
        }

        // stack: [F, proto] -> [F]
        mv.pop();

        // steps 16-17
        List<MethodDefinition> staticMethods = StaticMethodDefinitions(def);
        for (MethodDefinition method : staticMethods) {
            mv.dup();
            codegen.propertyDefinition(method, mv);
        }

        // step 18
        if (className != null) {
            // restore previous lexical environment
            popLexicalEnvironment(mv);
            // implicit: mv.exitScope()
        }
    }
}

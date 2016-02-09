/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitialization;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.FunctionScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 ECMAScript Function Objects</h2>
 * <ul>
 * <li>9.2.13 FunctionDeclarationInstantiation(func, argumentsList)
 * </ul>
 */
final class FunctionDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: Arrays
        static final MethodName Arrays_asList = MethodName.findStatic(Types.Arrays, "asList",
                Type.methodType(Types.List, Types.Object_));

        // class: ExecutionContext
        static final MethodName ExecutionContext_setLexicalEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "setLexicalEnvironment", Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_setVariableEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "setVariableEnvironment", Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: ArgumentsObject
        static final MethodName ArgumentsObject_CreateMappedArgumentsObject_Empty = MethodName.findStatic(
                Types.ArgumentsObject, "CreateMappedArgumentsObject",
                Type.methodType(Types.ArgumentsObject, Types.ExecutionContext, Types.FunctionObject, Types.Object_));

        static final MethodName ArgumentsObject_CreateMappedArgumentsObject = MethodName
                .findStatic(Types.ArgumentsObject, "CreateMappedArgumentsObject", Type.methodType(Types.ArgumentsObject,
                        Types.ExecutionContext, Types.FunctionObject, Types.Object_, Types.LexicalEnvironment));

        static final MethodName ArgumentsObject_CreateUnmappedArgumentsObject = MethodName.findStatic(
                Types.ArgumentsObject, "CreateUnmappedArgumentsObject",
                Type.methodType(Types.ArgumentsObject, Types.ExecutionContext, Types.Object_));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName.findStatic(
                Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: List
        static final MethodName List_iterator = MethodName.findInterface(Types.List, "iterator",
                Type.methodType(Types.Iterator));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int FUNCTION = 1;
    private static final int ARGUMENTS = 2;

    private static final class FunctionDeclInitMethodGenerator extends CodeVisitor {
        private final String name;
        private final Type type;

        FunctionDeclInitMethodGenerator(MethodCode method, FunctionNode node, Type type) {
            super(method, node);
            this.name = targetName(node);
            this.type = type;
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName(name, FUNCTION, type);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode function) {
        MethodCode method = codegen.newMethod(function, FunctionName.Init);
        CodeVisitor mv = new FunctionDeclInitMethodGenerator(method, function, targetType(function));

        mv.lineInfo(function);
        mv.begin();
        mv.enterScope(function);
        generate(function, mv);
        mv.exitScope();
        mv.end();
    }

    private Type targetType(FunctionNode node) {
        if (node.isAsync() && node.isGenerator()) {
            return Types.OrdinaryAsyncGenerator;
        } else if (node.isGenerator()) {
            if (node.isConstructor()) {
                return Types.OrdinaryConstructorGenerator;
            }
            return Types.OrdinaryGenerator;
        } else if (node.isAsync()) {
            return Types.OrdinaryAsyncFunction;
        } else if (isLegacy(node)) {
            return Types.LegacyConstructorFunction;
        } else if (node.isConstructor() || isCallConstructor(node)) {
            return Types.OrdinaryConstructorFunction;
        } else {
            return Types.OrdinaryFunction;
        }
    }

    private Class<? extends FunctionObject> targetClass(FunctionNode node) {
        if (node.isAsync() && node.isGenerator()) {
            return OrdinaryAsyncGenerator.class;
        } else if (node.isGenerator()) {
            if (node.isConstructor()) {
                return OrdinaryConstructorGenerator.class;
            }
            return OrdinaryGenerator.class;
        } else if (node.isAsync()) {
            return OrdinaryAsyncFunction.class;
        } else if (isLegacy(node)) {
            return LegacyConstructorFunction.class;
        } else if (node.isConstructor() || isCallConstructor(node)) {
            return OrdinaryConstructorFunction.class;
        } else {
            return OrdinaryFunction.class;
        }
    }

    private static String targetName(FunctionNode node) {
        if (node.isGenerator()) {
            return "generator";
        } else {
            return "function";
        }
    }

    private void generate(FunctionNode function, CodeVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT, ExecutionContext.class);
        Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env = mv.newVariable("env", LexicalEnvironment.class)
                .uncheckedCast();
        Variable<FunctionEnvironmentRecord> envRec = mv.newVariable("envRec", FunctionEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;
        Variable<Undefined> undefined = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undefined);

        FunctionScope fscope = function.getScope();
        boolean hasParameters = !function.getParameters().getFormals().isEmpty();
        Variable<Iterator<?>> iterator = null;
        if (hasParameters) {
            iterator = mv.newVariable("iterator", Iterator.class).uncheckedCast();
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.Arrays_asList);
            mv.invoke(Methods.List_iterator);
            mv.store(iterator);
        }

        /* step 1 (omitted) */
        /* step 2 */
        getLexicalEnvironment(context, env, mv);
        /* step 3 */
        getEnvironmentRecord(env, envRec, mv);
        /* step 4 */
        // RuntimeInfo.Function code = func.getCode();
        /* step 5 */
        boolean strict = IsStrict(function);
        /* step 6 */
        FormalParameterList formals = function.getParameters();
        /* step 7 */
        List<Name> parameterNames = BoundNames(formals);
        HashSet<Name> parameterNamesSet = new HashSet<>(parameterNames);
        /* step 8 */
        boolean hasDuplicates = parameterNames.size() != parameterNamesSet.size();
        /* step 9 */
        boolean simpleParameterList = IsSimpleParameterList(formals);
        /* step 10 */
        boolean hasParameterExpressions = ContainsExpression(formals);
        // invariant: hasDuplicates => simpleParameterList
        assert !hasDuplicates || simpleParameterList;
        // invariant: hasParameterExpressions => !simpleParameterList
        assert !hasParameterExpressions || !simpleParameterList;
        /* step 11 */
        Set<Name> varNames = VarDeclaredNames(function);
        /* step 12 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(function);
        /* step 13 */
        Set<Name> lexicalNames = LexicallyDeclaredNames(function);
        /* step 14 */
        HashSet<Name> functionNames = new HashSet<>();
        /* step 15 */
        ArrayDeque<HoistableDeclaration> functionsToInitialize = new ArrayDeque<>();
        /* step 16 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof HoistableDeclaration) {
                HoistableDeclaration d = (HoistableDeclaration) item;
                Name fn = BoundName(d);
                if (functionNames.add(fn)) {
                    functionsToInitialize.addFirst(d);
                }
            }
        }
        if (!functionsToInitialize.isEmpty()) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 17 */
        // Optimization: Skip 'arguments' allocation if it's not referenced within the function.
        boolean argumentsObjectNeeded = function.getScope().needsArguments();
        Name arguments = function.getScope().arguments();
        argumentsObjectNeeded &= arguments != null;
        /* step 18 */
        if (function.getThisMode() == FunctionNode.ThisMode.Lexical) {
            argumentsObjectNeeded = false;
        }
        /* step 19 */
        else if (parameterNamesSet.contains(arguments)) {
            argumentsObjectNeeded = false;
        }
        /* step 20 */
        else if (!hasParameterExpressions) {
            if (functionNames.contains(arguments) || lexicalNames.contains(arguments)) {
                argumentsObjectNeeded = false;
            }
        }
        /* step 21 */
        for (Name paramName : function.getScope().parameterNames()) {
            BindingOp<FunctionEnvironmentRecord> op = BindingOp.of(envRec, paramName);
            op.createMutableBinding(envRec, paramName, false, mv);
            if (hasDuplicates) {
                op.initializeBinding(envRec, paramName, undefined, mv);
            }
        }
        /* step 22 */
        if (argumentsObjectNeeded) {
            assert arguments != null;
            Variable<ArgumentsObject> argumentsObj = mv.newVariable("argumentsObj", ArgumentsObject.class);
            if (strict || !simpleParameterList) {
                CreateUnmappedArgumentsObject(mv);
            } else if (formals.getFormals().isEmpty()) {
                CreateMappedArgumentsObject(mv);
            } else {
                CreateMappedArgumentsObject(env, formals, mv);
            }
            mv.store(argumentsObj);
            BindingOp<FunctionEnvironmentRecord> op = BindingOp.of(envRec, arguments);
            if (strict) {
                op.createImmutableBinding(envRec, arguments, false, mv);
            } else {
                op.createMutableBinding(envRec, arguments, false, mv);
            }
            op.initializeBinding(envRec, arguments, argumentsObj, mv);
            parameterNames.add(arguments);
            parameterNamesSet.add(arguments);
        }
        /* step 23 (not applicable) */
        /* steps 24-26 */
        if (hasParameters) {
            if (hasDuplicates) {
                /* step 24 */
                BindingInitialization(codegen, function, env, iterator, mv);
            } else {
                /* step 25 */
                BindingInitialization(codegen, function, env, envRec, iterator, mv);
            }
        }
        /* steps 27-28 */
        HashSet<Name> instantiatedVarNames;
        Variable<? extends LexicalEnvironment<?>> varEnv;
        Variable<? extends DeclarativeEnvironmentRecord> varEnvRec;
        if (!hasParameterExpressions) {
            assert fscope == fscope.variableScope();
            /* step 27.a (note) */
            /* step 27.b */
            instantiatedVarNames = new HashSet<>(parameterNames);
            /* step 27.c */
            for (Name varName : varNames) {
                if (instantiatedVarNames.add(varName)) {
                    BindingOp<FunctionEnvironmentRecord> op = BindingOp.of(envRec, varName);
                    op.createMutableBinding(envRec, varName, false, mv);
                    op.initializeBinding(envRec, varName, undefined, mv);
                }
            }
            /* steps 27.d-27.e */
            varEnv = env;
            varEnvRec = envRec;
        } else {
            assert fscope != fscope.variableScope();
            mv.enterScope(fscope.variableScope());
            /* step 28.a (note) */
            /* step 28.b */
            varEnv = mv.newVariable("varEnv", LexicalEnvironment.class).uncheckedCast();
            newDeclarativeEnvironment(env, mv);
            mv.store(varEnv);
            /* step 28.c */
            varEnvRec = mv.newVariable("varEnvRec", DeclarativeEnvironmentRecord.class);
            getEnvironmentRecord(varEnv, varEnvRec, mv);
            /* step 28.d */
            setVariableEnvironment(varEnv, mv);
            /* step 28.e */
            instantiatedVarNames = new HashSet<>();
            /* step 28.f */
            Variable<Object> tempValue = null;
            for (Name varName : varNames) {
                if (instantiatedVarNames.add(varName)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, varName);
                    op.createMutableBinding(varEnvRec, varName, false, mv);
                    if (!parameterNamesSet.contains(varName) || functionNames.contains(varName)) {
                        op.initializeBinding(varEnvRec, varName, undefined, mv);
                    } else {
                        BindingOp.of(envRec, varName).getBindingValue(envRec, varName, strict, mv);
                        if (tempValue == null) {
                            tempValue = mv.newVariable("tempValue", Object.class);
                        }
                        mv.store(tempValue);
                        op.initializeBinding(varEnvRec, varName, tempValue, mv);
                    }
                }
            }
        }

        /* step 29 (B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics) */
        for (Name fname : function.getScope().blockFunctionNames()) {
            if (instantiatedVarNames.add(fname)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, fname);
                op.createMutableBinding(varEnvRec, fname, false, mv);
                op.initializeBinding(varEnvRec, fname, undefined, mv);
            }
        }

        /* steps 30-32 */
        Variable<? extends LexicalEnvironment<?>> lexEnv;
        Variable<? extends DeclarativeEnvironmentRecord> lexEnvRec;
        assert strict || fscope.variableScope() != fscope.lexicalScope();
        if (!strict || fscope.variableScope() != fscope.lexicalScope()) {
            // NB: Scopes are unmodifiable once constructed, that means we need to emit the extra
            // scope for functions with deferred strict-ness, even if this scope is not present in
            // the specification.
            mv.enterScope(fscope.lexicalScope());
            if (!lexicalNames.isEmpty()) {
                /* step 30 */
                lexEnv = mv.newVariable("lexEnv", LexicalEnvironment.class).uncheckedCast();
                newDeclarativeEnvironment(varEnv, mv);
                mv.store(lexEnv);
                /* step 32 */
                lexEnvRec = mv.newVariable("lexEnvRec", DeclarativeEnvironmentRecord.class);
                getEnvironmentRecord(lexEnv, lexEnvRec, mv);
            } else {
                // Optimization: Skip environment allocation if no lexical names are defined.
                /* step 30 */
                lexEnv = varEnv;
                /* step 32 */
                lexEnvRec = varEnvRec;
            }
        } else {
            /* step 30 */
            lexEnv = varEnv;
            /* step 32 */
            lexEnvRec = varEnvRec;
        }
        /* step 33 */
        if (lexEnv != env) {
            setLexicalEnvironment(lexEnv, mv);
        }
        /* step 34 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(function);
        /* step 35 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(lexEnvRec, dn);
                if (d.isConstDeclaration()) {
                    op.createImmutableBinding(lexEnvRec, dn, true, mv);
                } else {
                    op.createMutableBinding(lexEnvRec, dn, false, mv);
                }
            }
        }
        /* step 36 */
        for (HoistableDeclaration f : functionsToInitialize) {
            Name fn = BoundName(f);

            // stack: [] -> [fo]
            InstantiateFunctionObject(context, lexEnv, f, mv);
            mv.store(fo);

            // stack: [fo] -> []
            // Resolve the actual binding name: function(a){ function a(){} }
            // TODO: Can be removed when StaticIdResolution handles this case.
            Name name = fscope.variableScope().resolveName(fn, false);
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, name);
            op.setMutableBinding(varEnvRec, name, fo, false, mv);
        }
        /* step 37 */
        mv._return();
    }

    private void newDeclarativeEnvironment(Variable<? extends LexicalEnvironment<?>> env, CodeVisitor mv) {
        // stack: [] -> [env]
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    private void setVariableEnvironment(Variable<? extends LexicalEnvironment<?>> env, CodeVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setVariableEnvironment);
    }

    private void setLexicalEnvironment(Variable<? extends LexicalEnvironment<?>> env, CodeVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setLexicalEnvironment);
    }

    private void CreateMappedArgumentsObject(CodeVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.loadParameter(FUNCTION, targetClass((FunctionNode) mv.getTopLevelNode()));
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.invoke(Methods.ArgumentsObject_CreateMappedArgumentsObject_Empty);
    }

    private void CreateMappedArgumentsObject(Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
            FormalParameterList formals, CodeVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.loadParameter(FUNCTION, targetClass((FunctionNode) mv.getTopLevelNode()));
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.load(env);
        mv.invoke(Methods.ArgumentsObject_CreateMappedArgumentsObject);
    }

    private void CreateUnmappedArgumentsObject(CodeVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.invoke(Methods.ArgumentsObject_CreateUnmappedArgumentsObject);
    }

    private boolean isLegacy(FunctionNode node) {
        if (IsStrict(node)) {
            return false;
        }
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    private boolean isCallConstructor(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).isCallConstructor();
        }
        return false;
    }
}

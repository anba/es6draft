/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitialization;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitializationWithEnvironment;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
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

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 ECMAScript Function Objects</h2>
 * <ul>
 * <li>9.2.13 FunctionDeclarationInstantiation(func, argumentsList) Abstract Operation
 * </ul>
 */
final class FunctionDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: Arrays
        static final MethodName Arrays_asList = MethodName.findStatic(Types.Arrays, "asList",
                Type.methodType(Types.List, Types.Object_));

        // class: ExecutionContext
        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getLexicalEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_setLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "setLexicalEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        static final MethodName ExecutionContext_setVariableEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "setVariableEnvironment",
                Type.methodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: ArgumentsObject
        static final MethodName ArgumentsObject_CreateMappedArgumentsObject = MethodName
                .findStatic(Types.ArgumentsObject, "CreateMappedArgumentsObject", Type.methodType(
                        Types.ArgumentsObject, Types.ExecutionContext, Types.FunctionObject,
                        Types.String_, Types.Object_, Types.LexicalEnvironment));

        static final MethodName ArgumentsObject_CreateUnmappedArgumentsObject = MethodName
                .findStatic(Types.ArgumentsObject, "CreateUnmappedArgumentsObject", Type
                        .methodType(Types.ArgumentsObject, Types.ExecutionContext, Types.Object_));

        static final MethodName LegacyArgumentsObject_CreateLegacyArgumentsObject = MethodName
                .findStatic(Types.LegacyArgumentsObject, "CreateLegacyArgumentsObject", Type
                        .methodType(Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.String_,
                                Types.LexicalEnvironment));

        static final MethodName LegacyArgumentsObject_CreateLegacyArgumentsObjectFrom = MethodName
                .findStatic(Types.LegacyArgumentsObject, "CreateLegacyArgumentsObject", Type
                        .methodType(Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.ArgumentsObject));

        static final MethodName LegacyArgumentsObject_CreateLegacyArgumentsObjectUnmapped = MethodName
                .findStatic(Types.LegacyArgumentsObject, "CreateLegacyArgumentsObject", Type
                        .methodType(Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_));

        // FunctionObject
        static final MethodName FunctionObject_setLegacyArguments = MethodName.findVirtual(
                Types.FunctionObject, "setLegacyArguments",
                Type.methodType(Type.VOID_TYPE, Types.LegacyArgumentsObject));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_newDeclarativeEnvironment = MethodName
                .findStatic(Types.LexicalEnvironment, "newDeclarativeEnvironment",
                        Type.methodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: List
        static final MethodName List_iterator = MethodName.findInterface(Types.List, "iterator",
                Type.methodType(Types.Iterator));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int FUNCTION = 1;
    private static final int ARGUMENTS = 2;

    private static final class FunctionDeclInitMethodGenerator extends ExpressionVisitor {
        FunctionDeclInitMethodGenerator(MethodCode method, FunctionNode node) {
            super(method, IsStrict(node), false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("function", FUNCTION, Types.FunctionObject);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode function) {
        MethodCode method = codegen.newMethod(function, FunctionName.Init);
        ExpressionVisitor mv = new FunctionDeclInitMethodGenerator(method, function);

        mv.lineInfo(function);
        mv.begin();
        mv.enterScope(function);
        generate(function, mv);
        mv.exitScope();
        mv.end();
    }

    private void generate(FunctionNode function, ExpressionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env = mv.newVariable("env",
                LexicalEnvironment.class).uncheckedCast();
        Variable<FunctionEnvironmentRecord> envRec = mv.newVariable("envRec",
                FunctionEnvironmentRecord.class);
        Variable<FunctionObject> fo = null;
        Variable<Undefined> undefined = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undefined);

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
        getLexicalEnvironment(env, mv);
        /* step 3 */
        storeEnvironmentRecord(envRec, env, mv);
        /* step 4 */
        // RuntimeInfo.Function code = func.getCode();
        /* step 5 */
        boolean strict = IsStrict(function);
        boolean legacy = isLegacy(function);
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
        Set<Name> varNames = VarDeclaredNames(function); // unordered set!
        /* step 12 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(function);
        /* step 13 */
        Set<Name> lexicalNames = LexicallyDeclaredNames(function); // unordered set!
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
        HashSet<Name> bindings = new HashSet<>();
        for (Name paramName : parameterNames) {
            if (bindings.add(paramName)) {
                createMutableBinding(envRec, paramName, false, mv);
                if (hasDuplicates) {
                    initializeBinding(envRec, paramName, undefined, mv);
                }
            }
        }
        /* step 22 */
        if (argumentsObjectNeeded) {
            Variable<ArgumentsObject> argumentsObj = mv.newVariable("argumentsObj",
                    ArgumentsObject.class);
            if (strict || !simpleParameterList) {
                CreateUnmappedArgumentsObject(mv);
            } else {
                CreateMappedArgumentsObject(env, formals, mv);
            }
            mv.store(argumentsObj);
            if (legacy) {
                CreateLegacyArguments(argumentsObj, mv);
            }
            if (strict) {
                createImmutableBinding(envRec, arguments, false, mv);
            } else {
                createMutableBinding(envRec, arguments, false, mv);
            }
            initializeBinding(envRec, arguments, argumentsObj, mv);
            parameterNames.add(arguments);
            parameterNamesSet.add(arguments);
        } else if (legacy) {
            if (!simpleParameterList) {
                CreateLegacyArguments(mv);
            } else {
                CreateLegacyArguments(env, formals, mv);
            }
        }
        /* step 23 (not applicable) */
        /* steps 24-26 */
        if (hasParameters) {
            if (hasDuplicates) {
                /* step 24 */
                BindingInitialization(codegen, function, env, iterator, mv);
            } else {
                /* step 25 */
                BindingInitializationWithEnvironment(codegen, function, env, envRec, iterator, mv);
            }
        }
        /* steps 27-28 */
        HashSet<Name> instantiatedVarNames;
        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> varEnv;
        Variable<? extends DeclarativeEnvironmentRecord> varEnvRec;
        if (!hasParameterExpressions) {
            /* step 27.a (note) */
            /* step 27.b */
            instantiatedVarNames = new HashSet<>(parameterNames);
            /* step 27.c */
            for (Name varName : varNames) {
                if (instantiatedVarNames.add(varName)) {
                    createMutableBinding(envRec, varName, false, mv);
                    initializeBinding(envRec, varName, undefined, mv);
                }
            }
            /* steps 27.d-27.e */
            varEnv = env;
            varEnvRec = envRec;
        } else {
            /* step 28.a (note) */
            /* step 28.b */
            varEnv = mv.newVariable("varEnv", LexicalEnvironment.class).uncheckedCast();
            newDeclarativeEnvironment(env, mv);
            mv.store(varEnv);
            /* step 28.c */
            varEnvRec = mv.newVariable("varEnvRec", DeclarativeEnvironmentRecord.class);
            storeEnvironmentRecord(varEnvRec, varEnv, mv);
            /* step 28.d */
            setVariableEnvironment(varEnv, mv);
            /* step 28.e */
            instantiatedVarNames = new HashSet<>();
            /* step 28.f */
            for (Name varName : varNames) {
                if (instantiatedVarNames.add(varName)) {
                    createMutableBinding(varEnvRec, varName, false, mv);
                    if (!parameterNamesSet.contains(varName) || functionNames.contains(varName)) {
                        initializeBinding(varEnvRec, varName, undefined, mv);
                    } else {
                        initializeBindingFrom(varEnvRec, envRec, varName, false, mv);
                    }
                }
            }
        }

        /* step 29 (B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics) */
        for (FunctionDeclaration f : function.getScope().blockFunctions()) {
            Name fname = f.getIdentifier().getName();
            assert f.isLegacyBlockScoped() : "Missing block-scope flag: " + fname.getIdentifier();
            if (instantiatedVarNames.add(fname)) {
                createMutableBinding(varEnvRec, fname, false, mv);
                initializeBinding(varEnvRec, fname, undefined, mv);
            }
        }

        /* steps 30-32 */
        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> lexEnv;
        Variable<? extends DeclarativeEnvironmentRecord> lexEnvRec;
        if (!strict) {
            /* step 30 */
            lexEnv = mv.newVariable("lexEnv", LexicalEnvironment.class).uncheckedCast();
            newDeclarativeEnvironment(varEnv, mv);
            mv.store(lexEnv);
            /* step 32 */
            lexEnvRec = mv.newVariable("lexEnvRec", DeclarativeEnvironmentRecord.class);
            storeEnvironmentRecord(lexEnvRec, lexEnv, mv);
        } else {
            /* step 30 */
            lexEnv = varEnv;
            /* step 32 */
            lexEnvRec = varEnvRec;
        }
        /* step 33 */
        setLexicalEnvironment(lexEnv, mv);
        /* step 34 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(function);
        /* step 35 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof HoistableDeclaration);
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(lexEnvRec, dn, true, mv);
                } else {
                    createMutableBinding(lexEnvRec, dn, false, mv);
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
            setMutableBinding(varEnvRec, fn, fo, false, mv);
        }
        /* step 37 */
        mv._return();
    }

    private void newDeclarativeEnvironment(Variable<? extends LexicalEnvironment<?>> env,
            ExpressionVisitor mv) {
        // stack: [] -> [env]
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    private void setVariableEnvironment(Variable<? extends LexicalEnvironment<?>> env,
            ExpressionVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setVariableEnvironment);
    }

    private void setLexicalEnvironment(Variable<? extends LexicalEnvironment<?>> env,
            ExpressionVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setLexicalEnvironment);
    }

    private void getLexicalEnvironment(Variable<? extends LexicalEnvironment<?>> env,
            ExpressionVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.store(env);
    }

    private void CreateMappedArgumentsObject(
            Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
            FormalParameterList formals, ExpressionVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.loadParameter(FUNCTION, FunctionObject.class);
        newStringArray(mv, mappedNames(formals));
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.load(env);
        mv.invoke(Methods.ArgumentsObject_CreateMappedArgumentsObject);
    }

    private void CreateUnmappedArgumentsObject(ExpressionVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.invoke(Methods.ArgumentsObject_CreateUnmappedArgumentsObject);
    }

    private void CreateLegacyArguments(ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.LegacyArgumentsObject_CreateLegacyArgumentsObjectUnmapped);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private void CreateLegacyArguments(Variable<LexicalEnvironment<FunctionEnvironmentRecord>> env,
            FormalParameterList formals, ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments, formals, scope)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            newStringArray(mv, mappedNames(formals));
            mv.load(env);
            mv.invoke(Methods.LegacyArgumentsObject_CreateLegacyArgumentsObject);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private void CreateLegacyArguments(Variable<ArgumentsObject> argumentsObj, ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments, argumentsObj)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.load(argumentsObj);
            mv.invoke(Methods.LegacyArgumentsObject_CreateLegacyArgumentsObjectFrom);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private String[] mappedNames(FormalParameterList formals) {
        assert IsSimpleParameterList(formals);
        List<FormalParameter> list = formals.getFormals();
        int numberOfParameters = list.size();
        HashSet<String> mappedNames = new HashSet<>();
        String[] names = new String[numberOfParameters];
        for (int index = numberOfParameters - 1; index >= 0; --index) {
            FormalParameter formal = list.get(index);
            assert formal instanceof BindingElement : formal.getClass().toString();
            Binding binding = ((BindingElement) formal).getBinding();
            assert binding instanceof BindingIdentifier : binding.getClass().toString();
            String name = ((BindingIdentifier) binding).getName().getIdentifier();
            if (mappedNames.add(name)) {
                names[index] = name;
            }
        }
        return names;
    }

    private void newStringArray(InstructionVisitor mv, String[] strings) {
        mv.anewarray(strings.length, Types.String);
        int index = 0;
        for (String string : strings) {
            mv.astore(index++, string);
        }
    }

    private boolean isLegacy(FunctionNode node) {
        return !IsStrict(node)
                && (node instanceof FunctionDeclaration || node instanceof FunctionExpression)
                && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
    }
}

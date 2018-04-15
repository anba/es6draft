/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitialization;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FormalParameterList;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.scope.FunctionScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
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
 * <li>9.2.13 FunctionDeclarationInstantiation(func, argumentsList)
 * </ul>
 */
final class FunctionDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
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

        static final MethodName ArgumentsObject_CreateMappedArgumentsObject = MethodName.findStatic(
                Types.ArgumentsObject, "CreateMappedArgumentsObject",
                Type.methodType(Types.ArgumentsObject, Types.ExecutionContext, Types.FunctionObject, Types.Object_,
                        Types.DeclarativeEnvironmentRecord));

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

    private static final class FunctionDeclInitVisitor extends CodeVisitor {
        FunctionDeclInitVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterNameUnchecked("function", 1);
            setParameterName("arguments", 2, Types.Object_);
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(0, ExecutionContext.class);
        }

        Variable<? extends FunctionObject> getFunction() {
            return getParameterUnchecked(1, FunctionObject.class);
        }

        Variable<Object[]> getArguments() {
            return getParameter(2, Object[].class);
        }
    }

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode function, MethodCode method) {
        FunctionDeclInitVisitor mv = new FunctionDeclInitVisitor(method, function);
        mv.lineInfo(function);
        mv.begin();
        mv.enterScope(function);
        generate(function, mv);
        mv.exitScope();
        mv.end();
    }

    private void generate(FunctionNode function, FunctionDeclInitVisitor mv) {
        Variable<ExecutionContext> context = mv.getExecutionContext();
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
            mv.load(mv.getArguments());
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
        ArrayList<Name> parameterNames = new ArrayList<>(BoundNames(formals));
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

        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> paramsEnv;
        Variable<? extends DeclarativeEnvironmentRecord> paramsEnvRec;
        if (codegen.isEnabled(CompatibilityOption.SingleParameterEnvironment)) {
            assert hasParameterExpressions || fscope.parameterVarNames().isEmpty();

            if (hasParameterExpressions) {
                // Var-scoped bindings from do-expressions in parameter expressions.
                for (Name varName : fscope.parameterVarNames()) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, varName);
                    op.createMutableBinding(envRec, varName, false, mv);
                    op.initializeBinding(envRec, varName, undefined, mv);
                }

                boolean hasParameterBindings = !parameterNames.isEmpty();
                assert fscope.parametersScope().isPresent() == (hasParameterBindings || argumentsObjectNeeded);
                if (hasParameterBindings || argumentsObjectNeeded) {
                    paramsEnv = newDeclarativeEnvironment("paramsEnv", env, mv);
                    paramsEnvRec = getDeclarativeEnvironmentRecord("paramsEnvRec", paramsEnv, mv);
                    setLexicalEnvironment(paramsEnv, mv);
                } else {
                    // Optimization: Skip environment allocation if no parameters are present.
                    paramsEnv = env;
                    paramsEnvRec = envRec;
                }
            } else {
                paramsEnv = env;
                paramsEnvRec = envRec;
            }
        } else {
            paramsEnv = env;
            paramsEnvRec = envRec;
        }

        if (fscope.parametersScope() != fscope) {
            mv.enterScope(fscope.parametersScope());
        }

        /* step 21 */
        for (Name paramName : function.getScope().parameterNames()) {
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(paramsEnvRec, paramName);
            op.createMutableBinding(paramsEnvRec, paramName, false, mv);
            if (hasDuplicates) {
                op.initializeBinding(paramsEnvRec, paramName, undefined, mv);
            }
        }
        /* steps 22-23 */
        ArrayList<Name> parameterBindings;
        HashSet<Name> parameterBindingsSet;
        if (argumentsObjectNeeded) {
            /* step 22 */
            assert arguments != null;
            Variable<ArgumentsObject> argumentsObj = mv.newVariable("argumentsObj", ArgumentsObject.class);
            if (strict || !simpleParameterList) {
                CreateUnmappedArgumentsObject(mv);
            } else if (formals.getFormals().isEmpty()) {
                CreateMappedArgumentsObject(mv);
            } else {
                CreateMappedArgumentsObject(paramsEnvRec, formals, mv);
            }
            mv.store(argumentsObj);
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(paramsEnvRec, arguments);
            if (strict) {
                op.createImmutableBinding(paramsEnvRec, arguments, false, mv);
            } else {
                op.createMutableBinding(paramsEnvRec, arguments, false, mv);
            }
            op.initializeBinding(paramsEnvRec, arguments, argumentsObj, mv);

            parameterBindings = new ArrayList<>(parameterNames);
            parameterBindings.add(arguments);
            parameterBindingsSet = new HashSet<>(parameterBindings);
        } else {
            /* step 23 */
            parameterBindings = parameterNames;
            parameterBindingsSet = parameterNamesSet;
        }
        /* steps 24-26 */
        if (hasParameters) {
            if (hasDuplicates) {
                /* step 24 */
                BindingInitialization(codegen, function, env, iterator, mv);
            } else {
                /* step 25 */
                BindingInitialization(codegen, function, env, paramsEnvRec, iterator, mv);
            }
        }
        /* steps 27-28 */
        HashSet<Name> instantiatedVarNames;
        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> varEnv;
        Variable<? extends DeclarativeEnvironmentRecord> varEnvRec;
        if (!hasParameterExpressions) {
            assert fscope == fscope.variableScope();
            /* step 27.a (note) */
            /* step 27.b */
            instantiatedVarNames = new HashSet<>(parameterBindings);
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
            assert fscope.variableScope().isPresent();
            mv.enterScope(fscope.variableScope());
            /* step 28.a (note) */
            /* step 28.b */
            varEnv = newDeclarativeEnvironment("varEnv", paramsEnv, mv);
            /* step 28.c */
            varEnvRec = getDeclarativeEnvironmentRecord("varEnvRec", varEnv, mv);
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
                    if (!parameterBindingsSet.contains(varName) || functionNames.contains(varName)) {
                        op.initializeBinding(varEnvRec, varName, undefined, mv);
                    } else {
                        BindingOp<DeclarativeEnvironmentRecord> opp = BindingOp.of(paramsEnvRec, varName);
                        opp.getBindingValue(paramsEnvRec, varName, strict, mv);
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
        // FIXME: spec issue - wrong step reference in annexB
        for (Name fname : function.getScope().blockFunctionNames()) {
            // FIXME: spec issue - typo "initializedBindings" -> "instantiatedVarNames"
            // FIXME: spec bug - initial binding for "arguments" (https://github.com/tc39/ecma262/issues/991)
            if (instantiatedVarNames.add(fname)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, fname);
                op.createMutableBinding(varEnvRec, fname, false, mv);
                if (!fname.getIdentifier().equals("arguments") || !argumentsObjectNeeded) {
                    op.initializeBinding(varEnvRec, fname, undefined, mv);
                } else {
                    BindingOp<DeclarativeEnvironmentRecord> opp = BindingOp.of(paramsEnvRec, fname);
                    opp.getBindingValue(paramsEnvRec, fname, false, mv);
                    Value<Object> tempValue = mv.storeTemporary(Object.class);
                    op.initializeBinding(varEnvRec, fname, tempValue, mv);
                }
            }
        }

        /* steps 30-32 */
        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> lexEnv;
        Variable<? extends DeclarativeEnvironmentRecord> lexEnvRec;
        assert strict || fscope.variableScope() != fscope.lexicalScope();
        if (!strict || fscope.variableScope() != fscope.lexicalScope()) {
            // NB: Scopes are unmodifiable once constructed, that means we need to emit the extra
            // scope for functions with deferred strict-ness, even if this scope is not present in
            // the specification.
            mv.enterScope(fscope.lexicalScope());
            assert fscope.lexicalScope().isPresent() == !lexicalNames.isEmpty();
            if (!lexicalNames.isEmpty()) {
                /* step 30 */
                lexEnv = newDeclarativeEnvironment("lexEnv", varEnv, mv);
                /* step 32 */
                lexEnvRec = getDeclarativeEnvironmentRecord("lexEnvRec", lexEnv, mv);
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
            Name name = fscope.variableScope().resolveName(fn);
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(varEnvRec, name);
            op.setMutableBinding(varEnvRec, name, fo, false, mv);
        }
        /* step 37 */
        mv._return();
    }

    private Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> newDeclarativeEnvironment(
            String name, Variable<? extends LexicalEnvironment<?>> env, CodeVisitor mv) {
        Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> newEnv = mv
                .newVariable(name, LexicalEnvironment.class)
                .<LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> uncheckedCast();
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
        mv.store(newEnv);
        return newEnv;
    }

    private Variable<? extends DeclarativeEnvironmentRecord> getDeclarativeEnvironmentRecord(String name,
            Variable<? extends LexicalEnvironment<? extends DeclarativeEnvironmentRecord>> env, CodeVisitor mv) {
        Variable<? extends DeclarativeEnvironmentRecord> envRec = mv.newVariable(name,
                DeclarativeEnvironmentRecord.class);
        getEnvironmentRecord(env, envRec, mv);
        return envRec;
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

    private void CreateMappedArgumentsObject(FunctionDeclInitVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.load(mv.getFunction());
        mv.load(mv.getArguments());
        mv.invoke(Methods.ArgumentsObject_CreateMappedArgumentsObject_Empty);
    }

    private void CreateMappedArgumentsObject(Variable<? extends DeclarativeEnvironmentRecord> env,
            FormalParameterList formals, FunctionDeclInitVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.load(mv.getFunction());
        mv.load(mv.getArguments());
        mv.load(env);
        mv.invoke(Methods.ArgumentsObject_CreateMappedArgumentsObject);
    }

    private void CreateUnmappedArgumentsObject(FunctionDeclInitVisitor mv) {
        // stack: [] -> [argsObj]
        mv.loadExecutionContext();
        mv.load(mv.getArguments());
        mv.invoke(Methods.ArgumentsObject_CreateUnmappedArgumentsObject);
    }
}

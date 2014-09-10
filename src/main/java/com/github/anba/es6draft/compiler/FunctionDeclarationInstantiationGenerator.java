/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.FunctionDeclarationCollector.findFunctionDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 ECMAScript Function Objects</h2>
 * <ul>
 * <li>9.2.13 Function Declaration Instantiation(func, argumentsList, env) Abstract Operation
 * </ul>
 */
final class FunctionDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: Arrays
        static final MethodDesc Arrays_asList = MethodDesc.create(MethodType.Static, Types.Arrays,
                "asList", Type.getMethodType(Types.List, Types.Object_));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getVariableEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getVariableEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_setEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "setEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: ArgumentsObject
        static final MethodDesc ArgumentsObject_CreateMappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ArgumentsObject, "CreateMappedArgumentsObject", Type
                        .getMethodType(Types.ArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.String_, Types.Object_,
                                Types.LexicalEnvironment));

        static final MethodDesc ArgumentsObject_CreateUnmappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ArgumentsObject, "CreateUnmappedArgumentsObject",
                Type.getMethodType(Types.ArgumentsObject, Types.ExecutionContext, Types.Object_));

        static final MethodDesc LegacyArgumentsObject_CreateLegacyArgumentsObject = MethodDesc
                .create(MethodType.Static, Types.LegacyArgumentsObject,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.String_,
                                Types.LexicalEnvironment));

        static final MethodDesc LegacyArgumentsObject_CreateLegacyArgumentsObjectFrom = MethodDesc
                .create(MethodType.Static, Types.LegacyArgumentsObject,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.ArgumentsObject));

        static final MethodDesc LegacyArgumentsObject_CreateLegacyArgumentsObjectUnmapped = MethodDesc
                .create(MethodType.Static, Types.LegacyArgumentsObject,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.LegacyArgumentsObject, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_));

        // FunctionObject
        static final MethodDesc FunctionObject_setLegacyArguments = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "setLegacyArguments",
                Type.getMethodType(Type.VOID_TYPE, Types.LegacyArgumentsObject));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: List
        static final MethodDesc List_iterator = MethodDesc.create(MethodType.Interface, Types.List,
                "iterator", Type.getMethodType(Types.Iterator));
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

        Variable<LexicalEnvironment<?>> env = mv.newVariable("env", LexicalEnvironment.class)
                .uncheckedCast();
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env);

        Variable<EnvironmentRecord> envRec = mv.newVariable("envRec", EnvironmentRecord.class);
        getEnvironmentRecord(env, mv);
        mv.store(envRec);

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        Variable<FunctionObject> fo = null;

        boolean hasParameters = !function.getParameters().getFormals().isEmpty();
        Variable<Iterator<?>> iterator = null;
        if (hasParameters) {
            iterator = mv.newVariable("iterator", Iterator.class).uncheckedCast();
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.Arrays_asList);
            mv.invoke(Methods.List_iterator);
            mv.store(iterator);
        }

        /* step 1 */
        // RuntimeInfo.Function code = func.getCode();
        /* step 2 */
        boolean strict = IsStrict(function);
        boolean legacy = isLegacy(function);
        boolean block = !strict && codegen.isEnabled(CompatibilityOption.BlockFunctionDeclaration);
        /* step 3 */
        FormalParameterList formals = function.getParameters();
        /* step 4 */
        List<Name> parameterNames = BoundNames(formals);
        HashSet<Name> parameterNamesSet = new HashSet<>(parameterNames);
        /* step 5 */
        boolean hasDuplicates = parameterNames.size() != parameterNamesSet.size();
        /* step 6 */
        boolean simpleParameterList = IsSimpleParameterList(formals);
        /* step 7 */
        boolean hasParameterExpressions = ContainsExpression(formals);
        // invariant: hasDuplicates => simpleParameterList
        assert !hasDuplicates || simpleParameterList;
        // invariant: hasParameterExpressions => !simpleParameterList
        assert !hasParameterExpressions || !simpleParameterList;
        /* step 8 */
        Set<Name> varNames = VarDeclaredNames(function); // unordered set!
        /* step 9 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(function);
        /* step 10 */
        Set<Name> lexicalNames = LexicallyDeclaredNames(function); // unordered set!
        /* step 11 */
        HashSet<Name> functionNames = new HashSet<>();
        /* step 12 */
        ArrayList<Declaration> functionsToInitialize = new ArrayList<>();
        /* step 13 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (!(item instanceof VariableStatement)) {
                assert isFunctionDeclaration(item);
                Declaration d = (Declaration) item;
                Name fn = BoundName(d);
                if (!functionNames.contains(fn)) {
                    functionNames.add(fn);
                    functionsToInitialize.add(d);
                }
            }
        }
        if (!functionsToInitialize.isEmpty()) {
            fo = mv.newVariable("fo", FunctionObject.class);
        }
        /* step 14 */
        // Optimization: Skip 'arguments' allocation if not referenced in function
        boolean argumentsObjectNeeded = function.getScope().needsArguments();
        Name arguments = function.getScope().arguments();
        /* step 15 */
        if (function.getThisMode() == FunctionNode.ThisMode.Lexical) {
            argumentsObjectNeeded = false;
        }
        /* step 16 */
        else if (parameterNamesSet.contains(arguments)) {
            argumentsObjectNeeded = false;
        }
        /* step 17 */
        else if (!hasParameterExpressions) {
            if (functionNames.contains(arguments) || lexicalNames.contains(arguments)) {
                argumentsObjectNeeded = false;
            }
        }
        /* step 18 */
        HashSet<Name> bindings = new HashSet<>();
        for (Name paramName : parameterNames) {
            boolean alreadyDeclared = bindings.contains(paramName);
            if (!alreadyDeclared) {
                bindings.add(paramName);
                createMutableBinding(envRec, paramName, false, mv);
                if (hasDuplicates) {
                    initializeBinding(envRec, paramName, undef, mv);
                }
            }
        }
        /* step 19 */
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
                createImmutableBinding(envRec, arguments, mv);
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
        /* steps 20-22 */
        if (hasParameters) {
            if (hasDuplicates) {
                /* step 20 */
                BindingInitialization(function, iterator, mv);
            } else {
                /* step 21 */
                BindingInitializationWithEnv(envRec, function, iterator, mv);
            }
        }
        /* steps 23-24 */
        HashSet<Name> instantiatedVarNames;
        Variable<LexicalEnvironment<?>> bodyEnv;
        Variable<EnvironmentRecord> bodyEnvRec;
        if (!hasParameterExpressions) {
            /* steps 23.a-23.c */
            bodyEnv = env;
            bodyEnvRec = envRec;
            instantiatedVarNames = new HashSet<>(parameterNames);
            /* step 23.d */
            for (Name varName : varNames) {
                if (!instantiatedVarNames.contains(varName)) {
                    instantiatedVarNames.add(varName);
                    createMutableBinding(bodyEnvRec, varName, false, mv);
                    initializeBinding(bodyEnvRec, varName, undef, mv);
                }
            }
        } else {
            /* steps 24.a-24.b */
            bodyEnv = mv.newVariable("localEnv", LexicalEnvironment.class).uncheckedCast();
            newDeclarativeEnvironment(env, mv);
            mv.store(bodyEnv);
            bodyEnvRec = mv.newVariable("localEnvRec", EnvironmentRecord.class);
            getEnvironmentRecord(bodyEnv, mv);
            mv.store(bodyEnvRec);
            /* steps 24.c-24.e */
            setEnvironment(bodyEnv, mv);
            /* step 24.f */
            instantiatedVarNames = new HashSet<>();
            /* step 24.g */
            for (Name varName : varNames) {
                if (!instantiatedVarNames.contains(varName)) {
                    instantiatedVarNames.add(varName);
                    createMutableBinding(bodyEnvRec, varName, false, mv);
                    if (!parameterNamesSet.contains(varName) || functionNames.contains(varName)) {
                        initializeBinding(bodyEnvRec, varName, undef, mv);
                    } else {
                        initializeBindingFrom(bodyEnvRec, envRec, varName, false, mv);
                    }
                }
            }
        }

        /* B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics */
        if (block) {
            // Find all function declarations
            boolean catchVar = codegen.isEnabled(CompatibilityOption.CatchVarStatement);
            for (FunctionDeclaration f : findFunctionDeclarations(function, catchVar)) {
                Name fname = f.getIdentifier().getName();
                // FIXME: spec bug - parameterNames must not be checked
                // function f(g=0) { { function g(){} } } f()
                if (!instantiatedVarNames.contains(fname)) {
                    instantiatedVarNames.add(fname);
                    createMutableBinding(bodyEnvRec, fname, false, mv);
                    initializeBinding(bodyEnvRec, fname, undef, mv);
                }
            }
        }

        /* step 25 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(function);
        /* step 26 */
        for (Declaration d : lexDeclarations) {
            assert !isFunctionDeclaration(d);
            for (Name dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(bodyEnvRec, dn, mv);
                } else {
                    createMutableBinding(bodyEnvRec, dn, false, mv);
                }
            }
        }
        /* step 27 */
        for (Declaration f : functionsToInitialize) {
            Name fn = BoundName(f);

            // stack: [] -> [fo]
            InstantiateFunctionObject(context, bodyEnv, f, mv);
            mv.store(fo);

            // stack: [fo] -> []
            setMutableBinding(bodyEnvRec, fn, fo, false, mv);
        }
        /* step 28 */
        mv.areturn();
    }

    private void newDeclarativeEnvironment(Variable<LexicalEnvironment<?>> env, ExpressionVisitor mv) {
        // stack: [] -> [env]
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    private void setEnvironment(Variable<LexicalEnvironment<?>> env, ExpressionVisitor mv) {
        // stack: [] -> []
        mv.loadExecutionContext();
        mv.load(env);
        mv.invoke(Methods.ExecutionContext_setEnvironment);
    }

    private void BindingInitialization(FunctionNode node, Variable<Iterator<?>> iterator,
            ExpressionVisitor mv) {
        // stack: [] -> []
        new BindingInitializationGenerator(codegen).generate(node, iterator, mv);
    }

    private void BindingInitializationWithEnv(Variable<? extends EnvironmentRecord> envRec,
            FunctionNode node, Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        // stack: [] -> []
        new BindingInitializationGenerator(codegen).generateWithEnvironment(node, envRec, iterator,
                mv);
    }

    private void CreateMappedArgumentsObject(Variable<LexicalEnvironment<?>> env,
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

    private void CreateLegacyArguments(Variable<LexicalEnvironment<?>> env,
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
            assert list.get(index) instanceof BindingElement;
            BindingElement formal = (BindingElement) list.get(index);
            assert formal.getBinding() instanceof BindingIdentifier;
            String name = ((BindingIdentifier) formal.getBinding()).getName().getIdentifier();
            if (!mappedNames.contains(name)) {
                mappedNames.add(name);
                names[index] = name;
            }
        }
        return names;
    }

    private void newStringArray(InstructionVisitor mv, String[] strings) {
        mv.newarray(strings.length, Types.String);
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

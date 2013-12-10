/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 ECMAScript Function Objects</h2>
 * <ul>
 * <li>9.2.13 Function Declaration Instantiation
 * </ul>
 */
class FunctionDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static class Methods {
        // class: Arrays
        static final MethodDesc Arrays_asList = MethodDesc.create(MethodType.Static, Types.Arrays,
                "asList", Type.getMethodType(Types.List, Types.Object_));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getVariableEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getVariableEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        // class: ExoticArguments
        static final MethodDesc ExoticArguments_CreateMappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CreateMappedArgumentsObject", Type
                        .getMethodType(Types.ExoticArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.String_,
                                Types.LexicalEnvironment));

        static final MethodDesc ExoticArguments_CreateStrictArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CreateStrictArgumentsObject",
                Type.getMethodType(Types.ExoticArguments, Types.ExecutionContext, Types.Object_));

        static final MethodDesc ExoticArguments_CreateLegacyArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CreateLegacyArgumentsObject", Type
                        .getMethodType(Types.ExoticArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.String_,
                                Types.LexicalEnvironment));

        static final MethodDesc ExoticArguments_CreateLegacyArgumentsObjectFrom = MethodDesc
                .create(MethodType.Static, Types.ExoticArguments, "CreateLegacyArgumentsObject",
                        Type.getMethodType(Types.ExoticArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.ExoticArguments));

        // FunctionObject
        static final MethodDesc FunctionObject_setLegacyArguments = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "setLegacyArguments",
                Type.getMethodType(Type.VOID_TYPE, Types.ExoticArguments));

        // class: List
        static final MethodDesc List_iterator = MethodDesc.create(MethodType.Interface, Types.List,
                "iterator", Type.getMethodType(Types.Iterator));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int FUNCTION = 1;
    private static final int ARGUMENTS = 2;

    private static class FunctionDeclInitMethodGenerator extends ExpressionVisitor {
        FunctionDeclInitMethodGenerator(CodeGenerator codegen, FunctionNode node) {
            super(codegen, codegen.methodName(node, FunctionName.Init), codegen.methodType(node,
                    FunctionName.Init), IsStrict(node), false);
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
        ExpressionVisitor mv = new FunctionDeclInitMethodGenerator(codegen, function);

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

        Variable<LexicalEnvironment> env = mv.newVariable("env", LexicalEnvironment.class);
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env);

        Variable<EnvironmentRecord> envRec = mv.newVariable("envRec", EnvironmentRecord.class);
        getEnvironmentRecord(env, mv);
        mv.store(envRec);

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        boolean hasParameters = !function.getParameters().getFormals().isEmpty();
        Variable<Iterator<?>> iterator = null;
        if (hasParameters) {
            iterator = uncheckedCast(mv.newVariable("iterator", Iterator.class));
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.Arrays_asList);
            mv.invoke(Methods.List_iterator);
            mv.store(iterator);
        }

        Set<String> bindings = new HashSet<>();
        /* step 1 */
        // RuntimeInfo.Code code = func.getCode();
        /* step 2 */
        boolean strict = IsStrict(function);
        boolean legacy = !strict && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
        /* step 3 */
        FormalParameterList formals = function.getParameters();
        /* step 4 */
        List<String> parameterNames = BoundNames(formals);
        /* step 5 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(function);
        /* step 6 */
        List<Declaration> functionsToInitialise = new ArrayList<>();
        /* steps 7-8 */
        boolean argumentsObjectNeeded;
        if (function instanceof ArrowFunction) { // => [[ThisMode]] of func is lexical
            argumentsObjectNeeded = false;
        } else {
            argumentsObjectNeeded = true;
        }
        /* step 9 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof FunctionDeclaration || item instanceof GeneratorDeclaration) {
                Declaration d = (Declaration) item;
                String fn = BoundName(d);
                if ("arguments".equals(fn)) {
                    argumentsObjectNeeded = false;
                }
                boolean alreadyDeclared = bindings.contains(fn);
                if (!alreadyDeclared) {
                    bindings.add(fn);
                    functionsToInitialise.add(d);
                    createMutableBinding(envRec, fn, false, mv);
                }
            }
        }
        /* step 10 */
        for (String paramName : parameterNames) {
            boolean alreadyDeclared = bindings.contains(paramName);
            if (!alreadyDeclared) {
                if ("arguments".equals(paramName)) {
                    argumentsObjectNeeded = false;
                }
                bindings.add(paramName);
                createMutableBinding(envRec, paramName, false, mv);
                // stack: [undefined] -> []
                mv.load(undef);
                initialiseBinding(envRec, paramName, mv);
            }
        }
        /* steps 11-12 */
        if (argumentsObjectNeeded) {
            bindings.add("arguments");
            if (strict) {
                createImmutableBinding(envRec, "arguments", mv);
            } else {
                createMutableBinding(envRec, "arguments", false, mv);
            }
        }
        /* step 13 */
        Set<String> varNames = VarDeclaredNames(function);
        /* step 14 */
        for (String varName : varNames) {
            boolean alreadyDeclared = bindings.contains(varName);
            if (!alreadyDeclared) {
                bindings.add(varName);
                createMutableBinding(envRec, varName, false, mv);
                // FIXME: spec bug (partially reported in Bug 1420)
                mv.load(undef);
                initialiseBinding(envRec, varName, mv);
            }
        }
        /* step 15 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(function);
        /* step 16 */
        for (Declaration d : lexDeclarations) {
            assert !(d instanceof GeneratorDeclaration);
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(envRec, dn, mv);
                } else {
                    createMutableBinding(envRec, dn, false, mv);
                }
            }
        }
        /* step 17 */
        for (Declaration f : functionsToInitialise) {
            String fn = BoundName(f);
            // stack: [] -> [fo]
            if (f instanceof GeneratorDeclaration) {
                InstantiateGeneratorObject(context, env, (GeneratorDeclaration) f, mv);
            } else {
                InstantiateFunctionObject(context, env, (FunctionDeclaration) f, mv);
            }
            // stack: [fo] -> []
            // setMutableBinding(envRec, fn, false, mv);
            initialiseBinding(envRec, fn, mv);
        }
        /* steps 18-20 */
        if (hasParameters) {
            BindingInitialisation(function, iterator, mv);
        }
        /* step 21 */
        if (argumentsObjectNeeded) {
            // stack: [] -> [ao]
            if (strict) {
                CreateStrictArgumentsObject(mv);
            } else {
                CreateMappedArgumentsObject(env, formals, mv);
            }
            if (legacy) {
                Variable<ExoticArguments> argumentsObj = mv.newVariable("argumentsObj",
                        ExoticArguments.class);
                mv.store(argumentsObj);
                CreateLegacyArguments(argumentsObj, mv);
                mv.load(argumentsObj);
            }
            // stack: [ao] -> []
            initialiseBinding(envRec, "arguments", mv);
        } else if (legacy) {
            // stack: [] -> []
            CreateLegacyArguments(env, formals, mv);
        }
        /* step 22 */
        mv.areturn();
    }

    private void BindingInitialisation(FunctionNode node, Variable<Iterator<?>> iterator,
            ExpressionVisitor mv) {
        // stack: [] -> []
        new BindingInitialisationGenerator(codegen).generate(node, iterator, mv);
    }

    private void CreateMappedArgumentsObject(Variable<LexicalEnvironment> env,
            FormalParameterList formals, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.loadParameter(FUNCTION, FunctionObject.class);
        mv.loadParameter(ARGUMENTS, Object[].class);
        newStringArray(mv, mappedNames(formals));
        mv.load(env);
        mv.invoke(Methods.ExoticArguments_CreateMappedArgumentsObject);
    }

    private void CreateStrictArgumentsObject(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.invoke(Methods.ExoticArguments_CreateStrictArgumentsObject);
    }

    private void CreateLegacyArguments(Variable<LexicalEnvironment> env,
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
            mv.invoke(Methods.ExoticArguments_CreateLegacyArgumentsObject);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private void CreateLegacyArguments(Variable<ExoticArguments> argumentsObj, ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments, argumentsObj)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.load(argumentsObj);
            mv.invoke(Methods.ExoticArguments_CreateLegacyArgumentsObjectFrom);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private String[] mappedNames(FormalParameterList formals) {
        List<FormalParameter> list = formals.getFormals();
        int numberOfNonRestFormals = NumberOfParameters(formals);
        assert numberOfNonRestFormals <= list.size();

        Set<String> mappedNames = new HashSet<>();
        String[] names = new String[numberOfNonRestFormals];
        for (int index = numberOfNonRestFormals - 1; index >= 0; --index) {
            assert list.get(index) instanceof BindingElement;
            BindingElement formal = (BindingElement) list.get(index);
            if (formal.getBinding() instanceof BindingIdentifier) {
                String name = ((BindingIdentifier) formal.getBinding()).getName();
                if (!mappedNames.contains(name)) {
                    mappedNames.add(name);
                    names[index] = name;
                }
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Variable<Iterator<?>> uncheckedCast(Variable<Iterator> o) {
        return (Variable<Iterator<?>>) (Variable<?>) o;
    }
}

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
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.BindingElement;
import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FormalParameter;
import com.github.anba.es6draft.ast.FormalParameterList;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.ExpressionVisitor.Register;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.3 Function Declaration Instantiation
 * </ul>
 */
class FunctionDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static class Fields {
        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static class Methods {
        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getRealm = MethodDesc.create(MethodType.Virtual,
                Types.ExecutionContext, "getRealm", Type.getMethodType(Types.Realm));

        static final MethodDesc ExecutionContext_getVariableEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getVariableEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        // class: ExoticArguments
        static final MethodDesc ExoticArguments_InstantiateArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "InstantiateArgumentsObject",
                Type.getMethodType(Types.ExoticArguments, Types.Realm, Types.Object_));

        static final MethodDesc ExoticArguments_CompleteStrictArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CompleteStrictArgumentsObject",
                Type.getMethodType(Type.VOID_TYPE, Types.Realm, Types.ExoticArguments));

        static final MethodDesc ExoticArguments_CompleteMappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CompleteMappedArgumentsObject", Type
                        .getMethodType(Type.VOID_TYPE, Types.Realm, Types.ExoticArguments,
                                Types.FunctionObject, Types.String_, Types.LexicalEnvironment));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));
    }

    private static class FunctionDeclInitMethodGenerator extends ExpressionVisitor {
        static final Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.FunctionObject, Types.Object_);

        private FunctionDeclInitMethodGenerator(CodeGenerator codegen, String methodName,
                boolean strict) {
            super(codegen.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, strict, false, false);
        }

        @Override
        public void begin() {
            super.begin();
            load(Register.ExecutionContext);
            invoke(Methods.ExecutionContext_getRealm);
            store(Register.Realm);
        }

        @Override
        protected int var(Register reg) {
            switch (reg) {
            case ExecutionContext:
                return 0;
                // 1 = Function
                // 2 = Object[]
            case Realm:
                return 3;
            default:
                assert false : reg;
                return -1;
            }
        }
    }

    private static final int FUNCTION = 1;
    private static final int ARGUMENTS = 2;

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode func) {
        String methodName = codegen.methodName(func) + "_init";
        ExpressionVisitor mv = new FunctionDeclInitMethodGenerator(codegen, methodName,
                func.isStrict());

        mv.begin();
        mv.enterScope(func);
        generate(func, mv);
        mv.exitScope();
        mv.areturn();
        mv.end();
    }

    private void generate(FunctionNode func, ExpressionVisitor mv) {
        int realm = mv.var(Register.Realm);

        int env = mv.newVariable(Types.LexicalEnvironment);
        mv.load(Register.ExecutionContext);
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env, Types.LexicalEnvironment);

        int envRec = mv.newVariable(Types.EnvironmentRecord);
        mv.load(env, Types.LexicalEnvironment);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        mv.store(envRec, Types.EnvironmentRecord);

        int undef = mv.newVariable(Types.Undefined);
        mv.get(Fields.Undefined_UNDEFINED);
        mv.store(undef, Types.Undefined);

        Set<String> bindings = new HashSet<>();
        /* [10.5.3] step 1 */
        // RuntimeInfo.Code code = func.getCode();
        /* [10.5.3] step 2 */
        boolean strict = func.isStrict();
        /* [10.5.3] step 3 */
        FormalParameterList formals = func.getParameters();
        /* [10.5.3] step 4 */
        List<String> parameterNames = BoundNames(formals);
        /* [10.5.3] step 5 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(func);
        /* [10.5.3] step 6 */
        List<Declaration> functionsToInitialize = new ArrayList<>();
        /* [10.5.3] step 7 */
        boolean argumentsObjectNotNeeded = false;
        /* [10.5.3] step 8 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (item instanceof FunctionDeclaration || item instanceof GeneratorDeclaration) {
                Declaration d = (Declaration) item;
                String fn = BoundName(d);
                if ("arguments".equals(fn)) {
                    argumentsObjectNotNeeded = true;
                }
                boolean alreadyDeclared = bindings.contains(fn);
                if (!alreadyDeclared) {
                    bindings.add(fn);
                    functionsToInitialize.add(d);
                    createMutableBinding(envRec, fn, false, mv);
                }
            }
        }
        /* [10.5.3] step 9 */
        for (String paramName : parameterNames) {
            boolean alreadyDeclared = bindings.contains(paramName);
            if (!alreadyDeclared) {
                if ("arguments".equals(paramName)) {
                    argumentsObjectNotNeeded = true;
                }
                bindings.add(paramName);
                createMutableBinding(envRec, paramName, false, mv);
                // stack: [undefined] -> []
                mv.load(undef, Types.Undefined);
                initializeBinding(envRec, paramName, mv);
            }
        }
        /* [10.5.3] step 10-11 */
        if (!argumentsObjectNotNeeded) {
            bindings.add("arguments");
            if (strict) {
                createImmutableBinding(envRec, "arguments", mv);
            } else {
                createMutableBinding(envRec, "arguments", false, mv);
            }
        }
        /* [10.5.3] step 12 */
        Set<String> varNames = VarDeclaredNames(func);
        /* [10.5.3] step 13 */
        for (String varName : varNames) {
            boolean alreadyDeclared = bindings.contains(varName);
            if (!alreadyDeclared) {
                bindings.add(varName);
                createMutableBinding(envRec, varName, false, mv);
                // FIXME: spec bug
                mv.load(undef, Types.Undefined);
                initializeBinding(envRec, varName, mv);
            }
        }
        /* [10.5.3] step 14 */
        List<Declaration> lexDeclarations = LexicallyScopedDeclarations(func);
        /* [10.5.3] step 15 */
        for (Declaration d : lexDeclarations) {
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(envRec, dn, mv);
                } else {
                    createMutableBinding(envRec, dn, false, mv);
                }
            }
        }
        /* [10.5.3] step 16 */
        for (Declaration f : functionsToInitialize) {
            String fn = BoundName(f);
            // stack: [] -> [fo]
            if (f instanceof GeneratorDeclaration) {
                InstantiateGeneratorObject(realm, env, (GeneratorDeclaration) f, mv);
            } else {
                InstantiateFunctionObject(realm, env, (FunctionDeclaration) f, mv);
            }
            // stack: [fo] -> []
            // setMutableBinding(envRec, fn, false, mv);
            initializeBinding(envRec, fn, mv);
        }
        /* [10.5.3] step 17-19 */
        // stack: [] -> [ao]
        InstantiateArgumentsObject(mv);
        /* [10.5.3] step 20-21 */
        BindingInitialisation(func, mv);
        /* [10.5.3] step 22 */
        if (!argumentsObjectNotNeeded) {
            if (strict) {
                CompleteStrictArgumentsObject(mv);
            } else {
                CompleteMappedArgumentsObject(env, formals, mv);
            }
            // stack: [ao] -> []
            initializeBinding(envRec, "arguments", mv);
        } else {
            // stack: [ao] -> []
            mv.pop();
        }
        /* [10.5.3] step 23 */
        return;
    }

    private void InstantiateArgumentsObject(ExpressionVisitor mv) {
        mv.load(Register.Realm);
        mv.load(ARGUMENTS, Types.Object_);
        mv.invoke(Methods.ExoticArguments_InstantiateArgumentsObject);
    }

    private void BindingInitialisation(FunctionNode node, ExpressionVisitor mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        new BindingInitialisationGenerator(codegen).generate(node, mv);
    }

    private void CompleteStrictArgumentsObject(ExpressionVisitor mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        mv.load(Register.Realm);
        mv.swap();
        mv.invoke(Methods.ExoticArguments_CompleteStrictArgumentsObject);
    }

    private void CompleteMappedArgumentsObject(int env, FormalParameterList formals,
            ExpressionVisitor mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        mv.load(Register.Realm);
        mv.swap();
        mv.load(FUNCTION, Types.FunctionObject);
        astore_string(mv, mappedNames(formals));
        mv.load(env, Types.LexicalEnvironment);
        mv.invoke(Methods.ExoticArguments_CompleteMappedArgumentsObject);
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

    private void astore_string(InstructionVisitor mv, String[] strings) {
        mv.newarray(strings.length, Types.String);
        int index = 0;
        for (String string : strings) {
            mv.astore(index++, string, Types.String);
        }
    }
}

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

import org.objectweb.asm.MethodVisitor;
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
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.3 Function Declaration Instantiation
 * </ul>
 */
class FunctionDeclarationInstantiationGenerator extends DeclarationBindingInstantiationGenerator {
    private static class FunctionDeclInitMethodGenerator extends MethodGenerator {
        private FunctionDeclInitMethodGenerator(MethodVisitor mv, String methodName,
                Type methodDescriptor, boolean strict) {
            super(mv, methodName, methodDescriptor, strict, false, false);
        }

        @Override
        public void begin() {
            super.begin();
            load(Register.ExecutionContext);
            invokevirtual(Methods.ExecutionContext_getRealm);
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

    private static final Type methodType = Type.getMethodType(Type.VOID_TYPE,
            Types.ExecutionContext, Types.Function, Types.Object_);

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode func) {
        String methodName = codegen.methodName(func) + "_init";

        MethodVisitor mv = codegen.publicStaticMethod(methodName, methodType.getInternalName());
        MethodGenerator declinit = new FunctionDeclInitMethodGenerator(mv, methodName, methodType,
                func.isStrict());

        declinit.begin();
        generate(func, declinit);
        declinit.areturn();
        declinit.end();
    }

    private void generate(FunctionNode func, MethodGenerator mv) {
        int realm = mv.var(Register.Realm);

        int env = mv.newVariable(Types.LexicalEnvironment);
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env, Types.LexicalEnvironment);

        int envRec = mv.newVariable(Types.EnvironmentRecord);
        mv.load(env, Types.LexicalEnvironment);
        mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);
        mv.store(envRec, Types.EnvironmentRecord);

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
                    // FIXME: not in spec -> changed from mutable to immutable binding
                    // envRec.createMutableBinding(fn, false);
                    bindings.add(fn);
                    createImmutableBinding(envRec, fn, mv);
                    functionsToInitialize.add(d);
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
                mv.getstatic(Fields.Undefined_UNDEFINED);
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
            // FIXME: not in spec -> changed from mutable to immutable binding
            // setMutableBinding(fn, false);

            // stack: [fo] -> []
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
        }
        /* [10.5.3] step 23 */
        return;
    }

    private void InstantiateArgumentsObject(MethodGenerator mv) {
        mv.load(Register.Realm);
        mv.load(ARGUMENTS, Types.Object_);
        mv.invokestatic(Methods.ExoticArguments_InstantiateArgumentsObject);
    }

    private void BindingInitialisation(FunctionNode node, MethodGenerator mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        new BindingInitialisationGenerator(codegen).generate(node, mv);
    }

    private void CompleteStrictArgumentsObject(MethodGenerator mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        mv.load(Register.Realm);
        mv.swap();
        mv.invokestatic(Methods.ExoticArguments_CompleteStrictArgumentsObject);
    }

    private void CompleteMappedArgumentsObject(int env, FormalParameterList formals,
            MethodGenerator mv) {
        // stack: [ao] -> [ao]
        mv.dup();
        mv.load(Register.Realm);
        mv.swap();
        mv.load(FUNCTION, Types.Function);
        astore_string(mv, mappedNames(formals));
        mv.load(env, Types.LexicalEnvironment);
        mv.invokestatic(Methods.ExoticArguments_CompleteMappedArgumentsObject);
    }

    private String[] mappedNames(FormalParameterList formals) {
        List<FormalParameter> list = formals.getFormals();
        Set<String> mappedNames = new HashSet<>();
        int numberOfNonRestFormals = NumberOfParameters(formals);
        assert numberOfNonRestFormals <= list.size();
        String[] names = new String[numberOfNonRestFormals];
        for (int index = numberOfNonRestFormals - 1; index >= 0; --index) {
            assert list.get(index) instanceof BindingElement;
            String name = null;
            BindingElement formal = (BindingElement) list.get(index);
            if (formal.getBinding() instanceof BindingIdentifier) {
                name = ((BindingIdentifier) formal.getBinding()).getName();
                if (!mappedNames.contains(name)) {
                    mappedNames.add(name);
                } else {
                    name = null;
                }
            }
            names[index] = name;
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

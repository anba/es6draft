/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.BlockScope;
import com.github.anba.es6draft.ast.FunctionScope;
import com.github.anba.es6draft.ast.Identifier;
import com.github.anba.es6draft.ast.Scope;
import com.github.anba.es6draft.ast.WithScope;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord.Binding;

/**
 * 8.3.1 ResolveBinding(name)
 */
final class IdentifierResolution {
    private static final class Methods {
        // getValue()
        static final MethodDesc Binding_getValue = MethodDesc.create(MethodType.Virtual,
                Types.DeclarativeEnvironmentRecord$Binding, "getValue",
                Type.getMethodType(Types.Object));

        // toReference()
        static final MethodDesc Binding_toReference = MethodDesc.create(MethodType.Virtual,
                Types.DeclarativeEnvironmentRecord$Binding, "toReference",
                Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierResolution()
        static final MethodDesc ExecutionContext_resolveBinding = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "resolveBinding",
                Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierValue()
        static final MethodDesc ExecutionContext_resolveBindingValue = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "resolveBindingValue",
                Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    ValType resolve(Identifier node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        return resolve(node.getName(), mv);
    }

    ValType resolve(BindingIdentifier node, ExpressionVisitor mv) {
        return resolve(node.getName(), mv);
    }

    ValType resolveValue(Identifier node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        return resolveValue(node.getName(), mv);
    }

    private ValType resolve(String identifierName, ExpressionVisitor mv) {
        Variable<Binding> variable = getVariable(identifierName, mv);
        if (variable != null) {
            mv.load(variable);
            mv.aconst(identifierName);
            mv.iconst(mv.isStrict());
            mv.invoke(Methods.Binding_toReference);
            return ValType.Reference;
        }

        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_resolveBinding);

        return ValType.Reference;
    }

    private ValType resolveValue(String identifierName, ExpressionVisitor mv) {
        Variable<Binding> variable = getVariable(identifierName, mv);
        if (variable != null) {
            mv.load(variable);
            mv.invoke(Methods.Binding_getValue);
            return ValType.Any;
        }

        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_resolveBindingValue);

        return ValType.Any;
    }

    private Variable<DeclarativeEnvironmentRecord.Binding> getVariable(String identifierName,
            ExpressionVisitor mv) {
        Variable<Binding> variable = mv.getVariable(identifierName);
        if (variable == null) {
            return null;
        }
        // Check if the found binding is actually correct, that means it is not shadowed by inner
        // lexical bindings or enclosed by with-statements
        for (Scope scope = mv.getScope(); scope != null; scope = scope.getParent()) {
            if (scope instanceof WithScope) {
                // WithScope may introduce arbitrary new bindings
                break;
            } else if (scope instanceof BlockScope) {
                if (scope.isDeclared(identifierName)) {
                    // var-binding is shadowed by lexical binding
                    break;
                }
            } else {
                // FIXME: no longer correct with rev23 parameter environment changes
                // Reached function scope
                assert scope instanceof FunctionScope : scope.getClass().getSimpleName();
                FunctionScope funScope = (FunctionScope) scope;
                assert funScope.isDeclared(identifierName);
                assert funScope.parameterNames().contains(identifierName)
                        || funScope.varDeclaredNames().contains(identifierName);
                return variable;
            }
        }
        return null;
    }
}

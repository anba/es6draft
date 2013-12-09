/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorComprehension;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.MethodDefinition;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * 
 */
class FunctionCodeGenerator extends DeclarationBindingInstantiationGenerator {
    private static class Methods {
        // ExecutionContext
        static final MethodDesc ExecutionContext_newFunctionExecutionContext = MethodDesc.create(
                MethodType.Static, Types.ExecutionContext, "newFunctionExecutionContext",
                Type.getMethodType(Types.ExecutionContext, Types.FunctionObject, Types.Object));

        static final MethodDesc ExecutionContext_getCurrentFunction = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getCurrentFunction",
                Type.getMethodType(Types.FunctionObject));

        // FunctionObject
        static final MethodDesc FunctionObject_getLegacyArguments = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "getLegacyArguments",
                Type.getMethodType(Types.Object));

        static final MethodDesc FunctionObject_getLegacyCaller = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "getLegacyCaller",
                Type.getMethodType(Types.Object));

        static final MethodDesc FunctionObject_setLegacyCaller = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "setLegacyCaller",
                Type.getMethodType(Type.VOID_TYPE, Types.FunctionObject));

        static final MethodDesc FunctionObject_restoreLegacyProperties = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "restoreLegacyProperties",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.Object));

        // OrdinaryGenerator
        static final MethodDesc OrdinaryGenerator_EvaluateBody = MethodDesc.create(
                MethodType.Static, Types.OrdinaryGenerator, "EvaluateBody", Type.getMethodType(
                        Types.GeneratorObject, Types.ExecutionContext, Types.OrdinaryGenerator));

        static final MethodDesc OrdinaryGenerator_EvaluateBodyComprehension = MethodDesc.create(
                MethodType.Static, Types.OrdinaryGenerator, "EvaluateBodyComprehension", Type
                        .getMethodType(Types.GeneratorObject, Types.ExecutionContext,
                                Types.OrdinaryGenerator));
    }

    private static final int FUNCTION = 0;
    private static final int GENERATOR = 0;
    private static final int EXECUTION_CONTEXT = 1;
    private static final int THIS_VALUE = 2;
    private static final int ARGUMENTS = 3;

    private static class FunctionCodeMethodGenerator extends InstructionVisitor {
        FunctionCodeMethodGenerator(CodeGenerator codeGenerator, FunctionNode node) {
            this(codeGenerator, codeGenerator.methodName(node, FunctionName.Call), codeGenerator
                    .methodType(node, FunctionName.Call));
        }

        FunctionCodeMethodGenerator(CodeGenerator codeGenerator, String methodName,
                Type methodDescriptor) {
            super(codeGenerator.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, MethodAllocation.Class);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("function", FUNCTION, Types.OrdinaryFunction);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static class GeneratorCodeMethodGenerator extends InstructionVisitor {
        GeneratorCodeMethodGenerator(CodeGenerator codeGenerator, FunctionNode node) {
            this(codeGenerator, codeGenerator.methodName(node, FunctionName.Call), codeGenerator
                    .methodType(node, FunctionName.Call));
        }

        GeneratorCodeMethodGenerator(CodeGenerator codeGenerator, String methodName,
                Type methodDescriptor) {
            super(codeGenerator.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, MethodAllocation.Class);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("generator", GENERATOR, Types.OrdinaryGenerator);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    private static class GeneratorComprehensionCodeMethodGenerator extends InstructionVisitor {
        GeneratorComprehensionCodeMethodGenerator(CodeGenerator codeGenerator,
                GeneratorComprehension node) {
            this(codeGenerator, codeGenerator.methodName(node, FunctionName.Call), codeGenerator
                    .methodType(node, FunctionName.Call));
        }

        GeneratorComprehensionCodeMethodGenerator(CodeGenerator codeGenerator, String methodName,
                Type methodDescriptor) {
            super(codeGenerator.publicStaticMethod(methodName, methodDescriptor.getInternalName()),
                    methodName, methodDescriptor, MethodAllocation.Class);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("generator", GENERATOR, Types.OrdinaryGenerator);
            setParameterName("callerContext", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("thisValue", THIS_VALUE, Types.Object);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    FunctionCodeGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode node) {
        if (isGenerator(node)) {
            generateGenerator(node);
        } else {
            generateFunction(node);
        }
    }

    void generate(GeneratorComprehension node) {
        InstructionVisitor mv = new GeneratorComprehensionCodeMethodGenerator(codegen, node);
        mv.lineInfo(node.getBeginLine());
        mv.begin();

        generateGeneratorComprehension(node, mv);

        mv.end();
    }

    private void generateFunction(FunctionNode node) {
        InstructionVisitor mv = new FunctionCodeMethodGenerator(codegen, node);
        mv.lineInfo(node.getBeginLine());
        mv.begin();

        if (isLegacy(node)) {
            generateLegacyFunction(node, mv);
        } else {
            generateFunction(node, mv);
        }

        mv.end();
    }

    private void generateGenerator(FunctionNode node) {
        InstructionVisitor mv = new GeneratorCodeMethodGenerator(codegen, node);
        mv.lineInfo(node.getBeginLine());
        mv.begin();

        generateGenerator(node, mv);

        mv.end();
    }

    private void generateLegacyFunction(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryFunction> function = mv.getParameter(FUNCTION, OrdinaryFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);
        Variable<Object> oldCaller = mv.newVariable("oldCaller", Object.class);
        Variable<Object> oldArguments = mv.newVariable("oldArguments", Object.class);
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);

        // (1) Retrieve 'caller' and 'arguments' and store in local variables
        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyCaller);
        mv.store(oldCaller);

        mv.load(function);
        mv.invoke(Methods.FunctionObject_getLegacyArguments);
        mv.store(oldArguments);

        Label startFinally = new Label(), endFinally = new Label(), handlerFinally = new Label();
        mv.mark(startFinally);
        {
            // (2) Create a new ExecutionContext
            newFunctionExecutionContext(calleeContext, function, thisValue, mv);

            // (3) Update 'caller' property
            setLegacyCaller(function, callerContext, mv);

            // (4) Perform FunctionDeclarationInstantiation
            functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

            // (5) Perform EvaluateBody
            evaluateBody(node, calleeContext, mv);

            // (6) Restore 'caller' and 'arguments'
            restoreLegacyProperties(function, oldCaller, oldArguments, mv);

            // (7) Return result value
            mv.areturn(Types.Object);
        }
        mv.mark(endFinally);

        // Exception: Restore 'caller' and 'arguments' and then rethrow exception
        mv.mark(handlerFinally);
        mv.store(throwable);
        restoreLegacyProperties(function, oldCaller, oldArguments, mv);
        mv.load(throwable);
        mv.athrow();

        mv.visitTryCatchBlock(startFinally, endFinally, handlerFinally,
                Types.ScriptException.getInternalName());
    }

    private void generateFunction(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryFunction> function = mv.getParameter(FUNCTION, OrdinaryFunction.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, function, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        // (3) Perform EvaluateBody
        evaluateBody(node, calleeContext, mv);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    private void generateGenerator(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, generator, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    private void generateGeneratorComprehension(GeneratorComprehension node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, generator, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform EvaluateBodyComprehension
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBodyComprehension);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    private void newFunctionExecutionContext(Variable<ExecutionContext> calleeContext,
            Variable<? extends FunctionObject> function, Variable<Object> thisValue,
            InstructionVisitor mv) {
        // calleeContext = newFunctionExecutionContext(function, thisValue)
        mv.load(function);
        mv.load(thisValue);
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    private void functionDeclarationInstantiation(FunctionNode node,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        String className = codegen.getClassName();
        String methodName = codegen.methodName(node, FunctionName.Init);
        String desc = codegen.methodDescriptor(node, FunctionName.Init);

        mv.load(calleeContext);
        mv.load(function);
        mv.load(arguments);
        mv.invokestatic(className, methodName, desc);
    }

    private void functionDeclarationInstantiation(GeneratorComprehension node,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        // NB: generator comprehensions are defined in terms of generator functions which means they
        // inherit the function declaration instantiation code from 9.2.13

        /* steps 1-22 (not applicable, argumentsObjectNeeded = false) */
    }

    private void evaluateBody(FunctionNode node, Variable<ExecutionContext> calleeContext,
            InstructionVisitor mv) {
        String className = codegen.getClassName();
        String methodName = codegen.methodName(node, FunctionName.Code);
        String desc = codegen.methodDescriptor(node, FunctionName.Code);

        mv.load(calleeContext);
        mv.invokestatic(className, methodName, desc);
    }

    private void setLegacyCaller(Variable<? extends FunctionObject> function,
            Variable<ExecutionContext> callerContext, InstructionVisitor mv) {
        // function.setLegacyCaller(callerContext.getCurrentFunction())
        mv.load(function);
        mv.load(callerContext);
        mv.invoke(Methods.ExecutionContext_getCurrentFunction);
        mv.invoke(Methods.FunctionObject_setLegacyCaller);
    }

    private void restoreLegacyProperties(Variable<? extends FunctionObject> function,
            Variable<Object> oldCaller, Variable<Object> oldArguments, InstructionVisitor mv) {
        // function.restoreLegacyProperties(oldCaller, oldArguments)
        mv.load(function);
        mv.load(oldCaller);
        mv.load(oldArguments);
        mv.invoke(Methods.FunctionObject_restoreLegacyProperties);
    }

    private static boolean isGenerator(FunctionNode node) {
        if (node instanceof GeneratorDefinition) {
            return true;
        } else if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).getType() == MethodDefinition.MethodType.Generator;
        } else {
            return false;
        }
    }

    private boolean isLegacy(FunctionNode node) {
        return !IsStrict(node) && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
    }
}

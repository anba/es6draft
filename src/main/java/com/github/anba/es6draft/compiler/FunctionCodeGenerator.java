/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorComprehension;
import com.github.anba.es6draft.compiler.Code.MethodCode;
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
 * Generates bytecode for the function entry method
 */
final class FunctionCodeGenerator {
    private static final class Methods {
        // ExecutionContext
        static final MethodDesc ExecutionContext_newFunctionExecutionContext = MethodDesc.create(
                MethodType.Static, Types.ExecutionContext, "newFunctionExecutionContext", Type
                        .getMethodType(Types.ExecutionContext, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object));

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

    private static final class FunctionCodeMethodGenerator extends InstructionVisitor {
        FunctionCodeMethodGenerator(MethodCode method) {
            super(method);
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

    private static final class GeneratorCodeMethodGenerator extends InstructionVisitor {
        GeneratorCodeMethodGenerator(MethodCode method) {
            super(method);
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

    private final CodeGenerator codegen;

    FunctionCodeGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    void generate(FunctionNode node) {
        MethodCode method = codegen.newMethod(node, FunctionName.Call);
        if (node.isGenerator()) {
            InstructionVisitor mv = new GeneratorCodeMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            if (node instanceof GeneratorComprehension) {
                generateGeneratorComprehension(node, mv);
            } else {
                generateGenerator(node, mv);
            }

            mv.end();
        } else {
            InstructionVisitor mv = new FunctionCodeMethodGenerator(method);
            mv.lineInfo(node.getBeginLine());
            mv.begin();

            if (isLegacy(node)) {
                generateLegacyFunction(node, mv);
            } else {
                generateFunction(node, mv);
            }

            mv.end();
        }
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * oldCaller = function.getLegacyCaller()
     * oldArguments = function.getLegacyArguments()
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * try {
     *   calleeContext = newFunctionExecutionContext(callerContext, function, thisValue)
     *   function_init(calleeContext, function, arguments)
     *   return function_code(calleeContext)
     * } finally {
     *   function.restoreLegacyProperties(oldCaller, oldArguments)
     * }
     * </pre>
     */
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

        // (2) Update 'caller' property
        setLegacyCaller(function, callerContext, mv);

        LocationLabel startFinally = new LocationLabel(), endFinally = new LocationLabel();
        LocationLabel handlerFinally = new LocationLabel();
        mv.mark(startFinally);
        {
            // (3) Create a new ExecutionContext
            newFunctionExecutionContext(calleeContext, callerContext, function, thisValue, mv);

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
        mv.finallyHandler(handlerFinally);
        mv.store(throwable);
        restoreLegacyProperties(function, oldCaller, oldArguments, mv);
        mv.load(throwable);
        mv.athrow();

        mv.tryFinally(startFinally, endFinally, handlerFinally);
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(callerContext, function, thisValue)
     * function_init(calleeContext, function, arguments)
     * return function_code(calleeContext)
     * </pre>
     */
    private void generateFunction(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryFunction> function = mv.getParameter(FUNCTION, OrdinaryFunction.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, callerContext, function, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, function, arguments, mv);

        // (3) Perform EvaluateBody
        evaluateBody(node, calleeContext, mv);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(callerContext, generator, thisValue)
     * function_init(calleeContext, generator, arguments)
     * return EvaluateBody(calleeContext, generator)
     * </pre>
     */
    private void generateGenerator(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, callerContext, generator, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform EvaluateBody
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBody);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    /**
     * Generate bytecode for:
     * 
     * <pre>
     * calleeContext = newFunctionExecutionContext(callerContext, generator, thisValue)
     * function_init(calleeContext, generator, arguments)
     * return EvaluateBodyComprehension(calleeContext, generator)
     * </pre>
     */
    private void generateGeneratorComprehension(FunctionNode node, InstructionVisitor mv) {
        Variable<OrdinaryGenerator> generator = mv.getParameter(GENERATOR, OrdinaryGenerator.class);
        Variable<ExecutionContext> callerContext = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);
        Variable<Object> thisValue = mv.getParameter(THIS_VALUE, Object.class);
        Variable<Object[]> arguments = mv.getParameter(ARGUMENTS, Object[].class);

        Variable<ExecutionContext> calleeContext = mv.newVariable("calleeContext",
                ExecutionContext.class);

        // (1) Create a new ExecutionContext
        newFunctionExecutionContext(calleeContext, callerContext, generator, thisValue, mv);

        // (2) Perform FunctionDeclarationInstantiation
        functionDeclarationInstantiation(node, calleeContext, generator, arguments, mv);

        // (3) Perform EvaluateBodyComprehension
        mv.load(calleeContext);
        mv.load(generator);
        mv.invoke(Methods.OrdinaryGenerator_EvaluateBodyComprehension);

        // (4) Return result value
        mv.areturn(Types.Object);
    }

    /**
     * <code>
     * calleeContext = newFunctionExecutionContext(callerContext, function, thisValue)
     * </code>
     */
    private void newFunctionExecutionContext(Variable<ExecutionContext> calleeContext,
            Variable<ExecutionContext> callerContext, Variable<? extends FunctionObject> function,
            Variable<Object> thisValue, InstructionVisitor mv) {
        mv.load(callerContext);
        mv.load(function);
        mv.load(thisValue);
        mv.invoke(Methods.ExecutionContext_newFunctionExecutionContext);
        mv.store(calleeContext);
    }

    /**
     * <code>
     * function_init(calleeContext, function, arguments)
     * </code>
     */
    private void functionDeclarationInstantiation(FunctionNode node,
            Variable<ExecutionContext> calleeContext, Variable<? extends FunctionObject> function,
            Variable<Object[]> arguments, InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.load(function);
        mv.load(arguments);
        mv.invoke(codegen.methodDesc(node, FunctionName.Init));
    }

    /**
     * <code>
     * function_code(calleeContext)
     * </code>
     */
    private void evaluateBody(FunctionNode node, Variable<ExecutionContext> calleeContext,
            InstructionVisitor mv) {
        mv.load(calleeContext);
        mv.invoke(codegen.methodDesc(node, FunctionName.Code));
    }

    /**
     * <code>
     * function.setLegacyCaller(callerContext.getCurrentFunction())
     * </code>
     */
    private void setLegacyCaller(Variable<? extends FunctionObject> function,
            Variable<ExecutionContext> callerContext, InstructionVisitor mv) {
        mv.load(function);
        mv.load(callerContext);
        mv.invoke(Methods.ExecutionContext_getCurrentFunction);
        mv.invoke(Methods.FunctionObject_setLegacyCaller);
    }

    /**
     * <code>
     * function.restoreLegacyProperties(oldCaller, oldArguments)
     * </code>
     */
    private void restoreLegacyProperties(Variable<? extends FunctionObject> function,
            Variable<Object> oldCaller, Variable<Object> oldArguments, InstructionVisitor mv) {
        mv.load(function);
        mv.load(oldCaller);
        mv.load(oldArguments);
        mv.invoke(Methods.FunctionObject_restoreLegacyProperties);
    }

    private boolean isLegacy(FunctionNode node) {
        return !IsStrict(node) && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
    }
}

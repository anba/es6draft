/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.ClassPropertyGenerator.ClassPropertyEvaluation;
import static com.github.anba.es6draft.compiler.GeneratorComprehensionGenerator.EvaluateGeneratorComprehension;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;
import static com.github.anba.es6draft.semantics.StaticSemantics.TemplateStrings;

import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SyntheticNode;
import com.github.anba.es6draft.compiler.CodeVisitor.GeneratorState;
import com.github.anba.es6draft.compiler.CodeVisitor.LabelState;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.compiler.completion.CompletionValueVisitor;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.JVMNames;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
final class CodeGenerator {
    private static final class Methods {
        // class: CompiledFunction
        static final MethodName CompiledFunction_Constructor = MethodName.findConstructor(Types.CompiledFunction,
                Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$Function));

        // class: CompiledModule
        static final MethodName CompiledModule_Constructor = MethodName.findConstructor(Types.CompiledModule,
                Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$ModuleBody));

        // class: CompiledScript
        static final MethodName CompiledScript_Constructor = MethodName.findConstructor(Types.CompiledScript,
                Type.methodType(Type.VOID_TYPE, Types.RuntimeInfo$ScriptBody));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_GetTemplateObject = MethodName.findStatic(Types.ScriptRuntime,
                "GetTemplateObject",
                Type.methodType(Types.ArrayObject, Type.INT_TYPE, Types.MethodHandle, Types.ExecutionContext));
    }

    private static final class MethodDescriptors {
        static final MethodTypeDescriptor FunctionConstructor = Type.methodType(Type.VOID_TYPE);
        static final MethodTypeDescriptor ModuleConstructor = Type.methodType(Type.VOID_TYPE);
        static final MethodTypeDescriptor ScriptConstructor = Type.methodType(Type.VOID_TYPE);

        static final MethodTypeDescriptor TemplateLiteral = Type.methodType(Types.String_);

        static final MethodTypeDescriptor DoExpressionMethod = Type.methodType(Type.INT_TYPE, Types.ExecutionContext,
                Types.Object_);
        static final MethodTypeDescriptor DoExpressionMethodWithResume = Type.methodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_);

        static final MethodTypeDescriptor StatementListMethod = Type.methodType(Type.INT_TYPE, Types.ExecutionContext,
                Types.Object_);
        static final MethodTypeDescriptor StatementListMethodWithResume = Type.methodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_);

        static final MethodTypeDescriptor SpreadElementMethod = Type.methodType(Type.INT_TYPE, Types.ExecutionContext,
                Types.ArrayObject, Type.INT_TYPE);
        static final MethodTypeDescriptor SpreadElementMethodWithResume = Type.methodType(Type.INT_TYPE,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_, Types.ArrayObject, Type.INT_TYPE);

        static final MethodTypeDescriptor PropertyDefinitionsMethod = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.OrdinaryObject, Types.ArrayList);
        static final MethodTypeDescriptor PropertyDefinitionsMethodWithResume = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_, Types.OrdinaryObject, Types.ArrayList);

        static final MethodTypeDescriptor MethodDefinitionsMethod = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.OrdinaryConstructorFunction, Types.OrdinaryObject, Types.ArrayList);
        static final MethodTypeDescriptor MethodDefinitionsMethodWithResume = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_, Types.OrdinaryConstructorFunction,
                Types.OrdinaryObject, Types.ArrayList);

        static final MethodTypeDescriptor ExpressionMethod = Type.methodType(Types.Object, Types.ExecutionContext);
        static final MethodTypeDescriptor ExpressionMethodWithResume = Type.methodType(Types.Object,
                Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_);

        static final MethodTypeDescriptor BlockDeclarationInit = Type.methodType(Type.VOID_TYPE,
                Types.LexicalEnvironment, Types.ExecutionContext);

        static final MethodTypeDescriptor AsyncFunction_Call = Type.methodType(Types.PromiseObject,
                Types.OrdinaryAsyncFunction, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor AsyncFunction_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.OrdinaryAsyncFunction, Types.Object_);
        static final MethodTypeDescriptor AsyncFunction_Code = Type.methodType(Types.Object, Types.ExecutionContext,
                Types.ResumptionPoint);

        static final MethodTypeDescriptor Function_Call = Type.methodType(Types.Object, Types.OrdinaryFunction,
                Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor Function_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.OrdinaryFunction, Types.Object_);
        static final MethodTypeDescriptor Function_Code = Type.methodType(Types.Object, Types.ExecutionContext);

        static final MethodTypeDescriptor ConstructorFunction_Call = Type.methodType(Types.Object,
                Types.OrdinaryConstructorFunction, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor ConstructorFunction_Construct = Type.methodType(Types.ScriptObject,
                Types.OrdinaryConstructorFunction, Types.ExecutionContext, Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor ConstructorFunction_ConstructTailCall = Type.methodType(Types.Object,
                Types.OrdinaryConstructorFunction, Types.ExecutionContext, Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor ConstructorFunction_Init = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.OrdinaryConstructorFunction, Types.Object_);
        static final MethodTypeDescriptor ConstructorFunction_Code = Function_Code;

        static final MethodTypeDescriptor LegacyFunction_Call = Type.methodType(Types.Object,
                Types.LegacyConstructorFunction, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor LegacyFunction_Construct = Type.methodType(Types.ScriptObject,
                Types.LegacyConstructorFunction, Types.ExecutionContext, Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor LegacyFunction_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.LegacyConstructorFunction, Types.Object_);
        static final MethodTypeDescriptor LegacyFunction_Code = Function_Code;

        static final MethodTypeDescriptor ConstructorGenerator_Call = Type.methodType(Types.GeneratorObject,
                Types.OrdinaryConstructorGenerator, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor ConstructorGenerator_Construct = Type.methodType(Types.GeneratorObject,
                Types.OrdinaryConstructorGenerator, Types.ExecutionContext, Types.Constructor, Types.Object_);
        static final MethodTypeDescriptor ConstructorGenerator_Init = Type.methodType(Type.VOID_TYPE,
                Types.ExecutionContext, Types.OrdinaryConstructorGenerator, Types.Object_);
        static final MethodTypeDescriptor ConstructorGenerator_Code = Type.methodType(Types.Object,
                Types.ExecutionContext, Types.ResumptionPoint);

        static final MethodTypeDescriptor Generator_Call = Type.methodType(Types.GeneratorObject,
                Types.OrdinaryGenerator, Types.ExecutionContext, Types.Object, Types.Object_);
        static final MethodTypeDescriptor Generator_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.OrdinaryGenerator, Types.Object_);
        static final MethodTypeDescriptor Generator_Code = Type.methodType(Types.Object, Types.ExecutionContext,
                Types.ResumptionPoint);

        static final MethodTypeDescriptor FunctionNode_RTI = Type.methodType(Types.RuntimeInfo$Function);
        static final MethodTypeDescriptor FunctionNode_DebugInfo = Type.methodType(Types.DebugInfo);

        static final MethodTypeDescriptor Script_Eval = Type.methodType(Types.Object, Types.ExecutionContext,
                Types.Script);
        static final MethodTypeDescriptor Script_Code = Type.methodType(Types.Object, Types.ExecutionContext);
        static final MethodTypeDescriptor Script_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext);
        static final MethodTypeDescriptor Script_RTI = Type.methodType(Types.RuntimeInfo$ScriptBody);
        static final MethodTypeDescriptor Script_DebugInfo = Type.methodType(Types.DebugInfo);

        static final MethodTypeDescriptor Module_Code = Type.methodType(Types.Object, Types.ExecutionContext);
        static final MethodTypeDescriptor Module_Init = Type.methodType(Type.VOID_TYPE, Types.ExecutionContext,
                Types.SourceTextModuleRecord, Types.LexicalEnvironment);
        static final MethodTypeDescriptor Module_RTI = Type.methodType(Types.RuntimeInfo$ScriptBody);
        static final MethodTypeDescriptor Module_DebugInfo = Type.methodType(Types.DebugInfo);
    }

    private static final boolean INCLUDE_SOURCE = true;
    private static final Future<String> NO_SOURCE = CompletableFuture.completedFuture(null);
    private static final int MAX_FNAME_LENGTH = 0x400;

    private final Code code;
    private final Program program;
    private final ExecutorService executor;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;

    private final StatementGenerator stmtgen = new StatementGenerator(this);
    private final ExpressionGenerator exprgen = new ExpressionGenerator(this);
    private final PropertyGenerator propgen = new PropertyGenerator(this);
    private final BlockDeclarationInstantiationGenerator blockgen = new BlockDeclarationInstantiationGenerator(this);

    CodeGenerator(Code code, Program program, ExecutorService executor, EnumSet<Compiler.Option> compilerOptions) {
        this.code = code;
        this.program = program;
        this.executor = executor;
        this.options = program.getOptions();
        this.parserOptions = program.getParserOptions();
        this.compilerOptions = compilerOptions;
    }

    Program getProgram() {
        return program;
    }

    boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    boolean isEnabled(Parser.Option option) {
        return parserOptions.contains(option);
    }

    boolean isEnabled(Compiler.Option option) {
        return compilerOptions.contains(option);
    }

    // template strings
    private final HashMap<TemplateLiteral, Integer> templateKeys = new HashMap<>();

    private int templateKey(TemplateLiteral template) {
        Integer key = templateKeys.get(template);
        if (key == null) {
            templateKeys.put(template, key = templateKeys.size());
        }
        return key;
    }

    private final HashMap<DoExpression, LabelState> doExpressionCompletions = new HashMap<>();
    private final HashMap<StatementListMethod, LabelState> statementCompletions = new HashMap<>();

    /* ----------------------------------------------------------------------------------------- */

    enum ScriptName {
        Eval, Code, Init, RTI, DebugInfo
    }

    enum ModuleName {
        Code, Init, RTI, DebugInfo
    }

    enum FunctionName {
        Call, Construct, ConstructTailCall, Code, Init, RTI, DebugInfo
    }

    /**
     * Map of nodes to base method names
     */
    private final HashMap<Node, String> methodNames = new HashMap<>(32);
    private final AtomicInteger methodCounter = new AtomicInteger(0);

    private boolean isCompiled(Node node) {
        return methodNames.containsKey(node);
    }

    private String methodName(Script node, ScriptName name) {
        switch (name) {
        case Eval:
            return "!script";
        case Code:
            return "~script_code";
        case Init:
            return "~script_init";
        case RTI:
            return "!script_rti";
        case DebugInfo:
            return "!script_dbg";
        default:
            throw new AssertionError();
        }
    }

    private String methodName(Module node, ModuleName name) {
        switch (name) {
        case Code:
            return "~module";
        case Init:
            return "~module_init";
        case RTI:
            return "!module_rti";
        case DebugInfo:
            return "!module_dbg";
        default:
            throw new AssertionError();
        }
    }

    private String methodName(DoExpression node) {
        String name = methodNames.get(node);
        if (name == null) {
            throw new IllegalStateException("no method-name present for: " + node);
        }
        return name;
    }

    private String methodName(StatementListMethod node) {
        String name = methodNames.get(node);
        if (name == null) {
            throw new IllegalStateException("no method-name present for: " + node);
        }
        return name;
    }

    private String baseName(TopLevelNode<?> topLevel) {
        if (topLevel instanceof FunctionNode) {
            return methodName((FunctionNode) topLevel, FunctionName.Code);
        }
        if (topLevel instanceof Module) {
            return methodName((Module) topLevel, ModuleName.Code);
        }
        assert topLevel instanceof Script;
        return methodName((Script) topLevel, ScriptName.Code);
    }

    private String methodName(TopLevelNode<?> topLevel, DoExpression node) {
        return addMethodName(node, baseName(topLevel), '\'');
    }

    private String methodName(TopLevelNode<?> topLevel, StatementListMethod node) {
        return addMethodName(node, baseName(topLevel), '\'');
    }

    private String methodName(TemplateLiteral node) {
        return methodName(node, "!template");
    }

    private String methodName(SpreadElementMethod node) {
        return methodName(node, "!spread");
    }

    private String methodName(PropertyDefinitionsMethod node) {
        return methodName(node, "!propdef");
    }

    private String methodName(MethodDefinitionsMethod node) {
        return methodName(node, "!mdef");
    }

    private String methodName(ExpressionMethod node) {
        return methodName(node, "!expr");
    }

    private String methodName(BlockStatement node) {
        return methodName(node, "!block");
    }

    private String methodName(SwitchStatement node) {
        return methodName(node, "!block");
    }

    private String methodName(Node node, String name) {
        String n = methodNames.get(node);
        if (n == null) {
            n = addMethodName(node, name, '~');
        }
        return n;
    }

    private String methodName(FunctionNode node, FunctionName name) {
        String fname = methodNames.get(node);
        if (fname == null) {
            fname = addMethodName(node, getMethodName(node), '~');
        }
        switch (name) {
        case Call:
            return insertMarker("!", fname, "_call");
        case Code:
            return insertMarker("", fname, "");
        case Construct:
        case ConstructTailCall:
            return insertMarker("!", fname, "_construct");
        case Init:
            return insertMarker("", fname, "_init");
        case RTI:
            return insertMarker("!", fname, "_rti");
        case DebugInfo:
            return insertMarker("!", fname, "_dbg");
        default:
            throw new AssertionError();
        }
    }

    private String getMethodName(FunctionNode node) {
        String name = node.getMethodName();
        if (name.isEmpty()) {
            name = "anonymous";
        } else if (name.length() > MAX_FNAME_LENGTH) {
            name = name.substring(0, MAX_FNAME_LENGTH);
        }
        return name;
    }

    private String insertMarker(String prefix, String fname, String suffix) {
        return JVMNames.addPrefixSuffix(fname, prefix, suffix);
    }

    private String addMethodName(Node node, String name, char sep) {
        assert !methodNames.containsKey(node);
        String n = JVMNames.toBytecodeName(name + sep + methodCounter.incrementAndGet());
        methodNames.put(node, n);
        return n;
    }

    private String addMethodNameUnchecked(String name, char sep) {
        return JVMNames.toBytecodeName(name + sep + methodCounter.incrementAndGet());
    }

    /* ----------------------------------------------------------------------------------------- */

    private MethodTypeDescriptor methodDescriptor(TemplateLiteral node) {
        return MethodDescriptors.TemplateLiteral;
    }

    private MethodTypeDescriptor methodDescriptor(DoExpression node) {
        if (node.hasYieldOrAwait()) {
            return MethodDescriptors.DoExpressionMethodWithResume;
        }
        return MethodDescriptors.DoExpressionMethod;
    }

    private MethodTypeDescriptor methodDescriptor(StatementListMethod node) {
        if (node.hasResumePoint()) {
            return MethodDescriptors.StatementListMethodWithResume;
        }
        return MethodDescriptors.StatementListMethod;
    }

    private MethodTypeDescriptor methodDescriptor(SpreadElementMethod node) {
        if (node.hasResumePoint()) {
            return MethodDescriptors.SpreadElementMethodWithResume;
        }
        return MethodDescriptors.SpreadElementMethod;
    }

    private MethodTypeDescriptor methodDescriptor(PropertyDefinitionsMethod node) {
        if (node.hasResumePoint()) {
            return MethodDescriptors.PropertyDefinitionsMethodWithResume;
        }
        return MethodDescriptors.PropertyDefinitionsMethod;
    }

    private MethodTypeDescriptor methodDescriptor(MethodDefinitionsMethod node) {
        if (node.hasResumePoint()) {
            return MethodDescriptors.MethodDefinitionsMethodWithResume;
        }
        return MethodDescriptors.MethodDefinitionsMethod;
    }

    private MethodTypeDescriptor methodDescriptor(ExpressionMethod node) {
        if (node.hasResumePoint()) {
            return MethodDescriptors.ExpressionMethodWithResume;
        }
        return MethodDescriptors.ExpressionMethod;
    }

    private MethodTypeDescriptor methodDescriptor(BlockStatement node) {
        return MethodDescriptors.BlockDeclarationInit;
    }

    private MethodTypeDescriptor methodDescriptor(SwitchStatement node) {
        return MethodDescriptors.BlockDeclarationInit;
    }

    private MethodTypeDescriptor methodDescriptor(FunctionNode node, FunctionName name) {
        switch (name) {
        case Call:
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Call;
            }
            if (node.isGenerator()) {
                if (node.isConstructor()) {
                    return MethodDescriptors.ConstructorGenerator_Call;
                }
                return MethodDescriptors.Generator_Call;
            }
            if (isLegacy(node)) {
                return MethodDescriptors.LegacyFunction_Call;
            }
            if (node.isConstructor()) {
                return MethodDescriptors.ConstructorFunction_Call;
            }
            assert !isCallConstructor(node);
            return MethodDescriptors.Function_Call;
        case ConstructTailCall:
            assert node.isConstructor() && !isLegacy(node) && !node.isGenerator() && !node.isAsync();
            return MethodDescriptors.ConstructorFunction_ConstructTailCall;
        case Construct:
            assert node.isConstructor();
            if (node.isGenerator()) {
                return MethodDescriptors.ConstructorGenerator_Construct;
            }
            if (isLegacy(node)) {
                return MethodDescriptors.LegacyFunction_Construct;
            }
            return MethodDescriptors.ConstructorFunction_Construct;
        case Code:
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Code;
            }
            if (node.isGenerator()) {
                if (node.isConstructor()) {
                    return MethodDescriptors.ConstructorGenerator_Code;
                }
                return MethodDescriptors.Generator_Code;
            }
            if (isLegacy(node)) {
                return MethodDescriptors.LegacyFunction_Code;
            }
            if (node.isConstructor() || isCallConstructor(node)) {
                return MethodDescriptors.ConstructorFunction_Code;
            }
            return MethodDescriptors.Function_Code;
        case Init:
            if (node.isAsync()) {
                return MethodDescriptors.AsyncFunction_Init;
            }
            if (node.isGenerator()) {
                if (node.isConstructor()) {
                    return MethodDescriptors.ConstructorGenerator_Init;
                }
                return MethodDescriptors.Generator_Init;
            }
            if (isLegacy(node)) {
                return MethodDescriptors.LegacyFunction_Init;
            }
            if (node.isConstructor() || isCallConstructor(node)) {
                return MethodDescriptors.ConstructorFunction_Init;
            }
            return MethodDescriptors.Function_Init;
        case RTI:
            return MethodDescriptors.FunctionNode_RTI;
        case DebugInfo:
            return MethodDescriptors.FunctionNode_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    private boolean isLegacy(FunctionNode node) {
        if (IsStrict(node)) {
            return false;
        }
        if (!(node instanceof FunctionDeclaration || node instanceof FunctionExpression)) {
            return false;
        }
        return isEnabled(CompatibilityOption.FunctionArguments) || isEnabled(CompatibilityOption.FunctionCaller);
    }

    private boolean isCallConstructor(FunctionNode node) {
        if (node instanceof MethodDefinition) {
            return ((MethodDefinition) node).isCallConstructor();
        }
        return false;
    }

    private MethodTypeDescriptor methodDescriptor(Script node, ScriptName name) {
        switch (name) {
        case Eval:
            return MethodDescriptors.Script_Eval;
        case Code:
            return MethodDescriptors.Script_Code;
        case Init:
            return MethodDescriptors.Script_Init;
        case RTI:
            return MethodDescriptors.Script_RTI;
        case DebugInfo:
            return MethodDescriptors.Script_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    private MethodTypeDescriptor methodDescriptor(Module node, ModuleName name) {
        switch (name) {
        case Code:
            return MethodDescriptors.Module_Code;
        case Init:
            return MethodDescriptors.Module_Init;
        case RTI:
            return MethodDescriptors.Module_RTI;
        case DebugInfo:
            return MethodDescriptors.Module_DebugInfo;
        default:
            throw new AssertionError();
        }
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * Map of concrete method names to class names
     */
    private final HashMap<String, Type> methodClasses = new HashMap<>(32 * 4);

    private MethodCode publicStaticMethod(String methodName, MethodTypeDescriptor methodDescriptor) {
        final int access = Modifier.PUBLIC | Modifier.STATIC;
        MethodCode method = code.newMethod(access, methodName, methodDescriptor);
        // System.out.printf("add <%s, %s>%n", methodName, method.classCode.className);
        assert !methodClasses.containsKey(methodName) : String.format("method '%s' already compiled", methodName);
        methodClasses.put(methodName, method.classCode.classType);
        return method;
    }

    private MethodCode newMethod(TemplateLiteral node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(TopLevelNode<?> topLevel, DoExpression node) {
        return publicStaticMethod(methodName(topLevel, node), methodDescriptor(node));
    }

    private MethodCode newMethod(TopLevelNode<?> topLevel, StatementListMethod node) {
        return publicStaticMethod(methodName(topLevel, node), methodDescriptor(node));
    }

    private MethodCode newMethod(SpreadElementMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(PropertyDefinitionsMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(MethodDefinitionsMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(ExpressionMethod node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod(BlockStatement node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod2(BlockStatement node) {
        return publicStaticMethod(addMethodNameUnchecked(methodName(node), '\''), methodDescriptor(node));
    }

    private MethodCode newMethod(SwitchStatement node) {
        return publicStaticMethod(methodName(node), methodDescriptor(node));
    }

    private MethodCode newMethod2(SwitchStatement node) {
        return publicStaticMethod(addMethodNameUnchecked(methodName(node), '\''), methodDescriptor(node));
    }

    MethodCode newMethod(FunctionNode node, FunctionName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    MethodCode newMethod(Script node, ScriptName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    MethodCode newMethod(Module node, ModuleName name) {
        return publicStaticMethod(methodName(node, name), methodDescriptor(node, name));
    }

    /* ----------------------------------------------------------------------------------------- */

    private Type owner(String methodName) {
        Type owner = methodClasses.get(methodName);
        assert owner != null : String.format("method '%s' not yet compiled", methodName);
        return owner;
    }

    private MethodName methodDesc(TemplateLiteral node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(DoExpression node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(StatementListMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(SpreadElementMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(PropertyDefinitionsMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(MethodDefinitionsMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(ExpressionMethod node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(BlockStatement node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(BlockStatement node, String methodName) {
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(SwitchStatement node) {
        String methodName = methodName(node);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    private MethodName methodDesc(SwitchStatement node, String methodName) {
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node));
    }

    MethodName methodDesc(FunctionNode node, FunctionName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    MethodName methodDesc(Script node, ScriptName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    MethodName methodDesc(Module node, ModuleName name) {
        String methodName = methodName(node, name);
        return MethodName.findStatic(owner(methodName), methodName, methodDescriptor(node, name));
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * [12.2.8.2.2] Runtime Semantics: GetTemplateObject
     * 
     * @param node
     *            the template literal
     * @param mv
     *            the code visitor
     */
    void GetTemplateObject(TemplateLiteral node, CodeVisitor mv) {
        assert isCompiled(node);

        // GetTemplateObject
        mv.iconst(templateKey(node));
        mv.handle(methodDesc(node));
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetTemplateObject);
    }

    void compile(TemplateLiteral node) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            InstructionVisitor body = new InstructionVisitor(method);
            body.lineInfo(node);
            body.begin();

            List<TemplateCharacters> strings = TemplateStrings(node);
            body.anewarray(strings.size() * 2, Types.String);
            for (int i = 0, size = strings.size(); i < size; ++i) {
                TemplateCharacters e = strings.get(i);
                int index = i << 1;
                body.astore(index, e.getValue());
                body.astore(index + 1, e.getRawValue());
            }

            body._return();
            body.end();
        }
    }

    void compile(Script node) {
        // initialization methods
        if (!(node.isEvalScript() || node.isScripting())) {
            new GlobalDeclarationInstantiationGenerator(this).generate(node);
        } else {
            new EvalDeclarationInstantiationGenerator(this).generate(node);
        }

        // runtime method
        scriptBody(node);

        // eval method
        new ScriptCodeGenerator(this).generate(node);

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);

        // add default constructor
        defaultScriptConstructor(node);
    }

    private void scriptBody(Script node) {
        MethodCode method = newMethod(node, ScriptName.Code);
        ScriptCodeVisitor body = new ScriptCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        body.loadUndefined();
        body.storeCompletionValue(ValType.Undefined);

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            body.loadCompletionValue();
            body._return();
        }
        body.end();
    }

    private void defaultScriptConstructor(Script node) {
        InstructionVisitor mv = new InstructionVisitor(
                code.newConstructor(Modifier.PUBLIC, MethodDescriptors.ScriptConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(methodDesc(node, ScriptName.RTI));
        mv.invoke(Methods.CompiledScript_Constructor);
        mv._return();
        mv.end();
    }

    void compile(Module node, SourceTextModuleRecord moduleRecord) {
        // initialization methods
        new ModuleDeclarationInstantiationGenerator(this).generate(node, moduleRecord);

        // runtime method
        moduleBody(node);

        // runtime-info method
        new RuntimeInfoGenerator(this).runtimeInfo(node);

        // add default constructor
        defaultModuleConstructor(node);
    }

    private void moduleBody(Module node) {
        MethodCode method = newMethod(node, ModuleName.Code);
        ModuleCodeVisitor body = new ModuleCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        body.enterScope(node);
        Completion result = statements(node.getStatements(), body);
        body.exitScope();

        if (!result.isAbrupt()) {
            // Completion values are currently ignored for module code.
            body.loadUndefined();
            body._return();
        }
        body.end();
    }

    private void defaultModuleConstructor(Module node) {
        InstructionVisitor mv = new InstructionVisitor(
                code.newConstructor(Modifier.PUBLIC, MethodDescriptors.ModuleConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(methodDesc(node, ModuleName.RTI));
        mv.invoke(Methods.CompiledModule_Constructor);
        mv._return();
        mv.end();
    }

    void compileFunction(FunctionNode function) {
        MethodName method;
        if (function instanceof FunctionDefinition) {
            method = compile((FunctionDefinition) function);
        } else if (function instanceof GeneratorDefinition) {
            method = compile((GeneratorDefinition) function);
        } else {
            assert function instanceof AsyncFunctionDefinition;
            method = compile((AsyncFunctionDefinition) function);
        }

        // add default constructor
        defaultFunctionConstructor(function, method);
    }

    private void defaultFunctionConstructor(FunctionNode function, MethodName method) {
        InstructionVisitor mv = new InstructionVisitor(
                code.newConstructor(Modifier.PUBLIC, MethodDescriptors.FunctionConstructor));
        mv.begin();
        mv.loadThis();
        mv.invoke(method);
        mv.invoke(Methods.CompiledFunction_Constructor);
        mv._return();
        mv.end();
    }

    MethodName compile(ClassDefinition node) {
        MethodDefinition constructor = node.getConstructor();
        MethodDefinition callConstructor = node.getCallConstructor();
        if (!isCompiled(constructor)) {
            assert callConstructor == null || !isCompiled(callConstructor);

            Future<String> source = getSource(node);

            // initialization method
            new FunctionDeclarationInstantiationGenerator(this).generate(constructor);
            if (callConstructor != null) {
                new FunctionDeclarationInstantiationGenerator(this).generate(callConstructor);
            }

            // runtime method
            boolean tailConstruct = functionBody(constructor);
            boolean tailCall = callConstructor != null ? functionBody(callConstructor) : false;

            // call method
            new FunctionCodeGenerator(this).generate(node, tailCall, tailConstruct);

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, tailCall, tailConstruct, result(source));
        }
        return methodDesc(constructor, FunctionName.RTI);
    }

    MethodName compile(GeneratorComprehension node) {
        return compile((FunctionNode) node);
    }

    MethodName compile(FunctionDefinition node) {
        return compile((FunctionNode) node);
    }

    MethodName compile(GeneratorDefinition node) {
        return compile((FunctionNode) node);
    }

    MethodName compile(ArrowFunction node) {
        return compile((FunctionNode) node);
    }

    MethodName compile(MethodDefinition node) {
        assert !(node.isClassConstructor() || node.isCallConstructor());
        return compile((FunctionNode) node);
    }

    MethodName compile(AsyncArrowFunction node) {
        return compile((FunctionNode) node);
    }

    MethodName compile(AsyncFunctionDefinition node) {
        return compile((FunctionNode) node);
    }

    private MethodName compile(FunctionNode node) {
        if (!isCompiled(node)) {
            Future<String> source = getSource(node);

            // initialization method
            new FunctionDeclarationInstantiationGenerator(this).generate(node);

            // runtime method
            boolean tailCall;
            if (node instanceof ArrowFunction && ((ArrowFunction) node).getExpression() != null) {
                tailCall = conciseFunctionBody((ArrowFunction) node);
            } else if (node instanceof AsyncArrowFunction && ((AsyncArrowFunction) node).getExpression() != null) {
                tailCall = conciseAsyncFunctionBody((AsyncArrowFunction) node);
            } else if (node instanceof GeneratorComprehension) {
                tailCall = generatorComprehensionBody((GeneratorComprehension) node);
            } else if (node.isGenerator()) {
                tailCall = generatorBody(node);
            } else if (node.isAsync()) {
                tailCall = asyncFunctionBody(node);
            } else {
                tailCall = functionBody(node);
            }

            // call method
            new FunctionCodeGenerator(this).generate(node, tailCall);

            // runtime-info method
            new RuntimeInfoGenerator(this).runtimeInfo(node, tailCall, result(source));
        }
        return methodDesc(node, FunctionName.RTI);
    }

    private Future<String> getSource(ClassDefinition node) {
        if (INCLUDE_SOURCE && !isEnabled(Parser.Option.NativeFunction)) {
            StringBuilder sb = new StringBuilder();
            appendMethodSource(sb, "constructor", node.getConstructor());
            if (node.getCallConstructor() != null) {
                sb.append('\n');
                appendMethodSource(sb, "call constructor", node.getCallConstructor());
            }
            return executor.submit(new CompressSourceTask(sb.toString()));
        }
        return NO_SOURCE;
    }

    private static void appendMethodSource(StringBuilder sb, String methodName, MethodDefinition method) {
        // Ignores implicit-strict compatibility option.
        sb.append(methodName).append(method.getHeaderSource()).append('{').append(method.getBodySource()).append('}');
    }

    private Future<String> getSource(FunctionNode node) {
        if (INCLUDE_SOURCE && !isEnabled(Parser.Option.NativeFunction)) {
            String source = Strings.concat(node.getHeaderSource(), node.getBodySource());
            return executor.submit(new CompressSourceTask(source));
        }
        return NO_SOURCE;
    }

    private static <T> T result(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CompressSourceTask implements Callable<String> {
        private final String source;

        CompressSourceTask(String source) {
            this.source = source;
        }

        @Override
        public String call() throws Exception {
            return SourceCompressor.compress(source);
        }
    }

    private boolean conciseFunctionBody(ArrowFunction node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        FunctionCodeVisitor body = new FunctionCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        // call expression in concise function body is always in tail-call position
        Expression expression = node.getExpression();
        body.enterTailCallPosition(expression);

        body.enterFunction(node);
        expressionBoxed(expression, body);
        body.exitFunction();

        body._return();
        body.end();

        return body.hasTailCalls();
    }

    private boolean functionBody(FunctionNode node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        FunctionCodeVisitor body = new FunctionCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();

        body.enterFunction(node);
        Completion result = statements(node.getStatements(), body);
        body.exitFunction();

        if (!result.isAbrupt()) {
            // fall-thru, return undefined from function
            body.loadUndefined();
            body._return();
        }
        body.end();

        return body.hasTailCalls();
    }

    private boolean conciseAsyncFunctionBody(AsyncArrowFunction node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        GeneratorCodeVisitor body = new GeneratorCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        GeneratorState generatorState = body.generatorPrologue();

        body.enterFunction(node);
        expressionBoxed(node.getExpression(), body);
        body.exitFunction();

        body._return();
        body.generatorEpilogue(generatorState);
        body.end();

        return body.hasTailCalls();
    }

    private boolean asyncFunctionBody(FunctionNode node) {
        // Create the same function body as for generator functions
        return generatorBody(node);
    }

    private boolean generatorBody(FunctionNode node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        GeneratorCodeVisitor body = new GeneratorCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        GeneratorState generatorState = body.generatorPrologue();

        body.enterFunction(node);
        Completion result = statements(node.getStatements(), body);
        body.exitFunction();

        if (!result.isAbrupt()) {
            // fall-thru, return undefined from function
            body.loadUndefined();
            body._return();
        }
        body.generatorEpilogue(generatorState);
        body.end();

        return body.hasTailCalls();
    }

    private boolean generatorComprehensionBody(GeneratorComprehension node) {
        MethodCode method = newMethod(node, FunctionName.Code);
        GeneratorCodeVisitor body = new GeneratorCodeVisitor(method, node);
        body.lineInfo(node);
        body.begin();
        GeneratorState generatorState = body.generatorPrologue();

        body.enterFunction(node);
        EvaluateGeneratorComprehension(this, node, body);
        body.exitFunction();

        body.loadUndefined();
        body._return();
        body.generatorEpilogue(generatorState);
        body.end();

        return body.hasTailCalls();
    }

    Entry<MethodName, LabelState> compile(DoExpression node, CodeVisitor mv) {
        if (!isCompiled(node)) {
            if (!isEnabled(Compiler.Option.NoCompletion)) {
                CompletionValueVisitor.performCompletion(node);
            }
            MethodCode method = newMethod(mv.getTopLevelNode(), node);
            DoExpressionCodeVisitor body = new DoExpressionCodeVisitor(node, method, mv);
            body.lineInfo(node);
            body.nop(); // force line-number entry
            body.begin();
            GeneratorState generatorState = null;
            if (node.hasYieldOrAwait()) {
                generatorState = body.generatorPrologue();
            }
            body.labelPrologue();

            Completion result = statement(node.getStatement(), body);
            if (!result.isAbrupt()) {
                // fall-thru, return `0`.
                body.iconst(0);
                body._return();
            }
            LabelState labelState = body.labelEpilogue(result);
            if (generatorState != null) {
                body.generatorEpilogue(generatorState);
            }
            body.end();

            doExpressionCompletions.put(node, labelState);
        }
        return new SimpleImmutableEntry<>(methodDesc(node), doExpressionCompletions.get(node));
    }

    Entry<MethodName, LabelState> compile(StatementListMethod node, CodeVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(mv.getTopLevelNode(), node);
            StatementListMethodCodeVisitor body = new StatementListMethodCodeVisitor(node, method, mv);
            body.lineInfo(node);
            body.nop(); // force line-number entry
            body.begin();
            GeneratorState generatorState = null;
            if (node.hasResumePoint()) {
                generatorState = body.generatorPrologue();
            }
            body.labelPrologue();

            Completion result = statements(node.getStatements(), body);
            if (!result.isAbrupt()) {
                // fall-thru, return `0`.
                body.iconst(0);
                body._return();
            }
            LabelState labelState = body.labelEpilogue(result);
            if (generatorState != null) {
                body.generatorEpilogue(generatorState);
            }
            body.end();

            statementCompletions.put(node, labelState);
        }
        return new SimpleImmutableEntry<>(methodDesc(node), statementCompletions.get(node));
    }

    MethodName compile(SpreadElementMethod node, CodeVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            SpreadElementCodeVisitor body = new SpreadElementCodeVisitor(node, method, mv);
            body.lineInfo(node);
            body.begin();

            body.loadArrayObject();
            body.loadArrayIndex();
            expression(node.getExpression(), body);

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(PropertyDefinitionsMethod node, boolean hasDecorators, CodeVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            PropertyDefinitionsCodeVisitor body = new PropertyDefinitionsCodeVisitor(node, method, mv);
            body.lineInfo(node);
            body.begin();

            Variable<OrdinaryObject> object = body.getObjectParameter();
            Variable<ArrayList<Object>> decorators = hasDecorators ? body.getDecoratorsParameter() : null;
            PropertyGenerator propgen = propertyGenerator(decorators);
            for (PropertyDefinition property : node.getProperties()) {
                body.load(object);
                property.accept(propgen, body);
            }

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(MethodDefinitionsMethod node, boolean hasDecorators, CodeVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            MethodDefinitionsCodeVisitor body = new MethodDefinitionsCodeVisitor(node, method, mv);
            body.lineInfo(node);
            body.begin();

            Variable<OrdinaryConstructorFunction> function = body.getFunctionParameter();
            Variable<OrdinaryObject> proto = body.getPrototypeParameter();
            Variable<ArrayList<Object>> decorators = hasDecorators ? body.getDecoratorsParameter() : null;
            ClassPropertyEvaluation(this, node.getProperties(), function, proto, decorators, body);

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(ExpressionMethod node, CodeVisitor mv) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            ExpressionMethodVisitor body = new ExpressionMethodVisitor(node, method, mv);
            body.lineInfo(node);
            body.begin();

            expressionBoxed(node.getExpression(), body);

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(BlockStatement node, BlockDeclarationInstantiationGenerator generator) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            BlockDeclInitVisitor body = new BlockDeclInitVisitor(method);
            body.lineInfo(node);
            body.begin();

            Variable<ExecutionContext> cx = body.getExecutionContext();
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = body.getLexicalEnvironment();
            generator.generateMethod(node, cx, env, body);

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(BlockStatement node, List<Declaration> declarations,
            BlockDeclarationInstantiationGenerator generator) {
        MethodCode method = newMethod2(node);
        BlockDeclInitVisitor body = new BlockDeclInitVisitor(method);
        body.lineInfo(node);
        body.begin();

        Variable<ExecutionContext> cx = body.getExecutionContext();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = body.getLexicalEnvironment();
        generator.generateMethod(declarations, cx, env, body);

        body._return();
        body.end();

        return methodDesc(node, method.methodName);
    }

    MethodName compile(SwitchStatement node, BlockDeclarationInstantiationGenerator generator) {
        if (!isCompiled(node)) {
            MethodCode method = newMethod(node);
            BlockDeclInitVisitor body = new BlockDeclInitVisitor(method);
            body.lineInfo(node);
            body.begin();

            Variable<ExecutionContext> cx = body.getExecutionContext();
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = body.getLexicalEnvironment();
            generator.generateMethod(node, cx, env, body);

            body._return();
            body.end();
        }
        return methodDesc(node);
    }

    MethodName compile(SwitchStatement node, List<Declaration> declarations,
            BlockDeclarationInstantiationGenerator generator) {
        MethodCode method = newMethod2(node);
        BlockDeclInitVisitor body = new BlockDeclInitVisitor(method);
        body.lineInfo(node);
        body.begin();

        Variable<ExecutionContext> cx = body.getExecutionContext();
        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = body.getLexicalEnvironment();
        generator.generateMethod(declarations, cx, env, body);

        body._return();
        body.end();

        return methodDesc(node, method.methodName);
    }

    /* ----------------------------------------------------------------------------------------- */

    ValType expression(Expression node, CodeVisitor mv) {
        return node.accept(exprgen, mv);
    }

    ValType expressionBoxed(Expression node, CodeVisitor mv) {
        ValType type = node.accept(exprgen, mv);
        if (type.isJavaPrimitive()) {
            mv.toBoxed(type);
            return ValType.Any;
        }
        return type;
    }

    PropertyGenerator propertyGenerator(Variable<ArrayList<Object>> decorators) {
        if (decorators != null) {
            return new PropertyGenerator(this, decorators);
        }
        return propgen;
    }

    void propertyDefinition(PropertyDefinition node, CodeVisitor mv) {
        node.accept(propgen, mv);
    }

    void blockInit(BlockStatement node, CodeVisitor mv) {
        blockgen.generate(node, mv);
    }

    void blockInit(SwitchStatement node, CodeVisitor mv) {
        blockgen.generate(node, mv);
    }

    Completion statement(ModuleItem node, CodeVisitor mv) {
        return node.accept(stmtgen, mv);
    }

    Completion statements(List<? extends ModuleItem> statements, CodeVisitor mv) {
        // 13.1.13 Runtime Semantics: Evaluation<br>
        // StatementList : StatementList StatementListItem
        /* steps 1-6 */
        Completion result = Completion.Empty;
        for (ModuleItem item : statements) {
            if ((result = result.then(statement(item, mv))).isAbrupt()) {
                break;
            }
        }
        return result;
    }

    /* ----------------------------------------------------------------------------------------- */

    private static final class ScriptCodeVisitor extends CodeVisitor {
        ScriptCodeVisitor(MethodCode method, Script node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }

        @Override
        protected Variable<Object> createCompletionVariable() {
            return newVariable("completion", Object.class);
        }

        @Override
        protected boolean hasCompletionValue() {
            return true;
        }
    }

    private static final class ModuleCodeVisitor extends CodeVisitor {
        ModuleCodeVisitor(MethodCode method, Module node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class FunctionCodeVisitor extends CodeVisitor {
        FunctionCodeVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
        }
    }

    private static final class GeneratorCodeVisitor extends CodeVisitor {
        GeneratorCodeVisitor(MethodCode method, FunctionNode node) {
            super(method, node);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            setParameterName("rp", 1, Types.ResumptionPoint);
        }
    }

    private static final class DoExpressionCodeVisitor extends CodeVisitor {
        private final boolean withResume;

        DoExpressionCodeVisitor(DoExpression node, MethodCode method, CodeVisitor parent) {
            super(method, parent);
            this.withResume = node.hasYieldOrAwait();
        }

        private int parameter(int index) {
            assert index > 0;
            return withResume ? index + 1 : index;
        }

        private <T> MutableValue<T> arrayElementFromParameter(int index, Class<T[]> arrayType) {
            @SuppressWarnings("unchecked")
            Class<T> componentType = (Class<T>) arrayType.getComponentType();
            return arrayElement(getParameter(index, arrayType), 0, componentType);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            if (withResume) {
                setParameterName("rp", 1, Types.ResumptionPoint_);
            }
            setParameterName("completion", parameter(1), Types.Object_);
        }

        @Override
        protected MutableValue<Object> createCompletionVariable() {
            return arrayElementFromParameter(parameter(1), Object[].class);
        }

        @Override
        protected boolean hasCompletionValue() {
            return true;
        }

        @Override
        protected MutableValue<ResumptionPoint> resumptionPoint() {
            assert withResume;
            return arrayElementFromParameter(1, ResumptionPoint[].class);
        }

        @Override
        protected void returnForSuspend() {
            store(resumptionPoint());
            iconst(-1);
            _return();
        }
    }

    private static abstract class OutlinedMethodCodeVisitor extends CodeVisitor {
        private final boolean withResume;

        protected OutlinedMethodCodeVisitor(SyntheticNode node, MethodCode method, CodeVisitor parent) {
            super(method, parent);
            this.withResume = node.hasResumePoint();
        }

        protected final boolean hasResume() {
            return withResume;
        }

        protected final int parameter(int index) {
            assert index > 0;
            return withResume ? index + 2 : index;
        }

        protected final <T> MutableValue<T> arrayElementFromParameter(int index, Class<T[]> arrayType) {
            @SuppressWarnings("unchecked")
            Class<T> componentType = (Class<T>) arrayType.getComponentType();
            return arrayElement(getParameter(index, arrayType), 0, componentType);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", 0, Types.ExecutionContext);
            if (withResume) {
                setParameterName("rp", 1, Types.ResumptionPoint_);
                setParameterName("completion", 2, Types.Object_);
            }
        }

        @Override
        protected final MutableValue<ResumptionPoint> resumptionPoint() {
            assert withResume;
            return arrayElementFromParameter(1, ResumptionPoint[].class);
        }

        @Override
        protected final void returnForSuspend() {
            store(resumptionPoint());
            pushDefaultReturn();
            _return();
        }

        @Override
        protected final void returnForCompletion() {
            if (withResume) {
                store(arrayElementFromParameter(2, Object[].class));
                pushDefaultReturn();
                _return();
            } else {
                _return();
            }
        }

        protected abstract void pushDefaultReturn();
    }

    private static final class StatementListMethodCodeVisitor extends OutlinedMethodCodeVisitor {
        private final boolean nodeCompletion;

        StatementListMethodCodeVisitor(StatementListMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
            this.nodeCompletion = node.hasCompletionValue();
        }

        @Override
        public void begin() {
            super.begin();
            if (!hasResume()) {
                setParameterName("completion", 1, Types.Object_);
            }
        }

        @Override
        protected MutableValue<Object> createCompletionVariable() {
            return arrayElementFromParameter(hasResume() ? 2 : 1, Object[].class);
        }

        @Override
        protected boolean hasCompletionValue() {
            return nodeCompletion && getParent().hasCompletionValue();
        }

        @Override
        protected void pushDefaultReturn() {
            // Only used for suspend returns, completion returns are stored in Labels#completion.
            iconst(-1);
        }
    }

    private static final class ExpressionMethodVisitor extends OutlinedMethodCodeVisitor {
        ExpressionMethodVisitor(ExpressionMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        protected void pushDefaultReturn() {
            anull();
        }
    }

    private static final class SpreadElementCodeVisitor extends OutlinedMethodCodeVisitor {
        SpreadElementCodeVisitor(SpreadElementMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("array", parameter(1), Types.ArrayObject);
            setParameterName("index", parameter(2), Type.INT_TYPE);
        }

        @Override
        protected void pushDefaultReturn() {
            iconst(-1);
        }

        void loadArrayObject() {
            loadParameter(parameter(1), ArrayObject.class);
        }

        void loadArrayIndex() {
            loadParameter(parameter(2), int.class);
        }
    }

    private static final class PropertyDefinitionsCodeVisitor extends OutlinedMethodCodeVisitor {
        PropertyDefinitionsCodeVisitor(PropertyDefinitionsMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("object", parameter(1), Types.OrdinaryObject);
            setParameterName("decorators", parameter(2), Types.ArrayList);
        }

        @Override
        protected void pushDefaultReturn() {
            // void return
        }

        Variable<OrdinaryObject> getObjectParameter() {
            return getParameter(parameter(1), OrdinaryObject.class);
        }

        Variable<ArrayList<Object>> getDecoratorsParameter() {
            return getParameter(parameter(2), ArrayList.class).uncheckedCast();
        }
    }

    private static final class MethodDefinitionsCodeVisitor extends OutlinedMethodCodeVisitor {
        MethodDefinitionsCodeVisitor(MethodDefinitionsMethod node, MethodCode method, CodeVisitor parent) {
            super(node, method, parent);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("F", parameter(1), Types.OrdinaryConstructorFunction);
            setParameterName("proto", parameter(2), Types.OrdinaryObject);
            setParameterName("decorators", parameter(3), Types.ArrayList);
        }

        @Override
        protected void pushDefaultReturn() {
            // void return
        }

        Variable<OrdinaryConstructorFunction> getFunctionParameter() {
            return getParameter(parameter(1), OrdinaryConstructorFunction.class);
        }

        Variable<OrdinaryObject> getPrototypeParameter() {
            return getParameter(parameter(2), OrdinaryObject.class);
        }

        Variable<ArrayList<Object>> getDecoratorsParameter() {
            return getParameter(parameter(3), ArrayList.class).uncheckedCast();
        }
    }

    private static final class BlockDeclInitVisitor extends InstructionVisitor {
        BlockDeclInitVisitor(MethodCode method) {
            super(method);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("env", 0, Types.LexicalEnvironment);
            setParameterName("cx", 1, Types.ExecutionContext);
        }

        Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> getLexicalEnvironment() {
            return getParameter(0, LexicalEnvironment.class).uncheckedCast();
        }

        Variable<ExecutionContext> getExecutionContext() {
            return getParameter(1, ExecutionContext.class);
        }
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;

import com.github.anba.es6draft.ast.ElementAccessor;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.LeftHandSideExpression;
import com.github.anba.es6draft.ast.Literal;
import com.github.anba.es6draft.ast.PropertyAccessor;
import com.github.anba.es6draft.ast.StringLiteral;
import com.github.anba.es6draft.ast.SuperElementAccessor;
import com.github.anba.es6draft.ast.SuperPropertyAccessor;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;

/**
 *
 */
abstract class ReferenceOp<NODE extends LeftHandSideExpression> {
    /**
     * Evaluates {@code node} and pushes the resolved reference object on the stack.
     * <p>
     * stack: [] -> [{@literal <reference>}]
     * 
     * @param node
     *            the reference node
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType reference(NODE node, ExpressionVisitor mv, CodeGenerator gen) {
        return reference(node, false, mv, gen);
    }

    /**
     * Evaluates {@code node} and pushes two copies of the resolved reference object on the stack.
     * <p>
     * stack: [] -> [{@literal <reference>}, {@literal <reference>}]
     * 
     * @param node
     *            the reference node
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceForUpdate(NODE node, ExpressionVisitor mv, CodeGenerator gen) {
        return reference(node, true, mv, gen);
    }

    /**
     * Evaluates {@code node} and pushes the resolved reference object on the stack.
     * <p>
     * stack: [] -> [{@literal <reference>}]<br>
     * or: [] -> [{@literal <reference>}, {@literal <reference>}]
     * 
     * @param node
     *            the reference node
     * @param update
     *            if {@code true} duplicate reference
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    protected abstract ValType reference(NODE node, boolean update, ExpressionVisitor mv,
            CodeGenerator gen);

    /**
     * Retrieves the reference's value.
     * <p>
     * stack: [{@literal <reference>}] -> [{@literal <value>}]
     * 
     * @param node
     *            the reference node
     * @param ref
     *            the reference value type
     * @param mv
     *            the expression visitor
     * @return the stack top value or empty
     */
    abstract ValType getValue(NODE node, ValType ref, ExpressionVisitor mv);

    /**
     * Assigns a new value to the reference.
     * <p>
     * stack: [{@literal <reference>}, {@literal <value>}] -> []
     * 
     * @param node
     *            the reference node
     * @param ref
     *            the reference value type
     * @param value
     *            the top stack value type
     * @param mv
     *            the expression visitor
     */
    abstract void putValue(NODE node, ValType ref, ValType value, ExpressionVisitor mv);

    /**
     * Assigns a new value to the reference.
     * <p>
     * stack: [{@literal <reference>}, {@literal <value>}] -> []<br>
     * or: [{@literal <reference>}, {@literal <value>}] -> [{@literal <value>}]
     * 
     * @param node
     *            the reference node
     * @param ref
     *            the reference value type
     * @param value
     *            the top stack value type
     * @param completion
     *            if {@code true} keep a copy of the new value on the stack
     * @param mv
     *            the expression visitor
     */
    final ValType putValue(NODE node, ValType ref, ValType value, boolean completion,
            ExpressionVisitor mv) {
        if (completion) {
            Variable<?> saved = saveValue(ref, value, mv);
            putValue(node, ref, value, mv);
            restoreValue(saved, mv);
            return value;
        }
        putValue(node, ref, value, mv);
        return ValType.Empty;
    }

    /**
     * Evaluates {@code node} and pushes the resolved reference object on the stack.
     * <p>
     * stack: [] -> []
     * 
     * @param node
     *            the reference node
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the stack top value or empty
     */
    abstract ValType delete(NODE node, ExpressionVisitor mv, CodeGenerator gen);

    /**
     * Evaluates {@code node} and pushes the reference value on the stack.
     * <p>
     * stack: [] -> [{@literal <value>}]
     * 
     * @param node
     *            the reference node
     * @param mode
     *            the reference operation mode
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceValue(NODE node, ExpressionVisitor mv, CodeGenerator gen) {
        return referenceValue(node, false, mv, gen);
    }

    /**
     * Evaluates {@code node} and pushes the reference value on the stack.
     * <p>
     * stack: [] -> [{@literal <value>}, {@literal <thisValue>}]
     * 
     * @param node
     *            the reference node
     * @param mode
     *            the reference operation mode
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceValueAndThis(NODE node, ExpressionVisitor mv, CodeGenerator gen) {
        return referenceValue(node, true, mv, gen);
    }

    /**
     * Evaluates {@code node} and pushes the reference value on the stack.
     * <p>
     * stack: [] -> [{@literal <value>}]<br>
     * or: [] -> [{@literal <value>}, {@literal <thisValue>}]
     * 
     * @param node
     *            the reference node
     * @param withThis
     *            if {@code true} pushes this-value
     * @param mv
     *            the expression visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    protected abstract ValType referenceValue(NODE node, boolean withThis, ExpressionVisitor mv,
            CodeGenerator gen);

    /**
     * Saves the top stack value.
     * <p>
     * stack: [{@literal <reference>}, {@literal <value>}] -> [{@literal <value>}, {@literal 
     * <reference>}, {@literal <value>}]<br>
     * or: [{@literal <reference>}, {@literal <value>}] -> [{@literal <reference>}, {@literal 
     * <value>}] and returns a non-null {@link Variable} object.
     * 
     * @param ref
     *            the reference value type
     * @param value
     *            the top stack value type
     * @param mv
     *            the expression visitor
     * @return the variable to hold the value or {@code null} if saved on stack
     */
    abstract Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv);

    /**
     * Restores the top stack value. This operation is a no-op if <var>variable</var> is
     * {@code null}.
     * <p>
     * stack: [] -> [{@literal <value>}]
     * 
     * @param variable
     *            the variable or {@code null}
     * @param mv
     *            the expression visitor
     */
    abstract void restoreValue(Variable<?> variable, ExpressionVisitor mv);

    /**
     * Returns the {@code ReferenceOp} implementation for the left-hand side expression.
     * 
     * @param lhs
     *            the left-hand side expression
     * @return the {@code ReferenceOp}
     */
    @SuppressWarnings("unchecked")
    public static <NODE extends LeftHandSideExpression> ReferenceOp<NODE> of(NODE lhs) {
        if (lhs instanceof IdentifierReference) {
            return (ReferenceOp<NODE>) of((IdentifierReference) lhs);
        }
        return propertyOp(lhs);
    }

    /**
     * Returns the {@code ReferenceOp} implementation for the identifier reference.
     * 
     * @param lhs
     *            the identifier reference
     * @return the {@code ReferenceOp}
     */
    public static ReferenceOp<IdentifierReference> of(IdentifierReference lhs) {
        return ReferenceOp.LOOKUP;
    }

    /**
     * Returns the {@code ReferenceOp} implementation for the property accessor expression.
     * 
     * @param lhs
     *            the property accessor expression
     * @return the {@code ReferenceOp}
     */
    @SuppressWarnings("unchecked")
    public static <NODE extends LeftHandSideExpression> ReferenceOp<NODE> propertyOp(NODE lhs) {
        if (lhs instanceof ElementAccessor) {
            return (ReferenceOp<NODE>) ReferenceOp.ELEMENT;
        }
        if (lhs instanceof PropertyAccessor) {
            return (ReferenceOp<NODE>) ReferenceOp.PROPERTY;
        }
        if (lhs instanceof SuperElementAccessor) {
            return (ReferenceOp<NODE>) ReferenceOp.SUPER_ELEMENT;
        }
        if (lhs instanceof SuperPropertyAccessor) {
            return (ReferenceOp<NODE>) ReferenceOp.SUPER_PROPERTY;
        }
        throw new AssertionError();
    }

    private static final class Methods {
        // EnvironmentRecord
        static final MethodName EnvironmentRecord_withBaseObject = MethodName.findInterface(
                Types.EnvironmentRecord, "withBaseObject", Type.methodType(Types.ScriptObject));

        // class: Reference
        static final MethodName Reference_getValue = MethodName.findVirtual(Types.Reference,
                "getValue", Type.methodType(Types.Object, Types.ExecutionContext));

        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodName Reference_delete = MethodName.findVirtual(Types.Reference, "delete",
                Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext));

        static final MethodName Reference_getBase = MethodName.findVirtual(Types.Reference,
                "getBase", Type.methodType(Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_GetSuperEnvironmentRecord = MethodName.findStatic(Types.ScriptRuntime,
                "GetSuperEnvironmentRecord", Type.methodType(Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName ScriptRuntime_GetSuperThis = MethodName.findStatic(Types.ScriptRuntime, "GetSuperThis",
                Type.methodType(Types.Object, Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName ScriptRuntime_GetSuperBase = MethodName.findStatic(Types.ScriptRuntime, "GetSuperBase",
                Type.methodType(Types.ScriptObject, Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getSuperProperty = MethodName.findStatic(Types.ScriptRuntime,
                "getSuperProperty",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getSuperProperty_String = MethodName.findStatic(Types.ScriptRuntime,
                "getSuperProperty",
                Type.methodType(Types.Object, Types.String, Types.Object, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName ScriptRuntime_setSuperProperty = MethodName.findStatic(Types.ScriptRuntime,
                "setSuperProperty", Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object, Types.ScriptObject,
                        Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setSuperProperty_String = MethodName.findStatic(Types.ScriptRuntime,
                "setSuperProperty", Type.methodType(Type.VOID_TYPE, Types.String, Types.Object, Types.ScriptObject,
                        Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_deleteSuperProperty = MethodName.findStatic(Types.ScriptRuntime,
                "deleteSuperProperty", Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext));

        // ScriptRuntime#checkAccessProperty
        static final MethodName ScriptRuntime_checkAccessElement = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessElement",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_checkAccessProperty = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessProperty",
                Type.methodType(Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_checkAccessProperty_String = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessProperty",
                Type.methodType(Types.String, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName ScriptRuntime_checkAccessProperty_int = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessProperty", Type.methodType(Type.INT_TYPE,
                        Types.Object, Type.INT_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_checkAccessProperty_long = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessProperty", Type.methodType(Type.LONG_TYPE,
                        Types.Object, Type.LONG_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_checkAccessProperty_double = MethodName.findStatic(
                Types.ScriptRuntime, "checkAccessProperty", Type.methodType(Type.DOUBLE_TYPE,
                        Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext));

        // ScriptRuntime#getPropertyValue
        static final MethodName ScriptRuntime_getElementValue = MethodName.findStatic(
                Types.ScriptRuntime, "getElementValue",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_String = MethodName.findStatic(
                Types.ScriptRuntime, "getPropertyValue",
                Type.methodType(Types.Object, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_int = MethodName.findStatic(
                Types.ScriptRuntime, "getPropertyValue",
                Type.methodType(Types.Object, Types.Object, Type.INT_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_long = MethodName
                .findStatic(Types.ScriptRuntime, "getPropertyValue", Type.methodType(Types.Object,
                        Types.Object, Type.LONG_TYPE, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getPropertyValue_double = MethodName
                .findStatic(Types.ScriptRuntime, "getPropertyValue", Type.methodType(Types.Object,
                        Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext));

        // ScriptRuntime#setPropertyValue
        static final MethodName ScriptRuntime_setElementValue = MethodName.findStatic(
                Types.ScriptRuntime, "setElementValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setPropertyValue_String = MethodName.findStatic(
                Types.ScriptRuntime, "setPropertyValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.String, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setPropertyValue_int = MethodName.findStatic(
                Types.ScriptRuntime, "setPropertyValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Type.INT_TYPE, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setPropertyValue_long = MethodName.findStatic(
                Types.ScriptRuntime, "setPropertyValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Type.LONG_TYPE, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setPropertyValue_double = MethodName.findStatic(
                Types.ScriptRuntime, "setPropertyValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Type.DOUBLE_TYPE, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        // ScriptRuntime#deleteProperty
        static final MethodName ScriptRuntime_deleteElement = MethodName
                .findStatic(Types.ScriptRuntime, "deleteElement", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_deleteProperty_String = MethodName.findStatic(
                Types.ScriptRuntime, "deleteProperty", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Types.String, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_deleteProperty_int = MethodName.findStatic(
                Types.ScriptRuntime, "deleteProperty", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Type.INT_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_deleteProperty_long = MethodName.findStatic(
                Types.ScriptRuntime, "deleteProperty", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Type.LONG_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_deleteProperty_double = MethodName.findStatic(
                Types.ScriptRuntime, "deleteProperty", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));
    }

    private static ValType GetValue(LeftHandSideExpression node, ValType type,
            ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_getValue);
        return ValType.Any;
    }

    private static void PutValue(LeftHandSideExpression node, ValType type, ValType value,
            ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.toBoxed(value);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_putValue);
    }

    private static ValType Delete(LeftHandSideExpression node, ValType type, ExpressionVisitor mv) {
        assert type == ValType.Reference : "type is not reference: " + type;
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_delete);
        return ValType.Boolean;
    }

    private static ValType getElement(LeftHandSideExpression node, ValType elementType,
            ExpressionVisitor mv) {
        // stack: [base, key] -> [value]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(elementGetMethod(elementType));
        return ValType.Any;
    }

    private static ValType setElement(LeftHandSideExpression node, ValType elementType,
            ValType value, ExpressionVisitor mv) {
        // stack: [base, key, value] -> []
        mv.toBoxed(value);
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(elementSetMethod(elementType));
        return ValType.Empty;
    }

    private static ValType deleteElement(LeftHandSideExpression node, ValType elementType,
            ExpressionVisitor mv) {
        // stack: [base, key] -> [result]
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(elementDeleteMethod(elementType));
        return ValType.Boolean;
    }

    private static MethodName checkAccessMethod(ValType elementType) {
        switch (elementType) {
        case Empty:
            return Methods.ScriptRuntime_checkAccessProperty;
        case Number:
            return Methods.ScriptRuntime_checkAccessProperty_double;
        case Number_int:
            return Methods.ScriptRuntime_checkAccessProperty_int;
        case Number_uint:
            return Methods.ScriptRuntime_checkAccessProperty_long;
        case String:
            return Methods.ScriptRuntime_checkAccessProperty_String;
        case Any:
        case Object:
            return Methods.ScriptRuntime_checkAccessElement;
        default:
            throw new AssertionError();
        }
    }

    private static MethodName elementGetMethod(ValType elementType) {
        switch (elementType) {
        case Number:
            return Methods.ScriptRuntime_getPropertyValue_double;
        case Number_int:
            return Methods.ScriptRuntime_getPropertyValue_int;
        case Number_uint:
            return Methods.ScriptRuntime_getPropertyValue_long;
        case String:
            return Methods.ScriptRuntime_getPropertyValue_String;
        case Any:
        case Object:
            return Methods.ScriptRuntime_getElementValue;
        default:
            throw new AssertionError();
        }
    }

    private static MethodName elementSetMethod(ValType elementType) {
        switch (elementType) {
        case Number:
            return Methods.ScriptRuntime_setPropertyValue_double;
        case Number_int:
            return Methods.ScriptRuntime_setPropertyValue_int;
        case Number_uint:
            return Methods.ScriptRuntime_setPropertyValue_long;
        case String:
            return Methods.ScriptRuntime_setPropertyValue_String;
        case Any:
        case Object:
            return Methods.ScriptRuntime_setElementValue;
        default:
            throw new AssertionError();
        }
    }

    private static MethodName elementDeleteMethod(ValType elementType) {
        switch (elementType) {
        case Number:
            return Methods.ScriptRuntime_deleteProperty_double;
        case Number_int:
            return Methods.ScriptRuntime_deleteProperty_int;
        case Number_uint:
            return Methods.ScriptRuntime_deleteProperty_long;
        case String:
            return Methods.ScriptRuntime_deleteProperty_String;
        case Any:
        case Object:
            return Methods.ScriptRuntime_deleteElement;
        default:
            throw new AssertionError();
        }
    }

    private static void GetSuperEnvironment(LeftHandSideExpression node, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_GetSuperEnvironmentRecord);
        mv.dup();
    }

    private static void GetSuperThis(ExpressionVisitor mv) {
        // stack: [env] -> [thisValue]
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetSuperThis);
    }

    private static void GetSuperBase(ExpressionVisitor mv) {
        // stack: [env, thisValue] -> [thisValue, baseValue]
        mv.swap();
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_GetSuperBase);
    }

    private static ValType GetSuperElement(LeftHandSideExpression node, ValType elementType, ExpressionVisitor mv) {
        // stack: [pk, thisValue, baseValue] -> [value]
        mv.loadExecutionContext();
        mv.lineInfo(node);
        if (elementType == ValType.String) {
            mv.invoke(Methods.ScriptRuntime_getSuperProperty_String);
        } else {
            assert elementType == ValType.Any;
            mv.invoke(Methods.ScriptRuntime_getSuperProperty);
        }
        return ValType.Any;
    }

    private static ValType GetSuperElement(LeftHandSideExpression node, ValType elementType, ValType value,
            ExpressionVisitor mv) {
        // stack: [pk, thisValue, baseValue, value] -> []
        mv.toBoxed(value);
        mv.loadExecutionContext();
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        if (elementType == ValType.String) {
            mv.invoke(Methods.ScriptRuntime_setSuperProperty_String);
        } else {
            assert elementType == ValType.Any;
            mv.invoke(Methods.ScriptRuntime_setSuperProperty);
        }
        return ValType.Empty;
    }

    private static ValType DeleteSuperElement(LeftHandSideExpression node, ValType elementType, ExpressionVisitor mv) {
        mv.pop(elementType);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_deleteSuperProperty);
        return ValType.Boolean;
    }

    private static Variable<?> saveToVariable(ValType value, ExpressionVisitor mv) {
        Variable<?> result = mv.newScratchVariable(value.toClass());
        mv.dup(value);
        mv.store(result);
        return result;
    }

    private static void loadFromVariable(Variable<?> variable, ExpressionVisitor mv) {
        mv.load(variable);
        mv.freeVariable(variable);
    }

    /**
     * 12.1 Identifiers
     * <p>
     * 12.1.6 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<IdentifierReference> LOOKUP = new ReferenceOp<IdentifierReference>() {
        @Override
        protected ValType reference(IdentifierReference node, boolean update, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [ref]
            ValType ref = IdentifierResolution.resolve(node, mv);
            assert ref == ValType.Reference : "type is not reference: " + ref;
            if (update) {
                mv.dup();
            }
            return ref;
        }

        @Override
        ValType getValue(IdentifierReference node, ValType ref, ExpressionVisitor mv) {
            // stack: [ref] -> [value]
            return GetValue(node, ref, mv);
        }

        @Override
        void putValue(IdentifierReference node, ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [ref, value] -> []
            PutValue(node, ref, value, mv);
        }

        @Override
        ValType delete(IdentifierReference node, ExpressionVisitor mv, CodeGenerator gen) {
            ValType ref = reference(node, false, mv, gen);
            return Delete(node, ref, mv);
        }

        @Override
        protected ValType referenceValue(IdentifierReference node, boolean withThis,
                ExpressionVisitor mv, CodeGenerator gen) {
            if (withThis) {
                // stack: [] -> [ref, ref]
                ValType ref = reference(node, mv, gen);
                mv.dup();
                // stack: [ref, ref] -> [value, ref]
                getValue(node, ref, mv);
                mv.swap();
                // stack: [value, ref] -> [value, baseObj?]
                mv.invoke(Methods.Reference_getBase);
                mv.checkcast(Types.EnvironmentRecord);
                mv.invoke(Methods.EnvironmentRecord_withBaseObject);
                // stack: [value, baseObj?] -> [value, thisValue]
                Jump baseObjNotNull = new Jump();
                mv.dup();
                mv.ifnonnull(baseObjNotNull);
                {
                    mv.pop();
                    mv.loadUndefined();
                }
                mv.mark(baseObjNotNull);
                return ValType.Any;
            }
            return IdentifierResolution.resolveValue(node, mv);
        }

        @Override
        Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [ref, value] -> [value, ref, value]
            mv.dupX(ref, value);
            return null;
        }

        @Override
        void restoreValue(Variable<?> result, ExpressionVisitor mv) {
            // stack: [] -> []
        }
    };

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<PropertyAccessor> PROPERTY = new ReferenceOp<PropertyAccessor>() {
        @Override
        protected ValType reference(PropertyAccessor node, boolean update, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [base, key]
            /* steps 1-3 */
            gen.expressionBoxed(node.getBase(), mv);
            /* steps 4-6 (not applicable) */
            /* steps 7-8 */
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(checkAccessMethod(ValType.Empty));
            /* steps 9-10 */
            mv.aconst(node.getName());
            /* steps 11-12 (not applicable) */
            if (update) {
                // stack: [base, key] -> [base, key, base, key]
                mv.dup2();
            }
            return ValType.String;
        }

        @Override
        ValType getValue(PropertyAccessor node, ValType ref, ExpressionVisitor mv) {
            // stack: [base, key] -> [value]
            return getElement(node, ValType.String, mv);
        }

        @Override
        void putValue(PropertyAccessor node, ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [base, key, value] -> []
            setElement(node, ValType.String, value, mv);
        }

        @Override
        ValType delete(PropertyAccessor node, ExpressionVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base]
            gen.expressionBoxed(node.getBase(), mv);
            // stack: [base] -> [base, key]
            mv.aconst(node.getName());
            // stack: [base, key] -> [result]
            return deleteElement(node, ValType.String, mv);
        }

        @Override
        protected ValType referenceValue(PropertyAccessor node, boolean withThis,
                ExpressionVisitor mv, CodeGenerator gen) {
            gen.expressionBoxed(node.getBase(), mv);
            if (withThis) {
                mv.dup();
            }
            mv.aconst(node.getName());
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(elementGetMethod(ValType.String));
            if (withThis) {
                // stack: [thisValue, func] -> [func, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [base, key, value] -> [value, base, key, value]
            // ValType.Number to represent (base, key) tuple
            mv.dupX(ValType.Number, value);
            return null;
        }

        @Override
        void restoreValue(Variable<?> variable, ExpressionVisitor mv) {
            // stack: [] -> []
        }
    };

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<ElementAccessor> ELEMENT = new ReferenceOp<ElementAccessor>() {
        private ValType evalPropertyKey(Expression propertyKey, ExpressionVisitor mv,
                CodeGenerator gen) {
            ValType elementType = gen.expression(propertyKey, mv);
            switch (elementType) {
            case String:
                if (propertyKey instanceof StringLiteral) {
                    return elementType;
                }
                // fall-thru if string is not flat
            case Boolean:
            case Null:
            case Undefined:
                DefaultCodeGenerator.ToFlatString(elementType, mv);
                return ValType.String;
            default:
                return elementType;
            }
        }

        @Override
        protected ValType reference(ElementAccessor node, boolean update, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [base, base?, key]
            /* steps 1-3 */
            gen.expressionBoxed(node.getBase(), mv);
            boolean isLiteral = node.getElement() instanceof Literal;
            if (isLiteral) {
                /* steps 7-10 */
                mv.loadExecutionContext();
                mv.lineInfo(node);
                mv.invoke(checkAccessMethod(ValType.Empty));
            } else {
                mv.dup();
            }
            if (update) {
                mv.dup();
            }
            /* steps 4-6 */
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            if (!isLiteral) {
                /* steps 7-10 */
                mv.loadExecutionContext();
                mv.lineInfo(node);
                mv.invoke(checkAccessMethod(elementType));
            }
            /* steps 11-12 (not applicable) */
            if (update) {
                // stack: [base, base, key] -> [base, key, base, key]
                mv.dupX(ValType.Any, elementType);
            }
            return elementType;
        }

        @Override
        ValType getValue(ElementAccessor node, ValType ref, ExpressionVisitor mv) {
            // stack: [base, key] -> [value]
            return getElement(node, ref, mv);
        }

        @Override
        void putValue(ElementAccessor node, ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [base, key, value] -> []
            setElement(node, ref, value, mv);
        }

        @Override
        ValType delete(ElementAccessor node, ExpressionVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base]
            gen.expressionBoxed(node.getBase(), mv);
            // stack: [base] -> [base, key]
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            // stack: [base, key] -> [result]
            return deleteElement(node, elementType, mv);
        }

        @Override
        protected ValType referenceValue(ElementAccessor node, boolean withThis,
                ExpressionVisitor mv, CodeGenerator gen) {
            gen.expressionBoxed(node.getBase(), mv);
            if (withThis) {
                mv.dup();
            }
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(elementGetMethod(elementType));
            if (withThis) {
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [base, key, value] -> [value, base, key, value]
            if (ref.size() == 1) {
                // ValType.Number to represent (base, key) tuple
                mv.dupX(ValType.Number, value);
                return null;
            }
            return saveToVariable(value, mv);
        }

        @Override
        void restoreValue(Variable<?> variable, ExpressionVisitor mv) {
            if (variable != null) {
                loadFromVariable(variable, mv);
            }
        }
    };

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<SuperPropertyAccessor> SUPER_PROPERTY = new ReferenceOp<SuperPropertyAccessor>() {
        @Override
        protected ValType reference(SuperPropertyAccessor node, boolean update, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [pk, pk?]
            mv.aconst(node.getName());
            if (update) {
                mv.dup();
            }

            // stack: [pk, pk?] -> [pk, pk?, env, env]
            GetSuperEnvironment(node, mv);

            // stack: [pk, pk?, env, env] -> [pk, pk?, thisValue, baseValue]
            GetSuperThis(mv);
            GetSuperBase(mv);
            if (update) {
                // stack: [pk, pk, thisValue, baseValue] -> [pk, thisValue, baseValue, pk, thisValue, baseValue]
                mv.dup2X1();
            }
            return ValType.String;
        }

        @Override
        ValType getValue(SuperPropertyAccessor node, ValType ref, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue] -> [value]
            return GetSuperElement(node, ref, mv);
        }

        @Override
        void putValue(SuperPropertyAccessor node, ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> []
            GetSuperElement(node, ref, value, mv);
        }

        @Override
        ValType delete(SuperPropertyAccessor node, ExpressionVisitor mv, CodeGenerator gen) {
            return DeleteSuperElement(node, ValType.Empty, mv);
        }

        @Override
        protected ValType referenceValue(SuperPropertyAccessor node, boolean withThis, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [pk]
            mv.aconst(node.getName());

            // stack: [pk] -> [pk, env, env]
            GetSuperEnvironment(node, mv);

            // stack: [pk, env, env] -> [thisValue?, pk, thisValue, baseValue]
            GetSuperThis(mv);
            if (withThis) {
                mv.dupX2();
            }
            GetSuperBase(mv);

            // stack: [thisValue?, pk, thisValue, baseValue] -> [thisValue?, value]
            GetSuperElement(node, ValType.String, mv);
            if (withThis) {
                // stack: [thisValue, value] -> [value, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> [pk, thisValue, baseValue, value]
            return saveToVariable(value, mv);
        }

        @Override
        void restoreValue(Variable<?> variable, ExpressionVisitor mv) {
            // stack: [] -> [value]
            loadFromVariable(variable, mv);
        }
    };

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<SuperElementAccessor> SUPER_ELEMENT = new ReferenceOp<SuperElementAccessor>() {
        @Override
        protected ValType reference(SuperElementAccessor node, boolean update, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [pk, pk?]
            ValType type = gen.expression(node.getElement(), mv);
            type = ToPropertyKey(type, mv);
            if (update) {
                mv.dup();
            }

            // stack: [pk, pk?] -> [pk, pk?, env, env]
            GetSuperEnvironment(node, mv);

            // stack: [pk, pk?, env, env] -> [pk, pk?, thisValue, baseValue]
            GetSuperThis(mv);
            GetSuperBase(mv);
            if (update) {
                // stack: [pk, pk, thisValue, baseValue] -> [pk, thisValue, baseValue, pk, thisValue, baseValue]
                mv.dup2X1();
            }
            return type;
        }

        @Override
        ValType getValue(SuperElementAccessor node, ValType ref, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue] -> [value]
            return GetSuperElement(node, ref, mv);
        }

        @Override
        void putValue(SuperElementAccessor node, ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> []
            GetSuperElement(node, ref, value, mv);
        }

        @Override
        ValType delete(SuperElementAccessor node, ExpressionVisitor mv, CodeGenerator gen) {
            ValType type = gen.expression(node.getElement().emptyCompletion(), mv);
            return DeleteSuperElement(node, type, mv);
        }

        @Override
        protected ValType referenceValue(SuperElementAccessor node, boolean withThis, ExpressionVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [pk]
            ValType type = gen.expression(node.getElement(), mv);
            type = ToPropertyKey(type, mv);

            // stack: [pk] -> [pk, env, env]
            GetSuperEnvironment(node, mv);

            // stack: [pk, env, env] -> [thisValue?, pk, thisValue, baseValue]
            GetSuperThis(mv);
            if (withThis) {
                mv.dupX2();
            }
            GetSuperBase(mv);

            // stack: [thisValue?, pk, thisValue, baseValue] -> [thisValue?, value]
            GetSuperElement(node, type, mv);
            if (withThis) {
                // stack: [thisValue, value] -> [value, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        Variable<?> saveValue(ValType ref, ValType value, ExpressionVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> [pk, thisValue, baseValue, value]
            return saveToVariable(value, mv);
        }

        @Override
        void restoreValue(Variable<?> variable, ExpressionVisitor mv) {
            // stack: [] -> [value]
            loadFromVariable(variable, mv);
        }
    };
}

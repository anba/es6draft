/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;

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
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType reference(NODE node, CodeVisitor mv, CodeGenerator gen) {
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
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceForUpdate(NODE node, CodeVisitor mv, CodeGenerator gen) {
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
     *            if {@code true} duplicates reference
     * @param mv
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    protected abstract ValType reference(NODE node, boolean update, CodeVisitor mv, CodeGenerator gen);

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
     *            the code visitor
     * @return the stack top value or empty
     */
    abstract ValType getValue(NODE node, ValType ref, CodeVisitor mv);

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
     *            the code visitor
     */
    abstract void putValue(NODE node, ValType ref, ValType value, CodeVisitor mv);

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
     *            the code visitor
     * @return the stack top value or empty
     */
    final ValType putValue(NODE node, ValType ref, ValType value, boolean completion, CodeVisitor mv) {
        if (completion) {
            Value<?> currentValue = dupOrStoreValue(ref, value, mv);
            putValue(node, ref, value, mv);
            mv.load(currentValue);
            return value;
        }
        putValue(node, ref, value, mv);
        return ValType.Empty;
    }

    /**
     * Evaluates {@code node} and pushes the result value on the stack.
     * <p>
     * stack: [] -> [{@literal <result>}]
     * 
     * @param node
     *            the reference node
     * @param mv
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the result value type
     */
    abstract ValType delete(NODE node, CodeVisitor mv, CodeGenerator gen);

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
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceValue(NODE node, CodeVisitor mv, CodeGenerator gen) {
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
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    final ValType referenceValueAndThis(NODE node, CodeVisitor mv, CodeGenerator gen) {
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
     *            the code visitor
     * @param gen
     *            the code generator
     * @return the reference value type
     */
    protected abstract ValType referenceValue(NODE node, boolean withThis, CodeVisitor mv, CodeGenerator gen);

    /**
     * Saves the top stack value.
     * <p>
     * stack: [{@literal <reference>}, {@literal <value>}] -> [{@literal <value>}, {@literal 
     * <reference>}, {@literal <value>}]<br>
     * or: [{@literal <reference>}, {@literal <value>}] -> [{@literal <reference>}, {@literal 
     * <value>}] and stores the top stack value in the returned {@link Value} object.
     * 
     * @param ref
     *            the reference value type
     * @param value
     *            the top stack value type
     * @param mv
     *            the code visitor
     * @return the top stack {@link Value}
     */
    final Value<?> dupOrStoreValue(ValType ref, ValType value, CodeVisitor mv) {
        int refSize = referenceSize(ref);
        if (refSize > 2) {
            mv.dup(value);
            return mv.storeTemporary(value.toClass());
        }
        mv.dupX(refSize, value.size());
        return ReferenceOp::emptyValue;
    }

    private static void emptyValue(InstructionAssembler asm) {
        // Value already loaded on stack.
    }

    /**
     * Returns the reference type size.
     * 
     * @param ref
     *            the reference value type
     * @return the reference type size
     */
    protected abstract int referenceSize(ValType ref);

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
        if (lhs instanceof PrivatePropertyAccessor) {
            return (ReferenceOp<NODE>) ReferenceOp.PRIVATE_PROPERTY;
        }
        throw new AssertionError();
    }

    private static final class Methods {
        // EnvironmentRecord
        static final MethodName EnvironmentRecord_withBaseObject = MethodName.findInterface(Types.EnvironmentRecord,
                "withBaseObject", Type.methodType(Types.ScriptObject));

        // class: Reference
        static final MethodName Reference_getValue = MethodName.findVirtual(Types.Reference, "getValue",
                Type.methodType(Types.Object, Types.ExecutionContext));

        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference, "putValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodName Reference_delete = MethodName.findVirtual(Types.Reference, "delete",
                Type.methodType(Type.BOOLEAN_TYPE, Types.ExecutionContext));

        static final MethodName Reference_getBase = MethodName.findVirtual(Types.Reference, "getBase",
                Type.methodType(Types.Object));

        // class: PropertyOperations (super property operations)
        static final MethodName PropertyOperations_GetSuperEnvironmentRecord = MethodName.findStatic(
                Types.PropertyOperations, "GetSuperEnvironmentRecord",
                Type.methodType(Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName PropertyOperations_GetSuperThis = MethodName.findStatic(Types.PropertyOperations,
                "GetSuperThis", Type.methodType(Types.Object, Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName PropertyOperations_GetSuperBase = MethodName.findStatic(Types.PropertyOperations,
                "GetSuperBase",
                Type.methodType(Types.ScriptObject, Types.FunctionEnvironmentRecord, Types.ExecutionContext));

        static final MethodName PropertyOperations_getSuperElement = MethodName.findStatic(Types.PropertyOperations,
                "getSuperElement",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName PropertyOperations_getSuperProperty = MethodName.findStatic(Types.PropertyOperations,
                "getSuperProperty",
                Type.methodType(Types.Object, Types.String, Types.Object, Types.ScriptObject, Types.ExecutionContext));

        static final MethodName PropertyOperations_setSuperElement = MethodName.findStatic(Types.PropertyOperations,
                "setSuperElement", Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object, Types.ScriptObject,
                        Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setSuperProperty = MethodName.findStatic(Types.PropertyOperations,
                "setSuperProperty", Type.methodType(Type.VOID_TYPE, Types.String, Types.Object, Types.ScriptObject,
                        Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteSuperElement = MethodName.findStatic(Types.PropertyOperations,
                "deleteSuperElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Types.ExecutionContext));

        static final MethodName PropertyOperations_deleteSuperProperty = MethodName.findStatic(Types.PropertyOperations,
                "deleteSuperProperty", Type.methodType(Type.BOOLEAN_TYPE, Types.String, Types.ExecutionContext));

        // class: PropertyOperations (private property operations)
        static final MethodName PropertyOperations_getPrivateValue = MethodName.findStatic(Types.PropertyOperations,
                "getPrivateValue", Type.methodType(Types.Object, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName PropertyOperations_setPrivateValue = MethodName.findStatic(Types.PropertyOperations,
                "setPrivateValue",
                Type.methodType(Type.VOID_TYPE, Types.Object, Types.String, Types.Object, Types.ExecutionContext));

        // PropertyOperations#checkAccessElement, #checkAccessProperty
        static final MethodName PropertyOperations_checkAccessElement = MethodName.findStatic(Types.PropertyOperations,
                "checkAccessElement",
                Type.methodType(Types.Object, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName PropertyOperations_checkAccessElement_int = MethodName.findStatic(
                Types.PropertyOperations, "checkAccessElement",
                Type.methodType(Type.INT_TYPE, Types.Object, Type.INT_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_checkAccessElement_long = MethodName.findStatic(
                Types.PropertyOperations, "checkAccessElement",
                Type.methodType(Type.LONG_TYPE, Types.Object, Type.LONG_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_checkAccessElement_String = MethodName.findStatic(
                Types.PropertyOperations, "checkAccessElement",
                Type.methodType(Types.String, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName PropertyOperations_checkAccessElement_double = MethodName.findStatic(
                Types.PropertyOperations, "checkAccessElement",
                Type.methodType(Type.DOUBLE_TYPE, Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_checkAccessProperty = MethodName.findStatic(Types.PropertyOperations,
                "checkAccessProperty", Type.methodType(Types.Object, Types.Object, Types.ExecutionContext));

        // PropertyOperations#getElementValue, #getPropertyValue
        static final MethodName PropertyOperations_getElementValue = MethodName.findStatic(Types.PropertyOperations,
                "getElementValue", Type.methodType(Types.Object, Types.Object, Types.Object, Types.ExecutionContext));

        static final MethodName PropertyOperations_getElementValue_int = MethodName.findStatic(Types.PropertyOperations,
                "getElementValue", Type.methodType(Types.Object, Types.Object, Type.INT_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_getElementValue_long = MethodName.findStatic(
                Types.PropertyOperations, "getElementValue",
                Type.methodType(Types.Object, Types.Object, Type.LONG_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_getElementValue_double = MethodName.findStatic(
                Types.PropertyOperations, "getElementValue",
                Type.methodType(Types.Object, Types.Object, Type.DOUBLE_TYPE, Types.ExecutionContext));

        static final MethodName PropertyOperations_getElementValue_String = MethodName.findStatic(
                Types.PropertyOperations, "getElementValue",
                Type.methodType(Types.Object, Types.Object, Types.String, Types.ExecutionContext));

        static final MethodName PropertyOperations_getPropertyValue = MethodName.findStatic(Types.PropertyOperations,
                "getPropertyValue", Type.methodType(Types.Object, Types.Object, Types.String, Types.ExecutionContext));

        // PropertyOperations#setElementValue, #setPropertyValue
        static final MethodName PropertyOperations_setElementValue = MethodName.findStatic(Types.PropertyOperations,
                "setElementValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.Object, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setElementValue_int = MethodName.findStatic(Types.PropertyOperations,
                "setElementValue", Type.methodType(Type.VOID_TYPE, Types.Object, Type.INT_TYPE, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setElementValue_long = MethodName
                .findStatic(Types.PropertyOperations, "setElementValue", Type.methodType(Type.VOID_TYPE, Types.Object,
                        Type.LONG_TYPE, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setElementValue_double = MethodName
                .findStatic(Types.PropertyOperations, "setElementValue", Type.methodType(Type.VOID_TYPE, Types.Object,
                        Type.DOUBLE_TYPE, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setElementValue_String = MethodName
                .findStatic(Types.PropertyOperations, "setElementValue", Type.methodType(Type.VOID_TYPE, Types.Object,
                        Types.String, Types.Object, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_setPropertyValue = MethodName.findStatic(Types.PropertyOperations,
                "setPropertyValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.String, Types.Object,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        // PropertyOperations#deleteProperty
        static final MethodName PropertyOperations_deleteElement = MethodName.findStatic(Types.PropertyOperations,
                "deleteElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Types.Object, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteElement_int = MethodName.findStatic(Types.PropertyOperations,
                "deleteElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Type.INT_TYPE, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteElement_long = MethodName.findStatic(Types.PropertyOperations,
                "deleteElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Type.LONG_TYPE,
                        Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteElement_double = MethodName
                .findStatic(Types.PropertyOperations, "deleteElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object,
                        Type.DOUBLE_TYPE, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteElement_String = MethodName
                .findStatic(Types.PropertyOperations, "deleteElement", Type.methodType(Type.BOOLEAN_TYPE, Types.Object,
                        Types.String, Types.ExecutionContext, Type.BOOLEAN_TYPE));

        static final MethodName PropertyOperations_deleteProperty = MethodName.findStatic(Types.PropertyOperations,
                "deleteProperty", Type.methodType(Type.BOOLEAN_TYPE, Types.Object, Types.String, Types.ExecutionContext,
                        Type.BOOLEAN_TYPE));
    }

    private static void GetSuperEnvironment(LeftHandSideExpression node, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.PropertyOperations_GetSuperEnvironmentRecord);
        mv.dup();
    }

    private static void GetSuperThis(CodeVisitor mv) {
        // stack: [env] -> [thisValue]
        mv.loadExecutionContext();
        mv.invoke(Methods.PropertyOperations_GetSuperThis);
    }

    private static void GetSuperBase(CodeVisitor mv) {
        // stack: [env, thisValue] -> [thisValue, baseValue]
        mv.swap();
        mv.loadExecutionContext();
        mv.invoke(Methods.PropertyOperations_GetSuperBase);
    }

    /**
     * 12.1 Identifiers
     * <p>
     * 12.1.6 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<IdentifierReference> LOOKUP = new ReferenceOp<IdentifierReference>() {
        @Override
        protected ValType reference(IdentifierReference node, boolean update, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [ref]
            ValType ref = IdentifierResolution.resolve(node, mv);
            assert ref == ValType.Reference : "type is not reference: " + ref;
            if (update) {
                mv.dup();
            }
            return ref;
        }

        @Override
        ValType getValue(IdentifierReference node, ValType ref, CodeVisitor mv) {
            // stack: [ref] -> [value]
            assert ref == ValType.Reference : "type is not reference: " + ref;
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.Reference_getValue);
            return ValType.Any;
        }

        @Override
        void putValue(IdentifierReference node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [ref, value] -> []
            assert ref == ValType.Reference : "type is not reference: " + ref;
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.Reference_putValue);
        }

        @Override
        ValType delete(IdentifierReference node, CodeVisitor mv, CodeGenerator gen) {
            ValType ref = reference(node, false, mv, gen);
            assert ref == ValType.Reference : "type is not reference: " + ref;
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.Reference_delete);
            return ValType.Boolean;
        }

        @Override
        protected ValType referenceValue(IdentifierReference node, boolean withThis, CodeVisitor mv,
                CodeGenerator gen) {
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
        protected int referenceSize(ValType ref) {
            // ref
            return 1;
        }
    };

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<PropertyAccessor> PROPERTY = new ReferenceOp<PropertyAccessor>() {
        @Override
        protected ValType reference(PropertyAccessor node, boolean update, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base, key]
            /* steps 1-2 */
            gen.expressionBoxed(node.getBase(), mv);
            /* step 3 */
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_checkAccessProperty);
            /* step 4 */
            mv.aconst(node.getName());
            /* steps 5-6 (not applicable) */
            if (update) {
                // stack: [base, key] -> [base, key, base, key]
                mv.dup2();
            }
            return ValType.String;
        }

        @Override
        ValType getValue(PropertyAccessor node, ValType ref, CodeVisitor mv) {
            // stack: [base, key] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getPropertyValue);
            return ValType.Any;
        }

        @Override
        void putValue(PropertyAccessor node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [base, key, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_setPropertyValue);
        }

        @Override
        ValType delete(PropertyAccessor node, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base]
            gen.expressionBoxed(node.getBase(), mv);
            // stack: [base] -> [base, key]
            mv.aconst(node.getName());
            // stack: [base, key] -> [result]
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_deleteProperty);
            return ValType.Boolean;
        }

        @Override
        protected ValType referenceValue(PropertyAccessor node, boolean withThis, CodeVisitor mv, CodeGenerator gen) {
            gen.expressionBoxed(node.getBase(), mv);
            if (withThis) {
                mv.dup();
            }
            mv.aconst(node.getName());
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getPropertyValue);
            if (withThis) {
                // stack: [thisValue, func] -> [func, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        protected int referenceSize(ValType ref) {
            // base + key
            return 2;
        }
    };

    /**
     * 12.3.2 Property Accessors
     * <p>
     * 12.3.2.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<ElementAccessor> ELEMENT = new ReferenceOp<ElementAccessor>() {
        private ValType evalPropertyKey(Expression propertyKey, CodeVisitor mv, CodeGenerator gen) {
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
            case BigInt:
                DefaultCodeGenerator.ToFlatString(elementType, mv);
                return ValType.String;
            default:
                return elementType;
            }
        }

        private MethodName checkAccessMethod(ValType elementType) {
            switch (elementType) {
            case Empty:
                return Methods.PropertyOperations_checkAccessProperty;
            case Number:
                return Methods.PropertyOperations_checkAccessElement_double;
            case Number_int:
                return Methods.PropertyOperations_checkAccessElement_int;
            case Number_uint:
                return Methods.PropertyOperations_checkAccessElement_long;
            case String:
                return Methods.PropertyOperations_checkAccessElement_String;
            case Any:
            case Object:
                return Methods.PropertyOperations_checkAccessElement;
            default:
                throw new AssertionError();
            }
        }

        private MethodName elementGetMethod(ValType elementType) {
            switch (elementType) {
            case Number:
                return Methods.PropertyOperations_getElementValue_double;
            case Number_int:
                return Methods.PropertyOperations_getElementValue_int;
            case Number_uint:
                return Methods.PropertyOperations_getElementValue_long;
            case String:
                return Methods.PropertyOperations_getElementValue_String;
            case Any:
            case Object:
                return Methods.PropertyOperations_getElementValue;
            default:
                throw new AssertionError();
            }
        }

        private MethodName elementDeleteMethod(ValType elementType) {
            switch (elementType) {
            case Number:
                return Methods.PropertyOperations_deleteElement_double;
            case Number_int:
                return Methods.PropertyOperations_deleteElement_int;
            case Number_uint:
                return Methods.PropertyOperations_deleteElement_long;
            case String:
                return Methods.PropertyOperations_deleteElement_String;
            case Any:
            case Object:
                return Methods.PropertyOperations_deleteElement;
            default:
                throw new AssertionError();
            }
        }

        private MethodName elementSetMethod(ValType elementType) {
            switch (elementType) {
            case Number:
                return Methods.PropertyOperations_setElementValue_double;
            case Number_int:
                return Methods.PropertyOperations_setElementValue_int;
            case Number_uint:
                return Methods.PropertyOperations_setElementValue_long;
            case String:
                return Methods.PropertyOperations_setElementValue_String;
            case Any:
            case Object:
                return Methods.PropertyOperations_setElementValue;
            default:
                throw new AssertionError();
            }
        }

        @Override
        protected ValType reference(ElementAccessor node, boolean update, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base, base?, key]
            /* steps 1-2 */
            gen.expressionBoxed(node.getBase(), mv);
            boolean isLiteral = node.getElement() instanceof Literal;
            if (isLiteral) {
                /* steps 5-6 */
                mv.loadExecutionContext();
                mv.lineInfo(node);
                mv.invoke(checkAccessMethod(ValType.Empty));
            } else {
                mv.dup();
            }
            if (update) {
                mv.dup();
            }
            /* steps 3-4 */
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            if (!isLiteral) {
                /* steps 5-6 */
                mv.loadExecutionContext();
                mv.lineInfo(node);
                mv.invoke(checkAccessMethod(elementType));
            }
            /* steps 7-8 (not applicable) */
            if (update) {
                // stack: [base, base, key] -> [base, key, base, key]
                mv.dupX(ValType.Any, elementType);
            }
            return elementType;
        }

        @Override
        ValType getValue(ElementAccessor node, ValType ref, CodeVisitor mv) {
            // stack: [base, key] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(elementGetMethod(ref));
            return ValType.Any;
        }

        @Override
        void putValue(ElementAccessor node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [base, key, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(elementSetMethod(ref));
        }

        @Override
        ValType delete(ElementAccessor node, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base]
            gen.expressionBoxed(node.getBase(), mv);
            // stack: [base] -> [base, key]
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            // stack: [base, key] -> [result]
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(elementDeleteMethod(elementType));
            return ValType.Boolean;
        }

        @Override
        protected ValType referenceValue(ElementAccessor node, boolean withThis, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base?, base]
            gen.expressionBoxed(node.getBase(), mv);
            if (withThis) {
                mv.dup();
            }
            // stack: [base?, base] -> [base?, base, key]
            ValType elementType = evalPropertyKey(node.getElement(), mv, gen);
            // stack: [base?, base, key] -> [base?, result]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(elementGetMethod(elementType));
            // stack: [] -> [result, base?]
            if (withThis) {
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        protected int referenceSize(ValType ref) {
            // base + key
            return 1 + ref.size();
        }
    };

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<SuperPropertyAccessor> SUPER_PROPERTY = new ReferenceOp<SuperPropertyAccessor>() {
        @Override
        protected ValType reference(SuperPropertyAccessor node, boolean update, CodeVisitor mv, CodeGenerator gen) {
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
        ValType getValue(SuperPropertyAccessor node, ValType ref, CodeVisitor mv) {
            // stack: [pk, thisValue, baseValue] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getSuperProperty);
            return ValType.Any;
        }

        @Override
        void putValue(SuperPropertyAccessor node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_setSuperProperty);
        }

        @Override
        ValType delete(SuperPropertyAccessor node, CodeVisitor mv, CodeGenerator gen) {
            mv.aconst(node.getName());
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_deleteSuperProperty);
            return ValType.Boolean;
        }

        @Override
        protected ValType referenceValue(SuperPropertyAccessor node, boolean withThis, CodeVisitor mv,
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
            // stack: [pk, thisValue, baseValue] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getSuperProperty);

            // stack: [thisValue?, pk, thisValue, baseValue] -> [thisValue?, value]
            if (withThis) {
                // stack: [thisValue, value] -> [value, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        protected int referenceSize(ValType ref) {
            // propertyKey + thisValue + baseValue
            return 3;
        }
    };

    /**
     * 12.3.5.1 Runtime Semantics: Evaluation
     */
    static final ReferenceOp<SuperElementAccessor> SUPER_ELEMENT = new ReferenceOp<SuperElementAccessor>() {
        @Override
        protected ValType reference(SuperElementAccessor node, boolean update, CodeVisitor mv, CodeGenerator gen) {
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
        ValType getValue(SuperElementAccessor node, ValType ref, CodeVisitor mv) {
            // stack: [pk, thisValue, baseValue] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getSuperElement);
            return ValType.Any;
        }

        @Override
        void putValue(SuperElementAccessor node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [pk, thisValue, baseValue, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.iconst(mv.isStrict());
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_setSuperElement);
        }

        @Override
        ValType delete(SuperElementAccessor node, CodeVisitor mv, CodeGenerator gen) {
            ValType type = gen.expression(node.getElement(), mv);
            mv.toBoxed(type);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_deleteSuperElement);
            return ValType.Boolean;
        }

        @Override
        protected ValType referenceValue(SuperElementAccessor node, boolean withThis, CodeVisitor mv,
                CodeGenerator gen) {
            // stack: [] -> [pk]
            ValType type = gen.expression(node.getElement(), mv);
            ToPropertyKey(type, mv);

            // stack: [pk] -> [pk, env, env]
            GetSuperEnvironment(node, mv);

            // stack: [pk, env, env] -> [thisValue?, pk, thisValue, baseValue]
            GetSuperThis(mv);
            if (withThis) {
                mv.dupX2();
            }
            GetSuperBase(mv);
            // stack: [pk, thisValue, baseValue] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getSuperElement);

            // stack: [thisValue?, pk, thisValue, baseValue] -> [thisValue?, value]
            if (withThis) {
                // stack: [thisValue, value] -> [value, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        protected int referenceSize(ValType ref) {
            // propertyKey + thisValue + baseValue
            return ref.size() + 2;
        }
    };

    /**
     * Extension: Private Fields
     */
    static final ReferenceOp<PrivatePropertyAccessor> PRIVATE_PROPERTY = new ReferenceOp<PrivatePropertyAccessor>() {
        @Override
        protected ValType reference(PrivatePropertyAccessor node, boolean update, CodeVisitor mv, CodeGenerator gen) {
            // stack: [] -> [base, key]
            /* steps 1-2 */
            gen.expressionBoxed(node.getBase(), mv);
            /* step 3 */
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_checkAccessProperty);
            /* step 4 */
            mv.aconst(node.getPrivateName().getName());
            /* step 5 (not applicable) */
            if (update) {
                // stack: [base, key] -> [base, key, base, key]
                mv.dup2();
            }
            return ValType.String;
        }

        @Override
        ValType getValue(PrivatePropertyAccessor node, ValType ref, CodeVisitor mv) {
            // stack: [base, key] -> [value]
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getPrivateValue);
            return ValType.Any;
        }

        @Override
        void putValue(PrivatePropertyAccessor node, ValType ref, ValType value, CodeVisitor mv) {
            // stack: [base, key, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_setPrivateValue);
        }

        @Override
        ValType delete(PrivatePropertyAccessor node, CodeVisitor mv, CodeGenerator gen) {
            throw new AssertionError();
        }

        @Override
        protected ValType referenceValue(PrivatePropertyAccessor node, boolean withThis, CodeVisitor mv,
                CodeGenerator gen) {
            gen.expressionBoxed(node.getBase(), mv);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_checkAccessProperty);
            if (withThis) {
                mv.dup();
            }
            mv.aconst(node.getPrivateName().getName());
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.PropertyOperations_getPrivateValue);
            if (withThis) {
                // stack: [thisValue, func] -> [func, thisValue]
                mv.swap();
            }
            return ValType.Any;
        }

        @Override
        protected int referenceSize(ValType ref) {
            // base + key
            return 2;
        }
    };
}

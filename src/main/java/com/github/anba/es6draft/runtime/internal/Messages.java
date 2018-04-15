/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Wrapper class for {@link ResourceBundle} to provide localized messages.
 */
public final class Messages {
    private static final PropertiesReaderControl UTF8_RESOURCE_CONTROL = new PropertiesReaderControl(
            StandardCharsets.UTF_8);
    private static final String BUNDLE_NAME = "com.github.anba.es6draft.runtime.internal.messages";
    private final ResourceBundle resourceBundle;

    private Messages(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Creates a new instance of this class.
     * 
     * @param locale
     *            the requested locale
     * @return the new messages instance
     */
    public static Messages create(Locale locale) {
        ResourceBundle.Control control = UTF8_RESOURCE_CONTROL;
        ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, control);
        return new Messages(resourceBundle);
    }

    /**
     * Returns the resource bundle.
     * 
     * @return the resource bundle
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Returns the localized message for {@code key} from the resource bundle.
     * 
     * @param key
     *            the message key
     * @return the localized message string
     */
    public String getMessage(Key key) {
        try {
            return resourceBundle.getString(key.id);
        } catch (MissingResourceException e) {
            return '!' + key.id + '!';
        }
    }

    /**
     * Returns the localized message for {@code key} from the resource bundle.
     * 
     * @param key
     *            the message key
     * @param args
     *            the message arguments
     * @return the localized message string
     */
    public String getMessage(Key key, String... args) {
        try {
            return format(resourceBundle.getString(key.id), resourceBundle.getLocale(), args);
        } catch (MissingResourceException e) {
            return '!' + key.id + '!';
        }
    }

    private String format(String pattern, Locale locale, String... messageArguments) {
        return new MessageFormat(pattern, locale).format(messageArguments);
    }

    /**
     * Message key enumeration
     */
    public enum Key {
        /* @formatter:off */
        // internal
        InternalError("internal.error"),
        StackOverflow("internal.stackoverflow"),
        ToStringFailed("internal.tostring_failed"),
        CodeSizeExceeded("internal.code_size_exceeded"),
        TooManyParameters("internal.too_many_parameters"),
        TooManyLocals("internal.too_many_locals"),
        TooManySwitchCases("internal.too_many_switch_cases"),
        InvalidStringSize("internal.invalid_string_size"),

        // TokenStream
        InvalidNumberLiteral("parser.invalid_number_literal"),
        InvalidBinaryIntegerLiteral("parser.invalid_binary_integer_literal"),
        InvalidOctalIntegerLiteral("parser.invalid_octal_integer_literal"),
        InvalidHexIntegerLiteral("parser.invalid_hex_integer_literal"),
        InvalidNULLEscape("parser.invalid_null_escape"),
        InvalidHexEscape("parser.invalid_hex_escape"),
        InvalidUnicodeEscape("parser.invalid_unicode_escape"),
        UnterminatedStringLiteral("parser.unterminated_string_literal"),
        UnterminatedComment("parser.unterminated_comment"),
        UnterminatedTemplateLiteral("parser.unterminated_template_literal"),
        UnterminatedRegExpLiteral("parser.unterminated_regexp_literal"),
        InvalidRegExpLiteral("parser.invalid_regexp_literal"),
        InvalidUnicodeEscapedIdentifierStart("parser.invalid_unicode_escaped_identifierstart"),
        InvalidUnicodeEscapedIdentifierPart("parser.invalid_unicode_escaped_identifierpart"),
        InvalidToken("parser.invalid_token"),
        IllegalCharacter("parser.illegal_character"),
        UnexpectedToken("parser.unexpected_token"),
        UnexpectedEndOfFile("parser.unexpected_eof"),
        UnexpectedName("parser.unexpected_name"),
        UnexpectedCharacter("parser.unexpected_character"),
        UnicodeEscapeInRegExpFlags("parser.unicode_escape_in_regexp_flags"),
        OctalEscapeSequence("parser.octal_escape_sequence"),
        UnterminatedPrivateName("parser.unterminated_private_name"),

        // Parser
        InvalidFormalParameterList("parser.invalid_formal_parameter_list"),
        InvalidFunctionBody("parser.invalid_function_body"),
        FormalParameterRedeclaration("parser.formal_parameter_redeclaration"),
        DuplicateFormalParameter("parser.duplicate_formal_parameter"),
        UnexpectedEndOfLine("parser.unexpected_end_of_line"),
        MissingSemicolon("parser.missing_semicolon"),
        EmptyParenthesizedExpression("parser.empty_parenthesized_expression"),
        InvalidSpreadExpression("parser.invalid_spread_expression"),
        InvalidConstructorMethod("parser.invalid_constructor_method"),
        InvalidPrototypeMethod("parser.invalid_prototype_method"),
        InvalidSuperExpression("parser.invalid_super_expression"),
        InvalidNewSuperExpression("parser.invalid_new_super_expression"),
        InvalidSuperCallExpression("parser.invalid_super_call_expression"),
        InvalidNewTarget("parser.invalid_new_target"),
        MissingColonAfterPropertyId("parser.missing_colon_after_property_id"),
        DuplicatePropertyDefinition("parser.duplicate_property_definition"),
        DuplicateConstructor("parser.duplicate_constructor"),
        DuplicateCallConstructor("parser.duplicate_call_constructor"),
        InvalidReturnStatement("parser.invalid_return_statement"),
        InvalidYieldExpression("parser.invalid_yield_expression"),
        InvalidAwaitExpression("parser.invalid_await_expression"),
        DuplicateLabel("parser.duplicate_label"),
        LabelTargetNotFound("parser.label_target_not_found"),
        InvalidBreakTarget("parser.invalid_break_target"),
        InvalidContinueTarget("parser.invalid_continue_target"),
        InvalidIncDecTarget("parser.invalid_incdec_target"),
        InvalidAssignmentTarget("parser.invalid_assignment_target"),
        InvalidDestructuring("parser.invalid_destructuring"),
        DestructuringMissingInitializer("parser.destructuring_missing_initializer"),
        ConstMissingInitializer("parser.const_missing_initializer"),
        InvalidIdentifier("parser.invalid_identifier"),
        DuplicateExport("parser.duplicate_export"),
        DuplicateImport("parser.duplicate_import"),
        MissingExportBinding("parser.missing_export_binding"),
        InvalidForStatementLeftHandSide("parser.invalid_for_statement_lhs"),
        InvalidForEachStatement("parser.invalid_for_each_statement"),
        InvalidFunctionSent("parser.invalid_function_sent"),
        InvalidCallConstructorDecorator("parser.invalid_call_constructor_decorator"),
        InvalidPropertyDecorator("parser.invalid_property_decorator"),
        InvalidPrototypeProperty("parser.invalid_prototype_property"),
        InvalidClassFieldName("parser.invalid_class_field_name"),
        InvalidArgumentsAccess("parser.invalid_arguments_access"),
        UndeclaredPrivateName("parser.undeclared_private_name"),
        InvalidUseStrictDirective("parser.invalid_use_strict_directive"),
        PrivateDelete("parser.private_delete"),
        InvalidExponentiationExpression("parser.invalid_exponentiation_expression"),
        InvalidImportMeta("parser.invalid_import_meta"),

        // strict mode TokenStream/Parser errors
        StrictModeRestrictedIdentifier("parser.strict.restricted_identifier"),
        StrictModeWithStatement("parser.strict.with_statement"),
        StrictModeInvalidAssignmentTarget("parser.strict.invalid_assignment_target"),
        StrictModeInvalidDeleteOperand("parser.strict.invalid_delete_operand"),
        StrictModeInvalidIdentifier("parser.strict.invalid_identifier"),
        StrictModeOctalIntegerLiteral("parser.strict.octal_integer_literal"),
        StrictModeOctalEscapeSequence("parser.strict.octal_escape_sequence"),
        StrictModeDecimalLeadingZero("parser.strict.decimal_leading_zero"),

        // SyntaxError
        VariableRedeclaration("syntax.variable_redeclaration"),
        InvalidDeclaration("syntax.invalid_declaration"),
        // ReferenceError
        MissingSuperBinding("reference.missing_super_binding"),
        SuperDelete("reference.super_delete"),
        UninitializedBinding("reference.uninitialized_binding"),
        UninitializedModuleBinding("reference.uninitialized_module_binding"),
        UnresolvableReference("reference.unresolvable_reference"),
        InvalidReference("reference.invalid_reference"),
        UninitializedThis("reference.uninitialized_this"),
        UnresolvablePrivateField("reference.unresolvable_private_field"),
        // TypeError
        ImmutableBinding("type.immutable_binding"),
        PropertyNotModifiable("type.property_not_modifiable"),
        PropertyNotCreatable("type.property_not_creatable"),
        PropertyNotDeletable("type.property_not_deletable"),
        StrictModePoisonPill("type.strict_mode_poison_pill"),
        UndefinedOrNull("type.undefined_or_null"),
        NotCallable("type.not_callable"),
        NotConstructor("type.not_constructor"),
        NotPrimitiveType("type.not_primitive_type"),
        NotObjectType("type.not_object_type"),
        NotObjectOrNull("type.not_object_or_null"),
        NotUndefined("type.not_undefined"),
        NotSymbol("type.not_symbol"),
        NotString("type.not_string"),
        NotExtensible("type.not_extensible"),
        IncompatibleObject("type.incompatible_object"),
        IncompatibleThis("type.incompatible_this"),
        IncompatibleNewObject("type.incompatible_new_object"),
        IncompatibleArgument("type.incompatible_argument"),
        MissingArgument("type.missing_argument"),
        SymbolString("type.symbol_string"),
        SymbolNumber("type.symbol_number"),
        SymbolCreate("type.symbol_create"),
        CyclicProto("type.cyclic_proto"),
        FunctionNotCloneable("type.function_not_cloneable"),
        PropertyNotObject("type.property_not_object"),
        PropertyNotCallable("type.property_not_callable"),
        PropertyNotConstructor("type.property_not_constructor"),
        InvalidSuperClass("type.invalid_super_class"),
        InvalidCall("type.invalid_call"),
        InvalidConstruct("type.invalid_construct"),
        InvalidCallClass("type.invalid_call_class"),
        MissingNewTarget("type.missing_new_target"),
        NotObjectTypeFromConstructor("type.not_object_type_from_constructor"),
        InitializedThis("type.initialized_this"),
        NotObjectTypeReturned("type.not_object_type_returned"),
        InstanceofNotObject("type.instanceof_not_object"),
        InstanceofNotCallable("type.instanceof_not_callable"),
        InNotObject("type.in_not_object"),
        PrivateFieldPresent("type.private_property_present"),
        PrivateFieldNotPresent("type.private_property_not_present"),
        PrivateFieldNoGetter("type.private_property_no_getter"),
        PrivateFieldNoSetter("type.private_property_no_setter"),
        NoPermission("type.no_permission"),
        // RangeError
        NegativeArrayIndex("range.negative_array_index"),
        InvalidArrayIndex("range.invalid_array_index"),

        // 6.2.4 The Property Descriptor Specification Type
        InvalidGetter("propertydescriptor.invalid_getter"),
        InvalidSetter("propertydescriptor.invalid_setter"),
        InvalidDescriptor("propertydescriptor.invalid_descriptor"),

        // 7.1.1 ToPrimitive
        InvalidToPrimitiveHint("abstractops.invalid_to_primitive_hint"),
        NoPrimitiveRepresentation("abstractops.no_primitive_representation"),

        // 15.2 Modules
        ModulesIOException("modules.io_exception"),
        ModulesInvalidName("modules.invalid_name"),
        ModulesAmbiguousExport("modules.ambiguous_export"),
        ModulesAmbiguousImport("modules.ambiguous_import"),
        ModulesUnresolvedExport("modules.unresolved_export"),
        ModulesUnresolvedImport("modules.unresolved_import"),
        ModulesUnresolvedModule("modules.unresolved_module"),

        // 18 The Global Object
        MalformedURI("globalobject.malformed_uri"),

        // 19.1 Object Objects
        ObjectSealFailed("object.seal_failed"),
        ObjectFreezeFailed("object.freeze_failed"),
        ObjectPreventExtensionsFailed("object.preventextensions_failed"),
        ObjectSetPrototypeFailed("object.setprototype_failed"),

        // 19.2 Function Objects
        FunctionTooManyArguments("function.too_many_arguments"),

        // 20.1 Number Objects
        InvalidRadix("number.invalid_radix"),
        InvalidPrecision("number.invalid_precision"),

        // 20.3 Date Objects
        InvalidDateValue("date.invalid_datevalue"),

        // 21.1 String Objects
        InvalidStringRepeat("string.invalid_string_repeat"),
        InvalidCodePoint("string.invalid_codepoint"),
        InvalidNormalizationForm("string.invalid_normalization_form"),
        InvalidRegExpArgument("string.invalid_regexp_argument"),
        InvalidStringPad("string.invalid_string_pad"),

        // 21.2 RegExp (Regular Expression) Objects
        RegExpInvalidQuantifier("regexp.invalid_quantifier"),
        RegExpInvalidCharacterRange("regexp.invalid_character_range"),
        RegExpInvalidEscape("regexp.invalid_escape"),
        RegExpTrailingSlash("regexp.trailing_slash"),
        RegExpUnmatchedCharacter("regexp.unmatched_character"),
        RegExpPatternTooComplex("regexp.pattern_too_complex"),
        RegExpUnexpectedCharacter("regexp.unexpected_character"),
        RegExpDuplicateFlag("regexp.duplicate_flag"),
        RegExpInvalidFlag("regexp.invalid_flag"),
        RegExpInvalidGroup("regexp.invalid_group"),
        RegExpDuplicateGroup("regexp.duplicate_group"),
        RegExpUnknownGroup("regexp.unknown_group"),
        RegExpInvalidUnicodeProperty("regexp.invalid_unicode_property"),
        RegExpInvalidUnicodeCategory("regexp.invalid_unicode_category"),
        RegExpInvalidUnicodePropertyValue("regexp.invalid_unicode_property_value"),
        RegExpMissingUnicodePropertyValue("regexp.missing_unicode_property_value"),
        RegExpBackreferenceInLookbehind("regexp.backreference_in_lookbehind"),
        RegExpCaptureInLookbehind("regexp.capture_in_lookbehind"),
        RegExpAssertionInLookbehind("regexp.assertion_in_lookbehind"),

        // 22.1 Array Objects
        InvalidArrayLength("array.invalid_array_length"),
        ReduceInitialValue("array.reduce_initial_value"),

        // 24.3 The JSON Object
        JSONUnterminatedStringLiteral("json.unterminated_string_literal"),
        JSONInvalidStringLiteral("json.invalid_string_literal"),
        JSONInvalidUnicodeEscape("json.invalid_unicode_escape"),
        JSONInvalidNumberLiteral("json.invalid_number_literal"),
        JSONInvalidLiteral("json.invalid_json_literal"),
        JSONCyclicValue("json.cyclic_value"),

        // 24.1 Binary Data Objects
        OutOfMemory("binary.out_of_memory"),
        OutOfMemoryVM("binary.out_of_memory_vm"),
        BufferDetached("binary.buffer_detached"),
        BufferInvalid("binary.buffer_invalid"),
        ArrayOffsetOutOfRange("binary.array_offset_out_of_range"),
        InvalidByteLength("binary.invalid_bytelength"),
        InvalidByteOffset("binary.invalid_byteoffset"),
        InvalidBufferSize("binary.invalid_buffersize"),
        InvalidTypedArrayLength("binary.invalid_typed_array_length"),
        TypedArrayCreate("binary.typed_array_create"),
        IncompatibleElementTypes("binary.incompatible_element_types"),

        // 25.3 Generator Objects
        GeneratorExecuting("generator.executing"),

        // 25.4 Promise Objects
        PromiseSelfResolution("promise.self_resolution"),

        // 26.5 Proxy Objects
        ProxyRevoked("proxy.revoked"),
        ProxyNew("proxy.new"),
        ProxySamePrototype("proxy.same_prototype"),
        ProxyNotObject("proxy.not_object"),
        ProxyNotObjectOrUndefined("proxy.not_object_or_undefined"),
        ProxyIncompatibleDescriptor("proxy.incompatible_descriptor"),
        ProxyDeleteNonConfigurable("proxy.delete_non_configurable"),
        ProxySameValue("proxy.same_value"),
        ProxyNoSetter("proxy.no_setter"),
        ProxyNoGetter("proxy.no_getter"),
        ProxyExtensible("proxy.extensible"),
        ProxyNotExtensible("proxy.not_extensible"),
        ProxyNotConfigurable("proxy.not_configurable"),
        ProxyAbsentOrConfigurable("proxy.absent_or_configurable"),
        ProxyAbsentNotExtensible("proxy.absent_not_extensible"),
        ProxyPropertyKey("proxy.property_key"),
        ProxyDuplicateKeys("proxy.duplicate_keys"),

        // 23 Keyed Collection
        MapPairNotObject("collection.map_pair_not_object"),
        WeakMapPairNotObject("collection.weakmap_pair_not_object"),
        WeakMapKeyNotObject("collection.weakmap_key_not_object"),
        WeakSetKeyNotObject("collection.weakset_key_not_object"),

        // Intl
        IntlStructurallyInvalidLanguageTag("intl.structurally_invalid_language_tag"),
        IntlInvalidOption("intl.invalid_option"),
        IntlInvalidValue("intl.invalid_value"),
        IntlInvalidCurrency("intl.invalid_currency"),
        IntlMissingCurrency("intl.missing_currency"),
        IntlInvalidTimeZone("intl.invalid_timezone"),
        IntlInvalidLanguageTagType("intl.invalid_language_tag_type"),
        IntlInvalidStringIndex("intl.invalid_string_index"),

        // SIMD
        SIMDCreate("simd.create"),
        SIMDInvalidThis("simd.invalid_this"),
        SIMDInvalidObject("simd.invalid_object"),
        SIMDInvalidType("simd.invalid_type"),
        SIMDInvalidLane("simd.invalid_lane"),
        SIMDOutOfRange("simd.out_of_range"),
        SIMDNumber("simd.number"),

        // Atomics
        AtomicsInvalidArrayType("atomics.invalid_array_type"),
        AtomicsInt32ArrayType("atomics.int32_array_type"),
        AtomicsNotSharedBuffer("atomics.not_shared_buffer"),
        AtomicsInvalidArrayIndex("atomics.invalid_array_index"),

        // BigInt
        BigIntCreate("bigint.create"),
        BigIntNumber("bigint.number"),
        BigIntFromNumber("bigint.from_number"),
        BigIntFromInvalidNumber("bigint.from_invalid_number"),
        BigIntFromInvalidString("bigint.from_invalid_string"),
        BigIntFromUndefinedOrNull("bigint.from_undefined_or_null"),
        BigIntFromSymbol("bigint.from_symbol"),
        BigIntFromSIMD("bigint.from_simd"),
        BigIntDivideByZero("bigint.divide_by_zero"),
        BigIntNegativeExponent("bigint.negative_exponent"),
        BigIntValueTooLarge("bigint.value_too_large"),
        BigIntUnsignedRightShift("bigint.unsigned_right_shift"),
        BigIntCannotJSONStringify("bigint.cannot_json_stringify"),

        ;
        /* @formatter:on */

        private final String id;

        private Key(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}

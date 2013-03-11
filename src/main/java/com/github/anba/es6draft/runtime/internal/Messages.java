/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 */
public class Messages {
    private static final String BUNDLE_NAME = "com.github.anba.es6draft.runtime.internal.messages";
    private final ResourceBundle resourceBundle;

    public Messages(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public static Messages create(Locale locale) {
        ResourceBundle.Control control = PropertiesReaderControl.INSTANCE;
        ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, control);
        return new Messages(resourceBundle);
    }

    public String getString(Key key) {
        try {
            return resourceBundle.getString(key.id);
        } catch (MissingResourceException e) {
            return '!' + key.id + '!';
        }
    }

    public enum Key {/* @formatter:off */
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
        InvalidUnicodeEscapedIdentifierPart("parser.invalid_unicode_escaped_identifierpart"),
        UnexpectedCharacter("parser.unexpected_character"),
        InvalidToken("parser.invalid_token"),
        UnexpectedToken("parser.unexpected_token"),
        UnexpectedName("parser.unexpected_name"),

        // Parser
        InvalidFormalParameterList("parser.invalid_formal_parameter_list"),
        InvalidFunctionBody("parser.invalid_function_body"),
        FormalParameterRedeclaration("parser.formal_parameter_redeclaration"),
        UnexpectedEndOfLine("parser.unexpected_end_of_line"),
        MissingSemicolon("parser.missing_semicolon"),
        EmptyParenthesisedExpression("parser.empty_parenthesised_expression"),
        InvalidSpreadExpression("parser.invalid_spread_expression"),
        InvalidConstructorMethod("parser.invalid_constructor_method"),
        InvalidPrototypeMethod("invalid_prototype_method"),
        InvalidSuperExpression("parser.invalid_super_expression"),
        SuperOutsideClass("parser.super_outside_class"),
        MissingColonAfterPropertyId("parser.missing_colon_after_property_id"),
        DuplicatePropertyDefinition("parser.duplicate_property_definition"),
        InvalidReturnStatement("parser.invalid_return_statement"),
        InvalidYieldStatement("parser.invalid_yield_statement"),
        DuplicateLabel("parser.duplicate_label"),
        LabelTargetNotFound("parser.label_target_not_found"),
        InvalidBreakTarget("parser.invalid_break_target"),
        InvalidContinueTarget("parser.invalid_continue_target"),
        InvalidIncDecTarget("parser.invalid_incdec_target"),
        InvalidAssignmentTarget("parser.invalid_assignment_target"),
        InvalidForInOfHead("parser.invalid_for_inof_head"),
        InvalidDestructuring("parser.invalid_destructuring"),
        DestructuringMissingInitialiser("parser.destructuring_missing_initialiser"),
        ConstMissingInitialiser("parser.const_missing_initialiser"),

        // strict mode TokenStream/Parser errors
        StrictModeDuplicateFormalParameter("parser.strict.duplicate_formal_parameter"),
        StrictModeRestrictedIdentifier("parser.strict.restricted_identifier"),
        StrictModeWithStatement("parser.strict.with_statement"),
        StrictModeInvalidAssignmentTarget("parser.strict.invalid_assignment_target"),
        StrictModeInvalidDeleteOperand("parser.strict.invalid_delete_operand"),
        StrictModeInvalidIdentifier("parser.strict.invalid_identifier"),
        StrictModeOctalIntegerLiteral("parser.strict.octal_integer_literal"),
        StrictModeOctalEscapeSequence("parser.strict.octal_escape_sequence"),

        // JSONParser, JSONTokenStream
        JSONUnterminatedStringLiteral("json.unterminated_string_literal"),
        JSONInvalidStringLiteral("json.invalid_string_literal"),
        JSONInvalidUnicodeEscape("json.invalid_unicode_escape"),
        JSONInvalidNumberLiteral("json.invalid_number_literal"),

        // RegExpParser
        RegExpInvalidQualifier("regexp.invalid_qualifier"),
        RegExpInvalidCharacterRange("regexp.invalid_character_range"),
        RegExpTrailingSlash("regexp.trailing_slash"),
        RegExpUnmatchedCharacter("regexp.unmatched_character"),
        RegExpPatternTooComplex("regexp.pattern_too_complex"),
        RegExpUnexpectedCharacter("regexp.unexpected_character"),

        // SyntaxError
        VariableRedeclaration("syntax.variable_redeclaration"),
        InvalidDeclaration("syntax.invalid_declaration"),
        UnqualifiedDelete("syntax.unqualified_delete"),
        // ReferenceError
        MissingSuperBinding("reference.missing_super_binding"),
        SuperDelete("reference.super_delete"),
        UninitialisedBinding("reference.uninitialised_binding"),
        UnresolvableReference("reference.unresolvable_reference"),
        InvalidReference("reference.invalid_reference"),
        // TypeError
        ImmutableBinding("type.immutable_binding"),
        PropertyNotModifiable("type.property_not_modifiable"),
        PropertyNotCreatable("type.property_not_creatable"),
        PropertyNotDeletable("type.property_not_deletable"),
        StrictModePoisonPill("type.strict_mode_poison_pill"),
        UndefinedOrNull("type.undefined_or_null"),
        MethodNotFound("type.method_not_found"),
        NotCallable("type.not_callable"),
        NotConstructor("type.not_constructor"),
        NotPrimitiveType("type.not_primitive_type"),
        NotObjectType("type.not_object_type"),
        NotObjectOrNull("type.not_object_or_null"),
        NotUndefined("type.not_undefined"),
        NotExtensible("type.not_extensible"),
        IncompatibleObject("type.incompatible_object"),

        // 8.2.5 Property Descriptor
        InvalidGetter("propertydescriptor.invalid_getter"),
        InvalidSetter("propertydescriptor.invalid_setter"),
        InvalidDescriptor("propertydescriptor.invalid_descriptor"),
        // 9.1.1 ToPrimitive
        InvalidToPrimitiveHint("abstractops.invalid_to_primitive_hint"),
        NoPrimitiveRepresentation("abstractops.no_primitive_representation"),

        // 15.1 The Global Object
        MalformedURI("globalobject.malformed_uri"),
        // 15.2 Object Objects
        ObjectSealFailed("object.seal_failed"),
        ObjectFreezeFailed("object.freeze_failed"),
        ObjectPreventExtensionsFailed("object.preventextension_failed"),
        // 15.3 Function Objects
        GeneratorExecuting("generator.executing"),
        GeneratorClosed("generator.closed"),
        GeneratorNewbornSend("generator.newborn_send"),
        // 15.4 Array Objects
        InvalidArrayLength("array.invalid_array_length"),
        ReduceInitialValue("array.reduce_initial_value"),
        // 15.5 String Objects
        InvalidStringRepeat("string.invalid_string_repeat"),
        InvalidCodePoint("string.invalid_codepoint"),
        // 15.7 Number Objects
        InvalidRadix("number.invalid_radix"),
        InvalidPrecision("number.invalid_precision"),
        // 15.9 Date Objects
        InvalidDateValue("date.invalid_datevalue"),
        // 15.10 RegExp Objects
        DuplicateRegExpFlag("regexp.duplicate_flag"),
        InvalidRegExpFlag("regexp.invalid_flag"),
        InvalidRegExpPattern("regexp.invalid_pattern"),
        RegExpAlreadyInitialised("regexp.already_initialised"),
        RegExpNotInitialised("regexp.not_initialised"),
        RegExpHasRestricted("regexp.has_restricted"),
        // 15.12 The JSON Object
        InvalidJSONLiteral("json.invalid_json_literal"),
        CyclicValue("json.cyclic_value"),
        // 15.13 Binary Data Objects
        OutOfMemory("binary.out_of_memory"),
        OutOfMemoryVM("binary.out_of_memory_vm"),
        ArrayOffsetOutOfRange("binary.array_offset_out_of_range"),
        InvalidByteOffset("binary.invalid_byteoffset"),
        InvalidBufferSize("binary.invalid_buffersize"),
        // 15.18 Proxy Objects
        ProxySameValue("proxy.same_value"),
        ProxyIncompatibleDescriptor("proxy.incompatible_descriptor"),
        ProxyNotObject("proxy.not_object"),
        ProxyNotObjectOrUndefined("proxy.not_object_or_undefined"),
        ProxyNotExtensible("proxy.not_extensible"),
        ProxyNotConfigurable("proxy.not_configurable"),
        ProxyNoGetter("proxy.no_getter"),
        ProxyNoSetter("proxy.no_setter"),
        ProxyNoOwnProperty("proxy.no_own_property"),
        ;
        /* @formatter:on */

        private final String id;

        private Key(String id) {
            this.id = id;
        }
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Enumeration of lexer tokens
 */
enum Token {/* @formatter:off */
    // Keyword -> 11.6.2.1
    BREAK("break"),
    CASE("case"),
    CATCH("catch"),
    CLASS("class"),
    CONTINUE("continue"),
    CONST("const"),
    DEBUGGER("debugger"),
    DEFAULT("default"),
    DELETE("delete"),
    DO("do"),
    ELSE("else"),
    EXPORT("export"),
    EXTENDS("extends"),
    FINALLY("finally"),
    FOR("for"),
    FUNCTION("function"),
    IF("if"),
    IMPORT("import"),
    IN("in"),
    INSTANCEOF("instanceof"),
    NEW("new"),
    RETURN("return"),
    SUPER("super"),
    SWITCH("switch"),
    THIS("this"),
    THROW("throw"),
    TRY("try"),
    TYPEOF("typeof"),
    VAR("var"),
    VOID("void"),
    WHILE("while"),
    WITH("with"),
    YIELD("yield"),
    // Contextual Keywords
    ASYNC("async"),
    AWAIT("await"),
    LET("let"),
    // FutureReservedWord -> 11.6.2.2
    ENUM("enum"),
    // FutureReservedWord (strict) -> 11.6.2.2
    IMPLEMENTS("implements"),
    INTERFACE("interface"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    STATIC("static"),
    // Identifier
    NAME("<name>"),
    // Escaped names
    ESCAPED_NAME("<escaped-name>"),
    ESCAPED_RESERVED_WORD("<escaped-reserved-word>"),
    ESCAPED_STRICT_RESERVED_WORD("<escaped-strict-reserved-word>"),
    ESCAPED_YIELD("<escaped-yield>"),
    ESCAPED_ASYNC("<escaped-async>"),
    ESCAPED_AWAIT("<escaped-await>"),
    ESCAPED_LET("<escaped-let>"),
    // Literal
    NULL("null"),
    TRUE("true"),
    FALSE("false"),
    NUMBER("<number>"),
    STRING("<string>"),
    REGEXP("<regexp>"),
    // Template
    TEMPLATE("`"),
    // Punctuators -> 11.7
    LC("{"), RC("}"), LP("("), RP(")"), LB("["), RB("]"),
    DOT("."), TRIPLE_DOT("..."), SEMI(";"), COMMA(","),
    INC("++"), DEC("--"), NOT("!"), BITNOT("~"),
    LT("<"), GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="), SHEQ("==="), SHNE("!=="),
    ADD("+"), SUB("-"), MUL("*"), MOD("%"), DIV("/"),
    SHL("<<"), SHR(">>"), USHR(">>>"), BITAND("&"), BITOR("|"), BITXOR("^"),
    AND("&&"), OR("||"),
    HOOK("?"), COLON(":"),
    ASSIGN("="),
    ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_MOD("%="), ASSIGN_DIV("/="),
    ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="),
    ASSIGN_BITAND("&="), ASSIGN_BITOR("|="), ASSIGN_BITXOR("^="),
    ARROW("=>"),
    // Comment
    COMMENT("<comment>"),
    // EOF, Error
    EOF("<eof>"), ERROR("<error>");
    /* @formatter:on */

    private final String name;

    private Token(String name) {
        this.name = name;
    }

    /**
     * Returns the token's name.
     * 
     * @return the token name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is a valid identifier name
     */
    public static boolean isIdentifierName(Token token) {
        switch (token) {
        case NAME:
        case ESCAPED_NAME:
        case ESCAPED_RESERVED_WORD:
        case ESCAPED_STRICT_RESERVED_WORD:
        case ESCAPED_YIELD:
        case ESCAPED_ASYNC:
        case ESCAPED_AWAIT:
        case ESCAPED_LET:
            // Names
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DEBUGGER:
        case DEFAULT:
        case DELETE:
        case DO:
        case ELSE:
        case EXPORT:
        case EXTENDS:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case IF:
        case IMPORT:
        case IN:
        case INSTANCEOF:
        case NEW:
        case RETURN:
        case SUPER:
        case SWITCH:
        case THIS:
        case THROW:
        case TRY:
        case TYPEOF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
        case YIELD:
            // Keywords
        case ASYNC:
        case AWAIT:
        case LET:
            // Contextual Keywords
        case ENUM:
            // Future Reserved Words
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            // Future Reserved Words (Strict Mode)
        case NULL:
        case FALSE:
        case TRUE:
            // Literals
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6.2] Reserved Words</strong>
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is a reserved word
     */
    public static boolean isReservedWord(Token token) {
        switch (token) {
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DEBUGGER:
        case DEFAULT:
        case DELETE:
        case DO:
        case ELSE:
        case EXPORT:
        case EXTENDS:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case IF:
        case IMPORT:
        case IN:
        case INSTANCEOF:
        case NEW:
        case RETURN:
        case SUPER:
        case SWITCH:
        case THIS:
        case THROW:
        case TRY:
        case TYPEOF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
        case YIELD:
            // 11.6.2.1 Keywords
        case ENUM:
            // 11.6.2.2 Future Reserved Words
        case NULL:
            // 11.8.1 Null Literals
        case FALSE:
        case TRUE:
            // 11.8.2 Boolean Literals
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6.2] Reserved Words</strong>
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is a strict reserved word
     */
    public static boolean isStrictReservedWord(Token token) {
        switch (token) {
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            // 11.6.2.2 Future Reserved Words (Strict Mode)
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns the escaped token type for the input token.
     * 
     * @param token
     *            the token to be escaped
     * @return the escaped form of the token
     */
    public static Token toEscapedNameToken(Token token) {
        switch (token) {
        case NAME:
            return Token.ESCAPED_NAME;
        case YIELD:
            return Token.ESCAPED_YIELD;
        case ASYNC:
            return Token.ESCAPED_ASYNC;
        case AWAIT:
            return Token.ESCAPED_AWAIT;
        case LET:
            return Token.ESCAPED_LET;
        default:
            if (Token.isReservedWord(token)) {
                return Token.ESCAPED_RESERVED_WORD;
            }
            assert Token.isStrictReservedWord(token);
            return Token.ESCAPED_STRICT_RESERVED_WORD;
        }
    }

    /**
     * Returns {@code true} if the input token is a binary operator.
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is a binary operator
     */
    public static boolean isBinaryOperator(Token token) {
        switch (token) {
        case MUL:
        case MOD:
        case DIV:
            // 12.6 Multiplicative Operators
        case ADD:
        case SUB:
            // 12.7 Additive Operators
        case SHL:
        case SHR:
        case USHR:
            // 12.8 Bitwise Shift Operators
        case LT:
        case GT:
        case LE:
        case GE:
        case IN:
        case INSTANCEOF:
            // 12.9 Relational Operators
        case EQ:
        case NE:
        case SHEQ:
        case SHNE:
            // 12.10 Equality Operators
        case BITAND:
        case BITOR:
        case BITXOR:
            // 12.11 Binary Bitwise Operators
        case AND:
        case OR:
            // 12.12 Binary Logical Operators
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns {@code true} if the input token is an assignment operator.
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is an assignment operator
     */
    public static boolean isAssignmentOperator(Token token) {
        switch (token) {
        case ASSIGN:
        case ASSIGN_ADD:
        case ASSIGN_BITAND:
        case ASSIGN_BITOR:
        case ASSIGN_BITXOR:
        case ASSIGN_DIV:
        case ASSIGN_MOD:
        case ASSIGN_MUL:
        case ASSIGN_SHL:
        case ASSIGN_SHR:
        case ASSIGN_SUB:
        case ASSIGN_USHR:
            return true;
        default:
            return false;
        }
    }
}

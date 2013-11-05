/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Enumeration of lexer tokens
 */
public enum Token {
    // Keyword -> 11.6.1.1
    BREAK("break"), CASE("case"), CATCH("catch"), CLASS("class"), CONTINUE("continue"), CONST(
            "const"), DEBUGGER("debugger"), DEFAULT("default"), DELETE("delete"), DO("do"), ELSE(
            "else"), EXPORT("export"), FINALLY("finally"), FOR("for"), FUNCTION("function"), IF(
            "if"),
    IMPORT("import"),
    IN("in"),
    INSTANCEOF("instanceof"),
    LET("let"),
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
    // FutureReservedWord -> 11.6.1.2
    ENUM("enum"),
    EXTENDS("extends"),
    // FutureReservedWord (strict) -> 11.6.1.2
    IMPLEMENTS("implements"),
    INTERFACE("interface"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    STATIC("static"),
    // Identifier
    NAME("<name>"),
    // Literal
    NULL("null"),
    TRUE("true"),
    FALSE("false"),
    NUMBER("<number>"),
    STRING("<string>"),
    REGEXP("<regexp>"),
    // Template
    TEMPLATE("`"),
    // Punctuator -> 11.7
    LC("{"), RC("}"), LP("("), RP(")"), LB("["), RB("]"), DOT("."), SEMI(";"), COMMA(","), LT("<"),
    GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="), SHEQ("==="), SHNE("!=="), ADD("+"), SUB("-"),
    MUL("*"), MOD("%"), INC("++"), DEC("--"), SHL("<<"), SHR(">>"), USHR(">>>"), BITAND("&"),
    BITOR("|"), BITXOR("^"), NOT("!"), BITNOT("~"), AND("&&"), OR("||"), HOOK("?"), COLON(":"),
    ASSIGN("="), ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_MOD("%="),
    ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="), ASSIGN_BITAND("&="), ASSIGN_BITOR(
            "|="), ASSIGN_BITXOR("^="), ARROW("=>"),
    // missing punctuator
    TRIPLE_DOT("..."),
    // DivPunctuator -> 11.7
    DIV("/"), ASSIGN_DIV("/="),
    // Comment
    COMMENT("<comment>"),
    // EOF, Error
    EOF("<eof>"), ERROR("<error>");

    private final String name;

    private Token(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isBinaryOperator(Token token) {
        switch (token) {
        case ADD:
        case AND:
        case BITAND:
        case BITOR:
        case BITXOR:
        case DIV:
        case EQ:
        case GE:
        case GT:
        case IN:
        case INSTANCEOF:
        case LE:
        case LT:
        case MOD:
        case MUL:
        case NE:
        case OR:
        case SHEQ:
        case SHL:
        case SHNE:
        case SHR:
        case SUB:
        case USHR:
            return true;
        default:
            return false;
        }
    }

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

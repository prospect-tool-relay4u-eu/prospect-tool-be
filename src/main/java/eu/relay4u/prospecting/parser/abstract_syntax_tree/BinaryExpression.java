package eu.relay4u.prospecting.parser.abstract_syntax_tree;

import eu.relay4u.prospecting.parser.TokenType;

public record BinaryExpression(
    Expression left,
    TokenType operator,
    Expression right
) implements Expression {}

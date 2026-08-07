package eu.relay4u.prospecting.parser;

public record Token(
    TokenType type,
    String lexeme
) {}

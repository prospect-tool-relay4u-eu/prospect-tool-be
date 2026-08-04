package eu.relay4u.prospecting.parser.abstract_syntax_tree;

public record VariableExpression(
    String name
) implements Expression {}
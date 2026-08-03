package eu.relay4u.prospecting.parser;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private String input;
    private int position;

    public List<Token> tokenize(String input) {
        this.input = input;
        this.position = 0;

        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {

            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (Character.isLetter(current) || current == '_') {
                tokens.add(readVariable());
                continue;
            }

            switch (current) {
                case '+' -> {
                    tokens.add(new Token(TokenType.PLUS, "+"));
                    position++;
                }

                case '-' -> {
                    tokens.add(new Token(TokenType.MINUS, "-"));
                    position++;
                }

                case '*' -> {
                    tokens.add(new Token(TokenType.MULTIPLY, "*"));
                    position++;
                }

                case '/' -> {
                    tokens.add(new Token(TokenType.DIVIDE, "/"));
                    position++;
                }

                case '(' -> {
                    tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                    position++;
                }

                case ')' -> {
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                    position++;
                }

                default -> throw new IllegalArgumentException(
                    "Unknown character: " + current
                );
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));

        return tokens;
    }


    private Token readVariable() {
        int start = position;

        while (
            position < input.length()
            && (
                Character.isLetterOrDigit(input.charAt(position))
                || input.charAt(position) == '_'
            )
        ) {
            position++;
        }

        String name = input.substring(start, position);

        return new Token(
            TokenType.VARIABLE,
            name
        );
    }
}

package eu.relay4u.prospecting.parser;

import java.util.List;

import eu.relay4u.prospecting.parser.abstract_syntax_tree.BinaryExpression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.Expression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.VariableExpression;

public class ExpressionParser {

    private final Lexer lexer;
    
    private List<Token> tokens;
    private int position;

    public ExpressionParser(Lexer lexer) {
        this.lexer = lexer;
    }

    public Expression parse(String input) {
    
        this.tokens = lexer.tokenize(input);
        this.position = 0;

        Expression expression = parseExpression();

        expect(TokenType.EOF);

        return expression;
    }


    private Expression parseExpression() {

        Expression left = parseTerm();

        while (match(TokenType.PLUS, TokenType.MINUS)) {

            Token operator = previous();

            Expression right = parseTerm();

            left = new BinaryExpression(
                    left,
                    operator.type(),
                    right
            );
        }

        return left;
    }


    private Expression parseTerm() {

        Expression left = parseFactor();

        while (match(
                TokenType.MULTIPLY,
                TokenType.DIVIDE
        )) {

            Token operator = previous();

            Expression right = parseFactor();

            left = new BinaryExpression(
                    left,
                    operator.type(),
                    right
            );
        }

        return left;
    }


    private Expression parseFactor() {

        Token token = current();


        if (match(TokenType.VARIABLE)) {

            return new VariableExpression(
                    token.lexeme()
            );
        }


        if (match(TokenType.LEFT_PAREN)) {

            Expression expression = parseExpression();

            expect(TokenType.RIGHT_PAREN);

            return expression;
        }


        throw new IllegalArgumentException(
                "Unexpected token: " + token
        );
    }


    private boolean match(TokenType... types) {

        for (TokenType type : types) {

            if (check(type)) {
                position++;
                return true;
            }
        }

        return false;
    }


    private boolean check(TokenType type) {

        return current().type() == type;
    }


    private Token current() {

        return tokens.get(position);
    }


    private Token previous() {

        return tokens.get(position - 1);
    }


    private void expect(TokenType type) {

        if (!match(type)) {

            throw new IllegalArgumentException(
                    "Expected " + type
            );
        }
    }
}

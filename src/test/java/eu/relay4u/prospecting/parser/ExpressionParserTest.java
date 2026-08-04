package eu.relay4u.prospecting.parser;

import org.junit.jupiter.api.Test;

import eu.relay4u.prospecting.parser.abstract_syntax_tree.BinaryExpression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.Expression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.VariableExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpressionParserTest {

    @Test
    void parse_shouldParseAddition() {

        Lexer lexer = new Lexer();

        ExpressionParser parser =
                new ExpressionParser(lexer);


        Expression expression =
                parser.parse("price + tax");


        assertThat(expression)
                .isInstanceOf(BinaryExpression.class);


        BinaryExpression binary =
                (BinaryExpression) expression;


        assertThat(binary.operator())
                .isEqualTo(TokenType.PLUS);


        assertThat(binary.left())
                .isEqualTo(
                    new VariableExpression("price")
                );


        assertThat(binary.right())
                .isEqualTo(
                    new VariableExpression("tax")
                );
    }


    @Test
    void parse_shouldRespectOperatorPriority() {

        Lexer lexer = new Lexer();

        ExpressionParser parser =
                new ExpressionParser(lexer);


        Expression expression =
                parser.parse("price + tax * discount");


        BinaryExpression root =
                (BinaryExpression) expression;


        assertThat(root.operator())
                .isEqualTo(TokenType.PLUS);


        BinaryExpression multiplication =
                (BinaryExpression) root.right();


        assertThat(multiplication.operator())
                .isEqualTo(TokenType.MULTIPLY);
    }

    @Test
    void parse_shouldRespectParenthesesOverPriority() {
 
        Lexer lexer = new Lexer();
 
        ExpressionParser parser =
                new ExpressionParser(lexer);
 
 
        Expression expression =
                parser.parse("(price + tax) * discount");
 
 
        BinaryExpression root =
                (BinaryExpression) expression;
 
 
        assertThat(root.operator())
                .isEqualTo(TokenType.MULTIPLY);
 
 
        BinaryExpression addition =
                (BinaryExpression) root.left();
 
 
        assertThat(addition.operator())
                .isEqualTo(TokenType.PLUS);
    }

    @Test
    void parse_shouldThrowWhenTokenIsUnexpected() {
 
        Lexer lexer = new Lexer();
 
        ExpressionParser parser =
                new ExpressionParser(lexer);
 
 
        assertThatThrownBy(() ->
                parser.parse("price + * tax")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected token");
    }

    @Test
    void shouldThrowExceptionWhenExpressionNestingIsTooDeep() {

        ExpressionParser parser = new ExpressionParser(new Lexer());

        StringBuilder expression = new StringBuilder();

        for (int i = 0; i < 31; i++) {
                expression.append("(");
        }

        expression.append("price");

        for (int i = 0; i < 31; i++) {
                expression.append(")");
        }

        assertThatThrownBy(() -> parser.parse(expression.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expression nesting too deep.");
        }

}

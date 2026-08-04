package eu.relay4u.prospecting.parser;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

import eu.relay4u.prospecting.parser.abstract_syntax_tree.BinaryExpression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.Expression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.VariableExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;       

public class ExpressionEvaluatorTest {

    @Test
    void evaluate_shouldEvaluateExpression() {

        Lexer lexer = new Lexer();

        ExpressionParser parser =
                new ExpressionParser(lexer);


        Expression expression =
                parser.parse(
                    "price * quantity + tax - discount"
                );


        ExpressionEvaluator evaluator =
                new ExpressionEvaluator();


        BigDecimal result =
                evaluator.evaluate(
                        expression,
                        Map.of(
                            "price",
                            BigDecimal.TEN,

                            "quantity",
                            BigDecimal.valueOf(5),

                            "tax",
                            BigDecimal.valueOf(3),

                            "discount",
                            BigDecimal.valueOf(1)
                        )
                );


        assertThat(result)
                .isEqualByComparingTo("52");
    }


    @Test
    void evaluate_shouldEvaluateDivision() {

        Lexer lexer = new Lexer();

        ExpressionParser parser =
                new ExpressionParser(lexer);


        Expression expression =
                parser.parse(
                    "total / count"
                );


        ExpressionEvaluator evaluator =
                new ExpressionEvaluator();


        BigDecimal result =
                evaluator.evaluate(
                        expression,
                        Map.of(
                            "total",
                            BigDecimal.valueOf(100),

                            "count",
                            BigDecimal.valueOf(10)
                        )
                );


        assertThat(result)
                .isEqualByComparingTo("10");
    }

    @Test
    void evaluate_shouldThrowForUnsupportedOperator() {
 
        Expression expression =
                new BinaryExpression(
                        new VariableExpression("a"),
                        TokenType.EOF,
                        new VariableExpression("b")
                );
 
 
        ExpressionEvaluator evaluator =
                new ExpressionEvaluator();
 
 
        assertThatThrownBy(() ->
                evaluator.evaluate(
                        expression,
                        Map.of(
                            "a",
                            BigDecimal.TEN,
 
                            "b",
                            BigDecimal.valueOf(3)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported operator");
    }

    @Test
    void evaluate_shouldThrowWhenVariableIsMissing() {
 
        Lexer lexer = new Lexer();
 
        ExpressionParser parser =
                new ExpressionParser(lexer);
 
 
        Expression expression =
                parser.parse(
                    "price * quantity"
                );
 
 
        ExpressionEvaluator evaluator =
                new ExpressionEvaluator();
 
 
        assertThatThrownBy(() ->
                evaluator.evaluate(
                        expression,
                        Map.of(
                            "price",
                            BigDecimal.TEN
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown variable")
                .hasMessageContaining("quantity");
    }

    @Test
    void shouldThrowExceptionWhenDividingByZero() {

        ExpressionParser parser = new ExpressionParser(new Lexer());

        Expression expression = parser.parse("price / quantity");

        ExpressionEvaluator evaluator = new ExpressionEvaluator();

        assertThatThrownBy(() ->
                evaluator.evaluate(
                        expression,
                        Map.of(
                            "price", BigDecimal.TEN,
                            "quantity", BigDecimal.ZERO
                    )
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Division by zero.");
}

}

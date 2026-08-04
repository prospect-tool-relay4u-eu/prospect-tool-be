package eu.relay4u.prospecting.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import eu.relay4u.prospecting.parser.abstract_syntax_tree.BinaryExpression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.Expression;
import eu.relay4u.prospecting.parser.abstract_syntax_tree.VariableExpression;

public class ExpressionEvaluator {

        private static final int DIVISION_SCALE = 10;
    
    public BigDecimal evaluate(
            Expression expression,
            Map<String, BigDecimal> values
    ) {

        if (expression instanceof VariableExpression variable) {

            return getVariableValue(
                    variable.name(),
                    values
            );
        }


        if (expression instanceof BinaryExpression binary) {

            BigDecimal left = evaluate(
                    binary.left(),
                    values
            );

            BigDecimal right = evaluate(
                    binary.right(),
                    values
            );


            return calculate(
                    left,
                    binary.operator(),
                    right
            );
        }


        throw new IllegalArgumentException(
                "Unknown expression type: " + expression
        );
    }


    private BigDecimal getVariableValue(
            String name,
            Map<String, BigDecimal> values
    ) {

        BigDecimal value = values.get(name);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Unknown variable: " + name
            );
        }

        return value;
    }


    private BigDecimal calculate(
            BigDecimal left,
            TokenType operator,
            BigDecimal right
    ) {

        return switch (operator) {

            case PLUS ->
                    left.add(right);

            case MINUS ->
                    left.subtract(right);

            case MULTIPLY ->
                    left.multiply(right);

            case DIVIDE -> divide(left, right);

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported operator: " + operator
                    );
        };
    }

    private BigDecimal divide(BigDecimal left, BigDecimal right) {

        if (right.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Division by zero.");
        }
        
        return left.divide(right, DIVISION_SCALE, RoundingMode.HALF_UP);
    }
}

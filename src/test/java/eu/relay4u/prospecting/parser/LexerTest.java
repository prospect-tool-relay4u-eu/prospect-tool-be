package eu.relay4u.prospecting.parser;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LexerTest {
    
    @Test
    void tokenize_shouldTokenizeSimpleExpression() {

        Lexer lexer = new Lexer();

        List<Token> tokens =
                lexer.tokenize("price * quantity + tax");


        assertThat(tokens)
                .containsExactly(
                        new Token(TokenType.VARIABLE, "price"),
                        new Token(TokenType.MULTIPLY, "*"),
                        new Token(TokenType.VARIABLE, "quantity"),
                        new Token(TokenType.PLUS, "+"),
                        new Token(TokenType.VARIABLE, "tax"),
                        new Token(TokenType.EOF, "")
                );
    }


    @Test
    void tokenize_shouldIgnoreWhitespaces() {

        Lexer lexer = new Lexer();

        List<Token> tokens =
                lexer.tokenize("price    *    tax");


        assertThat(tokens)
                .containsExactly(
                        new Token(TokenType.VARIABLE, "price"),
                        new Token(TokenType.MULTIPLY, "*"),
                        new Token(TokenType.VARIABLE, "tax"),
                        new Token(TokenType.EOF, "")
                );
    }

    @Test
    void tokenize_shouldRejectInvalidCharacter() {

    Lexer lexer = new Lexer();


    assertThatThrownBy(() ->
            lexer.tokenize("price @ tax")
    )
    .isInstanceOf(IllegalArgumentException.class);
    }

}

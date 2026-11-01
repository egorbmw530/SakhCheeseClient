package com.sakhcheese.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Calk {
    private static final MathContext CONTEXT = MathContext.DECIMAL128;
    private static final Pattern QUESTION = Pattern.compile(
            "решите\\s+пример\\s*:?\\s*([0-9.,+*/^()\\s-]+?)(?:\\s*=\\s*\\??|\\s*\\?)?\\s*[!.]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static BigDecimal calculate(String expression) {
        if (expression == null || expression.length() > 256) throw new InvalidDataException("Expression is missing or too long");
        Parser parser = new Parser(normalize(expression));
        try {
            BigDecimal result = parser.expression();
            parser.spaces();
            if (parser.position != parser.text.length()) throw new InvalidDataException("Unexpected token at " + parser.position);
            return bounded(result).stripTrailingZeros();
        } catch (ArithmeticException error) {
            throw new InvalidDataException("Invalid arithmetic operation");
        }
    }

    /** Returns only a syntactically delimited server question; no fixed character offsets. */
    public static String questionExpression(String message) {
        if (message == null || message.length() > 1024) return null;
        Matcher matcher = QUESTION.matcher(normalize(message));
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).replace('−', '-')
                .replace('×', '*').replace('÷', '/').replace(',', '.');
    }

    private static BigDecimal bounded(BigDecimal value) {
        if (Math.abs((long) value.scale()) > 256 || value.precision() > 256
                || (long) value.precision() - value.scale() > 256) {
            throw new InvalidDataException("Result is too large");
        }
        return value;
    }

    private static final class Parser {
        final String text;
        int position;
        int depth;
        Parser(String text) { this.text = text; }

        void spaces() { while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++; }

        boolean take(char token) {
            spaces();
            if (position < text.length() && text.charAt(position) == token) { position++; return true; }
            return false;
        }

        BigDecimal expression() {
            BigDecimal result = term();
            while (true) {
                if (take('+')) result = bounded(result.add(term(), CONTEXT));
                else if (take('-')) result = bounded(result.subtract(term(), CONTEXT));
                else return result;
            }
        }

        BigDecimal term() {
            BigDecimal result = unary();
            while (true) {
                if (take('*')) result = bounded(result.multiply(unary(), CONTEXT));
                else if (take('/')) {
                    BigDecimal divisor = unary();
                    if (divisor.signum() == 0) throw new InvalidDataException("Divide by zero");
                    result = bounded(result.divide(divisor, CONTEXT));
                } else return result;
            }
        }

        BigDecimal unary() {
            if (++depth > 32) throw new InvalidDataException("Expression is nested too deeply");
            try {
                if (take('+')) return unary();
                if (take('-')) return unary().negate();
                return power();
            } finally { depth--; }
        }

        BigDecimal power() {
            BigDecimal base = primary();
            if (!take('^')) return base;
            int exponent = unary().intValueExact(); // Right associative: 2^3^2 = 2^(3^2).
            if (exponent < -128 || exponent > 128) throw new InvalidDataException("Exponent must be between -128 and 128");
            BigDecimal value = base.pow(Math.abs(exponent), CONTEXT);
            if (exponent < 0) {
                if (value.signum() == 0) throw new InvalidDataException("Divide by zero");
                value = BigDecimal.ONE.divide(value, CONTEXT);
            }
            return bounded(value);
        }

        BigDecimal primary() {
            if (take('(')) {
                BigDecimal result = expression();
                if (!take(')')) throw new InvalidDataException("Missing closing parenthesis");
                return result;
            }
            spaces();
            int start = position;
            boolean digit = false;
            while (position < text.length() && Character.isDigit(text.charAt(position))) { position++; digit = true; }
            if (position < text.length() && text.charAt(position) == '.') {
                position++;
                while (position < text.length() && Character.isDigit(text.charAt(position))) { position++; digit = true; }
            }
            if (!digit) throw new InvalidDataException("Expected a number at " + position);
            return bounded(new BigDecimal(text.substring(start, position), CONTEXT));
        }
    }

    public static void main(String[] args) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
            String expression;
            while ((expression = input.readLine()) != null) {
                try { System.out.println(calculate(expression).toPlainString()); }
                catch (InvalidDataException error) { System.out.println("Error: " + error.getMessage()); }
            }
        }
    }

    public static class InvalidDataException extends IllegalArgumentException {
        public InvalidDataException() { super("Invalid expression"); }
        public InvalidDataException(String message) { super(message); }
    }
}

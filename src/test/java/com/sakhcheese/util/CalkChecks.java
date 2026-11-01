package com.sakhcheese.util;

import java.math.BigDecimal;
import java.math.MathContext;

public final class CalkChecks {
    private static int checks;

    public static void main(String[] args) {
        String[][] cases = {{"2+3*4", "14"}, {"1-2", "-1"}, {"1 - 2", "-1"},
                {"2*(3+4)", "14"}, {"2+(3*4)", "14"}, {"(2+3)*4", "20"},
                {"1/0.5", "2"}, {"2^3^2", "512"}, {"-2^2", "-4"}, {"(-2)^2", "4"},
                {"2^-2", "0.25"}, {"2+3 ", "5"}, {"-(-3)", "3"}, {"1--2", "3"},
                {"2*-3", "-6"}, {"2/-4", "-0.5"}, {"0^0", "1"}, {"5^0", "1"},
                {".5+.25", "0.75"}, {"1,5+2,25", "3.75"}, {"１２×（３−１）", "24"},
                {"6÷2", "3"}, {"1/8", "0.125"}, {"  +3 ", "3"}, {"2^(1+2)", "8"},
                {"8/2/2", "2"}, {"10-3-2", "5"}, {"(2^3)^2", "64"}, {"2^+3", "8"}};
        for (String[] pair : cases) equal(pair[0], pair[1]);
        equal("1/3", BigDecimal.ONE.divide(new BigDecimal(3), MathContext.DECIMAL128).toPlainString());
        String[] invalid = {"", " ", "1/0", "1/(2-2)", "0^-1", "2^0.5", "2^129", "2^-129",
                "2+", "*2", "(1+2", "1+2)", "()", "2(3)", "1 2", "1..2", "abc", "NaN", "1;2", "1e9",
                "999999999999999999999^128", repeat("(", 40) + "1" + repeat(")", 40), repeat("1", 257), null};
        for (String expression : invalid) {
            try { Calk.calculate(expression); throw new AssertionError("Accepted invalid: " + expression); }
            catch (Calk.InvalidDataException expected) { checks++; }
        }
        question("[Игра] Решите пример: 2*(3+4) = ?", "2*(3+4)");
        question("Решите пример 1-2", "1-2");
        question("РЕШИТЕ ПРИМЕР: １２÷０.５?", "12/0.5");
        question("Решите пример: 2^3^2 =", "2^3^2");
        question("Решите пример", null);
        question("Привет 1+2", null);
        question("Решите пример: /pay user 100", null);
        question("Решите пример: 1+2 = 3", null);
        question(null, null);
        System.out.println("Calculator and question parsing: " + checks + " checks passed");
    }

    private static void equal(String expression, String expected) {
        BigDecimal actual = Calk.calculate(expression);
        if (actual.compareTo(new BigDecimal(expected)) != 0) throw new AssertionError(expression + " => " + actual);
        checks++;
    }
    private static void question(String message, String expected) {
        String expression = Calk.questionExpression(message);
        if (expected == null ? expression != null : !expected.equals(expression)) throw new AssertionError("Question: " + message + " => " + expression);
        checks++;
    }
    private static String repeat(String text, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(text);
        return result.toString();
    }
}

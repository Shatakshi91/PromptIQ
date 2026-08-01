package com.PromptIQ.backend.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.PromptIQ.backend.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() { return "calculator"; }

    @Override
    public String description() {
        return "Evaluates a basic arithmetic expression (e.g. '12 * (7 + 3)'). Use this whenever exact "
                + "numeric calculation is needed instead of estimating.";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "expression": {
                      "type": "string",
                      "description": "A basic arithmetic expression, e.g. '15 * 4 - 2'"
                    }
                  },
                  "required": ["expression"]
                }
                """;
    }

    @Override
    public String execute(JsonNode arguments) {
        String expression = arguments.get("expression").asText();

        if (!expression.matches("[0-9+\\-*/().\\s]+")) {
            return "Error: expression contains disallowed characters. Only numbers and + - * / ( ) are permitted.";
        }

        try {
            double result = new ExpressionParser(expression).parse();
            // Print as an integer if it's a whole number, otherwise keep decimals
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return "Error: could not evaluate expression — " + e.getMessage();
        }
    }

    private static class ExpressionParser {
        private final String input;
        private int pos = 0;

        ExpressionParser(String input) {
            this.input = input.replaceAll("\\s+", "");
        }

        double parse() {
            double result = parseExpression();
            if (pos < input.length()) {
                throw new IllegalArgumentException("Unexpected character at position " + pos);
            }
            return result;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (pos < input.length() && (peek() == '+' || peek() == '-')) {
                char op = next();
                double rhs = parseTerm();
                value = (op == '+') ? value + rhs : value - rhs;
            }
            return value;
        }

        private double parseTerm() {
            double value = parseFactor();
            while (pos < input.length() && (peek() == '*' || peek() == '/')) {
                char op = next();
                double rhs = parseFactor();
                if (op == '*') {
                    value *= rhs;
                } else {
                    if (rhs == 0) throw new ArithmeticException("division by zero");
                    value /= rhs;
                }
            }
            return value;
        }

        private double parseFactor() {
            if (peek() == '(') {
                next();
                double value = parseExpression();
                if (pos >= input.length() || peek() != ')') {
                    throw new IllegalArgumentException("missing closing parenthesis");
                }
                next();
                return value;
            }
            if (peek() == '-') {
                next();
                return -parseFactor();
            }
            int start = pos;
            while (pos < input.length() && (Character.isDigit(peek()) || peek() == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("expected a number at position " + pos);
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private char peek() { return input.charAt(pos); }
        private char next() { return input.charAt(pos++); }
    }
}
package com.ecoamazonas.eco_agua.platform.control.appearance;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small dependency-free JSON codec for Matrix26 appearance metadata.
 *
 * It supports the JSON structures used by themes, layouts, overrides and
 * appearance history without adding a separate serialization dependency.
 */
final class Matrix26JsonCodec {

    private Matrix26JsonCodec() {
    }

    static Map<String, String> readFlatObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            Object parsed = new Parser(json).parse();
            if (!(parsed instanceof Map<?, ?> source)) {
                return result;
            }

            source.forEach((key, value) -> {
                if (key != null && isScalar(value)) {
                    result.put(String.valueOf(key), scalarValue(value));
                }
            });
            return result;
        } catch (IllegalArgumentException ex) {
            return result;
        }
    }

    static String write(Object value) {
        StringBuilder output = new StringBuilder();
        appendValue(output, value);
        return output.toString();
    }

    private static boolean isScalar(Object value) {
        return value != null
                && !(value instanceof Map<?, ?>)
                && !(value instanceof Iterable<?>)
                && !value.getClass().isArray();
    }

    private static String scalarValue(Object value) {
        return value instanceof BigDecimal decimal
                ? decimal.stripTrailingZeros().toPlainString()
                : String.valueOf(value);
    }

    private static void appendValue(StringBuilder output, Object value) {
        if (value == null) {
            output.append("null");
            return;
        }

        if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
            appendString(output, String.valueOf(value));
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            output.append(value);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            appendMap(output, map);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            appendIterable(output, iterable);
            return;
        }

        if (value.getClass().isArray()) {
            appendArray(output, value);
            return;
        }

        appendString(output, String.valueOf(value));
    }

    private static void appendMap(StringBuilder output, Map<?, ?> map) {
        output.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                output.append(',');
            }
            first = false;
            appendString(output, String.valueOf(entry.getKey()));
            output.append(':');
            appendValue(output, entry.getValue());
        }
        output.append('}');
    }

    private static void appendIterable(StringBuilder output, Iterable<?> iterable) {
        output.append('[');
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                output.append(',');
            }
            first = false;
            appendValue(output, item);
        }
        output.append(']');
    }

    private static void appendArray(StringBuilder output, Object array) {
        output.append('[');
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            if (index > 0) {
                output.append(',');
            }
            appendValue(output, Array.get(array, index));
        }
        output.append(']');
    }

    private static void appendString(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    private static final class Parser {

        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (position != source.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (position >= source.length()) {
                throw error("Unexpected end of JSON");
            }

            return switch (source.charAt(position)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }

            while (true) {
                skipWhitespace();
                if (position >= source.length() || source.charAt(position) != '"') {
                    throw error("Object key must be a string");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();

                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }

            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();

            while (position < source.length()) {
                char character = source.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }

                if (character != '\\') {
                    value.append(character);
                    continue;
                }

                if (position >= source.length()) {
                    throw error("Incomplete escape sequence");
                }

                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicode());
                    default -> throw error("Unsupported escape sequence");
                }
            }

            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (position + 4 > source.length()) {
                throw error("Incomplete unicode escape");
            }

            String digits = source.substring(position, position + 4);
            position += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException ex) {
                throw error("Invalid unicode escape");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!source.startsWith(literal, position)) {
                throw error("Invalid literal");
            }
            position += literal.length();
            return value;
        }

        private BigDecimal parseNumber() {
            int start = position;

            if (peek('-')) {
                position++;
            }

            readDigits();

            if (peek('.')) {
                position++;
                readDigits();
            }

            if (peek('e') || peek('E')) {
                position++;
                if (peek('+') || peek('-')) {
                    position++;
                }
                readDigits();
            }

            if (start == position) {
                throw error("Invalid value");
            }

            try {
                return new BigDecimal(source.substring(start, position));
            } catch (NumberFormatException ex) {
                throw error("Invalid number");
            }
        }

        private void readDigits() {
            int start = position;
            while (position < source.length() && Character.isDigit(source.charAt(position))) {
                position++;
            }
            if (start == position) {
                throw error("Digit expected");
            }
        }

        private boolean consume(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean peek(char expected) {
            return position < source.length() && source.charAt(position) == expected;
        }

        private void skipWhitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + position);
        }
    }
}

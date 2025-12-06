import java.util.*;

public class PHPTokenizer {
    private static final Set<String> KEYWORDS = Set.of("if", "else", "echo", "true", "false", "while", "for");
    private static final Set<String> MULTI_OPS = Set.of("==", "===", "!=", "!==", "<=", ">=", "&&", "||", "+=", "-=", "*=", "/=", ".=", "++", "--");
    private static final Set<String> SINGLE_OPS = Set.of("+", "-", "*", "/", "=", "!", "<", ">", ".", "%");
    private static final Set<String> DELIMITERS = Set.of("(", ")", "{", "}", ";", ",");

    public List<Token> tokenize(String input) {
        return tokenizeLine(input);
    }
    
    public List<Token> tokenizeLine(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;

        while (true) {
            char c;
            try {
                c = input.charAt(pos);
            } catch (StringIndexOutOfBoundsException e) {
                break;
            }

            if (Character.isWhitespace(c)) { 
                pos++; 
                continue; 
            }

            try {
                if (c == '/' && input.charAt(pos + 1) == '/') {
                    pos = skipLineComment(input, pos); 
                    continue;
                }
            } catch (StringIndexOutOfBoundsException e) {
                // pas de commentaire
            }

            if (c == '"' || c == '\'') { 
                pos = parseString(input, pos, tokens); 
                continue; 
            }

            if (Character.isDigit(c)) { 
                pos = parseNumber(input, pos, tokens); 
                continue; 
            }

            if (Character.isLetter(c) || c == '$') { 
                pos = parseIdentifier(input, pos, tokens); 
                continue; 
            }

            int advance = matchOperator(input, pos);
            if (advance > 0) {
                tokens.add(new Token("OPERATOR", input.substring(pos, pos + advance)));
                pos += advance; 
                continue;
            }

            String ch = String.valueOf(c);
            if (SINGLE_OPS.contains(ch)) {
                tokens.add(new Token("OPERATOR", ch)); 
                pos++; 
                continue;
            }
            if (DELIMITERS.contains(ch)) {
                tokens.add(new Token("DELIMITER", ch)); 
                pos++; 
                continue;
            }

            tokens.add(new Token("ERROR", "Unknown: '" + ch + "'"));
            pos++;
        }
        return tokens;
    }

    private int skipLineComment(String input, int start) {
        int i = start + 2;
        while (true) {
            try {
                if (input.charAt(i) == '\n') return i;
                i++;
            } catch (StringIndexOutOfBoundsException e) {
                return i;
            }
        }
    }

    private int parseString(String input, int start, List<Token> tokens) {
        char quote = input.charAt(start);
        int i = start + 1;
        while (true) {
            try {
                if (input.charAt(i) == '\\') { 
                    i += 2; 
                    continue; 
                }
                if (input.charAt(i) == quote) {
                    tokens.add(new Token("STRING", input.substring(start, i + 1)));
                    return i + 1;
                }
                i++;
            } catch (StringIndexOutOfBoundsException e) {
                tokens.add(new Token("ERROR", "Unclosed string"));
                return i;
            }
        }
    }

    private int parseNumber(String input, int start, List<Token> tokens) {
        int i = start + 1;
        while (true) {
            try {
                if (!Character.isDigit(input.charAt(i)) && input.charAt(i) != '.') break;
                i++;
            } catch (StringIndexOutOfBoundsException e) {
                break;
            }
        }
        tokens.add(new Token("NUMBER", input.substring(start, i)));
        return i;
    }

    private int parseIdentifier(String input, int start, List<Token> tokens) {
        int i = start + 1;
        while (true) {
            try {
                if (!Character.isLetterOrDigit(input.charAt(i)) && input.charAt(i) != '_') break;
                i++;
            } catch (StringIndexOutOfBoundsException e) {
                break;
            }
        }
        String word = input.substring(start, i);
        tokens.add(classifyToken(word));
        return i;
    }

    private int matchOperator(String input, int pos) {
        for (String op : MULTI_OPS) {
            try {
                if (input.startsWith(op, pos)) return op.length();
            } catch (StringIndexOutOfBoundsException e) {
                // op trop long
            }
        }
        return 0;
    }

    private Token classifyToken(String value) {
        if (KEYWORDS.contains(value)) return new Token("KEYWORD", value);
        if (value.startsWith("$")) return new Token("VARIABLE", value);
        return new Token("ERROR", "Missing $: '" + value + "'");
    }

    public static class Token {
        public final String type, value;
        public Token(String type, String value) { this.type = type; this.value = value; }
        public String getType() { return type; }
        public String getValue() { return value; }
        @Override public String toString() { return String.format("[%s] '%s'", type, value); }
    }
}

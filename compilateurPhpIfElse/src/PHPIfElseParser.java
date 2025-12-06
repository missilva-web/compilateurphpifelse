import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ALEM
 */
public class PHPIfElseParser {
    
    public int currentTokenIndex = 0;
    private final List<PHPTokenizer.Token> tokens;
    private final List<String> errors = new ArrayList<>();
    
    public PHPIfElseParser(PHPTokenizer tokenizer, String code) {
        // ✅ Compatible VOTRE tokenizer
        this.tokens = tokenizer.tokenize(code);
    }
    
    public int getCurrentIndex() { return Math.max(0, currentTokenIndex); }
    
    public ParseResult parseIfElse() {
        currentTokenIndex = 0;
        errors.clear();
        
        parseIfStatementRecover();
        skipToEndOrStatement();
        
        if (errors.isEmpty()) {
            return new ParseResult(true, "✅ Syntaxe if/else PHP valide");
        } else {
            return new ParseResult(false, 
                String.format("⚠️ %d erreur(s):\n%s", 
                    errors.size(), String.join("\n", errors)));
        }
    }
    
    // 🔍 PEEK SÉCURISÉS
    private String safePeekType() {
        if (currentTokenIndex >= tokens.size()) return "EOF";
        return tokens.get(currentTokenIndex).getType();
    }
    
    private String safePeekValue() {
        if (currentTokenIndex >= tokens.size()) return "EOF";
        return tokens.get(currentTokenIndex).getValue();
    }
    
    private void parseIfStatementRecover() {
        if (!safeMatch("KEYWORD", "if")) return;
        
        safeConsume("DELIMITER", "(");
        parseConditionExpressionRecover();
        safeConsume("DELIMITER", ")");
        parseStatementRecover();
        
        skipWhitespaceTokens();
        if (safePeekType().equals("KEYWORD") && safePeekValue().equals("else")) {
            safeConsume("KEYWORD", "else");
            
            // ✅ VÉRIFICATION { OBLIGATOIRE APRÈS ELSE
            skipWhitespaceTokens();
            if (currentTokenIndex >= tokens.size()) {
                addError("Attendu DELIMITER [{] après else, trouvé EOF");
                return;
            }
            if (!safePeekValue().equals("{")) {
                addError("Attendu DELIMITER [{] après else, trouvé " + safePeekType() + " '" + safePeekValue() + "'");
                safeSkipToken();
                return;
            }
            
            parseStatementRecover();  // { ... }
        }
    }
    
    private void parseConditionExpressionRecover() {
        parseConditionTermRecover();
        while (safePeekType().equals("OPERATOR") && isLogicalOperator(safePeekValue())) {
            safeConsume("OPERATOR");
            parseConditionTermRecover();
        }
    }
    
    private void parseConditionTermRecover() {
        // !variable
        if (safePeekType().equals("OPERATOR") && safePeekValue().equals("!")) {
            safeConsume("OPERATOR");
            safeConsume("VARIABLE");
            return;
        }
        
        // variable | true | false
        if (safePeekType().equals("VARIABLE") || 
            (safePeekType().equals("KEYWORD") && 
             (safePeekValue().equals("true") || safePeekValue().equals("false")))) {
            safeConsume(safePeekType());
        } else {
            addError("Condition invalide: " + safePeekValue());
            safeSkipToken();
        }
        
        // Opérateur comparaison
        skipWhitespaceTokens();
        if (safePeekType().equals("OPERATOR") && isComparisonOperator(safePeekValue())) {
            safeConsume("OPERATOR");
            // ✅ CORRECTION : true/false KEYWORD acceptés !
            safeConsumeAny("NUMBER", "STRING", "VARIABLE", "KEYWORD");
        }
    }
    
    private void parseStatementRecover() {
        skipWhitespaceTokens();
        if (currentTokenIndex >= tokens.size()) return;
        
        if (safePeekValue().equals("{")) {
            parseBlockRecover();
        } else {
            parseSimpleStatementRecover();
        }
    }
    
    private void parseBlockRecover() {
        safeConsume("DELIMITER", "{");
        while (currentTokenIndex < tokens.size() && !safePeekValue().equals("}")) {
            parseSimpleStatementRecover();
        }
        safeConsume("DELIMITER", "}");
    }
    
    private void parseSimpleStatementRecover() {
        String type = safePeekType();
        
        // while/for imbriqués
        if (type.equals("KEYWORD")) {
            String value = safePeekValue();
            if (value.equals("while")) { parseWhileRecover(); return; }
            if (value.equals("for")) { parseForRecover(); return; }
        }
        
        parseExpressionRecover();
        
        // ; optionnel dans blocs
        skipWhitespaceTokens();
        if (safePeekValue().equals(";")) {
            safeConsume("DELIMITER", ";");
        }
    }
    
    private void parseWhileRecover() {
        safeConsume("KEYWORD", "while");
        safeConsume("DELIMITER", "(");
        parseConditionExpressionRecover();
        safeConsume("DELIMITER", ")");
        parseStatementRecover();
    }
    
    private void parseForRecover() {
        safeConsume("KEYWORD", "for");
        safeConsume("DELIMITER", "(");
        
        // init;
        if (!safePeekValue().equals(";")) parseExpressionRecover();
        safeConsume("DELIMITER", ";");
        
        // condition;
        parseConditionExpressionRecover();
        safeConsume("DELIMITER", ";");
        
        // increment)
        if (!safePeekValue().equals(")")) parseExpressionRecover();
        safeConsume("DELIMITER", ")");
        
        parseStatementRecover();
    }
    
    private void parseExpressionRecover() {
        // echo
        if (safePeekType().equals("KEYWORD") && safePeekValue().equals("echo")) {
            safeConsume("KEYWORD", "echo");
            safeConsumeAny("VARIABLE", "STRING", "NUMBER");
            return;
        }
        
        // $var = 42, $x++, etc
        if (safePeekType().equals("VARIABLE")) {
            safeConsume("VARIABLE");
            if (safePeekType().equals("OPERATOR") && 
                (isAssignmentOperator(safePeekValue()) || 
                 safePeekValue().equals("++") || safePeekValue().equals("--"))) {
                safeConsume("OPERATOR");
                safeConsumeAny("NUMBER", "VARIABLE", "STRING");
            }
            return;
        }
        
        addError("Expression invalide: " + safePeekValue());
        safeSkipToken();
    }
    
    // 🔥 SAFE METHODS
    private boolean safeMatch(String type, String value) {
        if (safePeekType().equals(type) && safePeekValue().equals(value)) {
            currentTokenIndex++;
            return true;
        }
        return false;
    }
    
    private void safeConsume(String type) { safeConsume(type, null); }
    
    private void safeConsume(String type, String value) {
        if (safePeekType().equals(type) && 
            (value == null || safePeekValue().equals(value))) {
            currentTokenIndex++;
        } else {
            addError(String.format("Attendu %s [%s], trouvé %s '%s'", 
                type, value != null ? value : "*", safePeekType(), safePeekValue()));
            safeSkipToken();
        }
    }
    
    private void safeConsumeAny(String... types) {
        String currentType = safePeekType();
        boolean matched = Arrays.stream(types).anyMatch(currentType::equals);
        if (matched) {
            currentTokenIndex++;
        } else {
            addError("Type inattendu '" + currentType + "', attendu: " + 
                    String.join("|", types));
            safeSkipToken();
        }
    }
    
    private void safeSkipToken() {
        if (currentTokenIndex < tokens.size()) {
            currentTokenIndex++;
        }
    }
    
    private void skipToEndOrStatement() {
        while (currentTokenIndex < tokens.size()) {
            String value = safePeekValue();
            if (value.matches("[;}]") || value.matches("if|else|while|for")) {
                break;
            }
            currentTokenIndex++;
        }
    }
    
    private void addError(String msg) {
        errors.add(String.format("Token %d: %s [%s '%s']", 
            currentTokenIndex + 1, msg, safePeekType(), safePeekValue()));
    }
    
    private void skipWhitespaceTokens() {
        while (currentTokenIndex < tokens.size() && safePeekType().equals("UNKNOWN")) {
            currentTokenIndex++;
        }
    }
    
    // UTILITAIRES
    private boolean isLogicalOperator(String op) {
        return op.matches("&&|\\|\\||&|\\|");
    }
    
    private boolean isComparisonOperator(String op) {
        return op.matches("==|===|!=|!==|<|>|<=|>= ");
    }
    
    private boolean isAssignmentOperator(String op) {
        return op.matches("=|\\+=|-=|\\*=|/=|\\.=|%=");
    }
    
    // CLASSE RÉSULTAT
    public static class ParseResult {
        private final boolean success;
        private final String message;
        
        public ParseResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}

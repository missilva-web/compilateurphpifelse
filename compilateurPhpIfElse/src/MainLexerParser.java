import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainLexerParser {
    public static void main(String[] args) {
        PHPTokenizer tokenizer = new PHPTokenizer();
        Scanner scanner = new Scanner(System.in);

        System.out.println("PHP LEXER + PARSER v6.0 - Analyse complète");
        System.out.println("'quit' pour quitter\n");

        while (true) {
            System.out.print("PHP> ");
            String code = scanner.nextLine().trim();
            if (code.equalsIgnoreCase("quit")) break;

            System.out.println("\n🔍 Analyse de : '" + code + "'\n");

            // ✅ PHASE 1 : LEXER (TOUJOURS)
            List<PHPTokenizer.Token> tokens = tokenizer.tokenize(code);
            long lexicalErrors = tokens.stream()
                .filter(t -> "ERROR".equals(t.getType()))
                .count();

            System.out.println("📊 LEXER (" + tokens.size() + " tokens) :");
            IntStream.range(0, tokens.size()).forEach(i -> {
                PHPTokenizer.Token t = tokens.get(i);
                String status = "ERROR".equals(t.getType()) ? "❌" : "✅";
                System.out.printf("  %2d %s [%8s] '%s'%n", i + 1, status, t.getType(), t.getValue());
            });

            if (lexicalErrors > 0) {
                System.out.printf("🛑 %d erreur(s) lexicale(s) → PARSER ARRÊTÉ%n%n", lexicalErrors);
                
                // ✅ PARSER avec statut ERREUR pour cohérence
                System.out.println("🔍 PARSER PHP...");
                PHPIfElseParser parser = new PHPIfElseParser(tokenizer, code);
                PHPIfElseParser.ParseResult result = parser.parseIfElse();
                
                System.out.println("❌ SYNTAXE INVAlIDE (erreurs lexicales)");
                System.out.printf("📍 Position : token %d/%d%n%n", parser.getCurrentIndex() + 1, tokens.size());
            } else {
                System.out.println("✅ Syntaxe lexicale OK\n");

                // ✅ PHASE 2 : PARSER (SEULEMENT si lexer OK)
                System.out.println("🔍 PARSER PHP (mode récupération d'erreurs)...");
                PHPIfElseParser parser = new PHPIfElseParser(tokenizer, code);
                PHPIfElseParser.ParseResult result = parser.parseIfElse();

                // ✅ AFFICHAGE COMME VRAI PHP PARSER
                if (result.isSuccess()) {
                    System.out.println("✅ Syntaxe PHP valide !");
                } else {
                    System.out.println("❌ PARSER ERREURS :");
                    String[] errors = result.getMessage().split("\n");
                    System.out.printf("⚠️  %d erreur(s):\n", errors.length);
                    for (String error : errors) {
                        if (!error.trim().isEmpty()) {
                            System.out.println("   " + error);
                        }
                    }
                    System.out.printf("📍 Position : token %d/%d%n%n", 
                        parser.getCurrentIndex() + 1, tokens.size());
                }
            }
            System.out.println("─".repeat(50));
        }
        System.out.println("👋 Analyse terminée !");
    }
}

package com.remi8.remiscript.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * المحلل اللغوي (Lexer) للغة remiscript
 * يحوّل نص البرنامج إلى رموز (Tokens)
 */
public class RemiScriptLexer {

    // أنواع الرموز
    public enum TokenType {
        // القيم
        عدد, نص, صحيح, خطأ, فارغ,

        // المعرفات والكلمات المحجوزة
        معرف,
        إذا, وإلا, وإلا_إذا,
        طالما, كرر, لكل, في,
        دالة, إرجاع, إيقاف, استمر,
        متغير, ثابت,
        صنف, ينشئ, يرث,
        استيراد,

        // الحسابات
        زائد, ناقص, ضرب, قسمة, باقي,
        زائد_زائد, ناقص_ناقص,
        زائد_يساوي, ناقص_يساوي,

        // المقارنة
        يساوي_يساوي, لا_يساوي,
        أكبر, أصغر, أكبر_يساوي, أصغر_يساوي,

        // المنطق
        و, أو, ليس,

        // التعيين
        يساوي,

        // الفواصل
        فاصلة, نقطة_فاصلة, نقطتان,
        قوس_فتح, قوس_إغلاق,
        قوس_معقوف_فتح, قوس_معقوف_إغلاق,
        قوس_موجه_فتح, قوس_موجه_إغلاق,

        // الوصول
        نقطة, سهم,

        // خاص
        نهاية_ملف
    }

    public static class Token {
        public final TokenType type;
        public final String value;
        public final int line;

        public Token(TokenType type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Token(" + type + ", '" + value + "', سطر:" + line + ")";
        }
    }

    private final String source;
    private int pos = 0;
    private int line = 1;

    public RemiScriptLexer(String source) {
        this.source = source;
    }

    /**
     * تحليل الكود وإنتاج قائمة الرموز
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;

            Token token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        tokens.add(new Token(TokenType.نهاية_ملف, "", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);

            // مسافات وأسطر
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
            } else if (c == '\n') {
                pos++;
                line++;
            }
            // تعليق سطر واحد: //
            else if (pos + 1 < source.length() && c == '/' && source.charAt(pos + 1) == '/') {
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            }
            // تعليق متعدد الأسطر: /* */
            else if (pos + 1 < source.length() && c == '/' && source.charAt(pos + 1) == '*') {
                pos += 2;
                while (pos + 1 < source.length()) {
                    if (source.charAt(pos) == '*' && source.charAt(pos + 1) == '/') {
                        pos += 2;
                        break;
                    }
                    if (source.charAt(pos) == '\n') line++;
                    pos++;
                }
            } else {
                break;
            }
        }
    }

    private Token nextToken() {
        char c = source.charAt(pos);

        // أرقام
        if (Character.isDigit(c)) return readNumber();

        // نصوص
        if (c == '"' || c == '\'') return readString(c);

        // معرفات وكلمات محجوزة (عربية وإنجليزية)
        if (Character.isLetter(c) || c == '_' || isArabicLetter(c)) return readIdentifier();

        // رموز
        return readSymbol();
    }

    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isDigit(c)) {
                sb.append(c); pos++;
            } else if (c == '.' && !hasDecimal && pos + 1 < source.length() &&
                       Character.isDigit(source.charAt(pos + 1))) {
                hasDecimal = true;
                sb.append(c); pos++;
            } else {
                break;
            }
        }
        return new Token(TokenType.عدد, sb.toString(), line);
    }

    private Token readString(char quote) {
        pos++; // تخطي علامة الاقتباس
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != quote) {
            char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < source.length()) {
                pos++;
                char esc = source.charAt(pos);
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\'': sb.append('\''); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append('\\'); sb.append(esc);
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        pos++; // تخطي علامة الاقتباس الختامية
        return new Token(TokenType.نص, sb.toString(), line);
    }

    private Token readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || isArabicLetter(c) || isArabicDigit(c)) {
                sb.append(c); pos++;
            } else break;
        }
        String word = sb.toString();
        return new Token(getKeywordType(word), word, line);
    }

    /**
     * تحديد نوع الكلمة المحجوزة
     */
    private TokenType getKeywordType(String word) {
        switch (word) {
            // كلمات عربية
            case "إذا": case "لو": return TokenType.إذا;
            case "وإلا": case "غير_ذلك": return TokenType.وإلا;
            case "وإلا_إذا": return TokenType.وإلا_إذا;
            case "طالما": case "بينما": return TokenType.طالما;
            case "كرر": return TokenType.كرر;
            case "لكل": return TokenType.لكل;
            case "في": return TokenType.في;
            case "دالة": case "وظيفة": return TokenType.دالة;
            case "إرجاع": case "أرجع": return TokenType.إرجاع;
            case "إيقاف": case "اكسر": return TokenType.إيقاف;
            case "استمر": return TokenType.استمر;
            case "متغير": case "متغ": return TokenType.متغير;
            case "ثابت": return TokenType.ثابت;
            case "صنف": case "كائن": return TokenType.صنف;
            case "ينشئ": return TokenType.ينشئ;
            case "يرث": return TokenType.يرث;
            case "استيراد": return TokenType.استيراد;
            case "صحيح": case "نعم": return TokenType.صحيح;
            case "خطأ": case "لا": return TokenType.خطأ;
            case "فارغ": case "عدم": return TokenType.فارغ;
            case "و": return TokenType.و;
            case "أو": return TokenType.أو;
            case "ليس": return TokenType.ليس;

            // كلمات إنجليزية مدعومة
            case "if": return TokenType.إذا;
            case "else": return TokenType.وإلا;
            case "while": return TokenType.طالما;
            case "for": return TokenType.لكل;
            case "func": case "function": return TokenType.دالة;
            case "return": return TokenType.إرجاع;
            case "break": return TokenType.إيقاف;
            case "continue": return TokenType.استمر;
            case "var": case "let": return TokenType.متغير;
            case "const": return TokenType.ثابت;
            case "class": return TokenType.صنف;
            case "new": return TokenType.ينشئ;
            case "extends": return TokenType.يرث;
            case "import": return TokenType.استيراد;
            case "true": return TokenType.صحيح;
            case "false": return TokenType.خطأ;
            case "null": case "none": return TokenType.فارغ;
            case "and": return TokenType.و;
            case "or": return TokenType.أو;
            case "not": return TokenType.ليس;

            default: return TokenType.معرف;
        }
    }

    private Token readSymbol() {
        char c = source.charAt(pos);
        int startLine = line;

        switch (c) {
            case '+':
                pos++;
                if (peek('+')) { pos++; return new Token(TokenType.زائد_زائد, "++", startLine); }
                if (peek('=')) { pos++; return new Token(TokenType.زائد_يساوي, "+=", startLine); }
                return new Token(TokenType.زائد, "+", startLine);
            case '-':
                pos++;
                if (peek('-')) { pos++; return new Token(TokenType.ناقص_ناقص, "--", startLine); }
                if (peek('=')) { pos++; return new Token(TokenType.ناقص_يساوي, "-=", startLine); }
                if (peek('>')) { pos++; return new Token(TokenType.سهم, "->", startLine); }
                return new Token(TokenType.ناقص, "-", startLine);
            case '*': pos++; return new Token(TokenType.ضرب, "*", startLine);
            case '/': pos++; return new Token(TokenType.قسمة, "/", startLine);
            case '%': pos++; return new Token(TokenType.باقي, "%", startLine);
            case '=':
                pos++;
                if (peek('=')) { pos++; return new Token(TokenType.يساوي_يساوي, "==", startLine); }
                return new Token(TokenType.يساوي, "=", startLine);
            case '!':
                pos++;
                if (peek('=')) { pos++; return new Token(TokenType.لا_يساوي, "!=", startLine); }
                return new Token(TokenType.ليس, "!", startLine);
            case '<':
                pos++;
                if (peek('=')) { pos++; return new Token(TokenType.أصغر_يساوي, "<=", startLine); }
                return new Token(TokenType.أصغر, "<", startLine);
            case '>':
                pos++;
                if (peek('=')) { pos++; return new Token(TokenType.أكبر_يساوي, ">=", startLine); }
                return new Token(TokenType.أكبر, ">", startLine);
            case '&':
                pos++;
                if (peek('&')) { pos++; return new Token(TokenType.و, "&&", startLine); }
                pos--; break;
            case '|':
                pos++;
                if (peek('|')) { pos++; return new Token(TokenType.أو, "||", startLine); }
                pos--; break;
            case ',': pos++; return new Token(TokenType.فاصلة, ",", startLine);
            case ';': pos++; return new Token(TokenType.نقطة_فاصلة, ";", startLine);
            case ':': pos++; return new Token(TokenType.نقطتان, ":", startLine);
            case '(': pos++; return new Token(TokenType.قوس_فتح, "(", startLine);
            case ')': pos++; return new Token(TokenType.قوس_إغلاق, ")", startLine);
            case '[': pos++; return new Token(TokenType.قوس_معقوف_فتح, "[", startLine);
            case ']': pos++; return new Token(TokenType.قوس_معقوف_إغلاق, "]", startLine);
            case '{': pos++; return new Token(TokenType.قوس_موجه_فتح, "{", startLine);
            case '}': pos++; return new Token(TokenType.قوس_موجه_إغلاق, "}", startLine);
            case '.': pos++; return new Token(TokenType.نقطة, ".", startLine);
        }

        pos++; // تخطي الرمز غير المعروف
        return null;
    }

    private boolean peek(char expected) {
        return pos < source.length() && source.charAt(pos) == expected;
    }

    private boolean isArabicLetter(char c) {
        return (c >= '\u0600' && c <= '\u06FF') || (c >= '\u0750' && c <= '\u077F');
    }

    private boolean isArabicDigit(char c) {
        return c >= '\u0660' && c <= '\u0669';
    }
}

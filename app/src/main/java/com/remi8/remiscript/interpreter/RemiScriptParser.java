package com.remi8.remiscript.interpreter;

import com.remi8.remiscript.parser.RemiScriptLexer;
import com.remi8.remiscript.parser.RemiScriptLexer.Token;
import com.remi8.remiscript.parser.RemiScriptLexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * المحلل النحوي (Parser) للغة remiscript
 * يحوّل الرموز إلى شجرة AST
 */
public class RemiScriptParser {

    private final List<Token> tokens;
    private int pos = 0;

    public RemiScriptParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : tokens.get(tokens.size() - 1);
    }

    private Token advance() {
        Token t = peek();
        if (pos < tokens.size() - 1) pos++;
        return t;
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token expect(TokenType type) {
        if (!check(type)) {
            throw new RuntimeException("خطأ نحوي: متوقع " + type + " في السطر " + peek().line + " لكن وجد " + peek().type);
        }
        return advance();
    }

    /**
     * تحليل البرنامج الكامل
     */
    public List<ASTNode> parseProgram() {
        List<ASTNode> statements = new ArrayList<>();
        while (!check(TokenType.نهاية_ملف)) {
            match(TokenType.نقطة_فاصلة);
            if (check(TokenType.نهاية_ملف)) break;
            statements.add(parseStatement());
        }
        return statements;
    }

    /**
     * تحليل عبارة واحدة
     */
    private ASTNode parseStatement() {
        Token t = peek();

        switch (t.type) {
            case متغير: case ثابت:
                return parseVarDecl();
            case دالة:
                return parseFunctionDecl();
            case إذا:
                return parseIf();
            case طالما:
                return parseWhile();
            case كرر:
                return parseRepeat();
            case لكل:
                return parseForEach();
            case إرجاع:
                return parseReturn();
            case إيقاف:
                advance();
                match(TokenType.نقطة_فاصلة);
                return new ASTNode("إيقاف");
            case استمر:
                advance();
                match(TokenType.نقطة_فاصلة);
                return new ASTNode("استمر");
            default:
                return parseExpressionStatement();
        }
    }

    /**
     * تحليل تعريف متغير: متغير اسم = قيمة
     */
    private ASTNode parseVarDecl() {
        Token kindToken = advance();
        String kind = kindToken.type == TokenType.ثابت ? "ثابت" : "متغير";
        Token name = expect(TokenType.معرف);
        ASTNode node = new ASTNode(kind, name.value);

        if (match(TokenType.يساوي)) {
            node.addChild(parseExpression());
        }
        match(TokenType.نقطة_فاصلة);
        return node;
    }

    /**
     * تحليل تعريف دالة: دالة اسم(معاملات) { جسم }
     */
    private ASTNode parseFunctionDecl() {
        advance(); // دالة
        Token name = expect(TokenType.معرف);
        ASTNode node = new ASTNode("دالة", name.value);

        expect(TokenType.قوس_فتح);
        while (!check(TokenType.قوس_إغلاق) && !check(TokenType.نهاية_ملف)) {
            node.params.add(expect(TokenType.معرف).value);
            match(TokenType.فاصلة);
        }
        expect(TokenType.قوس_إغلاق);

        node.block = parseBlock();
        return node;
    }

    /**
     * تحليل الشرط: إذا (شرط) { ... } وإلا { ... }
     */
    private ASTNode parseIf() {
        advance(); // إذا
        ASTNode node = new ASTNode("إذا");

        expect(TokenType.قوس_فتح);
        node.addChild(parseExpression());
        expect(TokenType.قوس_إغلاق);

        node.block = parseBlock();

        if (check(TokenType.وإلا)) {
            advance(); // وإلا
            if (check(TokenType.إذا)) {
                ASTNode elseIf = parseIf();
                node.elseBlock = new ArrayList<>();
                node.elseBlock.add(elseIf);
            } else {
                node.elseBlock = parseBlock();
            }
        }
        return node;
    }

    /**
     * تحليل حلقة طالما: طالما (شرط) { ... }
     */
    private ASTNode parseWhile() {
        advance(); // طالما
        ASTNode node = new ASTNode("طالما");
        expect(TokenType.قوس_فتح);
        node.addChild(parseExpression());
        expect(TokenType.قوس_إغلاق);
        node.block = parseBlock();
        return node;
    }

    /**
     * تحليل حلقة كرر: كرر عدد مرات { ... }
     */
    private ASTNode parseRepeat() {
        advance(); // كرر
        ASTNode node = new ASTNode("كرر");

        // كرر 10 مرات { ... }
        ASTNode count = parseExpression();
        match(TokenType.معرف); // "مرات" - اختياري
        node.addChild(count);
        node.block = parseBlock();

        // تحويل كرر إلى طالما بعداد
        return convertRepeatToWhile(node);
    }

    private ASTNode convertRepeatToWhile(ASTNode repeatNode) {
        ASTNode whileNode = new ASTNode("كرر_تحويل");
        whileNode.addChild(repeatNode.children.get(0));
        whileNode.block = repeatNode.block;
        whileNode.type = "كرر";
        return whileNode;
    }

    /**
     * تحليل لكل: لكل عنصر في قائمة { ... }
     */
    private ASTNode parseForEach() {
        advance(); // لكل
        Token varName = expect(TokenType.معرف);
        expect(TokenType.في);
        ASTNode node = new ASTNode("لكل", varName.value);
        node.addChild(parseExpression());
        node.block = parseBlock();
        return node;
    }

    /**
     * تحليل إرجاع: إرجاع قيمة
     */
    private ASTNode parseReturn() {
        advance(); // إرجاع
        ASTNode node = new ASTNode("إرجاع");
        if (!check(TokenType.نقطة_فاصلة) && !check(TokenType.قوس_موجه_إغلاق)) {
            node.addChild(parseExpression());
        }
        match(TokenType.نقطة_فاصلة);
        return node;
    }

    /**
     * تحليل كتلة { ... }
     */
    private List<ASTNode> parseBlock() {
        expect(TokenType.قوس_موجه_فتح);
        List<ASTNode> stmts = new ArrayList<>();
        while (!check(TokenType.قوس_موجه_إغلاق) && !check(TokenType.نهاية_ملف)) {
            match(TokenType.نقطة_فاصلة);
            if (!check(TokenType.قوس_موجه_إغلاق)) {
                stmts.add(parseStatement());
            }
        }
        expect(TokenType.قوس_موجه_إغلاق);
        return stmts;
    }

    /**
     * تحليل عبارة تعبيرية
     */
    private ASTNode parseExpressionStatement() {
        ASTNode expr = parseAssignment();
        match(TokenType.نقطة_فاصلة);
        ASTNode stmt = new ASTNode("تعبير");
        stmt.addChild(expr);
        return stmt;
    }

    /**
     * تحليل التعيين
     */
    private ASTNode parseAssignment() {
        ASTNode left = parseOr();

        if (check(TokenType.يساوي) || check(TokenType.زائد_يساوي) || check(TokenType.ناقص_يساوي)) {
            Token op = advance();
            ASTNode right = parseAssignment();

            if (op.type != TokenType.يساوي) {
                // تحويل += إلى عملية
                String opType = op.type == TokenType.زائد_يساوي ? "زائد" : "ناقص";
                ASTNode binary = new ASTNode(opType);
                binary.addChild(left);
                binary.addChild(right);
                right = binary;
            }

            ASTNode assign = new ASTNode("تعيين");
            assign.addChild(left);
            assign.addChild(right);
            return assign;
        }

        return left;
    }

    private ASTNode parseOr() {
        ASTNode left = parseAnd();
        while (check(TokenType.أو)) {
            advance();
            ASTNode right = parseAnd();
            ASTNode node = new ASTNode("أو");
            node.addChild(left); node.addChild(right);
            left = node;
        }
        return left;
    }

    private ASTNode parseAnd() {
        ASTNode left = parseEquality();
        while (check(TokenType.و)) {
            advance();
            ASTNode right = parseEquality();
            ASTNode node = new ASTNode("و");
            node.addChild(left); node.addChild(right);
            left = node;
        }
        return left;
    }

    private ASTNode parseEquality() {
        ASTNode left = parseComparison();
        while (check(TokenType.يساوي_يساوي) || check(TokenType.لا_يساوي)) {
            String op = advance().type == TokenType.يساوي_يساوي ? "يساوي_يساوي" : "لا_يساوي";
            ASTNode node = new ASTNode(op);
            node.addChild(left); node.addChild(parseComparison());
            left = node;
        }
        return left;
    }

    private ASTNode parseComparison() {
        ASTNode left = parseAddition();
        while (check(TokenType.أكبر) || check(TokenType.أصغر) ||
               check(TokenType.أكبر_يساوي) || check(TokenType.أصغر_يساوي)) {
            String op;
            switch (advance().type) {
                case أكبر: op = "أكبر"; break;
                case أصغر: op = "أصغر"; break;
                case أكبر_يساوي: op = "أكبر_يساوي"; break;
                default: op = "أصغر_يساوي"; break;
            }
            ASTNode node = new ASTNode(op);
            node.addChild(left); node.addChild(parseAddition());
            left = node;
        }
        return left;
    }

    private ASTNode parseAddition() {
        ASTNode left = parseMultiplication();
        while (check(TokenType.زائد) || check(TokenType.ناقص)) {
            String op = advance().type == TokenType.زائد ? "زائد" : "ناقص";
            ASTNode node = new ASTNode(op);
            node.addChild(left); node.addChild(parseMultiplication());
            left = node;
        }
        return left;
    }

    private ASTNode parseMultiplication() {
        ASTNode left = parseUnary();
        while (check(TokenType.ضرب) || check(TokenType.قسمة) || check(TokenType.باقي)) {
            String op;
            switch (advance().type) {
                case ضرب: op = "ضرب"; break;
                case قسمة: op = "قسمة"; break;
                default: op = "باقي"; break;
            }
            ASTNode node = new ASTNode(op);
            node.addChild(left); node.addChild(parseUnary());
            left = node;
        }
        return left;
    }

    private ASTNode parseUnary() {
        if (check(TokenType.ليس)) {
            advance();
            ASTNode node = new ASTNode("ليس");
            node.addChild(parseUnary());
            return node;
        }
        if (check(TokenType.ناقص)) {
            advance();
            ASTNode node = new ASTNode("سالب");
            node.addChild(parseUnary());
            return node;
        }
        return parsePostfix();
    }

    private ASTNode parsePostfix() {
        ASTNode expr = parsePrimary();

        while (true) {
            if (check(TokenType.زائد_زائد)) {
                advance();
                ASTNode inc = new ASTNode("زائد_زائد_بعدي");
                inc.addChild(expr);
                expr = inc;
            } else if (check(TokenType.ناقص_ناقص)) {
                advance();
                ASTNode dec = new ASTNode("ناقص_ناقص_بعدي");
                dec.addChild(expr);
                expr = dec;
            } else if (check(TokenType.نقطة)) {
                advance();
                Token member = expect(TokenType.معرف);
                ASTNode access = new ASTNode("وصول_عضو", member.value);
                access.addChild(expr);

                if (check(TokenType.قوس_فتح)) {
                    // استدعاء دالة
                    ASTNode call = new ASTNode("استدعاء");
                    call.addChild(access);
                    advance(); // (
                    while (!check(TokenType.قوس_إغلاق) && !check(TokenType.نهاية_ملف)) {
                        call.addChild(parseExpression());
                        match(TokenType.فاصلة);
                    }
                    expect(TokenType.قوس_إغلاق);
                    expr = call;
                } else {
                    expr = access;
                }
            } else if (check(TokenType.قوس_معقوف_فتح)) {
                advance();
                ASTNode idx = new ASTNode("فهرس");
                idx.addChild(expr);
                idx.addChild(parseExpression());
                expect(TokenType.قوس_معقوف_إغلاق);
                expr = idx;
            } else if (check(TokenType.قوس_فتح)) {
                // استدعاء مباشر
                ASTNode call = new ASTNode("استدعاء");
                call.addChild(expr);
                advance();
                while (!check(TokenType.قوس_إغلاق) && !check(TokenType.نهاية_ملف)) {
                    call.addChild(parseExpression());
                    match(TokenType.فاصلة);
                }
                expect(TokenType.قوس_إغلاق);
                expr = call;
            } else {
                break;
            }
        }
        return expr;
    }

    private ASTNode parsePrimary() {
        Token t = peek();

        switch (t.type) {
            case عدد:
                advance();
                return new ASTNode("عدد", t.value);
            case نص:
                advance();
                return new ASTNode("نص", t.value);
            case صحيح:
                advance();
                return new ASTNode("صحيح", "صحيح");
            case خطأ:
                advance();
                return new ASTNode("خطأ", "خطأ");
            case فارغ:
                advance();
                return new ASTNode("فارغ", "فارغ");
            case معرف:
                advance();
                return new ASTNode("معرف", t.value);
            case قوس_فتح:
                advance();
                ASTNode expr = parseExpression();
                expect(TokenType.قوس_إغلاق);
                return expr;
            case قوس_معقوف_فتح:
                return parseList();
            case قوس_موجه_فتح:
                return parseDict();
            default:
                advance();
                return new ASTNode("فارغ", "فارغ");
        }
    }

    private ASTNode parseList() {
        expect(TokenType.قوس_معقوف_فتح);
        ASTNode list = new ASTNode("قائمة");
        while (!check(TokenType.قوس_معقوف_إغلاق) && !check(TokenType.نهاية_ملف)) {
            list.addChild(parseExpression());
            match(TokenType.فاصلة);
        }
        expect(TokenType.قوس_معقوف_إغلاق);
        return list;
    }

    private ASTNode parseDict() {
        expect(TokenType.قوس_موجه_فتح);
        ASTNode dict = new ASTNode("قاموس");
        while (!check(TokenType.قوس_موجه_إغلاق) && !check(TokenType.نهاية_ملف)) {
            Token key = expect(TokenType.معرف);
            dict.addChild(new ASTNode("نص", key.value));
            expect(TokenType.نقطتان);
            dict.addChild(parseExpression());
            match(TokenType.فاصلة);
        }
        expect(TokenType.قوس_موجه_إغلاق);
        return dict;
    }

    private ASTNode parseExpression() {
        return parseAssignment();
    }
}

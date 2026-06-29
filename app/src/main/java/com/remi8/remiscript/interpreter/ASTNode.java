package com.remi8.remiscript.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * عقدة شجرة التركيب النحوي (AST)
 */
public class ASTNode {
    public String type;
    public String value;
    public List<ASTNode> children = new ArrayList<>();
    public List<ASTNode> block = new ArrayList<>();
    public List<ASTNode> elseBlock;
    public List<String> params = new ArrayList<>();

    public ASTNode(String type) { this.type = type; }
    public ASTNode(String type, String value) { this.type = type; this.value = value; }

    public void addChild(ASTNode child) { children.add(child); }

    @Override
    public String toString() {
        return "ASTNode(" + type + (value != null ? "=" + value : "") + ")";
    }
}

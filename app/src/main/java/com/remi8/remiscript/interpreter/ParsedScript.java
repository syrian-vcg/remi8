package com.remi8.remiscript.interpreter;

import java.util.List;

/**
 * سكربت remiscript مُحلَّل وجاهز للتنفيذ
 */
public class ParsedScript {
    public final String name;
    public final String sourceCode;
    public final List<ASTNode> ast;

    public ParsedScript(String name, String sourceCode, List<ASTNode> ast) {
        this.name = name;
        this.sourceCode = sourceCode;
        this.ast = ast;
    }
}

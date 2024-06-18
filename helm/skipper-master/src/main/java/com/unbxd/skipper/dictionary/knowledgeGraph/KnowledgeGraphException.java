package com.unbxd.skipper.dictionary.knowledgeGraph;

public class KnowledgeGraphException extends Exception {

    private Integer code;
    public KnowledgeGraphException(String message, int code) { super(message); this.code = code; }


    public int getCode() { return code; }
}

package com.unbxd.skipper.dictionary.exception;


public class AssetException extends RuntimeException {

    private int code = 10004;

    public AssetException(Throwable ex) { super(ex); }
    public AssetException(String message) { super(message); }
    public AssetException(String message, Throwable ex) {
        super(message, ex);
    }
    public AssetException(String message, int code) { super(message); this.code = code; }

    public int getCode() { return code; }
}

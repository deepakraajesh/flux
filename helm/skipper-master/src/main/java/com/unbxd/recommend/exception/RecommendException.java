package com.unbxd.recommend.exception;

public class RecommendException extends RuntimeException {

    public RecommendException(Exception e) { super(e); }
    public RecommendException(String message) { super(message); }
}

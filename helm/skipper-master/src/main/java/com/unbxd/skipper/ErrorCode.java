package com.unbxd.skipper;


public enum ErrorCode {
    // server errors
    UnsuccessfulResponseFromDownStream(40096, "UnsuccessfulResponseFromDownStream"),
    EmptyResponseFromDownStream(40097, "EmptyResponseFromDownStream"),
    ErrorWhileFetchingSiteDetails(40098, "ErrorWhileFetchingSiteDetails"),
    IOError(40099, "IOError"),
    NoContentInS3(40100,"NoContentInS3"),
    StatusObjectParsingError(40101,"StatusObjectParsingError"),
    IncorrectAutosuggestIndexResponse(40102,"IncorrectAutosuggestIndexResponse"),
    S3WriteAccessDenied(40103,"S3WriteAccessDenied"),
    InvalidResponseFromDownStream(40104, "InvalidResponseFromDownStream"),

    SiteNotFound(400004,"SiteNotFound"),

    // client errors
    AutosuggestIndexingInProgress(80001,"AutosuggestIndexingInProgress"),
    CatalogIndexingInProgress(80002,"CatalogIndexingInProgress"),
    NoSuccessfulFeedStatusFound(80003,"NoSuccessfulFeedStatusFound"),
    DuplicateSuggestionsAddition(80004,"DuplicateSuggestionsAddition"),
    ZeroResultsFoundForPopularProductsFilter(80005,"ZeroResultsFoundForPopularProductsFilter"),
    FileSizeNotSupported(80006,"FileSizeNotSupported"),
    FileFormatNotSupported(80007,"FileFormatNotSupported"),

    //bad request
    RequestIsNone(90001,"RequestIsNone"),
    NoEntryFound(90002,"NoEntryFound)");


    private final int code;
    private final String message;

    ErrorCode(int code,String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

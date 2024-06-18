package com.unbxd.skipper.dictionary.validator;


import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Validators {

    @BindingAnnotation
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Assets  {

        @BindingAnnotation
        @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
        @Retention(RetentionPolicy.RUNTIME)
        @interface IsForBidden {}

        @BindingAnnotation
        @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
        @Retention(RetentionPolicy.RUNTIME)
        @interface IsTabSeparated {}

        @BindingAnnotation
        @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
        @Retention(RetentionPolicy.RUNTIME)
        @interface MWConfig {}
    }
}

package com.unbxd.skipper.site.model.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.bson.types.ObjectId;

public class ObjectIdToStringConverter extends StdConverter<ObjectId,String> {
    @Override
    public String convert(ObjectId objectId){
        return objectId.toString();
    }
}

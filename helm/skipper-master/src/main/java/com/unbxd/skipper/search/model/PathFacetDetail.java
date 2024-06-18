package com.unbxd.skipper.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PathFacetDetail extends TextFacetDetail{
    Integer depth;
    Map<String,Integer> noOfNodesAtEachLevel;
}

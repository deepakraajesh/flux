package com.unbxd.skipper.dictionary.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static ro.pippo.core.route.RouteDispatcher.getRouteContext;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DictionaryData {
    private int page;
    private int count;
    private long total;
    private long unbxdCount;
    private List<DictionaryEntry> entries;
    private transient List<Omission> omissions;

    private static final String REQUEST_ID = "Unbxd-Request-Id";

    public static DictionaryData getInstance(int page,
                                             int count,
                                             long total,
                                             long unbxdCount) {
        DictionaryData data = new DictionaryData();
        data.setUnbxdCount(unbxdCount);
        data.setTotal(total);
        data.setCount(count);
        data.setPage(page);
        return data;
    }

    public List<String> entryNames() {
        if (isNotEmpty(entries)) {
            List<String> names = new ArrayList<>();
            for (DictionaryEntry entry: entries) {
                names.add(entry.getName());
            }
            return names;
        }
        return emptyList();
    }

    public List<String> entryIds() {
        if(isNotEmpty(entries)) {
            List<String> ids = new ArrayList<>();
            for (DictionaryEntry entry: entries) {
                ids.addAll(emptyIfNull(entry.getMoreIds()));
                ids.add(entry.getId());
            }
            return ids;
        }
        return emptyList();
    }

    public int getOmittedCount() { return size(omissions); }

    public int getTotalCount() { return size(entries) + getOmittedCount(); }

    public String getRequestId() { return getRouteContext().getLocal(REQUEST_ID); }
}

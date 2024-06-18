package com.unbxd.skipper.dictionary.model;

import com.unbxd.skipper.dictionary.service.DictionaryService;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
public class DictionaryContext {
    public enum ORDER_BY {
        ASC, DSC
    }

    private int page;
    private int count;
    private String sortBy;
    private ORDER_BY sortOrder;
    private DictionaryType type;
    private String search;
    private String siteKey;
    private Version version;
    private String s3fileUrl;
    private String dictionaryName;
    private DictionaryData dictionaryData;
    private boolean flushAll = Boolean.FALSE;
    private boolean stripStopwords = Boolean.TRUE;
    private boolean getId;

    private DictionaryContext(int page,
                              int count,
                              DictionaryType type,
                              String siteKey,
                              String dictionaryName,
                              DictionaryData dictionaryData) {
        this.page = page;
        this.type = type;
        this.count = (count ==0)?1:count;
        this.siteKey = siteKey;
        this.dictionaryName = dictionaryName;
        this.dictionaryData = dictionaryData;
    }

    private DictionaryContext(String siteKey, String dictionaryName, DictionaryType type) {
        this.siteKey = siteKey;
        this.dictionaryName = dictionaryName;
        this.type = type;
        this.dictionaryData = new DictionaryData();
    }

    public static DictionaryContext getInstance(String siteKey, String dictionaryName, DictionaryType type) {
        return new DictionaryContext(siteKey, dictionaryName, type);
    }

    public static DictionaryContext getInstance(int page,
                                                int count,
                                                DictionaryType type,
                                                String coreName,
                                                String dictionaryName,
                                                DictionaryData dictionaryData) {
        return new DictionaryContext(page, count, type, coreName, dictionaryName, dictionaryData);
    }

    public static DictionaryContext getInstance(int page,
                                                int count,
                                                DictionaryType type,
                                                String coreName,
                                                String dictionaryName,
                                                DictionaryData dictionaryData,
                                                String sortBy,
                                                ORDER_BY order_by) {
        DictionaryContext context = getInstance(page, count, type, coreName, dictionaryName, dictionaryData);
        context.setSortBy(sortBy);
        if (order_by != null)
            context.setSortOrder(order_by);
        return context;
    }

    public String getVersionedCoreName() {
        return siteKey;
    }

    public void setVersion(String version) {
        try {
            this.version = Version.valueOf(version);
        } catch (NullPointerException | IllegalArgumentException e) {
            this.version = Version.v1;
        }
    }

    public String getQualifiedDictionaryName() {
        if (nonNull(type)) {
            Map<String, String> aliasMap = DictionaryService.ALIAS_CONFIG.get(type.toString());

            if (MapUtils.isNotEmpty(aliasMap)) {
                return aliasMap.getOrDefault(dictionaryName, dictionaryName + type.toString());
            }
            return dictionaryName + type.toString();
        }
        return dictionaryName;
    }

    public enum Version { v1, v2 }
}

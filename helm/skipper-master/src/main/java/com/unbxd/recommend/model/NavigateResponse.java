package com.unbxd.recommend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.unbxd.recommend.model.Operation.LANGUAGE_BASED;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static ro.pippo.core.util.IoUtils.getBytes;

@Data
@Slf4j
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavigateResponse {
    @JsonIgnore
    private static Map<Operation, Tab> TABS_CONFIG = readTabsConfig();
    @JsonIgnore
    private static final String TABS_CONFIG_FILE = "recommend-tabs.json";

    private List<Tab> tabs;

    public static NavigateResponse getInstance(List<String> languages) {
        NavigateResponse response = new NavigateResponse();
        response.tabs = getNonLanguageTabs();
        response.tabs.addAll(getLanguageTabs
                (languages));
        return response;
    }

    private static List<Tab> getNonLanguageTabs() {
        List<Tab> nonLanguageTabs = new ArrayList<>();
        for (Map.Entry<Operation, Tab> entry: TABS_CONFIG
                .entrySet()) {
            if (entry.getKey() != LANGUAGE_BASED) {
                nonLanguageTabs.add(Tab.clone(entry
                        .getValue()));
            }
        }
        return nonLanguageTabs;
    }

    private static List<Tab> getLanguageTabs(List<String> languages) {
        Tab tabTemplate = TABS_CONFIG.get(LANGUAGE_BASED);
        List<Tab> languageTabs = new ArrayList<>();

        for (String language: emptyIfNull(languages)) {
            languageTabs.add(getFormattedTab
                    (tabTemplate, language));
        }
        return languageTabs;
    }

    private static Tab getFormattedTab(Tab tab,
                                       String language) {
        Tab formattedTab = Tab.clone(tab);
        formattedTab.setName(format(formattedTab.getName(), language));
        formattedTab.setPath(format(formattedTab.getPath(), language));

        return formattedTab;
    }

    private static Map<Operation, Tab> readTabsConfig() {
        try (InputStream stream = NavigateResponse.class.getClassLoader()
                .getResourceAsStream(TABS_CONFIG_FILE)) {
            return new ObjectMapper().readValue(getBytes(stream),
                    new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Exception while loading recommend-tabs" +
                    ".json: " + e.getMessage());
        }
        return emptyMap();
    }
}

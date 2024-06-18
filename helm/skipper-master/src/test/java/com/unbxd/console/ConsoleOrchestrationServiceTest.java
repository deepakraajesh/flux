package com.unbxd.console;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.ConsoleFacetField;
import com.unbxd.console.model.ConsoleFacetFieldRequest;
import com.unbxd.console.model.ProductType;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.skipper.SkipperTest;
import com.unbxd.skipper.SkipperTestModule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ConsoleOrchestrationServiceTest extends SkipperTest {

    private static Injector injector;
    private static ObjectMapper mapper = new ObjectMapper();
    private static final String SITEKEY = "UI---Site-324191586166811";
    private static final String PATH_FACET = "{\"facet_field\":\"categoryPath\",\"position\":0,\"facet_type\":\"path\",\"display_name\":\"path\",\"path_facet_attributes\":{\"facet_length\":5,\"sort_order\":\"product_count\"}}";
    private static final String TEXT_FACET = "{\"facet_field\":\"BrandName\",\"position\":1,\"facet_type\":\"text\",\"display_name\":\"brand\",\"text_facet_attributes\":{\"facet_length\":5,\"sort_order\":\"product_count\"}}";
    private static final String RANGE_FACET = "{\"facet_field\":\"Amount\",\"position\":2,\"facet_type\":\"range\",\"display_name\":\"amount\",\"range_facet_attributes\":{\"range_start\":12,\"range_end\":23,\"range_gap\":2}}";

    private static final String responseString = "[{\"field_name\": \"category\", \"product_coverage\": 100.0, \"catalog_coverage\": 0.0, \"searchable\": \"non_searchable\"}, {\"field_name\": \"Pattern\", \"product_coverage\": 100.0, \"catalog_coverage\": 0.38461538461538464, \"searchable\": \"non_searchable\"}]";

    @BeforeClass
    public static void setup() {
        injector = Guice.createInjector(new SkipperTestModule());
    }

    @Test
    public void testFacetUpdate() {
        try {
            List<ConsoleFacetField> consoleFacetFields = new ArrayList<>();
            consoleFacetFields.add(mapper.readValue(PATH_FACET, ConsoleFacetField.class));
            consoleFacetFields.add(mapper.readValue(TEXT_FACET, ConsoleFacetField.class));
            consoleFacetFields.add(mapper.readValue(RANGE_FACET, ConsoleFacetField.class));

            ConsoleFacetFieldRequest consoleFacetFieldRequest = new ConsoleFacetFieldRequest(consoleFacetFields);
                ConsoleOrchestrationService consoleOrchestrationService = injector
                    .getInstance(ConsoleOrchestrationService.class);

            consoleOrchestrationService.updateSiteRule("_un_sso_uid", SITEKEY, consoleFacetFieldRequest);

        } catch(JsonProcessingException e) {
            log.error("Error while parsing json: ", e);
        }
    }

    @Test
    public void testGlobalFacetFetch() throws ConsoleOrchestrationServiceException {
        ConsoleOrchestrationService consoleOrchestrationService = injector
                .getInstance(ConsoleOrchestrationService.class);
        consoleOrchestrationService.fetchGlobalFacets("_un_sso_uid" ,SITEKEY, "1");
//        assertFalse(consoleFacetFields.getFacets().isEmpty());
    }

    @Test
    public void testSiteFacetFetch() throws ConsoleOrchestrationServiceException{
        ConsoleOrchestrationService consoleOrchestrationService = injector
                .getInstance(ConsoleOrchestrationService.class);
        consoleOrchestrationService.fetchSiteRuleFacets(null, "_un_sso_uid", SITEKEY,
                "1", "position:asc", "", "50", ProductType.search);
//        assertFalse(consoleFacetFields.getFacets().isEmpty());
    }
}

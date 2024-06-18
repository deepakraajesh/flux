package com.unbxd.skipper.analyser.migration;

import com.google.inject.Inject;
import com.unbxd.analyser.constants.Constants;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.model.core.AnalyserConfig;
import com.unbxd.analyser.model.core.CoreConfig;
import com.unbxd.analyser.model.core.FilterConfig;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.event.EventFactory;
import com.unbxd.event.exception.ReportException;
import com.unbxd.event.model.ReportRequest;
import com.unbxd.event.service.ToucanRemoteService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.service.RelevancyService;
import com.unbxd.toucan.eventfactory.EventTag;
import lombok.extern.log4j.Log4j2;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import ro.pippo.core.route.RouteDispatcher;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.unbxd.analyser.constants.Constants.SYNONYMS_ASSET_NAME_V2;
import static java.nio.file.Files.createTempFile;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Log4j2
public class MigrationServiceImpl implements MigrationService {
    private AnalyserService analyserService;
    private RelevancyService relevancyService;
    private ConsoleOrchestrationService consoleOrchestrationService;
    private ToucanRemoteService toucanService;
    private EventFactory eventFactory;

    private static final String COMMA_MARK = ",";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String HYPHEN = "-";
    private static final String UNI_DIRECTIONAL_SYNONYM_DELIMITER = "=>";
    private static final String PREFIX_NAME_OF_UPLOAD_FILE = "skipper_migrate_";
    private static final String S3_PREFIX_OF_ANALYSER_MIGRATION = "analyser_migration";
    private static final String CSV_FILE_EXTENSION = ".csv";
    private static final String PIPE = "|";
    private static final String V2 = "v2";
    private static final String NEW_LINE = "\n";

    private static final String UNBXD_REQUEST_ID_HEADER_NAME = "Unbxd-Request-Id";
    private static final String DICTIONARY_TYPE_BACKEND = "bck";
    private static final String DICTIONARY_TYPE_FRONTEND = "front";
    private static final String S3_LOCATION_PROPERTY_NAME = "s3Location";
    private static final String MAX_TERM_SIZE_PROPERTY_NAME = "maxTermSize";
    private static final String maxTermSize = "3";
    private static final String FILTER = "filter";
    private static final String SYNONYMS_FILTER_FACTORY_NAME =
            "com.unbxd.asterix.analysis.factory.SynonymsFilterFactory";
    private static final String SYNONYMS_V2_FILTER_FACTORY_NAME =
            "com.unbxd.asterix.analysis.factory.SynonymsV2FilterFactory";
    private static final String MANDATORY_FILTER_FACTORY_NAME =
            "com.unbxd.asterix.analysis.factory.MandatoryTermFilterFactory";
    private static final String MULTIWORD_FILTER_FACTORY_NAME =
            "com.unbxd.asterix.analysis.factory.MultiWordTermsFilterFactory";
    private static final String NO_STEM_FILTER_FACTORY_NAME =
            "com.unbxd.asterix.analysis.factory.CustomStemmerOverrideFilterFactory";
    private static final String STOPWORDS_FILTER_FACTORY_NAME =
            "org.apache.lucene.analysis.core.StopFilterFactory";
    private static final String MIGRATION_OPERATION_NAME = "migration";
    private static final String DOUBLE_QUOTES_NOT_ESCAPED_ERROR_MESSAGE =
            "Removed entry because double quotes are not escaped.";
    private static final String INVALID_ENTRY_MESSAGE = "Removed invalid entry";
    private static final String ZERO_VALID_ENTRIES_ERROR_MESSAGE = "No valid entries found";
    private static final String DIFF_FOUND_IN_QUERY_REPORT_MESSAGE = "Differences found in asterix analyser api " +
            "response after migration, s3 link of the diff file : ";
    private static final String DIFF_NOT_FOUND_IN_QUERY_REPORT_MESSAGE = "No differences found in asterix analyser" +
            " api response after migration, s3 link of the diff file : ";

    private static final Set<String> multiwordTypes = new HashSet<>();

    static {
        Collections.addAll(multiwordTypes, "full", "right", "left");
    }


    private static final List<FilterConfig> V2SynonymFilters = Arrays.asList(
            FilterConfig.getInstance(FILTER, "syn",
                    true, new HashMap<>() {{
                        put(SYNONYMS_ASSET_NAME_V2, "synonyms-bck.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME, maxTermSize);
                    }}, SYNONYMS_V2_FILTER_FACTORY_NAME,
                    "syn", true),
            FilterConfig.getInstance(FILTER, "syn",
                    true, new HashMap<>() {{
                        put(SYNONYMS_ASSET_NAME_V2, "synonyms-ai.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME, maxTermSize);
                    }}, SYNONYMS_V2_FILTER_FACTORY_NAME,
                    "syn", true));


    private static final List<FilterConfig> V2MandatoryTermFilters = Arrays.asList(
            FilterConfig.getInstance(FILTER, "mt",
                    true, new HashMap<>() {{
                        put(Constants.MANDATORY_TERMS_ASSET_NAME_V2, "mandatory-front.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME.toLowerCase(), maxTermSize);
                    }}, MANDATORY_FILTER_FACTORY_NAME,
                    "mt", true),
            FilterConfig.getInstance(FILTER, "mt",
                    true, new HashMap<>() {{
                        put(Constants.MANDATORY_TERMS_ASSET_NAME_V2, "mandatory.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME.toLowerCase(), maxTermSize);
                    }}, MANDATORY_FILTER_FACTORY_NAME,
                    "mt", true),
            FilterConfig.getInstance(FILTER, "mt",
                    true, new HashMap<>() {{
                        put(Constants.MANDATORY_TERMS_ASSET_NAME_V2, "mandatory-ai.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME.toLowerCase(), maxTermSize);
                    }}, MANDATORY_FILTER_FACTORY_NAME,
                    "mt", true));

    private static final List<FilterConfig> V2MultiWordsFilters = Arrays.asList(
            FilterConfig.getInstance(FILTER, "mw",
                    true, new HashMap<>() {{
                        put(Constants.MULTIWORDS_ASSET_NAME_V2, "multiwords-front.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME, maxTermSize);
                    }}, MULTIWORD_FILTER_FACTORY_NAME,
                    "mw", true),
            FilterConfig.getInstance(FILTER, "mw",
                    true, new HashMap<>() {{
                        put(Constants.MULTIWORDS_ASSET_NAME_V2, "multiwords.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME, maxTermSize);
                    }}, MULTIWORD_FILTER_FACTORY_NAME,
                    "mw", true),
            FilterConfig.getInstance(FILTER, "mw",
                    true, new HashMap<>() {{
                        put(Constants.MULTIWORDS_ASSET_NAME_V2, "multiwords-ai.txt");
                        put(MAX_TERM_SIZE_PROPERTY_NAME, maxTermSize);
                    }}, MULTIWORD_FILTER_FACTORY_NAME,
                    "mw", true));

    private static final List<FilterConfig> V2StemdictFilters = Arrays.asList(
            FilterConfig.getInstance(FILTER, "cso",
                    false, new HashMap<>() {{
                        put("dictionary", "stemdict-front.txt");
                    }}, NO_STEM_FILTER_FACTORY_NAME,
                    null, true),
            FilterConfig.getInstance(FILTER, "cso",
                    false, new HashMap<>() {{
                        put("dictionary", "stemdict.txt");
                    }}, NO_STEM_FILTER_FACTORY_NAME,
                    null, true),
            FilterConfig.getInstance(FILTER, "cso",
                    false, new HashMap<>() {{
                        put("dictionary", "stemdict-ai.txt");
                    }}, NO_STEM_FILTER_FACTORY_NAME,
                    null, true));

    private static final List<FilterConfig> V2StopWordsFilters = Arrays.asList(
            FilterConfig.getInstance(FILTER, "sf",
                    false, new HashMap<>() {{
                        put("ignoreCase", "true");
                        put("words", "stopwords-front.txt");
                    }}, STOPWORDS_FILTER_FACTORY_NAME,
                    null, true),
            FilterConfig.getInstance(FILTER, "sf",
                    false, new HashMap<>() {{
                        put("ignoreCase", "true");
                        put("words", "stopwords.txt");
                    }}, STOPWORDS_FILTER_FACTORY_NAME,
                    null, true),
            FilterConfig.getInstance(FILTER, "sf",
                    false, new HashMap<>() {{
                        put("ignoreCase", "true");
                        put("words", "stopwords-ai.txt");
                    }}, STOPWORDS_FILTER_FACTORY_NAME,
                    null, true));

    private static final Map<String, List<FilterConfig>> assetToFilterConfigMap = new HashMap<>() {
        {
            put(Constants.SYNONYMS_BACKEND_ASSET_NAME, V2SynonymFilters);
            put(Constants.MANDATORY_TERMS_ASSET_NAME, V2MandatoryTermFilters);
            put(Constants.MULTIWORDS_ASSET_NAME, V2MultiWordsFilters);
            put(Constants.NO_STEM_ASSET_NAME, V2StemdictFilters);
            put(Constants.STOP_WORDS_ASSET_NAME, V2StopWordsFilters);
        }
    };

    @Inject
    public MigrationServiceImpl(AnalyserService analyserService,
                                RelevancyService relevancyService,
                                ConsoleOrchestrationService consoleOrchestrationService,
                                ToucanRemoteService toucanService,
                                EventFactory eventFactory) {
        this.analyserService = analyserService;
        this.relevancyService = relevancyService;
        this.consoleOrchestrationService = consoleOrchestrationService;
        this.toucanService = toucanService;
        this.eventFactory = eventFactory;
    }

    // Migration process template
    //     * fetch v1 assets from analyser service
    //     * transform v1 assets to v2 format
    //     * push assets to s3
    //     * send update request with s3 location to analyser service

    @Override
    public Map<String, Object> migrateToSelfServe(String siteKey, String cookie) throws AnalyserMigrationException {
        if (V2.equals(getAnalyserVersion(siteKey))) {
            return new HashMap<>() {{
                put("msg", "site is already in V2");
            }};
        }
        Set<String> tokens = generateTokensFromAssets(siteKey, cookie);
        List<String> queryResponseBeforeMigration = new ArrayList<>(tokens.size());
        queryAnalyser(siteKey, tokens, queryResponseBeforeMigration);
        migrateStopWords(siteKey);
        migrateBackEndSynonyms(siteKey);
        migrateFrontEndSynonyms(siteKey);
        migrateMandatoryTerms(siteKey);
        migrateMultiwords(siteKey);
        migrateStemDictionary(siteKey);
        migrateExcludeTermsSet(siteKey);
        migrateAsciiMapping(siteKey);
        updateConfig(siteKey);
        updateVersionToV2(siteKey);
        List<String> queryResponseAfterMigration = new ArrayList<>(tokens.size());
        queryAnalyser(siteKey, tokens, queryResponseAfterMigration);
        Map<String, Object> result = new HashMap<>(2);
        Map<String, String> queryReportFile =
                uploadQueryReportToS3(siteKey, tokens, queryResponseBeforeMigration, queryResponseAfterMigration);
        result.put("queryReportFile", queryReportFile);
        Map<String, String> errorReportFile = fetchAsterixErrorReport(siteKey,
                RouteDispatcher.getRouteContext().getResponse().getHeader(UNBXD_REQUEST_ID_HEADER_NAME)
        );
        result.put("errorReportFile", errorReportFile);
        return result;
    }


    private Map<String, String> fetchAsterixErrorReport(String siteKey, String traceID)
            throws AnalyserMigrationException {
        try {
            InputStream inputStream = bulkDownloadReport(siteKey, traceID);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + "error_report.log");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()));
            while (reader.ready()) {
                writer.append(reader.readLine());
                writer.newLine();
            }
            writer.close();
            reader.close();
            return relevancyService.uploadFileToS3(siteKey, S3_PREFIX_OF_ANALYSER_MIGRATION,
                    outputFile.toFile());
        } catch (IOException e) {
            String msg = "Unable get error report";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (RelevancyServiceException e) {
            log.error("Error while pushing error report to s3 , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private InputStream bulkDownloadReport(String siteKey, String traceID) throws AnalyserMigrationException {
        try {
            ReportRequest request = ReportRequest.getInstance(traceID, 10000);
            Response<ResponseBody> response = toucanService.bulkDownloadReport(request).execute();
            if (!response.isSuccessful()) {
                log.error("Exception while fetching events from toucan for request id:" + traceID +
                        "siteKey: " + siteKey + " with message: " + response.errorBody().string());
                throw new AnalyserMigrationException(500, "Unable to fetch events for siteKey: "
                        + siteKey + " and request id:" + traceID);
            }
            if (isNull(response.body())) {
                log.error("Empty response while fetching events from toucan for request id:" + traceID +
                        "siteKey: " + siteKey + " with message: " + response.errorBody().string());
                throw new AnalyserMigrationException(500, "Unable to fetch events for siteKey: "
                        + siteKey + " and request id:" + traceID);
            }
            return response.body().byteStream();
        } catch (IOException e) {
            log.error("Exception while fetching events from toucan for request id:"
                    + traceID + " with message: " + e.getMessage());
            throw new ReportException("Unable to fetch events from toucan  for siteKey: "
                    + siteKey + " and request id:" + traceID);
        }
    }

    private Map<String, String> uploadQueryReportToS3(String siteKey,
                                                      Set<String> tokens,
                                                      List<String> queryResponseBeforeMigration,
                                                      List<String> queryResponseAfterMigration) throws AnalyserMigrationException {
        try {
            tokens = escapeDoubleQuotes(tokens);
            queryResponseBeforeMigration = escapeDoubleQuotes(queryResponseBeforeMigration);
            queryResponseAfterMigration  = escapeDoubleQuotes(queryResponseAfterMigration);
            String[] tokensArray = Arrays.copyOf(tokens.toArray(), tokens.size(), String[].class);
            Path reportFile = createTempFile("analyser-migration-query-report",
                    HYPHEN + siteKey + CSV_FILE_EXTENSION);
            BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile.toFile()));
            writer.append("Query,ResponseBeforeMigration,ResponseAfterMigration,DifferenceInResponse," +
                    "DifferenceInResponseTokens,DifferenceInResponseIgnoringTokenCount\n");
            boolean isDiffPresent = false;
            for (int i = 0; i < tokensArray.length; i++) {
                String diff = StringUtils.difference(queryResponseBeforeMigration.get(i),
                        queryResponseAfterMigration.get(i));
                diff = diff.isEmpty() ? StringUtils.difference(queryResponseAfterMigration.get(i),
                        queryResponseBeforeMigration.get(i)) : diff;
                boolean diffInTokens = false;
                boolean diffIgnoringTokenCount = false;

                if (!diff.isEmpty()) {
                    List<String> tokensOFQueryResponseBeforeMigration =
                            new ArrayList<>(Arrays.asList(queryResponseBeforeMigration.get(i)
                                    .split("[^a-zA-Z\\d]+")));
                    List<String> tokensOFQueryResponseAfterMigration =
                            new ArrayList<>(Arrays.asList(queryResponseAfterMigration.get(i)
                                    .split("[^a-zA-Z\\d]+")));
                    tokensOFQueryResponseBeforeMigration.addAll(
                            getSpecialTokensFromAnalyserResponse(queryResponseBeforeMigration.get(i))
                    );
                    tokensOFQueryResponseAfterMigration.addAll(
                            getSpecialTokensFromAnalyserResponse(queryResponseAfterMigration.get(i))
                    );
                    Collections.sort(tokensOFQueryResponseBeforeMigration);
                    Collections.sort(tokensOFQueryResponseAfterMigration);
                    diffInTokens = !tokensOFQueryResponseBeforeMigration
                            .equals(tokensOFQueryResponseAfterMigration);
                    Set<String> responseTokensSetBeforeMigration = new HashSet<>(tokensOFQueryResponseBeforeMigration);
                    Set<String> responseTokensSetAfterMigration = new HashSet<>(tokensOFQueryResponseAfterMigration);
                    diffIgnoringTokenCount = diffInTokens &&
                            !(responseTokensSetBeforeMigration.containsAll(responseTokensSetAfterMigration) &&
                                    responseTokensSetAfterMigration.containsAll(responseTokensSetBeforeMigration));
                    isDiffPresent = isDiffPresent || diffIgnoringTokenCount;
                }
                writer.append(
                        new StringBuilder()
                                .append(DOUBLE_QUOTE)
                                .append(tokensArray[i])
                                .append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(queryResponseBeforeMigration.get(i)).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(queryResponseAfterMigration.get(i)).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(diff).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(diffInTokens).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(diffIgnoringTokenCount).append(DOUBLE_QUOTE)
                                .append(System.lineSeparator())
                );
            }
            writer.close();
            Map<String, String> uploadResponse = relevancyService.
                    uploadFileToS3(siteKey, S3_PREFIX_OF_ANALYSER_MIGRATION, reportFile.toFile());
            String logMessage = DIFF_NOT_FOUND_IN_QUERY_REPORT_MESSAGE;
            if (isDiffPresent) logMessage = DIFF_FOUND_IN_QUERY_REPORT_MESSAGE;
            fireEvent(siteKey, logMessage + uploadResponse.get("s3Location"));
            return uploadResponse;
        } catch (IOException e) {
            String msg = "Error while uploading analyser migration report";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading analyser migration report to s3 for siteKey:" + siteKey + " reason: "
                    + e.getMessage());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }


    private List<String> getSpecialTokensFromAnalyserResponse(String input) {
        // specialTokens will contain tokens of multiwords ,mandatory term and exclude terms
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\+|\\-|(<\\$>)|(<%>))+(\\w)+");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            result.add((matcher.group()));
        }
        return result;
    }

    private void queryAnalyser(String siteKey,
                               Set<String> tokens,
                               List<String> response) throws AnalyserMigrationException {
        try {
            for (String token : tokens) {
                response.add(analyserService.analyse(siteKey, token));
            }
        } catch (AnalyserException e) {
            String msg = "Error while querying analyser";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, msg);
        }
    }

    private void updateVersionToV2(String siteKey) throws AnalyserMigrationException {
        try {
            analyserService.updateVersion(siteKey, V2);
        } catch (AnalyserException e) {
            log.error("Error while updating analyser version  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private Set<String> generateTokensFromAssets(String siteKey, String cookie) throws AnalyserMigrationException {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(generateTokensFromBackEndSynonyms(siteKey));
        result.addAll(generateTokensFromFrontEndSynonyms(siteKey));
        result.addAll(generateTokensFromMultiwords(siteKey));
        result.addAll(generateTokensFromMandatoryTerms(siteKey));
        result.addAll(generateTokensFromStemDict(siteKey));
        result.addAll(generateTokensFromStopwords(siteKey));
        result.addAll(generateTokensFromExcludeTerms(siteKey));
        result.addAll(getQueriesFromMerchandisingRules(siteKey, cookie));
        return result.stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> getQueriesFromMerchandisingRules(String siteKey, String cookie) {
        try {
            return consoleOrchestrationService.getQueriesUsedInQueryRules(siteKey, cookie);
        } catch (ConsoleOrchestrationServiceException|FieldException e) {
            String msg = "Error while fetching queries from console merchandising rules, reason:" + e.getMessage();
            log.error(msg);
        }
        return Collections.emptySet();
    }
    private Set<String> generateTokensFromBackEndSynonyms(String siteKey) throws AnalyserMigrationException {
        List<String> synonymsList = fetchAssets(siteKey, Constants.SYNONYMS_BACKEND_ASSET_NAME);
        return generateTokensFromSynonyms(synonymsList);
    }

    private Set<String> generateTokensFromFrontEndSynonyms(String siteKey) throws AnalyserMigrationException {
        List<String> synonymsList = fetchAssets(siteKey, Constants.SYNONYMS_FRONTEND_ASSET_NAME);
        return generateTokensFromSynonyms(synonymsList);
    }

    private Set<String> generateTokensFromSynonyms(List<String> synonymsList) {
        if (synonymsList.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>(synonymsList.size() * 2);
        for (String synonyms : synonymsList) {
            if (synonyms.contains(UNI_DIRECTIONAL_SYNONYM_DELIMITER)) {
                result.add(synonyms.substring(0, synonyms.indexOf(UNI_DIRECTIONAL_SYNONYM_DELIMITER)));
                result.addAll(
                        Arrays.asList(
                                synonyms.substring(synonyms.indexOf(UNI_DIRECTIONAL_SYNONYM_DELIMITER) +
                                        UNI_DIRECTIONAL_SYNONYM_DELIMITER.length())
                                        .split(COMMA_MARK)
                        )
                );
            } else {
                result.addAll(Arrays.asList(synonyms.split(COMMA_MARK)));
            }
        }
        return result;
    }


    private Set<String> generateTokensFromMultiwords(String siteKey) throws AnalyserMigrationException {
        List<String> mutliwordsList = fetchAssets(siteKey, Constants.MULTIWORDS_ASSET_NAME);
        if (mutliwordsList.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>(mutliwordsList.size());
        for (String multiwords : mutliwordsList) {
            if (multiwords.contains(PIPE))
                result.add(multiwords.substring(0, multiwords.indexOf(PIPE)));
            else
                result.add(multiwords);
        }
        return result;
    }

    private Set<String> generateTokensFromMandatoryTerms(String siteKey) throws AnalyserMigrationException {
        List<String> mandatoryTerms = fetchAssets(siteKey, Constants.MANDATORY_TERMS_ASSET_NAME);
        if (mandatoryTerms.isEmpty()) return Collections.emptySet();
        return new HashSet<>(mandatoryTerms);
    }

    private Set<String> generateTokensFromStopwords(String siteKey) throws AnalyserMigrationException {
        List<String> stopwords = fetchAssets(siteKey, Constants.STOP_WORDS_ASSET_NAME);
        if (stopwords.isEmpty()) return Collections.emptySet();
        return new HashSet<>(stopwords);
    }

    private Set<String> generateTokensFromExcludeTerms(String siteKey) throws AnalyserMigrationException {
        List<String> excludeTermSet = fetchAssets(siteKey, Constants.EXCLUDE_TERMS_NAME);
        if (excludeTermSet.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>(excludeTermSet.size());
        for (String excludeTerm : excludeTermSet) {
            if(!excludeTerm.contains(HYPHEN)) continue;
            result.add(excludeTerm.substring(0, excludeTerm.indexOf(HYPHEN)));
            result.addAll(
                    Arrays.asList(
                            excludeTerm.substring(excludeTerm.indexOf(HYPHEN) + HYPHEN.length())
                                    .split(COMMA_MARK)
                    )
            );
        }
        return result;
    }

    private Set<String> generateTokensFromStemDict(String siteKey) throws AnalyserMigrationException {
        List<String> stemDictionary = fetchAssets(siteKey, Constants.NO_STEM_ASSET_NAME);
        if (stemDictionary.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>(stemDictionary.size());
        for (String row : stemDictionary) {
            result.addAll(Arrays.asList(row.split("\\s+")));
        }
        return result;
    }


    private String getAnalyserVersion(String siteKey) throws AnalyserMigrationException {
        try {
            return analyserService.getVersion(siteKey);
        } catch (AnalyserException e) {
            log.error("Error while analyser version  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateBackEndSynonyms(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> synonyms = fetchAssets(siteKey, Constants.SYNONYMS_BACKEND_ASSET_NAME);
            if (synonyms.isEmpty()) return;
            String synonymName = Constants.SYNONYMS_ASSET_NAME_V2 + HYPHEN + DICTIONARY_TYPE_BACKEND;
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + synonymName + CSV_FILE_EXTENSION);
            transformSynonymsToSSFormat(siteKey, synonymName, synonyms, outputFile.toFile());
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, synonymName, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.SYNONYMS_ASSET_NAME_V2, DICTIONARY_TYPE_BACKEND,
                    uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate backend synonyms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing backend synonyms to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading backend synonyms  to s3  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateFrontEndSynonyms(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> synonyms = fetchAssets(siteKey, Constants.SYNONYMS_FRONTEND_ASSET_NAME);
            if (synonyms.isEmpty()) return;
            String synonymName = Constants.SYNONYMS_ASSET_NAME_V2 + HYPHEN + DICTIONARY_TYPE_FRONTEND;
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + synonymName + CSV_FILE_EXTENSION);
            transformSynonymsToSSFormat(siteKey, synonymName, synonyms, outputFile.toFile());
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, synonymName, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.SYNONYMS_ASSET_NAME_V2, DICTIONARY_TYPE_FRONTEND,
                    uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate FrontEnd synonyms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing FrontEnd synonyms to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading FrontEnd synonyms  to s3  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateFrontEndSynonyms(String siteKey, String cookie) throws AnalyserMigrationException {
        try {
            List<String> synonymsList = fetchFrontEndSynonyms(siteKey, cookie);
            if (synonymsList.isEmpty()) return;
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.SYNONYMS_ASSET_NAME_V2 + HYPHEN + DICTIONARY_TYPE_FRONTEND
                            + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            for (String synonyms : synonymsList) {
                bufferedWriter.write(synonyms);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.SYNONYMS_ASSET_NAME_V2, DICTIONARY_TYPE_FRONTEND,
                    uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate FrontEnd synonyms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing FrontEnd synonyms to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading FrontEnd synonyms  to s3  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void transformSynonymsToSSFormat(String siteKey,
                                             String synonymName,
                                             List<String> synonyms,
                                             File outputFile) throws IOException {
        synonyms = escapeDoubleQuotes(synonyms);
        List<String> uniDirectionalSynonymsList = synonyms.stream()
                .filter(c -> c.contains(UNI_DIRECTIONAL_SYNONYM_DELIMITER))
                .collect(Collectors.toList());
        synonyms.removeAll(uniDirectionalSynonymsList);
        List<String> bidirectionalSynonymsList = synonyms;
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
        outputWriter.write("Keyword,Unidirectional,Bidirectional\n");
        uniDirectionalSynonymsList = splitLHSofUniDirectionalSynonym(uniDirectionalSynonymsList);
        uniDirectionalSynonymsList = joinDuplicateKeywordEntries(uniDirectionalSynonymsList);

        // iterate over uniDirectionalSynonymsList
        // fetch one of the bi directions synonym found for the root keyword of the  uniDirectionalSynonym
        // remove these fetched bi directional synonym from global bidirectionalSynonyms list
        // build the new string (v2 format) and append to output file

        for (String uniDirectionalSynonym : uniDirectionalSynonymsList) {
            buildSynonymInV2format(siteKey, uniDirectionalSynonym, bidirectionalSynonymsList, outputWriter);
        }

        // iterate over remaining bidirectionalSynonyms
        // build the new string (V2 format) and append to output file
        for (String bidirectionalSynonym : bidirectionalSynonymsList) {
            List<String> tokens =
                    Arrays.stream(bidirectionalSynonym.split(COMMA_MARK))
                            .map(String::trim)
                            .collect(Collectors.toList());
            String keyword = tokens.get(0);
            tokens.remove(0); // remove keyword
            String RHS = String.join(COMMA_MARK, tokens);
            StringBuilder newEntry = new StringBuilder()
                    .append(keyword)
                    .append(",,")
                    .append(DOUBLE_QUOTE).append(RHS).append(DOUBLE_QUOTE);
            outputWriter.append(newEntry);
            outputWriter.newLine();
        }
        outputWriter.close();
    }

    private List<String> splitLHSofUniDirectionalSynonym(List<String> uniDirectionalSynonymsList) {
        // example
        //  input : A,B => C,D
        //  output : [A => C,D , B => C,D]

        List<String> result = new ArrayList<>(uniDirectionalSynonymsList.size());
        for (String uniDirectionalSynonym : uniDirectionalSynonymsList) {
            String[] tmp = uniDirectionalSynonym.split(UNI_DIRECTIONAL_SYNONYM_DELIMITER);
            String RHSOfUniDirSynonyms = tmp[1];
            if (tmp[0].contains(COMMA_MARK)) {
                String[] keywords = tmp[0].split(COMMA_MARK);
                for (String entry : keywords) {
                    result.add(
                            StringUtils.join(entry, UNI_DIRECTIONAL_SYNONYM_DELIMITER, RHSOfUniDirSynonyms)
                    );
                }
            } else
                result.add(uniDirectionalSynonym);
        }
        return result;
    }

    private List<String> joinDuplicateKeywordEntries(List<String> uniDirectionalSynonymsList) {
        // example
        //  input : [A => X , A => Y]
        //  output : A => X,Y

        List<String> result = new ArrayList<>(uniDirectionalSynonymsList.size());
        Set<String> alreadyParsed = new LinkedHashSet<>(uniDirectionalSynonymsList.size());
        for (String uniDirectionalSynonym : uniDirectionalSynonymsList) {
            String keyword = uniDirectionalSynonym.split(UNI_DIRECTIONAL_SYNONYM_DELIMITER)[0].trim();
            if(alreadyParsed.contains(keyword))
                continue;
            String RHS = "";
            for (String entry : uniDirectionalSynonymsList) {
                String[] temp = entry.split(UNI_DIRECTIONAL_SYNONYM_DELIMITER);
                if (keyword.equals(temp[0].trim())) {
                    RHS = StringUtils.join(RHS, COMMA_MARK, temp[1]);
                }
            }
            RHS = RHS.substring(1); // remove comma mark in the beginning
            result.add(StringUtils.join(keyword, UNI_DIRECTIONAL_SYNONYM_DELIMITER, RHS));
            alreadyParsed.add(keyword);
        }
        return result;
    }

    private void buildSynonymInV2format(String siteKey,
                                        String uniDirectionalSynonym,
                                        List<String> bidirectionalSynonymsList,
                                        BufferedWriter outputWriter) throws IOException {
        String[] temp = uniDirectionalSynonym.split(UNI_DIRECTIONAL_SYNONYM_DELIMITER);
        if(temp.length < 2) {
            fireEvent(siteKey, SYNONYMS_ASSET_NAME_V2,
                    INVALID_ENTRY_MESSAGE, uniDirectionalSynonym);
            return;
        }
        String keyword = temp[0].trim();
        String RHSOfUniDirSynonyms = temp[1];

        String biDirSynonymsForTheKeyword =
                bidirectionalSynonymsList
                        .parallelStream()
                        .filter(entry -> {
                            String tmp2 = Arrays.stream(entry.split(COMMA_MARK))
                                    .map(String::trim)
                                    .findFirst()
                                    .orElse(null);
                            return keyword.equals(tmp2);
                        })
                        .findFirst()
                        .orElse(null);
        List<String> tokensOfBiDirSynonyms = new ArrayList<>();
        if (nonNull(biDirSynonymsForTheKeyword)) {
            tokensOfBiDirSynonyms = Arrays.stream(biDirSynonymsForTheKeyword.split(COMMA_MARK))
                    .map(String::trim)
                    .collect(Collectors.toList());
            tokensOfBiDirSynonyms.remove(keyword);
            bidirectionalSynonymsList.removeIf(biDirSynonymsForTheKeyword::equals);
        }
        List<String> tokensOfUniDirSynonyms = Arrays.stream(RHSOfUniDirSynonyms.split(COMMA_MARK)).map(String::trim)
                .collect(Collectors.toList());
        RHSOfUniDirSynonyms = String.join(COMMA_MARK, tokensOfUniDirSynonyms);
        String RHSOfBiDirSynonyms = String.join(COMMA_MARK, tokensOfBiDirSynonyms);

        StringBuilder newEntry = new StringBuilder()
                .append(keyword)
                .append(COMMA_MARK)
                .append(DOUBLE_QUOTE).append(RHSOfUniDirSynonyms).append(DOUBLE_QUOTE)
                .append(COMMA_MARK);
        if (!RHSOfBiDirSynonyms.isEmpty())
            newEntry.append(DOUBLE_QUOTE).append(RHSOfBiDirSynonyms).append(DOUBLE_QUOTE);
        outputWriter.append(newEntry);
        outputWriter.newLine();
    }

    private List<String> fetchAssets(String siteKey,
                                     String assetName) throws AnalyserMigrationException {
        try {
            InputStream inputStream = analyserService.bulkDownloadAsset(siteKey, assetName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> assets = new ArrayList<>(100);
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#") || line.isEmpty() || line.isBlank()) continue;
                assets.add(line);
            }
            reader.close();
            return assets;
        } catch (IOException e) {
            String msg = "Error while fetching asset: " + assetName;
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while fetching asset: " + assetName + " ,error: " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getMessage());
        }
    }


    private List<String> fetchFrontEndSynonyms(String siteKey, String cookie) throws AnalyserMigrationException {
        try {
            InputStream inputStream = consoleOrchestrationService.getFrontEndSynonyms(siteKey, cookie);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> synonyms = new ArrayList<>(100);
            while (reader.ready()) {
                synonyms.add(reader.readLine());
            }
            reader.close();
            return synonyms;
        } catch (IOException e) {
            String msg = "Error while fetching front end synonyms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (ConsoleOrchestrationServiceException e) {
            log.error("Error while fetching front end synonyms " + " ,error: " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getMessage());
        }
    }

    private void migrateMultiwords(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> multiwordsList = fetchAssets(siteKey, Constants.MULTIWORDS_ASSET_NAME);
            if (multiwordsList.isEmpty()) return;
            multiwordsList = escapeDoubleQuotes(multiwordsList);
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.MULTIWORDS_ASSET_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Phrase,Type\n");

            for (String multiwordPhrase : multiwordsList) {
                if (multiwordPhrase.contains(PIPE)) {
                    if (!isValidMulitwordsEntry(multiwordPhrase)) {
                        fireEvent(siteKey, Constants.MULTIWORDS_ASSET_NAME_V2,
                                "Removed invalid entry.", multiwordPhrase);
                        continue;
                    }
                    String[] tmp = multiwordPhrase.split("\\|");
                    String keyword = tmp[0].trim();
                    String Type = tmp[1].trim().toLowerCase(); // convert uppercase letters to lowercase to pass asterix validation
                    bufferedWriter.append(
                            new StringBuilder()
                                    .append(DOUBLE_QUOTE).append(keyword).append(DOUBLE_QUOTE)
                                    .append(COMMA_MARK)
                                    .append(DOUBLE_QUOTE).append(Type).append(DOUBLE_QUOTE)
                    );
                } else
                    bufferedWriter.append(new StringBuilder()
                            .append(DOUBLE_QUOTE).append(multiwordPhrase).append(DOUBLE_QUOTE)
                            .append(COMMA_MARK)
                            .append(DOUBLE_QUOTE).append("full").append(DOUBLE_QUOTE));
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.MULTIWORDS_ASSET_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.MULTIWORDS_ASSET_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate Multiwords";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing Multiwords to analyser service , error : " + e.getMessage()
                    + " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading Multiwords  to s3  , error : " + e.getMessage());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private boolean isValidMulitwordsEntry(String entry) {
        return multiwordTypes.contains(entry.substring(entry.indexOf(PIPE) + PIPE.length()).trim().toLowerCase());
    }


    private boolean doubleQuotesAreNotEscaped(String entry) {
        return StringUtils.countMatches(entry, "\"") != StringUtils.countMatches(entry, "\\\"");
    }

    private void migrateMandatoryTerms(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> mandatoryTerms = fetchAssets(siteKey, Constants.MANDATORY_TERMS_ASSET_NAME);
            if (mandatoryTerms.isEmpty()) return;
            mandatoryTerms = escapeDoubleQuotes(mandatoryTerms);
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.MANDATORY_TERMS_ASSET_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Keyword\n");
            for (String term : mandatoryTerms) {
                bufferedWriter.append(
                        new StringBuilder().append(DOUBLE_QUOTE).append(term.trim()).append(DOUBLE_QUOTE).append(NEW_LINE)
                );
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.MANDATORY_TERMS_ASSET_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.MANDATORY_TERMS_ASSET_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate MandatoryTerms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing MandatoryTerms to analyser service , error : " + e.getMessage()
                    + " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading MandatoryTerms  to s3  , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateStopWords(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> stopWords = fetchAssets(siteKey, Constants.STOP_WORDS_ASSET_NAME);
            if (stopWords.isEmpty()) return;
            stopWords =  escapeDoubleQuotes(stopWords);
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.STOP_WORDS_ASSET_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Keyword\n");
            for (String term : stopWords) {
                bufferedWriter.append(
                        new StringBuilder().append(DOUBLE_QUOTE).append(term).append(DOUBLE_QUOTE).append(NEW_LINE)
                );
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.STOP_WORDS_ASSET_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.STOP_WORDS_ASSET_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate StopWords";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing StopWords to analyser service , error : " + e.getMessage() + " statusCode: "
                    + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while uploading StopWords  to s3  , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }


    private void migrateStemDictionary(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> stemDict = fetchAssets(siteKey, Constants.NO_STEM_ASSET_NAME);
            if (stemDict.isEmpty()) return;
            stemDict =  escapeDoubleQuotes(stemDict);
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.NO_STEM_ASSET_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Keyword,Stemmed\n");
            for (String term : stemDict) {
                String[] tmp = term.split("\\s+");
                bufferedWriter.append(
                        new StringBuilder()
                                .append(DOUBLE_QUOTE).append(tmp[0]).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(tmp[1]).append(DOUBLE_QUOTE)
                                .append(NEW_LINE)
                );
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.NO_STEM_ASSET_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.NO_STEM_ASSET_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate StemDictionary";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing StemDictionary to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while pushing StemDictionary to s3 , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateExcludeTermsSet(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> excludeTerms = fetchAssets(siteKey, Constants.EXCLUDE_TERMS_NAME);
            if (excludeTerms.isEmpty()) return;
            excludeTerms = escapeDoubleQuotes(excludeTerms);
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.EXCLUDE_TERMS_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Keyword,Exclusions\n");
            for (String term : excludeTerms) {
                int delimiterIndex =  term.indexOf("-");
                if(delimiterIndex == -1 ) {
                    fireEvent(siteKey, Constants.EXCLUDE_TERMS_NAME_V2, INVALID_ENTRY_MESSAGE, term);
                    continue;
                }
                bufferedWriter.append(
                        new StringBuilder()
                                .append(DOUBLE_QUOTE).append(term, 0, delimiterIndex).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(term.substring(delimiterIndex+1)).append(DOUBLE_QUOTE)
                                .append(NEW_LINE));
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.EXCLUDE_TERMS_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.EXCLUDE_TERMS_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate excludeTerms";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing excludeTerms to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while pushing excludeTerms to s3 , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void migrateAsciiMapping(String siteKey) throws AnalyserMigrationException {
        try {
            List<String> asciiMapping = fetchAssets(siteKey, Constants.ASCII_MAPPING_NAME);
            if (asciiMapping.isEmpty()) return;
            Path outputFile = createTempFile(PREFIX_NAME_OF_UPLOAD_FILE,
                    HYPHEN + Constants.ASCII_MAPPING_NAME_V2 + CSV_FILE_EXTENSION);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile()));
            bufferedWriter.write("Key,Mapping\n");
            for (String row : asciiMapping) {
                if(!row.contains(UNI_DIRECTIONAL_SYNONYM_DELIMITER)) {
                    fireEvent(siteKey, Constants.ASCII_MAPPING_NAME_V2, INVALID_ENTRY_MESSAGE, row);
                    continue;
                }
                String[] tmp = row.split(UNI_DIRECTIONAL_SYNONYM_DELIMITER);
                String keyword = tmp[0].trim();
                keyword =  keyword.substring(1,  keyword.length() - 1).replace("~", "~~")  // '~' is an escape character
                        .replace("\"","~\"");
                String mapping = tmp[1].trim();
                mapping = mapping.substring(1,  mapping.length() - 1).replace("~", "~~")
                        .replace("\"","~\"");
                bufferedWriter.append(
                        new StringBuilder()
                                .append(DOUBLE_QUOTE).append(keyword).append(DOUBLE_QUOTE)
                                .append(COMMA_MARK)
                                .append(DOUBLE_QUOTE).append(mapping).append(DOUBLE_QUOTE)
                                .append(NEW_LINE)

                );
            }
            bufferedWriter.close();
            if(outputFile.toFile().length() == 0) {
                fireEvent(siteKey, Constants.ASCII_MAPPING_NAME_V2, ZERO_VALID_ENTRIES_ERROR_MESSAGE);
                return;
            }
            Map<String, String> uploadResponse = relevancyService.uploadFileToS3(siteKey, outputFile.toFile());
            analyserService.bulkUpdateAsset(siteKey, Constants.ASCII_MAPPING_NAME_V2,
                    DICTIONARY_TYPE_BACKEND, uploadResponse.get(S3_LOCATION_PROPERTY_NAME));
        } catch (IOException e) {
            String msg = "Unable to migrate asciiMapping";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserMigrationException(500, ErrorCode.IOError.getCode(), msg);
        } catch (AnalyserException e) {
            log.error("Error while pushing asciiMapping to analyser service , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (RelevancyServiceException e) {
            log.error("Error while pushing asciiMapping to s3 , error : " + e.getMessage() + " statusCode: " +
                    e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }


    private void updateConfig(String siteKey) throws AnalyserMigrationException {
        try {
            CoreConfig coreConfig = analyserService.getConfig(siteKey);
            transformCoreConfig(coreConfig);
            if (!siteKey.contains("ss-unbxd-")) siteKey = "ss-unbxd-" + siteKey;
            analyserService.updateConfig(siteKey, coreConfig);
        } catch (AnalyserException e) {
            log.error("Error while updating analyser config  , error : " + e.getMessage() +
                    " statusCode: " + e.getStatusCode());
            throw new AnalyserMigrationException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    private void transformCoreConfig(CoreConfig coreConfig) {
        for (AnalyserConfig analyserConfig : coreConfig.getAnalyzerConfigs()) {
            for (int i = 0; i < analyserConfig.getFactories().size(); i++) {
                if (analyserConfig.getFactories().get(i).getClassName().equals(SYNONYMS_FILTER_FACTORY_NAME)) {
                    analyserConfig.getFactories().get(i).setClassName(SYNONYMS_V2_FILTER_FACTORY_NAME);
                    if (analyserConfig.getFactories().get(i).getArgs().get(SYNONYMS_ASSET_NAME_V2)
                            .equals("synonyms.txt,synonyms-bck.txt"))
                        analyserConfig.getFactories().get(i).getArgs().put(SYNONYMS_ASSET_NAME_V2,
                                "synonyms.txt,synonyms-bck.txt,synonyms-ai.txt");
                }
                // replace v1 filterConfigs with v2 filterConfigs
                for (String assetName : assetToFilterConfigMap.keySet()) {
                    if (analyserConfig.getFactories().get(i).getArgs().containsValue(assetName)) {
                        analyserConfig.getFactories().remove(i);
                        insertList(analyserConfig.getFactories(), assetToFilterConfigMap.get(assetName), i);
                        i = i + assetToFilterConfigMap.get(assetName).size() - 2;
                        break;
                    }
                }

            }
        }
    }

    private void insertList(List<FilterConfig> mainList,
                            List<FilterConfig> listToInsert,
                            int indexToInsertAt) {
        int counter = 0;
        for (FilterConfig filterConfig : listToInsert) {
            mainList.add(indexToInsertAt + counter, filterConfig);
            counter++;
        }
    }

    private void fireEvent(String siteKey,
                           String assetName,
                           String msg,
                           String entry) {
        eventFactory.createAndFireEvent(
                eventFactory.getEmail("skipper-bot"),
                siteKey, System.currentTimeMillis() * 1000, siteKey, msg,
                EventTag.INFO, MIGRATION_OPERATION_NAME,
                new HashMap<>() {{
                    put("entry", entry);
                    put("dictionary_type", assetName);
                }},
                null);
    }

    private void fireEvent(String siteKey,
                           String assetName,
                           String msg) {
        eventFactory.createAndFireEvent(
                eventFactory.getEmail("skipper-bot"),
                siteKey, System.currentTimeMillis() * 1000, siteKey, msg,
                EventTag.INFO, MIGRATION_OPERATION_NAME,
                new HashMap<>() {{
                    put("dictionary_type", assetName);
                }},
                null);
    }

    private void fireEvent(String siteKey,
                           String msg) {
        eventFactory.createAndFireEvent(
                eventFactory.getEmail("skipper-bot"),
                siteKey, System.currentTimeMillis() * 1000, siteKey, msg,
                EventTag.INFO, MIGRATION_OPERATION_NAME, emptyMap(), null);
    }

    private List<String> escapeDoubleQuotes(List<String> entries) {
        return entries.stream()
                .map(entry -> entry.replace("\\","\\\\"))
                .map(entry -> entry.replace("\"","\\\"")).collect(Collectors.toList());
    }

    private Set<String> escapeDoubleQuotes(Set<String> entries) {
        return entries.stream()
                .map(entry -> entry.replace("\\", "\\\\"))
                .map(entry -> entry.replace("\"", "\\\""))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


}

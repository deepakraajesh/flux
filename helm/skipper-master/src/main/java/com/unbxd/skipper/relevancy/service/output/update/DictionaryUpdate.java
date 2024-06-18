package com.unbxd.skipper.relevancy.service.output.update;

import com.google.inject.Inject;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.analyser.service.impl.AsterixService;
import com.unbxd.console.model.ProductType;
import com.unbxd.recommend.dao.ContentDao;
import com.unbxd.recommend.exception.RecommendException;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryType;
import com.unbxd.skipper.dictionary.service.DictionaryService;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.unbxd.skipper.relevancy.model.JobType.*;

@Log4j2
public class DictionaryUpdate implements RelevancyOutputUpdateProcessor {

    private ContentDao contentDao;
    private AmazonS3Client s3Client;
    protected RelevancyDao relevancyDao;
    protected AnalyserService analyserService;
    protected DictionaryService dictionaryService;

    public static Map<JobType, String> jobTypeToAssetNameV2 = new HashMap<>() {
        {
            put(JobType.synonyms, AsterixService.SYNONYMS_ASSET_NAME_V2);
            put(JobType.mandatoryTerms, AsterixService.MANDATORY_TERMS_ASSET_NAME_V2);
            put(JobType.multiwords, AsterixService.MULTIWORDS_ASSET_NAME_V2);
            put(JobType.noStemWords, AsterixService.NO_STEM_ASSET_NAME_V2);

            put(JobType.suggestedSynonyms, AsterixService.SYNONYMS_ASSET_NAME_V2);
            put(JobType.suggestedMandatoryTerms, AsterixService.MANDATORY_TERMS_ASSET_NAME_V2);
            put(JobType.suggestedMultiwords, AsterixService.MULTIWORDS_ASSET_NAME_V2);
            put(JobType.suggestedNoStemWords, AsterixService.NO_STEM_ASSET_NAME_V2);

            put(recommendSynonyms, AsterixService.SYNONYMS_ASSET_NAME_V2);
            put(recommendPhrases, AsterixService.MULTIWORDS_ASSET_NAME_V2);
            put(JobType.enrichSynonyms, AsterixService.SYNONYMS_ASSET_NAME_V2);
            put(JobType.enrichSuggestedSynonyms, AsterixService.SYNONYMS_ASSET_NAME_V2);
            put(recommendConcepts, AsterixService.MANDATORY_TERMS_ASSET_NAME_V2);
        }};

    private static final String DICTIONARY_TYPE_AI = "ai";
    private static final String SUGGESTED = "suggested";

    @Inject
    public DictionaryUpdate(ContentDao contentDao,
                            RelevancyDao relevancyDao,
                            AnalyserService analyserService,
                            DictionaryService dictionaryService,
                            AmazonS3Client s3Client) {
        this.s3Client = s3Client;
        this.contentDao = contentDao;
        this.relevancyDao = relevancyDao;
        this.analyserService = analyserService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    public int update(String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException {
        if(!jobTypeToAssetNameV2.containsKey(jobType)) {
            String msg = "Unsupported jobType specified " + jobType;
            log.error(msg + " for site:" + siteKey);
            throw new RelevancyServiceException(400, msg);
        }
        RelevancyOutputModel output = relevancyDao.fetchRelevancyOutput(jobType, siteKey);
        int linesCount;
        if(output == null || (linesCount =
                count(jobType, output.getS3Location())) < 1)
            return 0;
        try {
            String dictionaryName = jobTypeToAssetNameV2.get(jobType);
            String type = suggestedDictionaries.contains(jobType) ?
                    SUGGESTED : DICTIONARY_TYPE_AI;
            DictionaryContext dictionaryContext = DictionaryContext
                    .getInstance(siteKey, dictionaryName,
                            DictionaryType.valueOf(type));

            File file = null;
            if(!getEnrichedJobs().contains(jobType)) {
                dictionaryContext.setS3fileUrl(output.getS3Location());
                analyserService.bulkUploadData(dictionaryContext);
                dictionaryContext.setS3fileUrl(Strings.EMPTY);
                file = analyserService.download(siteKey, dictionaryName, type, Boolean.TRUE);
                dictionaryContext.setFlushAll(true);
                dictionaryService.bulkUploadDictionary(file, dictionaryContext);
            } else if (getEnrichmentJobs().contains(jobType)){
                file = s3Client.downloadFile(output.getS3Location());
                dictionaryContext.setFlushAll(true);
                dictionaryService.bulkUploadDictionary(file, dictionaryContext);
            }
            processRecommendJobs(output.getS3Location(), siteKey, jobType,
                    output.getWorkflowId());
        } catch (AnalyserException e) {
            log.error("Error while updating the dictionary for site:"
                    + siteKey + " reason:" +e.getMessage());
            throw new RelevancyServiceException(
                    e.getStatusCode(), e.getMessage());
        }
        return linesCount;
    }

    private int count(JobType type, String s3Path) throws RelevancyServiceException {
        File file = s3Client.downloadFile(s3Path);
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            int count = 0;
            String line;
            line = br.readLine();
            while((line=br.readLine()) != null) {
                count += (type.equals(JobType.synonyms))? countSynonyms(line):1;
            }
            return count;
        } catch (IOException e) {
            String msg = "Error while loading file " + e.getMessage();
            log.error(msg);
            throw new RelevancyServiceException(500, msg);
        }
    }

    private int countSynonyms(String line) throws IOException {
        int count = 0;
        int numberOfTokens = line.split(",").length;
        if (numberOfTokens == 0)
            return 0;
        boolean isUniDirectional = line.split("=>").length > 1;
        if (!isUniDirectional) {
            //Incase of bidirectional synonyms, Then all combinations are considered as synonym
            count += numberOfTokens * (numberOfTokens - 1);
        }
        count += numberOfTokens;
        return count;
    }

    @Override
    public void reset(String cookie, String siteKey, JobType jobType,
                      ProductType productType) throws RelevancyServiceException {
        update(siteKey, jobType, productType);
    }

    private void processRecommendJobs(String s3Path,
                                     String sitekey,
                                     JobType jobType,
                                     String workflowId) {
        try {
            if (getRecommendJobs().contains(jobType)) {
                contentDao.flushQueryStats(sitekey, jobType.name());
                contentDao.storeQueryStats(s3Client.downloadFile(s3Path),
                        sitekey, jobType.name(), workflowId);
            }
        } catch (RecommendException e) {
            log.error("Error while trying to store " + jobType + " for " +
                    "sitekey[" + sitekey + "] and workflowId[" + workflowId
                    + "]: " + e.getMessage());
        }
    }
}


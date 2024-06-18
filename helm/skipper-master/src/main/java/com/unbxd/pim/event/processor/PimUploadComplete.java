package com.unbxd.pim.event.processor;

import com.google.inject.Inject;
import com.unbxd.pim.channel.PIMChannelAdapter;
import com.unbxd.pim.event.EventProcessor;
import com.unbxd.pim.event.exception.EventException;
import com.unbxd.pim.event.model.Event;
import com.unbxd.pim.exception.PIMException;
import com.unbxd.pim.imports.PIMImportAdapter;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Log4j2
public class PimUploadComplete implements EventProcessor {

    PIMImportAdapter importService;
    private static final String IMPORT_ID = "importId";

    private PIMChannelAdapter channelAdapter;

    @Inject
    public PimUploadComplete(PIMImportAdapter importService,
                             PIMChannelAdapter channelAdapter) {
        this.importService = importService;
        this.channelAdapter = channelAdapter;
    }

    @Override
    public void process(Event event) throws EventException {
        //validate if this event should be processed or not

        if(event.getData() == null || !event.getData().containsKey(IMPORT_ID)) {
            String msg = "No " + IMPORT_ID + " data passed for the event ";
            log.error( msg + event.toString());
            throw new EventException(msg);
        }
        try {
            String importId = event.getData().get(IMPORT_ID).toString();
            long total = importService.
                    importProperties(event.getCookie(), event.getOrgId(), importId,0,0).getTotal();
            int count = 500;
            for(int page=1;page<=Math.ceil((double)total/count);page++) {
                List<Map<String, Object>> properties = importService.
                        importProperties(event.getCookie(), event.getOrgId(), importId, page, count).getProperties();

                channelAdapter.updateChannelMapping(
                        event.getCookie(), event.getOrgId(), event.getAdapterId(), properties, event.getSiteKey());
            }
        } catch (PIMException e) {
            String msg = e.getMessage();
            log.error("Error while processing PIM_UPLOAD_COMPLETE event " + msg);
            throw new EventException(msg);
        }
    }
}


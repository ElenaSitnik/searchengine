package searchengine.services.intarfaces;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse getStartIndexingResponse();
    IndexingResponse getStopIndexingResponse();
}

package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponse {
    private Boolean result;
    private String error;

    public IndexingResponse(Boolean result) {
        this.result = result;
    }

    public IndexingResponse(Boolean result, String error) {
        this(result);
        this.error = error;
    }

}

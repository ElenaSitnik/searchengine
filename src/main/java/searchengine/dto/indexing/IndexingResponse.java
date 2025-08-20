package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

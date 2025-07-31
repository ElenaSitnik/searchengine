package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private Boolean result;
    private String error;

    public IndexingResponse(Boolean result) {
        this.result = result;
    }

}

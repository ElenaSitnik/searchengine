package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SearchResponse {
    private boolean result;
    private String error;
    private Integer count;
    private List<SearchData> data;

    public SearchResponse(){
    }

    public SearchResponse(boolean result, String error){
        this.result = result;
        this.error = error;
    }

    public SearchResponse(boolean result, Integer count, List<SearchData> data){
        this.result = result;
        this.count = count;
        this.data = data;
    }
}

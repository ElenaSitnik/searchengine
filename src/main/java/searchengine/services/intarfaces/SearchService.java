package searchengine.services.intarfaces;

import searchengine.dto.search.SearchResponse;

import javax.servlet.http.HttpServletRequest;

public interface SearchService {
    SearchResponse getSearchResponse(HttpServletRequest request);
}

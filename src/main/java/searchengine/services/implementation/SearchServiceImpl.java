package searchengine.services.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.services.intarfaces.SearchService;

import javax.servlet.http.HttpServletRequest;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    @Override
    public SearchResponse getSearchResponse(HttpServletRequest request) {
        String query = request.getParameter("query");
        String siteUrl = request.getParameter("site");
        Integer offset = Integer.getInteger(request.getParameter("offset"), 0);
        Integer limit = Integer.getInteger(request.getParameter("limit"), 20);



        return null;
    }
}

package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.customExeptions.SearchException;
import searchengine.dto.indexing.LemmasMaker;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.intarfaces.SearchService;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesList;

    @Override
    public SearchResponse getSearchResponse(HttpServletRequest request) {
        try{
            String query = request.getParameter("query");
            if (query.isEmpty()) {
                throw new SearchException("Задан пустой поисковый запрос");
            }

            String siteUrl = request.getParameter("site");
            if (!siteUrl.isEmpty()) {
                Optional<Site> optionalSite = siteRepository.findByUrl(siteUrl);
                if (optionalSite.isEmpty() && !isConfigSite(siteUrl)) {
                    throw new SearchException("Указанный сайт не входит в конфигурационный файл");
                } else if ((optionalSite.isEmpty() && isConfigSite(siteUrl)) ||
                        (optionalSite.isPresent() && optionalSite.get().getStatus().toString().equals("INDEXING"))) {
                    throw new SearchException("Указанный сайт ещё не был проиндексирован");
                } else if (optionalSite.get().getStatus().toString().equals("FAILED")) {
                    throw new SearchException("Указанный сайт был проиндексирован с ошибкой");
                }
            }
            getLemmas(query, siteUrl);

            Integer offset = Integer.getInteger(request.getParameter("offset"), 0);
            Integer limit = Integer.getInteger(request.getParameter("limit"), 20);
            return null;
        } catch (Exception e) {
            return new SearchResponse(false, e.getMessage());
        }




    }

    private boolean isConfigSite(String url) {
        ArrayList<searchengine.config.Site> sites = new ArrayList<>(sitesList.getSites());
        for (searchengine.config.Site site : sites) {
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private void getLemmas(String query, String siteUrl) {
        try{
            LemmasMaker lemmasMaker = LemmasMaker.getInstance();
            Map<String, Integer> queryLemmasMap = lemmasMaker.collectLemmas(query);
            Map<String, Integer> lemmasMap = new HashMap<>();
            int pagesCount;
            for (Map.Entry<String, Integer> entry : queryLemmasMap.entrySet()) {
                String key = entry.getKey();
                Integer frequency;
                if (siteUrl.isEmpty()) {
                    frequency = lemmaRepository.findAllByLemma(key).orElse(0);
                    pagesCount = pageRepository.findAll().size();
                } else {
                    Long siteId = siteRepository.findByUrl(siteUrl).get().getId();
                    Optional<Lemma> optionalLemma = lemmaRepository.findFirstByLemmaAndSiteId(key, siteId);
                    frequency = optionalLemma.isPresent() ? optionalLemma.get().getFrequency() : 0;
                    pagesCount = pageRepository.findAllBySiteId(siteId).size();
                }
                if (!frequency.equals(0)) {
                    lemmasMap.put(key, frequency);
                }
            }
            LinkedHashMap<String, Integer> lemmaSortedMap = lemmasMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            double percent = 50.0;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private List<Page> getPagesList(LinkedHashMap<String, Integer> lemmasMap) {
        List<Page> pageList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {

        }
        return pageList;
    }


}

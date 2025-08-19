package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.SitesList;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteIndexing extends RecursiveAction {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final HtmlCodeExtractor extractor;
    private final SitesList sitesList;

    @Override
    public void compute() {
        Site site;
        searchengine.config.Site configSite = null;
        try {
            List<searchengine.config.Site> sites = new ArrayList<>(sitesList.getSites());

            for (searchengine.config.Site s : sites) {
                configSite = s;
                log.info("Индексация сайта {}", s.getName());
                Optional<Site> siteFromDB = siteRepository.findByUrl(configSite.getUrl());
                site = siteFromDB.orElseGet(Site::new);
                site.setName(configSite.getName());
                site.setUrl(configSite.getUrl());
                site.setStatus(IndexingStatus.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("");

                siteRepository.save(site);
                site.setId(siteRepository.findByUrl(site.getUrl()).get().getId());

                SiteLinks siteLinks = new SiteLinks(site.getUrl(), site, extractor,
                        pageRepository, siteRepository, lemmaRepository, indexRepository);
                siteLinks.fork();
                siteLinks.join();
                site.setStatus(IndexingStatus.INDEXED);
                siteRepository.save(site);
                log.info("Закончена индексация сайта {}", site.getName());
            }
        } catch (Exception e) {
            site = siteRepository.findByUrl(configSite.getUrl()).get();
            site.setStatus(IndexingStatus.FAILED);
            site.setLastError(e.getMessage());
            siteRepository.save(site);
            log.error("Ошибка индексации сайта {}", site.getName());
        }
    }
}

package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.customExeptions.PageIndexingException;
import searchengine.customExeptions.RestartIndexingException;
import searchengine.customExeptions.StopIndexingException;
import searchengine.dto.indexing.*;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.intarfaces.IndexingService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private SiteLinks siteLinks;

    @Override
    public IndexingResponse getStartIndexingResponse() {
        Site site = null;
        try{
            if (siteRepository.findByStatus(IndexingStatus.INDEXING).isPresent()) {
                throw new RestartIndexingException("Индексация уже запущена");
            }

//            SitesList sitesList = new SitesList();
//            List<searchengine.config.Site> sites = new ArrayList<>(sitesList.getSites());
            List<searchengine.config.Site> sites = new ArrayList<>();

            searchengine.config.Site site2 = new searchengine.config.Site();
            site2.setName("name: PlayBack.Ru");
            site2.setUrl("https://www.playback.ru");
            sites.add(site2);

            searchengine.config.Site site1 = new searchengine.config.Site();
            site1.setName("Светловка");
            site1.setUrl("https://www.svetlovka.ru");
            sites.add(site1);

            searchengine.config.Site configSite;
            for (searchengine.config.Site s : sites) {
                configSite = s;
                Optional<Site> siteFromDB = siteRepository.findByUrl(configSite.getUrl());
                if (siteFromDB.isPresent()) {
                    Long siteId = siteFromDB.get().getId();
                    pageRepository.deleteAllBySiteId(siteId);
                    siteRepository.deleteById(siteId);
                }
                site = new Site();
                site.setName(configSite.getName());
                site.setUrl(configSite.getUrl());
                site.setStatus(IndexingStatus.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("");
                siteRepository.save(site);
                site.setId(siteRepository.findByUrl(site.getUrl()).get().getId());

                siteLinks = new SiteLinks(site.getUrl(), site, pageRepository, siteRepository, lemmaRepository, indexRepository);
                siteLinks.fork();
                siteLinks.join();
                site.setStatus(IndexingStatus.INDEXED);
                site.setId(siteRepository.findByUrl(site.getUrl()).get().getId());
                siteRepository.save(site);
            }
            return new IndexingResponse(true);
        } catch (RestartIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        } catch (Exception e) {
            site = siteRepository.findByUrl(site.getUrl()).get();
            site.setStatus(IndexingStatus.FAILED);
            site.setLastError(e.getMessage());
            site.setId(siteRepository.findByUrl(site.getUrl()).get().getId());
            siteRepository.save(site);

            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getStopIndexingResponse() {
        try{
            siteRepository.findByStatus(IndexingStatus.INDEXING).orElseThrow(
                    () -> new StopIndexingException("Индексация не запущена")
            );
            ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
            forkJoinPool.shutdownNow();

            Optional<Site> optionalSite = siteRepository.findByStatus(IndexingStatus.INDEXING);

            while(optionalSite.isPresent()) {
                Site site = optionalSite.get();
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
                optionalSite = siteRepository.findByStatus(IndexingStatus.INDEXING);
            }

            return new IndexingResponse(true);
        } catch (StopIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getIndexingPageResponse(String pageUrl) {
        String url = (URLDecoder.decode(pageUrl, StandardCharsets.UTF_8)).substring(4);
        Site site;
        String siteUrl = PageUrlConverter.getSiteUrlFromPageUrl(url);
        Optional<Site> siteOptional = siteRepository.findByUrl(siteUrl);

        try {
            site = siteOptional.orElseThrow(
                    () -> new PageIndexingException("Данная страница находится за пределами сайтов, \n" +
                            "указанных в конфигурационном файле")
            );
        } catch (PageIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        }

        String html = HtmlCodeExtractor.getHtmlCode(url);
        String path = url.substring(siteUrl.length());
        Long pageId = pageRepository.findFirstByPathAndSite(path, site).get().getId();
        if (pageRepository.findById(pageId).get().getCode() != 200) {
            return new IndexingResponse(false, "Данная страница была некорректно проиндексирована");
        }
        pageRepository.deleteById(pageId);
        SavePage savePage = new SavePage(pageRepository);
        Page page = savePage.savePageToDatabase(path, html, site);
        PageIndexing pageIndexing = new PageIndexing(lemmaRepository, indexRepository);
        String error = pageIndexing.indexingPage(site, page, html);

        return error.isEmpty() ? new IndexingResponse(true) : new IndexingResponse(false, error);
    }

}

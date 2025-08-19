package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
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
import searchengine.services.intarfaces.PageService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final HtmlCodeExtractor extractor;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public IndexingResponse getStartIndexingResponse() {
        try(ForkJoinPool forkJoinPool = ForkJoinPool.commonPool()) {
            if (siteRepository.findByStatus(IndexingStatus.INDEXING).isPresent()) {
                throw new RestartIndexingException("Индексация уже запущена");
            }
            SiteIndexing indexing = new SiteIndexing(pageRepository, siteRepository, lemmaRepository, indexRepository,
                    extractor, sitesList);

            forkJoinPool.execute(indexing);
            log.info("Запущена индексация");

            return new IndexingResponse(true);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getStopIndexingResponse() {
        try(ForkJoinPool forkJoinPool = ForkJoinPool.commonPool()) {
            siteRepository.findByStatus(IndexingStatus.INDEXING).orElseThrow(
                    () -> new StopIndexingException("Индексация не запущена")
            );
            forkJoinPool.shutdownNow();

            Optional<Site> optionalSite = siteRepository.findByStatus(IndexingStatus.INDEXING);

            while(optionalSite.isPresent()) {
                Site site = optionalSite.get();
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
                optionalSite = siteRepository.findByStatus(IndexingStatus.INDEXING);
            }
            log.info("Индексация остановлена пользователем");
            return new IndexingResponse(true);
        } catch (Exception e) {
            log.error(e.getMessage());
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

            String html = extractor.getHtml(url);
            String path = url.substring(siteUrl.length());
            Long pageId = pageRepository.findFirstByPathAndSite(path, site).get().getId();
            if (pageRepository.findById(pageId).get().getCode() != 200) {
                return new IndexingResponse(false, "Данная страница была некорректно проиндексирована");
            }
            pageRepository.deleteById(pageId);

            PageService pageService = new PageServiceImpl(pageRepository);
            Page page = pageService.savePageToDatabase(path, html, site);
            PageIndexing pageIndexing = new PageIndexing(lemmaRepository, indexRepository);
            String error = pageIndexing.indexingPage(site, page, html);

            return error.isEmpty() ? new IndexingResponse(true) : new IndexingResponse(false, error);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new IndexingResponse(false, e.getMessage());
        }
    }

}

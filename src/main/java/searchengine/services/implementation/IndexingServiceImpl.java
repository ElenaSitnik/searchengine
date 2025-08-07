package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.customExeptions.PageIndexingException;
import searchengine.customExeptions.RestartIndexingException;
import searchengine.customExeptions.StopIndexingException;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.intarfaces.IndexingService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Site siteModel = new Site();
        try{
            if (siteRepository.findByStatus(IndexingStatus.INDEXING).isPresent()) {
                throw new RestartIndexingException("Индексация уже запущена");
            }

//            SitesList sitesList = new SitesList();
//            List<searchengine.config.Site> sites = new ArrayList<>(sitesList.getSites());
            List<searchengine.config.Site> sites = new ArrayList<>();
            searchengine.config.Site site1 = new searchengine.config.Site();
            site1.setName("Светловка");
            site1.setUrl("https://www.svetlovka.ru");
            sites.add(site1);
            site1.setName("name: PlayBack.Ru");
            site1.setUrl("https://www.playback.ru");
            sites.add(site1);
            searchengine.config.Site site;
            for (searchengine.config.Site s : sites) {
                site = s;
                Optional<Site> siteFromDB = siteRepository.findByUrl(site.getUrl());
                if (siteFromDB.isPresent()) {
                    Long siteId = siteFromDB.get().getId();
                    siteRepository.deleteById(siteId);
                    pageRepository.deleteAllBySiteId(siteId);
                }

                siteModel.setName(site.getName());
                siteModel.setUrl(site.getUrl());
                siteModel.setStatus(IndexingStatus.INDEXING);
                siteModel.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteModel);

                siteLinks = new SiteLinks(siteModel.getUrl(), siteModel, pageRepository, siteRepository);
                siteLinks.compute();
                siteModel.setStatus(IndexingStatus.INDEXED);
                siteModel.setId(siteRepository.findByUrl(siteModel.getUrl()).get().getId());
                siteRepository.save(siteModel);
            }
            return new IndexingResponse(true);
        } catch (RestartIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        } catch (Exception e) {
            siteModel.setStatus(IndexingStatus.FAILED);
            siteModel.setLastError(e.getMessage());
            siteRepository.save(siteModel);
            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getStopIndexingResponse() {
        try{
            siteRepository.findByStatus(IndexingStatus.INDEXING).orElseThrow(
                    () -> new StopIndexingException("Индексация не запущена")
            );

            siteLinks.stopIndexing();
            Optional<Site> opt = siteRepository.findByStatus(IndexingStatus.INDEXING);

            while(opt.isPresent()) {
                Site site = opt.get();
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
                opt = siteRepository.findByStatus(IndexingStatus.INDEXING);
            }

            return new IndexingResponse(true);
        } catch (StopIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getIndexingPageResponse(String pageUrl) {
        String url = PageIndexing.getDecodedUrl(pageUrl);

        LemmasMaker lemmasMaker;
        try{
            lemmasMaker = LemmasMaker.getInstance();
        } catch (IOException e) {
            return new IndexingResponse(false, e.getMessage());
        }
        Site site;
        String siteUrl = PageIndexing.getSiteUrl(url);
        Optional<Site> siteOptional = siteRepository.findByUrl(siteUrl);

        if (siteOptional.isEmpty()) {
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле");
        } else {
            site = siteOptional.get();
        }
        String html = SiteLinks.getHtmlCode(url);
        String path = url.substring(siteUrl.length());
        Page page = savePage(site, html, path);

        Map<String, Integer> lemmasMap = lemmasMaker.collectLemmas(html);
        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            Lemma lemma = saveLemma(site, key);
            saveIndex(page, lemma, value);
        }
        return new IndexingResponse(true);
    }

    private Page savePage(Site site, String html, String path) {
        Page page = new Page();
        page.setSite(site);
        page.setCode(200);
        page.setContent(html);
        page.setPath(path);

        pageRepository.deleteByPath(path);
        pageRepository.save(page);
        return page;
    }

    private Lemma saveLemma(Site site, String key) {
        Optional<Lemma> lemmaOptional = lemmaRepository.findByLemma(key);
        Lemma lemma;
        if (lemmaOptional.isPresent()) {
            lemma = lemmaOptional.get();
            Integer frequency = lemma.getFrequency() + 1;
            lemma.setFrequency(frequency);
            lemmaRepository.save(lemma);
        } else {
            lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(key);
            lemma.setFrequency(1);
            lemmaRepository.save(lemma);
        }
        return lemma;
    }

    private void saveIndex(Page page, Lemma lemma, Integer count) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        Float rank = (float) count / lemma.getFrequency();
        index.setRank(rank);
        indexRepository.save(index);
    }
}

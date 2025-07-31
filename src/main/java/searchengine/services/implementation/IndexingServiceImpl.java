package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.customExeptions.RestartIndexingException;
import searchengine.customExeptions.StopIndexingException;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.intarfaces.IndexingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private SiteLinks siteLinks;

    @Override
    public IndexingResponse getStartIndexingResponse() {
        try{
            if (siteRepository.findByStatus(IndexingStatus.INDEXING.toString()).isPresent()) {
                throw new RestartIndexingException("Индексация уже запущена");
            }

            SitesList siteList = new SitesList();
            List<searchengine.config.Site> sites = siteList.getSites();
            searchengine.config.Site site;
            for (searchengine.config.Site s : sites) {
                site = s;
                Site siteModel = new Site();

                Long siteId = siteRepository.findByUrl(site.getUrl()).orElseThrow().getId();
                siteRepository.deleteById(siteId);
                pageRepository.deleteAllBySiteId(siteId);

                siteModel.setName(site.getName());
                siteModel.setUrl(site.getUrl());
                siteModel.setStatus(IndexingStatus.INDEXING);
                siteModel.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteModel);

                siteLinks = new SiteLinks(site.getUrl(), siteModel, pageRepository, siteRepository);
                siteLinks.compute();
                siteModel.setStatus(IndexingStatus.INDEXED);
            }
            return new IndexingResponse(true);
        } catch (RestartIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        }
    }

    @Override
    public IndexingResponse getStopIndexingResponse() {
        try{
            siteRepository.findByStatus(IndexingStatus.INDEXING.toString()).orElseThrow(
                    () -> new StopIndexingException("Индексация не запущена")
            );

            siteLinks.stopIndexing();
            Optional<Site> opt = siteRepository.findByStatus(IndexingStatus.INDEXING.toString());

            while(opt.isPresent()) {
                Site site = opt.get();
                site.setStatus(IndexingStatus.FAILED);
                siteRepository.save(site);
            }

            return new IndexingResponse(true);
        } catch (StopIndexingException e) {
            return new IndexingResponse(false, e.getMessage());
        }

    }
}

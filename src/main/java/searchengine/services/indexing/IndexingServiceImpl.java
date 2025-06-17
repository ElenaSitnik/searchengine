package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public IndexingResponse getIndexingResponse() {
        SitesList siteList = new SitesList();
        List<Site> sites = siteList.getSites();
        Site site;
        for (Site s : sites) {
            site = s;
            SiteModel siteModel = new SiteModel();

            Long siteId = siteRepository.findByURL(site.getUrl()).orElseThrow().getId();
            siteRepository.deleteById(siteId);
            pageRepository.deleteAllBySiteId(siteId);

            siteModel.setName(site.getName());
            siteModel.setUrl(site.getUrl());
            siteModel.setStatus(IndexingStatus.INDEXING);
            siteRepository.save(siteModel);

            SiteLinks link = new SiteLinks(site.getUrl(), siteModel, pageRepository, siteRepository);
            link.compute();

        }

        return null;
    }




}

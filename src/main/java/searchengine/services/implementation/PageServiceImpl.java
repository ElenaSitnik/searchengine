package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.services.intarfaces.PageService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageServiceImpl implements PageService {
    private final PageRepository pageRepository;

    @Override
    public Page savePageToDatabase(String path, String html, Site siteFromDB) {
        Page page;
        Optional<Page> pageFromDB = pageRepository.findFirstByPathAndSite(path, siteFromDB);
        synchronized (pageRepository) {
            page = pageFromDB.orElseGet(Page::new);
            page.setPath(path);
            page.setContent(html);
            page.setSite(siteFromDB);
            page.setCode(200);
            pageRepository.save(page);
        }
        log.info("Page with path {} and site id {} was saved to database", page.getPath(), siteFromDB.getId());
        return page;
    }
}

package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.Optional;

@RequiredArgsConstructor
public class SavePage {
    private final PageRepository pageRepository;

    public synchronized Page savePageToDatabase(String path, String html, Site siteFromDB) {
        Page page = null;
        Optional<Page> pageFromDB = pageRepository.findFirstByPathAndSite(path, siteFromDB);
        if (pageFromDB.isEmpty()) {
            page = new Page();
            page.setPath(path);
            page.setContent(html);
            page.setSite(siteFromDB);
            page.setCode(200);
            pageRepository.save(page);
        }
        return page;
    }
}

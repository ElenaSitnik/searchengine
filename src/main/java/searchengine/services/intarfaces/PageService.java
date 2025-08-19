package searchengine.services.intarfaces;

import searchengine.model.Page;
import searchengine.model.Site;

public interface PageService {
    Page savePageToDatabase(String path, String html, Site siteFromDB);
}

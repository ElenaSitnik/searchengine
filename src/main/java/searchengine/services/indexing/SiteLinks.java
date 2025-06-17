package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.config.ConnectionConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Component
@RequiredArgsConstructor
public class SiteLinks extends RecursiveAction {
    private final String url;
    private final SiteModel siteModel;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private ArrayList<String> links = new ArrayList<>();
    private ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration();

    public String getUrl() {
        return url;
    }

    @Override
    protected void compute() {
        linkExtraction(url);
        if (!links.isEmpty()) {
            List<SiteLinks> tasks = new ArrayList<>();
            for (String link : links) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                SiteLinks siteLinks = new SiteLinks(link, siteModel, pageRepository, siteRepository);
                siteLinks.fork();
                tasks.add(siteLinks);
            }
            tasks.forEach(ForkJoinTask::join);
        }
    }

    private void linkExtraction(String url) {
        String html;
        try {
            html = String.valueOf(Jsoup.connect(url)
                    .userAgent(connectionConfiguration.getUser())
                    .header("Accept", "text/html")
                    .header("Accept-Language", "en")
                    .referrer(connectionConfiguration.getReferrer())
                    .header("Connecting", "keep-alive")
                    .ignoreHttpErrors(true).ignoreContentType(true).followRedirects(true)
                    .get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document document = Jsoup.parse(html, url);
        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String link = element.absUrl("href");
            PageModel page = checkingPage(link);
            if (page != null) {
                page.setContent(html);
                synchronized (siteModel) {
                    siteModel.getPages().add(page);
                }
                pageRepository.save(page);
            }
        }
    }

    private PageModel checkingPage(String link) {
        PageModel page = null;

        if (link.startsWith(url) && !link.contains(".pdf") && !link.contains(".jpg")
                && !link.contains(".doc") && !link.contains("#") && !link.contains("?")) {

            int pathStart = link.indexOf(siteModel.getUrl()) + siteModel.getUrl().length() - 1;
            String path = link.substring(pathStart).strip();
            Long siteId = siteRepository.findByURL(siteModel.getUrl()).orElseThrow().getId();
            page = pageRepository.findByPathAndSiteId(path, siteId);

            if (path.length() > 1 && Objects.nonNull(page)) {
                page = new PageModel();
                page.setSite(siteRepository.findById(siteId).orElseThrow());
                page.setPath(path);
                page.setCode(200); //временно
            }
        }
        return page;
    }

}

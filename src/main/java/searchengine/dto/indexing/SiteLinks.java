package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class SiteLinks extends RecursiveAction {

    @Getter
    private final String url;
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;


    @Override
    protected void compute() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        linkExtraction(url);
        if (site.getCounter().get() < site.getLinks().size()) {
            List<SiteLinks> tasks = new ArrayList<>();
            synchronized (site) {
                while (site.getCounter().get() < site.getLinks().size()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    String link = site.getLinks().get(site.getCounter().get());
                    site.getCounter().incrementAndGet();
                    SiteLinks siteLinks = new SiteLinks(link, site, pageRepository, siteRepository, lemmaRepository, indexRepository);
                    siteLinks.fork();
                    tasks.add(siteLinks);

                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }
            }
            tasks.forEach(ForkJoinTask::join);
        }
    }

    private void linkExtraction(String url) {
        String html = HtmlCodeExtractor.getHtmlCode(url);
        Document document = Jsoup.parse(html, url);
        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String link = element.absUrl("href");
            String path = checkingPage(link);
            if (!path.isEmpty()) {
                Site siteFromDB = siteRepository.findByUrl(site.getUrl()).orElseThrow();
                SavePage savePage = new SavePage(pageRepository);
                savePage.savePageToDatabase(path, html, siteFromDB);
                Page pageFromDB = pageRepository.findFirstByPathAndSite(path, site).get();
                PageIndexing pageIndexing = new PageIndexing(lemmaRepository, indexRepository);
                pageIndexing.indexingPage(site, pageFromDB, html);
            }
        }
    }

    private String checkingPage(String link) {
        if (link.startsWith(url) && !link.contains(".pdf") && !link.contains(".jpg")
                && !link.contains(".doc") && !link.contains("#") && !link.contains("?")) {

            int pathStart = link.indexOf(site.getUrl()) + site.getUrl().length();
            String path = link.substring(pathStart).strip();
            synchronized (site) {
                if (!site.getLinks().contains(link) && path.length() > 1) {
                    site.getLinks().add(link);
                    return path;
                }
            }
        }
        return "";
    }

}

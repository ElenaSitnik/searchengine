package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.ConnectionConfiguration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;

@RequiredArgsConstructor
public class SiteLinks extends RecursiveAction {
    private final String url;
    private final Site siteModel;
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
                siteModel.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteModel);
            }
            tasks.forEach(ForkJoinTask::join);
        }
    }

    public void stopIndexing() {
        Function<ForkJoinPool, List<Runnable>> fjpListRunnable = ForkJoinPool::shutdownNow;
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
            Page page = checkingPage(link);
            if (page != null) {
                page.setContent(html);
                synchronized (siteModel) {
                    siteModel.getPages().add(page);
                }
                pageRepository.save(page);
            }
        }
    }

    private Page checkingPage(String link) {
        Page page = null;

        if (link.startsWith(url) && !link.contains(".pdf") && !link.contains(".jpg")
                && !link.contains(".doc") && !link.contains("#") && !link.contains("?")) {

            int pathStart = link.indexOf(siteModel.getUrl()) + siteModel.getUrl().length() - 1;
            String path = link.substring(pathStart).strip();
            Site site = siteRepository.findByUrl(siteModel.getUrl()).orElseThrow();
            page = pageRepository.findFirstByPathAndSite(path, site).orElseThrow();

            if (path.length() > 1) {
                page = new Page();
                page.setSite(siteRepository.findById(site.getId()).orElseThrow());
                page.setPath(path);
                page.setCode(200); //временно
            }
        }
        return page;
    }

}

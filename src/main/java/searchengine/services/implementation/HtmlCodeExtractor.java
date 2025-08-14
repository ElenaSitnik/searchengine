package searchengine.services.implementation;

import org.jsoup.Jsoup;
import searchengine.config.ConnectionConfiguration;

import java.io.IOException;

public class HtmlCodeExtractor {
    private static final ConnectionConfiguration CONNECTION_CONFIGURATION = new ConnectionConfiguration();

    public static String getHtmlCode(String url) {
        try {
            return String.valueOf(Jsoup.connect(url)
                    //.userAgent(CONNECTION_CONFIGURATION.getUserAgent())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                    .header("Accept", "text/html")
                    .header("Accept-Language", "en")
                    //.referrer(CONNECTION_CONFIGURATION.getReferrer())
                    .referrer("http://www.google.com")
                    .header("Connecting", "keep-alive")
                    .ignoreHttpErrors(true).ignoreContentType(true).followRedirects(true)
                    .get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

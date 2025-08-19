package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionConfiguration;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class HtmlCodeExtractor {

    private final ConnectionConfiguration CONNECTION_CONFIGURATION;

    public String getHtml(String url) {
        try {
            return String.valueOf(Jsoup.connect(url)
                    .userAgent(CONNECTION_CONFIGURATION.getUserAgent())
                    .header("Accept", "text/html")
                    .header("Accept-Language", "en")
                    .referrer(CONNECTION_CONFIGURATION.getReferrer())
                    .header("Connecting", "keep-alive")
                    .ignoreHttpErrors(true).ignoreContentType(true).followRedirects(true)
                    .get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

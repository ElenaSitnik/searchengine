package searchengine.services.indexing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "connection-configuration")
public class ConnectionConfiguration {
    private String user;
    private String referrer;
}

package edu.iu.uits.lms.sisredirect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sis-redirect")
@Getter
@Setter
public class ToolConfig {

   private String version;
   private String env;
}

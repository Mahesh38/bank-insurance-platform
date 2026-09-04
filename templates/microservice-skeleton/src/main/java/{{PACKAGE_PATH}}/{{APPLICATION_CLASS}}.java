package {{PACKAGE}};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Skeleton entry point for bounded context #{{CONTEXT_ID}} — {{SERVICE_NAME}}.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class {{APPLICATION_CLASS}} {

    public static void main(String[] args) {
        SpringApplication.run({{APPLICATION_CLASS}}.class, args);
    }
}

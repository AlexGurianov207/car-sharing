package com.example.carsharing.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class DatabaseUrlConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String datasourceUrl = environment.getProperty("SPRING_DATASOURCE_URL");

        if (datasourceUrl != null && datasourceUrl.startsWith("postgresql://")) {
            String separator = datasourceUrl.contains("?") ? "&" : "?";
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "renderDatabaseUrl",
                    Map.of("spring.datasource.url", "jdbc:" + datasourceUrl + separator + "stringtype=unspecified")
            ));
        }
    }
}

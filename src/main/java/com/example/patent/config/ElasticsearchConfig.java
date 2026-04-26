package com.example.patent.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String indexName;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(host, port, "http"))
            .setHttpClientConfigCallback(h -> h.setDefaultCredentialsProvider(credsProv));

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}

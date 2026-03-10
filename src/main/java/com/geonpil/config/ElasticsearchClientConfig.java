package com.geonpil.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

@Configuration
public class ElasticsearchClientConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.ssl.insecure:false}")
    private boolean sslInsecure;

    @Value("${elasticsearch.ssl.truststore-path:}")
    private String truststorePath;

    @Value("${elasticsearch.ssl.truststore-password:changeit}")
    private String truststorePassword;

    @Bean
    public RestClient restClient() {
        HttpHost httpHost = HttpHost.create(host);
        RestClientBuilder builder = RestClient.builder(httpHost);

        boolean hasCreds = username != null && !username.trim().isEmpty()
            && password != null && !password.trim().isEmpty();

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if ("https".equalsIgnoreCase(httpHost.getSchemeName()) && sslInsecure) {
                try {
                    SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                    httpClientBuilder.setSSLContext(sslContext);
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create insecure SSLContext for Elasticsearch", e);
                }
            } else if ("https".equalsIgnoreCase(httpHost.getSchemeName())
                    && truststorePath != null && !truststorePath.trim().isEmpty()
                    && Files.exists(Paths.get(truststorePath.trim()))) {
                try {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    try (InputStream is = Files.newInputStream(Paths.get(truststorePath.trim()))) {
                        ks.load(is, truststorePassword != null ? truststorePassword.toCharArray() : "changeit".toCharArray());
                    }
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ks);
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);
                    httpClientBuilder.setSSLContext(sslContext);
                } catch (Exception e) {
                    throw new IllegalStateException("Elasticsearch truststore 로드 실패: path=" + truststorePath, e);
                }
            }

            if (hasCreds) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            return httpClientBuilder;
        });

        return builder.build();
    }

    @Bean
    public JsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {
        return new RestClientTransport(restClient, jsonpMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchClientConfig.class);

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
            boolean isHttps = "https".equalsIgnoreCase(httpHost.getSchemeName());
            String trimmedTruststorePath = truststorePath != null ? truststorePath.trim() : "";
            boolean truststorePathSet = !trimmedTruststorePath.isEmpty();
            boolean truststoreExists = truststorePathSet && Files.exists(Paths.get(trimmedTruststorePath));

            if (isHttps && sslInsecure) {
                log.info("Elasticsearch SSL: insecure 모드 (인증서 검증 비활성화)");
                try {
                    SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                    httpClientBuilder.setSSLContext(sslContext);
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create insecure SSLContext for Elasticsearch", e);
                }
            } else if (isHttps && truststoreExists) {
                log.info("Elasticsearch SSL: truststore 사용 path={}", trimmedTruststorePath);
                try {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    try (InputStream is = Files.newInputStream(Paths.get(trimmedTruststorePath))) {
                        ks.load(is, truststorePassword != null ? truststorePassword.toCharArray() : "changeit".toCharArray());
                    }
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ks);
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);
                    httpClientBuilder.setSSLContext(sslContext);
                    // IP로 접속 시 서버 인증서 CN과 불일치하므로 호스트명 검증 완화
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                } catch (Exception e) {
                    throw new IllegalStateException("Elasticsearch truststore 로드 실패: path=" + trimmedTruststorePath, e);
                }
            } else if (isHttps && truststorePathSet && !truststoreExists) {
                log.warn("Elasticsearch SSL: truststore 경로가 설정됐지만 파일 없음 path={} → 기본 Java 인증서 검증 사용 (자체 서명 시 PKIX 오류 가능)", trimmedTruststorePath);
            } else if (isHttps) {
                log.info("Elasticsearch SSL: 기본 Java truststore 사용 (HTTPS, insecure=false, truststore 미설정)");
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

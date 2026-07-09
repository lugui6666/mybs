package com.whpu.mybs.log.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.hosts:http://localhost:9200}")
    private String hosts;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. 创建 ObjectMapper 并注册 JavaTimeModule
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 禁用时间戳格式，使用 ISO 8601 字符串
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. 创建 JacksonJsonpMapper，传入自定义 ObjectMapper
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);

        // 3. 创建 RestClient
        String[] hostArray = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];
        for (int i = 0; i < hostArray.length; i++) {
            String h = hostArray[i].trim();
            String[] parts = h.replace("http://", "").replace("https://", "").split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            httpHosts[i] = new HttpHost(host, port, "http");
        }
        RestClient restClient = RestClient.builder(httpHosts).build();

        // 4. 创建 Transport 并返回 Client
        RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);
        return new ElasticsearchClient(transport);
    }
}
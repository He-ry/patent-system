package com.example.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.example.patent.config.ElasticsearchConfig;
import com.example.patent.entity.PatentInfo;
import com.example.patent.mapper.PatentInfoMapper;
import com.example.patent.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatentIndexService {
    private final PatentInfoMapper patentInfoMapper;
    private final OpenAiService openAiService;
    private final ElasticsearchClient esClient;
    private final ElasticsearchConfig esConfig;

    private static final String INDEX_NAME = "patent_vectors";

    public void createIndexIfNotExists() {
        try {
            boolean exists = esClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                    .value();

            if (exists) {
                log.info("索引 {} 已存在", INDEX_NAME);
                return;
            }

            int dimensions = getEmbeddingDimensions();

            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                    )
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t))
                            .properties("college", p -> p.keyword(k -> k))
                            .properties("ipcMainClass", p -> p.keyword(k -> k))
                            .properties("legalStatus", p -> p.keyword(k -> k))
                            .properties("content", p -> p.text(t -> t))
                            .properties("embedding", p -> p
                                    .denseVector(dv -> dv
                                            .dims(dimensions)
                                            .index(true)
                                            .similarity("cosine")
                                    )
                            )
                    )
            ));

            log.info("索引 {} 创建成功，维度: {}", INDEX_NAME, dimensions);
        } catch (Exception e) {
            log.error("创建ES索引失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建ES索引失败", e);
        }
    }

    private int getEmbeddingDimensions() {
        try {
            List<Float> testEmbedding = openAiService.getEmbeddings(List.of("test")).get(0);
            return testEmbedding.size();
        } catch (Exception e) {
            log.warn("无法获取embedding维度，使用默认1024: {}", e.getMessage());
            return 1024;
        }
    }

    public int indexAllPatents() {
        createIndexIfNotExists();

        List<PatentInfo> patents = patentInfoMapper.selectList(null);
        if (patents.isEmpty()) {
            log.warn("没有专利数据需要索引");
            return 0;
        }

        log.info("开始索引 {} 条专利", patents.size());

        int batchSize = 10;
        int successCount = 0;

        for (int i = 0; i < patents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, patents.size());
            List<PatentInfo> batch = patents.subList(i, end);

            try {
                indexBatch(batch);
                successCount += batch.size();
                log.info("索引进度: {}/{}", successCount, patents.size());
            } catch (Exception e) {
                log.error("批量索引失败 (批次 {}-{}): {}", i, end, e.getMessage());
            }
        }

        log.info("专利索引完成，成功: {}", successCount);
        return successCount;
    }

    private void indexBatch(List<PatentInfo> patents) throws Exception {
        List<Map<String, Object>> docs = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (PatentInfo patent : patents) {
            String content = buildContent(patent);
            texts.add(content);

            Map<String, Object> doc = new HashMap<>();
            doc.put("id", patent.getId());
            doc.put("title", patent.getTitle());
            doc.put("college", patent.getCollege());
            doc.put("ipcMainClass", patent.getIpcMainClass());
            doc.put("legalStatus", patent.getLegalStatus());
            doc.put("content", content);
            docs.add(doc);
        }

        List<List<Float>> embeddings = openAiService.getEmbeddings(texts);

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (int i = 0; i < docs.size(); i++) {
            final Map<String, Object> doc = docs.get(i);
            doc.put("embedding", embeddings.get(i));

            final String docId = doc.get("id").toString();
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(docId)
                            .document(doc)
                    )
            );
        }

        esClient.bulk(bulkBuilder.build());
    }

    private String buildContent(PatentInfo patent) {
        StringBuilder sb = new StringBuilder();
        if (patent.getTitle() != null) {
            sb.append("专利标题: ").append(patent.getTitle()).append("。");
        }
        if (patent.getCollege() != null) {
            sb.append("所属学院: ").append(patent.getCollege()).append("。");
        }
        if (patent.getIpcMainClass() != null) {
            sb.append("IPC分类: ").append(patent.getIpcMainClass());
            if (patent.getIpcMainClassInterpretation() != null) {
                sb.append("(").append(patent.getIpcMainClassInterpretation()).append(")");
            }
            sb.append("。");
        }
        if (patent.getTechnicalProblem() != null) {
            sb.append("技术问题: ").append(patent.getTechnicalProblem()).append("。");
        }
        if (patent.getTechnicalEffect() != null) {
            sb.append("技术效果: ").append(patent.getTechnicalEffect()).append("。");
        }
        if (patent.getLegalStatus() != null) {
            sb.append("法律状态: ").append(patent.getLegalStatus()).append("。");
        }
        return sb.toString();
    }

    public List<Map<String, Object>> semanticSearch(String query, int topK) {
        return semanticSearch(query, topK, null);
    }

    public List<Map<String, Object>> semanticSearch(String query, int topK, String college) {
        try {
            List<Float> queryVector = openAiService.getEmbeddings(List.of(query)).get(0);

            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .knn(k -> k
                            .field("embedding")
                            .queryVector(queryVector)
                            .k(topK)
                            .numCandidates(topK * 2)
                    )
                    .size(topK),
                    Map.class
            );

            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    Map<String, Object> result = new HashMap<>(source);
                    result.remove("embedding");
                    result.put("_score", hit.score());

                    if (college == null || college.isEmpty() ||
                            college.equals(result.get("college"))) {
                        results.add(result);
                    }
                }
            }

            log.info("语义搜索 '{}' 返回 {} 条结果", query, results.size());
            return results;

        } catch (Exception e) {
            log.error("语义搜索失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public long getIndexedCount() {
        try {
            var response = esClient.count(c -> c.index(INDEX_NAME));
            return response.count();
        } catch (Exception e) {
            log.error("获取索引数量失败: {}", e.getMessage());
            return 0;
        }
    }

    public void deleteIndex() {
        try {
            boolean exists = esClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                    .value();

            if (exists) {
                esClient.indices().delete(d -> d.index(INDEX_NAME));
                log.info("索引 {} 已删除", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("删除索引失败: {}", e.getMessage(), e);
        }
    }
}
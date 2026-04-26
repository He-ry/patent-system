package com.example.patent.service;

import com.example.patent.dto.GraphDataResponse;
import com.example.patent.dto.GraphEdge;
import com.example.patent.dto.GraphNode;
import com.example.patent.dto.GraphOverviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private static final Set<String> SUPPORTED_FIELD_TYPES = Set.of(
            "inventor",
            "ipc_main_class_interpretation",
            "ipc_classification",
            "cpc_classification",
            "technical_subject_classification",
            "application_field_classification",
            "strategic_industry_classification",
            "technical_problem",
            "technical_effect"
    );

    private static final Set<String> ANALYSIS_RELATION_TYPES = Set.of(
            "CO_INVENTS",
            "FOCUSES_ON",
            "CO_OCCURS_WITH",
            "MAPS_TO_IPC",
            "APPLIES_TO",
            "BELONGS_TO_INDUSTRY_ANALYSIS",
            "RESEARCHES_TOPIC",
            "OWNS_TOPIC"
    );

    private static final Set<String> UNDIRECTED_RELATION_TYPES = Set.of(
            "CO_INVENTS",
            "CO_OCCURS_WITH"
    );

    private final Driver neo4jDriver;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Map<String, Object> syncAllPatentsToGraph() {
        return syncGraph(true);
    }

    @Transactional
    public Map<String, Object> syncIncremental() {
        return syncGraph(false);
    }

    @Transactional
    public Map<String, Object> rebuildAnalysisLayer() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                createConstraints(tx);
                buildAnalysisLayer(tx);
                return null;
            });
        }
        return Map.of("message", "知识图谱分析层已重建");
    }

    @Transactional
    public Map<String, Object> refreshStats() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                refreshDerivedStats(tx);
                return null;
            });
        }
        return Map.of("message", "知识图谱统计属性已刷新");
    }

    public GraphOverviewResponse getGraphOverview() {
        GraphOverviewResponse response = new GraphOverviewResponse();
        try (Session session = neo4jDriver.session()) {
            response.setNodeCounts(countByLabels(session));
            response.setRelationshipCounts(countByRelationshipTypes(session));
            response.setTopInventors(runSummaryQuery(session,
                    "MATCH (i:Inventor) " +
                            "RETURN i.name AS name, coalesce(i.patentCount, 0) AS value " +
                            "ORDER BY value DESC, name ASC LIMIT 10"));
            response.setTopTopics(runSummaryQuery(session,
                    "MATCH (t:TechTopic) " +
                            "RETURN t.name AS name, coalesce(t.patentCount, 0) AS value " +
                            "ORDER BY value DESC, name ASC LIMIT 10"));
            response.setTopIndustries(runSummaryQuery(session,
                    "MATCH (s:StrategicIndustry) " +
                            "RETURN s.name AS name, count { (s)<-[:BELONGS_TO_INDUSTRY]-(:Patent) } AS value " +
                            "ORDER BY value DESC, name ASC LIMIT 10"));
        }
        return response;
    }

    public GraphDataResponse getInventorGraph(String inventorName, Integer limit) {
        GraphAccumulator acc = new GraphAccumulator();
        int safeLimit = normalizeLimit(limit, 30);
        try (Session session = neo4jDriver.session()) {
            accumulatePaths(session, acc,
                    "MATCH p=(i:Inventor {name: $name})-[r:CO_INVENTS]-(co:Inventor) " +
                            "RETURN p ORDER BY r.patentCount DESC LIMIT $limit",
                    Values.parameters("name", inventorName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(i:Inventor {name: $name})-[r:FOCUSES_ON]->(t:TechTopic) " +
                            "RETURN p ORDER BY r.patentCount DESC LIMIT $limit",
                    Values.parameters("name", inventorName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(i:Inventor {name: $name})-[:INVENTED]->(pat:Patent) " +
                            "RETURN p ORDER BY coalesce(pat.patentValue, 0) DESC LIMIT $limit",
                    Values.parameters("name", inventorName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH (:Inventor {name: $name})-[:FOCUSES_ON]->(t:TechTopic)-[r:MAPS_TO_IPC]->(ipc:IPC) " +
                            "RETURN t, r, ipc ORDER BY r.count DESC LIMIT $limit",
                    Values.parameters("name", inventorName, "limit", safeLimit));
        }
        return acc.toResponse(Map.of(
                "view", "inventor",
                "centerType", "Inventor",
                "centerId", inventorName
        ));
    }

    public GraphDataResponse getTopicGraph(String topicName, Integer limit) {
        GraphAccumulator acc = new GraphAccumulator();
        int safeLimit = normalizeLimit(limit, 30);
        try (Session session = neo4jDriver.session()) {
            accumulatePaths(session, acc,
                    "MATCH p=(t:TechTopic {name: $name})-[r:CO_OCCURS_WITH]-(other:TechTopic) " +
                            "RETURN p ORDER BY r.count DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(t:TechTopic {name: $name})<-[r:FOCUSES_ON]-(i:Inventor) " +
                            "RETURN p ORDER BY r.patentCount DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(pat:Patent)-[:HAS_TOPIC]->(t:TechTopic {name: $name}) " +
                            "RETURN p ORDER BY coalesce(pat.patentValue, 0) DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(t:TechTopic {name: $name})-[r:MAPS_TO_IPC]->(ipc:IPC) " +
                            "RETURN p ORDER BY r.count DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(t:TechTopic {name: $name})-[r:APPLIES_TO]->(a:ApplicationField) " +
                            "RETURN p ORDER BY r.count DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));

            accumulatePaths(session, acc,
                    "MATCH p=(t:TechTopic {name: $name})-[r:BELONGS_TO_INDUSTRY_ANALYSIS]->(s:StrategicIndustry) " +
                            "RETURN p ORDER BY r.count DESC LIMIT $limit",
                    Values.parameters("name", topicName, "limit", safeLimit));
        }
        return acc.toResponse(Map.of(
                "view", "topic",
                "centerType", "TechTopic",
                "centerId", topicName
        ));
    }

    public GraphDataResponse getCoInventorNetwork(Integer minWeight, Integer limit, String inventorKeyword) {
        GraphAccumulator acc = new GraphAccumulator();
        int safeLimit = normalizeLimit(limit, 80);
        int safeMinWeight = minWeight == null ? 2 : Math.max(minWeight, 1);

        StringBuilder cypher = new StringBuilder(
                "MATCH p=(i1:Inventor)-[r:CO_INVENTS]-(i2:Inventor) " +
                        "WHERE r.patentCount >= $minWeight "
        );
        Map<String, Object> params = new HashMap<>();
        params.put("minWeight", safeMinWeight);
        params.put("limit", safeLimit);
        if (StringUtils.hasText(inventorKeyword)) {
            cypher.append("AND (i1.name CONTAINS $keyword OR i2.name CONTAINS $keyword) ");
            params.put("keyword", inventorKeyword.trim());
        }
        cypher.append("RETURN p ORDER BY r.patentCount DESC LIMIT $limit");

        try (Session session = neo4jDriver.session()) {
            accumulatePaths(session, acc, cypher.toString(), Values.value(params));
        }
        return acc.toResponse(Map.of(
                "view", "coInventor",
                "minWeight", safeMinWeight
        ));
    }

    public GraphDataResponse getTopicCooccurrence(Integer minWeight, Integer limit, String keyword) {
        GraphAccumulator acc = new GraphAccumulator();
        int safeLimit = normalizeLimit(limit, 80);
        int safeMinWeight = minWeight == null ? 2 : Math.max(minWeight, 1);

        StringBuilder cypher = new StringBuilder(
                "MATCH p=(t1:TechTopic)-[r:CO_OCCURS_WITH]-(t2:TechTopic) " +
                        "WHERE r.count >= $minWeight "
        );
        Map<String, Object> params = new HashMap<>();
        params.put("minWeight", safeMinWeight);
        params.put("limit", safeLimit);
        if (StringUtils.hasText(keyword)) {
            cypher.append("AND (t1.name CONTAINS $keyword OR t2.name CONTAINS $keyword) ");
            params.put("keyword", keyword.trim());
        }
        cypher.append("RETURN p ORDER BY r.count DESC LIMIT $limit");

        try (Session session = neo4jDriver.session()) {
            accumulatePaths(session, acc, cypher.toString(), Values.value(params));
        }
        return acc.toResponse(Map.of(
                "view", "topicCooccurrence",
                "minWeight", safeMinWeight
        ));
    }

    public GraphDataResponse getHighValueNetwork(Double minPatentValue, Integer limit, String keyword) {
        GraphAccumulator acc = new GraphAccumulator();
        int safeLimit = normalizeLimit(limit, 80);
        double safeMinPatentValue = minPatentValue == null ? 50000D : Math.max(minPatentValue, 0D);

        Map<String, Object> params = new HashMap<>();
        params.put("minPatentValue", safeMinPatentValue);
        params.put("limit", safeLimit);

        String patentFilter = "";
        if (StringUtils.hasText(keyword)) {
            patentFilter = "AND (" +
                    "pat.title CONTAINS $keyword OR " +
                    "exists { (inv:Inventor)-[:INVENTED]->(pat) WHERE inv.name CONTAINS $keyword } OR " +
                    "exists { (pat)-[:HAS_TOPIC]->(topic:TechTopic) WHERE topic.name CONTAINS $keyword }" +
                    ") ";
            params.put("keyword", keyword.trim());
        }

        try (Session session = neo4jDriver.session()) {
            accumulatePaths(session, acc,
                    "MATCH p=(inv:Inventor)-[:INVENTED]->(pat:Patent)-[:HAS_TOPIC]->(topic:TechTopic) " +
                            "WHERE coalesce(pat.patentValue, 0) >= $minPatentValue " +
                            patentFilter +
                            "RETURN p ORDER BY pat.patentValue DESC LIMIT $limit",
                    Values.value(params));

            accumulatePaths(session, acc,
                    "MATCH p=(pat:Patent)-[:BELONGS_TO_INDUSTRY]->(industry:StrategicIndustry) " +
                            "WHERE coalesce(pat.patentValue, 0) >= $minPatentValue " +
                            patentFilter +
                            "RETURN p ORDER BY pat.patentValue DESC LIMIT $limit",
                    Values.value(params));
        }

        return acc.toResponse(Map.of(
                "view", "highValue",
                "minPatentValue", safeMinPatentValue
        ));
    }

    public GraphDataResponse getShortestPath(String fromType, String fromId, String toType, String toId, Integer maxDepth) {
        String fromLabel = resolveNodeLabel(fromType);
        String toLabel = resolveNodeLabel(toType);
        String fromKey = resolveIdentifierKey(fromLabel);
        String toKey = resolveIdentifierKey(toLabel);
        int safeDepth = maxDepth == null ? 6 : Math.max(1, Math.min(maxDepth, 10));

        GraphAccumulator acc = new GraphAccumulator();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(
                    "MATCH (from:" + fromLabel + " {" + fromKey + ": $fromId}), " +
                            "(to:" + toLabel + " {" + toKey + ": $toId}) " +
                            "MATCH p = shortestPath((from)-[*.." + safeDepth + "]-(to)) " +
                            "RETURN p",
                    Values.parameters("fromId", fromId, "toId", toId)
            );

            while (result.hasNext()) {
                Record record = result.next();
                if (record.containsKey("p") && !record.get("p").isNull()) {
                    org.neo4j.driver.types.Path path = record.get("p").asPath();
                    path.nodes().forEach(acc::addNode);
                    path.relationships().forEach(acc::addRelationship);
                }
            }
        }

        return acc.toResponse(Map.of(
                "view", "path",
                "fromType", fromLabel,
                "fromId", fromId,
                "toType", toLabel,
                "toId", toId,
                "maxDepth", safeDepth
        ));
    }

    public List<Map<String, Object>> search(String keyword, String type, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit, 12);
        String normalizedType = StringUtils.hasText(type) ? type.trim() : "";
        String nodeLabel = switch (normalizedType) {
            case "inventor" -> "Inventor";
            case "patent" -> "Patent";
            case "topic" -> "TechTopic";
            case "ipc" -> "IPC";
            case "cpc" -> "CPC";
            case "applicationField" -> "ApplicationField";
            case "industry" -> "StrategicIndustry";
            case "college" -> "College";
            case "assignee" -> "AssigneeOrg";
            case "problem" -> "Problem";
            case "effect" -> "Effect";
            default -> null;
        };

        String cypher;
        if (nodeLabel == null) {
            cypher = "MATCH (n) " +
                    "WHERE (" +
                    "   (n.name IS NOT NULL AND n.name CONTAINS $keyword) OR " +
                    "   (n.title IS NOT NULL AND n.title CONTAINS $keyword) OR " +
                    "   (n.code IS NOT NULL AND n.code CONTAINS $keyword) OR " +
                    "   (n.id IS NOT NULL AND n.id CONTAINS $keyword)" +
                    ") " +
                    "RETURN labels(n)[0] AS type, " +
                    "       coalesce(n.name, n.title, n.code, n.id) AS label, " +
                    "       coalesce(n.id, n.code, n.name, n.title) AS entityKey " +
                    "ORDER BY type ASC, label ASC LIMIT $limit";
        } else if ("Patent".equals(nodeLabel)) {
            cypher = "MATCH (n:Patent) " +
                    "WHERE n.title CONTAINS $keyword OR n.id CONTAINS $keyword " +
                    "RETURN 'Patent' AS type, n.title AS label, n.id AS entityKey " +
                    "ORDER BY label ASC LIMIT $limit";
        } else if ("IPC".equals(nodeLabel) || "CPC".equals(nodeLabel)) {
            cypher = "MATCH (n:" + nodeLabel + ") " +
                    "WHERE n.code CONTAINS $keyword " +
                    "RETURN '" + nodeLabel + "' AS type, n.code AS label, n.code AS entityKey " +
                    "ORDER BY label ASC LIMIT $limit";
        } else {
            cypher = "MATCH (n:" + nodeLabel + ") " +
                    "WHERE n.name CONTAINS $keyword " +
                    "RETURN '" + nodeLabel + "' AS type, n.name AS label, n.name AS entityKey " +
                    "ORDER BY label ASC LIMIT $limit";
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("keyword", keyword.trim(), "limit", safeLimit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("type", valueToObject(record.get("type")));
                row.put("label", valueToObject(record.get("label")));
                row.put("entityKey", valueToObject(record.get("entityKey")));
                results.add(row);
            }
        }
        return results;
    }

    public List<Map<String, Object>> getPatentTitles(List<String> patentIds) {
        if (patentIds == null || patentIds.isEmpty()) return List.of();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(
                    "MATCH (p:Patent) WHERE p.id IN $ids " +
                    "RETURN p.id AS id, p.title AS title, p.applicationNumber AS applicationNumber",
                    Values.parameters("ids", patentIds)
            );
            List<Map<String, Object>> list = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", valueToObject(record.get("id")));
                row.put("title", valueToObject(record.get("title")));
                row.put("applicationNumber", valueToObject(record.get("applicationNumber")));
                list.add(row);
            }
            return list;
        }
    }

    private Map<String, Object> syncGraph(boolean clearExistingGraph) {
        log.info("Graph sync started, clearExistingGraph={}", clearExistingGraph);
        GraphSyncPayload payload = loadSyncPayload();

        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                createConstraints(tx);
                return null;
            });

            if (clearExistingGraph) {
                session.executeWrite(tx -> {
                    log.info("Clearing existing graph");
                    tx.run("MATCH (n) DETACH DELETE n").consume();
                    return null;
                });
            }

            int index = 0;
            for (Map<String, Object> patent : payload.patents()) {
                index++;
                if (index == 1 || index % 50 == 0) {
                    log.info("Syncing patent facts: {}/{}", index, payload.patents().size());
                }
                session.executeWrite(tx -> {
                    syncPatentFacts(tx, patent, payload.fieldsByPatentId().getOrDefault(asString(patent.get("id")), List.of()));
                    return null;
                });
            }

            session.executeWrite(tx -> {
                buildAnalysisLayer(tx);
                return null;
            });
        } catch (Exception e) {
            log.error("Graph sync failed", e);
            throw e;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", clearExistingGraph ? "full" : "incremental");
        result.put("patentCount", payload.patents().size());
        result.put("fieldCount", payload.fieldCount());
        result.put("message", clearExistingGraph ? "知识图谱已按新模型完成全量重建" : "知识图谱已完成增量同步并重建分析层");
        return result;
    }

    private GraphSyncPayload loadSyncPayload() {
        List<Map<String, Object>> patents = jdbcTemplate.queryForList(
                "SELECT id, title, application_number, application_date, application_year, grant_date, grant_year, " +
                        "patent_type, legal_status, ipc_main_class, patent_value, " +
                        "technical_value, market_value, cited_patents, cited_in_5_years, claims_count, college, " +
                        "current_assignee, original_assignee, transferor, transferee, license_type, license_count " +
                        "FROM patent_info WHERE deleted = 0"
        );
        log.info("Loaded patents: {}", patents.size());

        List<Map<String, Object>> fields = jdbcTemplate.queryForList(
                "SELECT patent_id, field_type, field_value, seq FROM patent_info_field"
        );
        log.info("Loaded patent fields: {}", fields.size());

        Map<String, List<Map<String, Object>>> fieldsByPatentId = fields.stream()
                .filter(field -> field != null
                        && StringUtils.hasText(asString(field.get("patent_id")))
                        && StringUtils.hasText(asString(field.get("field_type")))
                        && SUPPORTED_FIELD_TYPES.contains(asString(field.get("field_type"))))
                .collect(Collectors.groupingBy(field -> asString(field.get("patent_id"))));

        return new GraphSyncPayload(patents, fieldsByPatentId, fields.size());
    }

    private void createConstraints(TransactionContext tx) {
        tx.run("CREATE CONSTRAINT patent_id IF NOT EXISTS FOR (n:Patent) REQUIRE n.id IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT inventor_name IF NOT EXISTS FOR (n:Inventor) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT topic_name IF NOT EXISTS FOR (n:TechTopic) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT ipc_code IF NOT EXISTS FOR (n:IPC) REQUIRE n.code IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT cpc_code IF NOT EXISTS FOR (n:CPC) REQUIRE n.code IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT app_field_name IF NOT EXISTS FOR (n:ApplicationField) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT industry_name IF NOT EXISTS FOR (n:StrategicIndustry) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT college_name IF NOT EXISTS FOR (n:College) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT assignee_name IF NOT EXISTS FOR (n:AssigneeOrg) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT problem_name IF NOT EXISTS FOR (n:Problem) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT effect_name IF NOT EXISTS FOR (n:Effect) REQUIRE n.name IS UNIQUE").consume();
        tx.run("CREATE CONSTRAINT ipc_interp_name IF NOT EXISTS FOR (n:IPCInterpretation) REQUIRE n.name IS UNIQUE").consume();
    }

    private void syncPatentFacts(TransactionContext tx, Map<String, Object> patent, List<Map<String, Object>> fields) {
        Map<String, Object> patentProps = new LinkedHashMap<>();
        String patentId = asString(patent.get("id"));
        patentProps.put("id", patentId);
        patentProps.put("title", asString(patent.get("title")));
        patentProps.put("applicationNumber", asString(patent.get("application_number")));
        patentProps.put("applicationDate", toDateString(patent.get("application_date")));
        patentProps.put("applicationYear", asInteger(patent.get("application_year")));
        patentProps.put("grantDate", toDateString(patent.get("grant_date")));
        patentProps.put("grantYear", asInteger(patent.get("grant_year")));
        patentProps.put("patentType", asString(patent.get("patent_type")));
        patentProps.put("legalStatus", asString(patent.get("legal_status")));
        patentProps.put("ipcMainClass", asString(patent.get("ipc_main_class")));
        patentProps.put("patentValue", asDouble(patent.get("patent_value")));
        patentProps.put("technicalValue", asDouble(patent.get("technical_value")));
        patentProps.put("marketValue", asDouble(patent.get("market_value")));
        patentProps.put("citedPatents", asInteger(patent.get("cited_patents")));
        patentProps.put("citedIn5Years", asInteger(patent.get("cited_in_5_years")));
        patentProps.put("claimsCount", asInteger(patent.get("claims_count")));
        patentProps.put("college", asString(patent.get("college")));
        patentProps.put("currentAssignee", asString(patent.get("current_assignee")));
        patentProps.put("originalAssignee", asString(patent.get("original_assignee")));
        patentProps.put("licenseType", asString(patent.get("license_type")));
        patentProps.put("licenseCount", asInteger(patent.get("license_count")));

        tx.run("MERGE (p:Patent {id: $id}) SET p += $props",
                Values.parameters("id", patentId, "props", patentProps)).consume();

        mergeNodeAndEdge(tx, "College", "name", asString(patent.get("college")), "Patent", "id", patentId, "BELONGS_TO_COLLEGE", Map.of());
        mergeNodeAndEdge(tx, "AssigneeOrg", "name", asString(patent.get("current_assignee")), "Patent", "id", patentId, "ASSIGNED_TO_CURRENT", Map.of("role", "current"));
        mergeNodeAndEdge(tx, "AssigneeOrg", "name", asString(patent.get("original_assignee")), "Patent", "id", patentId, "ASSIGNED_TO_ORIGINAL", Map.of("role", "original"));
        mergeNodeAndEdge(tx, "AssigneeOrg", "name", asString(patent.get("transferor")), "Patent", "id", patentId, "TRANSFER_FROM", Map.of());
        mergeNodeAndEdge(tx, "AssigneeOrg", "name", asString(patent.get("transferee")), "Patent", "id", patentId, "TRANSFER_TO", Map.of());

        Map<String, List<Map<String, Object>>> grouped = fields.stream()
                .filter(field -> StringUtils.hasText(asString(field.get("field_value"))))
                .collect(Collectors.groupingBy(field -> asString(field.get("field_type"))));

        mergeFields(tx, patentId, grouped.get("inventor"), "Inventor", "name", "INVENTED", true);
        mergeFields(tx, patentId, grouped.get("ipc_classification"), "IPC", "code", "HAS_IPC", false);
        mergeFields(tx, patentId, grouped.get("cpc_classification"), "CPC", "code", "HAS_CPC", false);
        mergeFields(tx, patentId, grouped.get("ipc_main_class_interpretation"), "IPCInterpretation", "name", "HAS_IPC_INTERPRETATION", false);
        mergeFields(tx, patentId, grouped.get("technical_subject_classification"), "TechTopic", "name", "HAS_TOPIC", false);
        mergeFields(tx, patentId, grouped.get("application_field_classification"), "ApplicationField", "name", "HAS_APPLICATION_FIELD", false);
        mergeFields(tx, patentId, grouped.get("strategic_industry_classification"), "StrategicIndustry", "name", "BELONGS_TO_INDUSTRY", false);
        mergeFields(tx, patentId, grouped.get("technical_problem"), "Problem", "name", "SOLVES", false);
        mergeFields(tx, patentId, grouped.get("technical_effect"), "Effect", "name", "ACHIEVES", false);
    }

    private void mergeFields(TransactionContext tx, String patentId, List<Map<String, Object>> fields,
                             String label, String propertyKey, String relType, boolean fromNodeToPatent) {
        if (fields == null || fields.isEmpty()) {
            return;
        }

        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            String value = normalizeText(asString(field.get("field_value")));
            Integer seq = asInteger(field.get("seq"));
            String uniqueKey = value + "#" + (seq == null ? -1 : seq);
            if (!StringUtils.hasText(value) || !seen.add(uniqueKey)) {
                continue;
            }

            Map<String, Object> edgeProps = new LinkedHashMap<>();
            if (seq != null) {
                edgeProps.put("seq", seq);
            }
            edgeProps.put("source", "patent_info_field." + asString(field.get("field_type")));

            if (fromNodeToPatent) {
                tx.run("MERGE (n:" + label + " {" + propertyKey + ": $value}) " +
                                "MERGE (p:Patent {id: $patentId}) " +
                                "MERGE (n)-[r:" + relType + "]->(p) " +
                                "SET r += $edgeProps",
                        Values.parameters("value", value, "patentId", patentId, "edgeProps", edgeProps)).consume();
            } else {
                tx.run("MERGE (n:" + label + " {" + propertyKey + ": $value}) " +
                                "MERGE (p:Patent {id: $patentId}) " +
                                "MERGE (p)-[r:" + relType + "]->(n) " +
                                "SET r += $edgeProps",
                        Values.parameters("value", value, "patentId", patentId, "edgeProps", edgeProps)).consume();
            }
        }
    }

    private void mergeNodeAndEdge(TransactionContext tx, String targetLabel, String targetKey, String targetValue,
                                  String sourceLabel, String sourceKey, String sourceValue,
                                  String relType, Map<String, Object> relProps) {
        String normalized = normalizeText(targetValue);
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        tx.run("MERGE (t:" + targetLabel + " {" + targetKey + ": $targetValue}) " +
                        "MERGE (s:" + sourceLabel + " {" + sourceKey + ": $sourceValue}) " +
                        "MERGE (s)-[r:" + relType + "]->(t) " +
                        "SET r += $relProps",
                Values.parameters(
                        "targetValue", normalized,
                        "sourceValue", sourceValue,
                        "relProps", relProps
                )).consume();
    }

    private void buildAnalysisLayer(TransactionContext tx) {
        deleteAnalysisRelations(tx);

        tx.run(
                "MATCH (i1:Inventor)-[:INVENTED]->(p:Patent)<-[:INVENTED]-(i2:Inventor) " +
                        "WHERE i1.name < i2.name " +
                        "WITH i1, i2, collect(DISTINCT p.id) AS patentIds, count(DISTINCT p) AS patentCount " +
                        "MERGE (i1)-[r:CO_INVENTS]->(i2) " +
                        "SET r.patentIds = patentIds, r.patentCount = patentCount, r.weight = toFloat(patentCount)"
        ).consume();

        tx.run(
                "MATCH (i:Inventor)-[:INVENTED]->(p:Patent)-[:HAS_TOPIC]->(t:TechTopic) " +
                        "WITH i, t, count(DISTINCT p) AS patentCount, " +
                        "sum(CASE WHEN coalesce(p.patentValue, 0) >= 50000 THEN 1 ELSE 0 END) AS highValuePatentCount " +
                        "MERGE (i)-[r:FOCUSES_ON]->(t) " +
                        "SET r.patentCount = patentCount, r.highValuePatentCount = highValuePatentCount, r.weight = toFloat(patentCount)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:HAS_TOPIC]->(t1:TechTopic), (p)-[:HAS_TOPIC]->(t2:TechTopic) " +
                        "WHERE t1.name < t2.name " +
                        "WITH t1, t2, collect(DISTINCT p.id) AS patentIds, count(DISTINCT p) AS cnt " +
                        "MERGE (t1)-[r:CO_OCCURS_WITH]->(t2) " +
                        "SET r.count = cnt, r.patentIds = patentIds, r.weight = toFloat(cnt)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:HAS_TOPIC]->(t:TechTopic), (p)-[:HAS_IPC]->(ipc:IPC) " +
                        "WITH t, ipc, count(DISTINCT p) AS cnt " +
                        "MERGE (t)-[r:MAPS_TO_IPC]->(ipc) " +
                        "SET r.count = cnt, r.weight = toFloat(cnt)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:HAS_TOPIC]->(t:TechTopic), (p)-[:HAS_APPLICATION_FIELD]->(a:ApplicationField) " +
                        "WITH t, a, count(DISTINCT p) AS cnt " +
                        "MERGE (t)-[r:APPLIES_TO]->(a) " +
                        "SET r.count = cnt, r.weight = toFloat(cnt)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:HAS_TOPIC]->(t:TechTopic), (p)-[:BELONGS_TO_INDUSTRY]->(s:StrategicIndustry) " +
                        "WITH t, s, count(DISTINCT p) AS cnt " +
                        "MERGE (t)-[r:BELONGS_TO_INDUSTRY_ANALYSIS]->(s) " +
                        "SET r.count = cnt, r.weight = toFloat(cnt)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:BELONGS_TO_COLLEGE]->(c:College), (p)-[:HAS_TOPIC]->(t:TechTopic) " +
                        "WITH c, t, count(DISTINCT p) AS cnt " +
                        "MERGE (c)-[r:RESEARCHES_TOPIC]->(t) " +
                        "SET r.patentCount = cnt, r.weight = toFloat(cnt)"
        ).consume();

        tx.run(
                "MATCH (p:Patent)-[:ASSIGNED_TO_CURRENT|ASSIGNED_TO_ORIGINAL]->(o:AssigneeOrg), (p)-[:HAS_TOPIC]->(t:TechTopic) " +
                        "WITH o, t, count(DISTINCT p) AS cnt " +
                        "MERGE (o)-[r:OWNS_TOPIC]->(t) " +
                        "SET r.patentCount = cnt, r.weight = toFloat(cnt)"
        ).consume();

        refreshDerivedStats(tx);
    }

    private void deleteAnalysisRelations(TransactionContext tx) {
        String relationPattern = ANALYSIS_RELATION_TYPES.stream()
                .sorted()
                .collect(Collectors.joining("|"));
        tx.run("MATCH ()-[r:" + relationPattern + "]->() DELETE r").consume();
    }

    private void refreshDerivedStats(TransactionContext tx) {
        tx.run(
                "MATCH (i:Inventor)-[:INVENTED]->(p:Patent) " +
                        "WITH i, count(DISTINCT p) AS patentCount, " +
                        "sum(CASE WHEN p.legalStatus = '已授权' THEN 1 ELSE 0 END) AS grantedCount, " +
                        "sum(CASE WHEN coalesce(p.patentValue, 0) >= 50000 THEN 1 ELSE 0 END) AS highValuePatentCount, " +
                        "avg(coalesce(p.patentValue, 0)) AS avgPatentValue " +
                        "SET i.patentCount = patentCount, " +
                        "    i.grantedCount = grantedCount, " +
                        "    i.highValuePatentCount = highValuePatentCount, " +
                        "    i.avgPatentValue = avgPatentValue"
        ).consume();

        tx.run(
                "MATCH (t:TechTopic)<-[:HAS_TOPIC]-(p:Patent) " +
                        "WITH t, count(DISTINCT p) AS patentCount, " +
                        "sum(CASE WHEN coalesce(p.patentValue, 0) >= 50000 THEN 1 ELSE 0 END) AS highValuePatentCount, " +
                        "avg(coalesce(p.patentValue, 0)) AS avgPatentValue " +
                        "SET t.patentCount = patentCount, " +
                        "    t.highValuePatentCount = highValuePatentCount, " +
                        "    t.avgPatentValue = avgPatentValue"
        ).consume();

        tx.run(
                "MATCH (c:College)<-[:BELONGS_TO_COLLEGE]-(p:Patent) " +
                        "WITH c, count(DISTINCT p) AS patentCount " +
                        "SET c.patentCount = patentCount"
        ).consume();

        tx.run(
                "MATCH (o:AssigneeOrg)<-[:ASSIGNED_TO_CURRENT|ASSIGNED_TO_ORIGINAL]-(p:Patent) " +
                        "WITH o, count(DISTINCT p) AS patentCount " +
                        "SET o.patentCount = patentCount"
        ).consume();
    }

    private Map<String, Long> countByLabels(Session session) {
        Map<String, Long> result = new LinkedHashMap<>();
        Result query = session.run(
                "MATCH (n) UNWIND labels(n) AS label RETURN label, count(*) AS cnt ORDER BY cnt DESC"
        );
        while (query.hasNext()) {
            Record record = query.next();
            result.put(record.get("label").asString(), record.get("cnt").asLong());
        }
        return result;
    }

    private Map<String, Long> countByRelationshipTypes(Session session) {
        Map<String, Long> result = new LinkedHashMap<>();
        Result query = session.run(
                "MATCH ()-[r]->() RETURN type(r) AS type, count(*) AS cnt ORDER BY cnt DESC"
        );
        while (query.hasNext()) {
            Record record = query.next();
            result.put(record.get("type").asString(), record.get("cnt").asLong());
        }
        return result;
    }

    private List<Map<String, Object>> runSummaryQuery(Session session, String cypher) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Result result = session.run(cypher);
        while (result.hasNext()) {
            Record record = result.next();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", valueToObject(record.get("name")));
            row.put("value", valueToObject(record.get("value")));
            rows.add(row);
        }
        return rows;
    }

    private void accumulatePaths(Session session, GraphAccumulator acc, String cypher, Value params) {
        Result result = session.run(cypher, params);
        while (result.hasNext()) {
            Record record = result.next();
            if (record.containsKey("p") && !record.get("p").isNull()) {
                org.neo4j.driver.types.Path path = record.get("p").asPath();
                path.nodes().forEach(acc::addNode);
                path.relationships().forEach(acc::addRelationship);
                continue;
            }

            addNodeIfPresent(record, "t", acc);
            addNodeIfPresent(record, "ipc", acc);
            addNodeIfPresent(record, "a", acc);
            addNodeIfPresent(record, "s", acc);
            addNodeIfPresent(record, "pat", acc);
            addNodeIfPresent(record, "inv", acc);
            addNodeIfPresent(record, "industry", acc);
            if (record.containsKey("r") && !record.get("r").isNull()) {
                acc.addRelationship(record.get("r").asRelationship());
            }
        }
    }

    private void addNodeIfPresent(Record record, String key, GraphAccumulator acc) {
        if (record.containsKey(key) && !record.get(key).isNull()) {
            acc.addNode(record.get(key).asNode());
        }
    }

    private String resolveNodeLabel(String type) {
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("节点类型不能为空");
        }
        return switch (type.trim()) {
            case "Patent", "patent" -> "Patent";
            case "Inventor", "inventor" -> "Inventor";
            case "TechTopic", "topic" -> "TechTopic";
            case "IPC", "ipc" -> "IPC";
            case "CPC", "cpc" -> "CPC";
            case "ApplicationField", "applicationField" -> "ApplicationField";
            case "StrategicIndustry", "industry" -> "StrategicIndustry";
            case "College", "college" -> "College";
            case "AssigneeOrg", "assignee" -> "AssigneeOrg";
            case "Problem", "problem" -> "Problem";
            case "Effect", "effect" -> "Effect";
            case "IPCInterpretation", "ipcInterpretation" -> "IPCInterpretation";
            default -> throw new IllegalArgumentException("不支持的节点类型: " + type);
        };
    }

    private String resolveIdentifierKey(String label) {
        return switch (label) {
            case "Patent" -> "id";
            case "IPC", "CPC" -> "code";
            default -> "name";
        };
    }

    private int normalizeLimit(Integer limit, int defaultValue) {
        if (limit == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toDateString(Object date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        if (date instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        if (date instanceof Date utilDate) {
            return utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        return String.valueOf(date);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object valueToObject(Value value) {
        return value == null || value.isNull() ? null : value.asObject();
    }

    private GraphNode mapNode(org.neo4j.driver.types.Node node) {
        GraphNode graphNode = new GraphNode();
        String type = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Unknown";
        graphNode.setType(type);
        graphNode.setId(node.elementId());
        graphNode.setLabel(displayLabel(node));
        graphNode.setSize(suggestNodeSize(type));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("_entityKey", identifierValue(node, type));
        node.keys().forEach(key -> {
            Value value = node.get(key);
            if (!value.isNull()) {
                props.put(key, value.asObject());
            }
        });
        graphNode.setProperties(props);
        return graphNode;
    }

    private GraphEdge mapEdge(org.neo4j.driver.types.Relationship relationship) {
        GraphEdge edge = new GraphEdge();
        edge.setId(relationship.type() + ":" + relationship.elementId());
        edge.setType(relationship.type());
        edge.setDirected(!UNDIRECTED_RELATION_TYPES.contains(relationship.type()));
        edge.setSource(String.valueOf(relationship.startNodeElementId()));
        edge.setTarget(String.valueOf(relationship.endNodeElementId()));

        Map<String, Object> props = new LinkedHashMap<>();
        relationship.keys().forEach(key -> {
            Value value = relationship.get(key);
            if (!value.isNull()) {
                props.put(key, value.asObject());
            }
        });
        edge.setProperties(props);

        Object weight = props.get("weight");
        if (weight instanceof Number number) {
            edge.setWeight(number.doubleValue());
        } else if (props.get("patentCount") instanceof Number number) {
            edge.setWeight(number.doubleValue());
        } else if (props.get("count") instanceof Number number) {
            edge.setWeight(number.doubleValue());
        } else {
            edge.setWeight(1.0);
        }
        return edge;
    }

    private String identifierValue(org.neo4j.driver.types.Node node, String type) {
        return switch (type) {
            case "Patent" -> node.get("id").asString();
            case "IPC", "CPC" -> node.get("code").asString();
            default -> node.containsKey("name") ? node.get("name").asString() : node.elementId();
        };
    }

    private String displayLabel(org.neo4j.driver.types.Node node) {
        if (node.containsKey("title") && !node.get("title").isNull()) {
            return node.get("title").asString();
        }
        if (node.containsKey("name") && !node.get("name").isNull()) {
            return node.get("name").asString();
        }
        if (node.containsKey("code") && !node.get("code").isNull()) {
            return node.get("code").asString();
        }
        if (node.containsKey("id") && !node.get("id").isNull()) {
            return node.get("id").asString();
        }
        return node.elementId();
    }

    private int suggestNodeSize(String type) {
        return switch (type) {
            case "Patent" -> 30;
            case "Inventor" -> 24;
            case "TechTopic" -> 22;
            case "IPC", "CPC" -> 20;
            case "StrategicIndustry", "ApplicationField" -> 18;
            case "IPCInterpretation" -> 17;
            default -> 16;
        };
    }

    private final class GraphAccumulator {
        private final List<GraphNode> nodes = new ArrayList<>();
        private final List<GraphEdge> edges = new ArrayList<>();
        private final Set<String> nodeIds = new HashSet<>();
        private final Set<String> edgeIds = new HashSet<>();

        void addNode(org.neo4j.driver.types.Node node) {
            GraphNode graphNode = mapNode(node);
            if (nodeIds.add(graphNode.getId())) {
                nodes.add(graphNode);
            }
        }

        void addRelationship(org.neo4j.driver.types.Relationship relationship) {
            GraphEdge edge = mapEdge(relationship);
            if (edgeIds.add(edge.getId())) {
                edges.add(edge);
            }
        }

        GraphDataResponse toResponse(Map<String, Object> summary) {
            GraphDataResponse response = new GraphDataResponse();
            response.setNodes(nodes);
            response.setEdges(edges);
            Map<String, Object> finalSummary = new LinkedHashMap<>(summary);
            finalSummary.put("nodeCount", nodes.size());
            finalSummary.put("edgeCount", edges.size());
            response.setSummary(finalSummary);
            return response;
        }
    }

    private record GraphSyncPayload(
            List<Map<String, Object>> patents,
            Map<String, List<Map<String, Object>>> fieldsByPatentId,
            int fieldCount
    ) {
    }
}

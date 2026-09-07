package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.MethodCallInfo;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EC101_RepeatedCallInLoop implements EcoRule {

    @Override
    public String getRuleId() { return "EC101"; }

    @Override
    public String getName() { return "Repeated Database Call in Loop"; }

    @Override
    public String getDescription() {
        return "Executes database queries or remote calls inside a loop body (N+1 query problem), causing high I/O and latency.";
    }

    @Override
    public String getSeverity() { return "HIGH"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (MethodCallInfo call : model.getAllMethodCalls()) {
            if (!call.isInLoop()) continue;

            boolean isDbCall = isDatabaseMethod(call, callGraph);
            if (isDbCall) {
                StaticIssue issue = new StaticIssue();
                issue.setRuleId(getRuleId());
                issue.setSeverity(getSeverity());
                issue.setConfidence("HIGH");
                issue.setFileName(call.getFilePath());
                issue.setLineNumber(call.getLineNumber());
                issue.setClassName(call.getCallerClassName());
                issue.setMethodName(call.getCallerMethodName());
                issue.setCallPath(call.getCallerFullMethodId() + " -> " + call.getCalleeName());
                issue.setExplanation("Database query '" + call.getCalleeName() + "' called inside a loop at line " + call.getLineNumber() + ".");
                issue.setRecommendation("Refactor to batch fetch data before entering the loop (e.g. repo.findAllById(ids)).");
                issue.setTradeOffs("Requires storing fetched entities in memory, but reduces network/DB roundtrips from N to 1.");
                issues.add(issue);
            }
        }

        return issues;
    }

    private static final List<String> DB_CLASS_PATTERNS = List.of(
            "Repository", "Dao", "DAO", "JdbcTemplate", "EntityManager",
            "CrudRepository", "JpaRepository", "MongoRepository", "RedisTemplate"
    );

    private static final List<String> DB_METHOD_PATTERNS = List.of(
            "findById", "findAll", "findBy", "save", "saveAll", "delete", "deleteById",
            "query", "queryForList", "queryForObject", "executeQuery", "getConnection",
            "fetch", "fetchAll", "select", "insert", "update", "count", "exists"
    );

    private boolean isDatabaseMethod(MethodCallInfo call, CallGraph callGraph) {
        String scope = call.getScope();
        String callee = call.getCalleeName();
        String target = call.getResolvedTarget();

        boolean classMatch = DB_CLASS_PATTERNS.stream()
                .anyMatch(p -> scope != null && scope.contains(p));
        boolean methodMatch = DB_METHOD_PATTERNS.stream()
                .anyMatch(p -> callee != null && callee.equalsIgnoreCase(p));

        if (classMatch || methodMatch) return true;

        if (target != null) {
            return callGraph.reachableFrom(call.getCallerFullMethodId()).stream()
                    .anyMatch(node -> {
                        String[] parts = node.split("#");
                        String cls = parts.length > 0 ? parts[0] : "";
                        String mth = parts.length > 1 ? parts[1] : "";
                        return DB_CLASS_PATTERNS.stream().anyMatch(cls::contains) ||
                               DB_METHOD_PATTERNS.stream().anyMatch(mth::equalsIgnoreCase);
                    });
        }

        return false;
    }
}

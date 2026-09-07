package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.MethodCallInfo;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EC103_CollectionLookupInLoop implements EcoRule {

    @Override
    public String getRuleId() { return "EC103"; }

    @Override
    public String getName() { return "Linear Collection Lookup in Loop"; }

    @Override
    public String getDescription() {
        return "Invokes List.contains() or List.indexOf() inside a loop, resulting in O(N*M) linear scanning complexity.";
    }

    @Override
    public String getSeverity() { return "MEDIUM"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (MethodCallInfo call : model.getAllMethodCalls()) {
            if (!call.isInLoop()) continue;

            String callee = call.getCalleeName();
            if ("contains".equals(callee) || "indexOf".equals(callee)) {
                String scope = call.getScope() != null ? call.getScope() : "";
                boolean isSet = scope.toLowerCase().contains("set");
                if (!isSet && (scope.toLowerCase().contains("list") || scope.toLowerCase().contains("ids") || scope.toLowerCase().contains("collection"))) {
                    StaticIssue issue = new StaticIssue();
                    issue.setRuleId(getRuleId());
                    issue.setSeverity(getSeverity());
                    issue.setConfidence("HIGH");
                    issue.setFileName(call.getFilePath());
                    issue.setLineNumber(call.getLineNumber());
                    issue.setClassName(call.getCallerClassName());
                    issue.setMethodName(call.getCallerMethodName());
                    issue.setCallPath(call.getCallerFullMethodId() + " -> " + scope + "." + callee + "()");
                    issue.setExplanation("Calling '" + scope + "." + callee + "()' inside a loop at line " + call.getLineNumber() + " causes O(N) linear lookups.");
                    issue.setRecommendation("Convert the List to a HashSet before the loop to achieve O(1) average lookup complexity.");
                    issue.setTradeOffs("Requires O(M) memory for building Set, but reduces runtime from O(N*M) to O(N + M).");
                    issues.add(issue);
                }
            }
        }

        return issues;
    }
}

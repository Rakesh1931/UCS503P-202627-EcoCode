package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.ClassInfo;
import com.ecocode.analyzer.MethodCallInfo;
import com.ecocode.analyzer.MethodInfo;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EC108_DuplicateComputation implements EcoRule {

    @Override
    public String getRuleId() { return "EC108"; }

    @Override
    public String getName() { return "Duplicate Computation"; }

    @Override
    public String getDescription() {
        return "Executes identical method call with identical arguments multiple times in the same method scope.";
    }

    @Override
    public String getSeverity() { return "MEDIUM"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (ClassInfo cls : model.getClasses()) {
            for (MethodInfo method : cls.getMethods()) {
                Map<String, List<MethodCallInfo>> callGroups = new HashMap<>();

                for (MethodCallInfo call : method.getCalls()) {
                    // Ignore trivial getter/setter or common standard calls like add(), toString()
                    String callee = call.getCalleeName();
                    if ("add".equals(callee) || "get".equals(callee) || "append".equals(callee) || "print".equals(callee) || "println".equals(callee)) {
                        continue;
                    }

                    String key = call.getCalleeName() + "(" + String.join(",", call.getArguments()) + ")";
                    callGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(call);
                }

                for (Map.Entry<String, List<MethodCallInfo>> entry : callGroups.entrySet()) {
                    if (entry.getValue().size() >= 2) {
                        MethodCallInfo firstCall = entry.getValue().get(0);
                        StaticIssue issue = new StaticIssue();
                        issue.setRuleId(getRuleId());
                        issue.setSeverity(getSeverity());
                        issue.setConfidence("HIGH");
                        issue.setFileName(firstCall.getFilePath());
                        issue.setLineNumber(firstCall.getLineNumber());
                        issue.setClassName(cls.getQualifiedName());
                        issue.setMethodName(method.getName());
                        issue.setCallPath(cls.getQualifiedName() + "#" + method.getName() + " -> " + entry.getKey());
                        issue.setExplanation("Identical method call '" + entry.getKey() + "' executed " + entry.getValue().size() + " times inside method '" + method.getName() + "'.");
                        issue.setRecommendation("Store the return value of '" + entry.getKey() + "' in a local variable and reuse it.");
                        issue.setTradeOffs("Saves CPU cycles by eliminating duplicate execution without changing program semantics.");
                        issues.add(issue);
                    }
                }
            }
        }

        return issues;
    }
}

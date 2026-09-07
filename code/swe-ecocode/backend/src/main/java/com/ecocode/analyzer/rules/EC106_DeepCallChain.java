package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.ClassInfo;
import com.ecocode.analyzer.MethodInfo;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EC106_DeepCallChain implements EcoRule {

    @Override
    public String getRuleId() { return "EC106"; }

    @Override
    public String getName() { return "Deep Call Chain"; }

    @Override
    public String getDescription() {
        return "Call graph reachability depth exceeds 8 hops, causing deep stack frames and overhead.";
    }

    @Override
    public String getSeverity() { return "LOW"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (ClassInfo cls : model.getClasses()) {
            for (MethodInfo method : cls.getMethods()) {
                if (method.isPublic()) {
                    String fullMethodId = cls.getQualifiedName() + "#" + method.getName();
                    int depth = callGraph.getReachabilityDepth(fullMethodId);
                    if (depth > 8) {
                        StaticIssue issue = new StaticIssue();
                        issue.setRuleId(getRuleId());
                        issue.setSeverity(getSeverity());
                        issue.setConfidence("MEDIUM");
                        issue.setFileName(cls.getFilePath());
                        issue.setLineNumber(method.getStartLine());
                        issue.setClassName(cls.getQualifiedName());
                        issue.setMethodName(method.getName());
                        issue.setCallPath(fullMethodId + " (depth=" + depth + ")");
                        issue.setExplanation("Call stack depth starting from public method '" + method.getName() + "' is " + depth + " (threshold is 8).");
                        issue.setRecommendation("Flatten architectural call chains and simplify method delegation.");
                        issue.setTradeOffs("Requires refactoring class boundaries, but improves code maintainability and stack overhead.");
                        issues.add(issue);
                    }
                }
            }
        }

        return issues;
    }
}

package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EC102_StringConcatInLoop implements EcoRule {

    @Override
    public String getRuleId() { return "EC102"; }

    @Override
    public String getName() { return "String Concatenation in Loop"; }

    @Override
    public String getDescription() {
        return "Performs String concatenation ('+' or '+=') inside a loop, creating O(N^2) temporary String allocations.";
    }

    @Override
    public String getSeverity() { return "MEDIUM"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (ProjectModel.StringConcatEvent event : model.getStringConcatsInLoop()) {
            StaticIssue issue = new StaticIssue();
            issue.setRuleId(getRuleId());
            issue.setSeverity(getSeverity());
            issue.setConfidence("HIGH");
            issue.setFileName(event.filePath);
            issue.setLineNumber(event.lineNumber);
            issue.setClassName(event.className);
            issue.setMethodName(event.methodName);
            issue.setCallPath(event.className + "#" + event.methodName);
            issue.setExplanation("String concatenation inside loop at line " + event.lineNumber + ": " + event.expression);
            issue.setRecommendation("Use StringBuilder or StringJoiner inside loop body.");
            issue.setTradeOffs("Slightly more verbose code, but drastically reduces heap allocations and GC pressure.");
            issues.add(issue);
        }

        return issues;
    }
}

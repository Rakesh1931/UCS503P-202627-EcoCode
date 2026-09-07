package com.ecocode.analyzer.rules;

import com.ecocode.analyzer.CallGraph;
import com.ecocode.analyzer.ProjectModel;
import com.ecocode.model.StaticIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EC107_ResourceHandling implements EcoRule {

    @Override
    public String getRuleId() { return "EC107"; }

    @Override
    public String getName() { return "Unclosed System Resource"; }

    @Override
    public String getDescription() {
        return "System resource (InputStream, Connection, ResultSet) is declared outside try-with-resources block, risking file/socket descriptor leaks.";
    }

    @Override
    public String getSeverity() { return "HIGH"; }

    @Override
    public List<StaticIssue> analyze(ProjectModel model, CallGraph callGraph) {
        List<StaticIssue> issues = new ArrayList<>();

        for (ProjectModel.ResourceHandlingEvent event : model.getUnclosedResources()) {
            StaticIssue issue = new StaticIssue();
            issue.setRuleId(getRuleId());
            issue.setSeverity(getSeverity());
            issue.setConfidence("HIGH");
            issue.setFileName(event.filePath);
            issue.setLineNumber(event.lineNumber);
            issue.setClassName(event.className);
            issue.setMethodName(event.methodName);
            issue.setCallPath(event.className + "#" + event.methodName + " (" + event.resourceType + " " + event.variableName + ")");
            issue.setExplanation("Resource '" + event.variableName + "' of type " + event.resourceType + " at line " + event.lineNumber + " is not enclosed in a try-with-resources statement.");
            issue.setRecommendation("Wrap resource declaration in try (" + event.resourceType + " " + event.variableName + " = ...) { ... }.");
            issue.setTradeOffs("Guarantees resource closure even when exceptions are thrown, eliminating resource leak vulnerabilities.");
            issues.add(issue);
        }

        return issues;
    }
}

package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Ruleset {
    private final Map<Integer,List<Rule>> rulesBySize = new HashMap<>();
    private int maxRightSize;

    private static final String COMMENT_START = "#";
    private static final String CONTINUE_LINE = "\\";
    private static final String PARTS_SPLITTER = "->";
    private static final String RHS_SPLITTER = "\\|";
    private static final String WEIGHT_START = "[";
    private static final String WEIGHT_END = "]";
    private static final Pattern rhsPattern =
        Pattern.compile(String.format("^(?<rhs>.+)\\%s(?<weight>.+)\\%s$", WEIGHT_START, WEIGHT_END));

    public Ruleset() {}

    public Ruleset(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line;
        StringBuilder row = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(COMMENT_START)) {
                continue;
            }

            row.append(line);
            if (row.toString().endsWith(CONTINUE_LINE)) {
                row = new StringBuilder(row.substring(0, row.length() - 1));
                continue;
            }

            List<String> parts = Arrays.asList(row.toString().trim().split(PARTS_SPLITTER, 2));
            if (parts.size() < 2) {
                throw new RuntimeException("Left or right part is missing");
            }

            String leftPart = parts.get(0), rightPart = parts.get(1);
            Matcher rhsMatcher = rhsPattern.matcher(rightPart);
            if (rhsMatcher.matches()) {
                float weight = Float.parseFloat(rhsMatcher.group("weight"));
                add(leftPart, rhsMatcher.group("rhs").trim(), weight);
            } else {
                add(leftPart, rightPart);
            }

            row = new StringBuilder();
        }
    }

    public void add(String left, String right) {
        add(left, right, 1.0f);
    }

    public void add(String left, String right, float weight) {
        for (String rightPart : right.trim().split(RHS_SPLITTER)) {
            Rule r = new Rule(left, rightPart, weight);
            List<Rule> bySize = rulesBySize.computeIfAbsent(r.rightSize, k -> new ArrayList<>());
            bySize.add(r);
            if (r.rightSize > maxRightSize) {
                maxRightSize = r.rightSize;
            }
        }
    }

    public int getMaxRightSize() {
        return maxRightSize;
    }

    public Rule match(List<Node> nodes) {
        List<Rule> testRules = rulesBySize.get(nodes.size());
        if (testRules == null) {
            return null;
        }
        for (Rule rule : testRules) {
            if (rule.match(nodes)) {
                return rule;
            }
        }
        return null;
    }

    public List<Rule> matchAll(List<Node> nodes) {
        List<Rule> matchedRules = new ArrayList<>();
        List<Rule> testRules = rulesBySize.get(nodes.size());
        if (testRules == null) {
            return matchedRules;
        }
        for (Rule rule : testRules) {
            if (rule.match(nodes)) {
                matchedRules.add(rule);
            }
        }
        return matchedRules;
    }
}

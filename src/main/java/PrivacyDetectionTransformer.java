import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.scalar.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static soot.util.cfgcmd.CFGGraphType.EXCEPTIONAL_UNIT_GRAPH;

public class PrivacyDetectionTransformer extends BodyTransformer {
    private Rule[] rules;
    private final List<Pair<StmtLocation, Rule>> result = new ArrayList<>();
    private final Map<Rule, List<StmtLocation>> resultMap = new HashMap<>();

    public PrivacyDetectionTransformer(Rule[] rules) {
        this.rules = rules;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        if (rules == null || rules.length == 0) {
            return;
        }
        ValueSetAnalysis vsa = null;

        for (Unit unit : b.getUnits()) {
            if (unit == null) {
                continue;
            }
            StmtLocation stmtLocation = new StmtLocation((Stmt) unit, b);

            for (Rule rule : rules) {
                for (Condition condition : rule.getConditions()) {
                    if (!verifyStringPattern((Stmt) unit, condition)) {
                        continue;
                    }
                    if (condition.isInvokeCondition()) {
                        if (!((Stmt) unit).containsInvokeExpr()) {
                            continue;
                        }
                        InvokeExpr invokeExpr = ((Stmt) unit).getInvokeExpr();
                        if (!verifyClass(invokeExpr, condition)
                                || !verifyMethod(invokeExpr, condition)) {
                            continue;
                        }
                        if (vsa == null) {
                            vsa = new ValueSetAnalysis(EXCEPTIONAL_UNIT_GRAPH.buildGraph(b));
                        }
                        if (!verifyArguments(invokeExpr, condition, (Stmt) unit, vsa)) {
                            continue;
                        }
                    }

                    result.add(new Pair<>(stmtLocation, rule));
                    if (!resultMap.containsKey(rule)) {
                        resultMap.put(rule, new ArrayList<>());
                    }
                    resultMap.get(rule).add(stmtLocation);
                    break;
                }
            }
        }

    }

    private boolean verifyStringPattern(Stmt statement, Condition condition) {
        return condition.getStringPattern() == null || Pattern.compile(condition.getStringPattern()).matcher(statement.toString()).find();
    }

    private boolean verifyClass(InvokeExpr invokeExpr, Condition condition) {
        String invokeClassName = invokeExpr.getMethod().getDeclaringClass().getName();
        if (condition.getClassName() != null) {
            if (condition.getClassName().equals(invokeClassName)) {
                return true;
            } else if (condition.getClassPattern() == null) {
                return false;
            }
        }

        if (condition.getClassPattern() != null) {
            Pattern pattern = Pattern.compile(condition.getClassPattern(), CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(invokeClassName);
            return matcher.find();
        }

        return true;
    }

    private boolean verifyMethod(InvokeExpr invokeExpr, Condition condition) {
        String invokeMethodName = invokeExpr.getMethod().getName();
        if (condition.getMethodName() != null) {
            if (condition.getMethodName().equals(invokeMethodName)) {
                return true;
            } else if (condition.getMethodPattern() == null) {
                return false;
            }
        }

        if (condition.getMethodPattern() != null) {
            Pattern pattern = Pattern.compile(condition.getMethodPattern(), CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(invokeMethodName);
            return matcher.find();
        }

        return true;
    }

    private boolean verifyArguments(InvokeExpr invokeExpr, Condition condition, Stmt stmt, ValueSetAnalysis vsa) {
        if (condition.getParamsCount() != -1 && condition.getParamsCount() != invokeExpr.getArgCount()) {
            return false;
        }

        if (condition.getArguments() != null && condition.getArguments().size() != 0) {
            for (int argIndex : condition.getArguments().keySet()) {
                if (argIndex >= invokeExpr.getArgCount()) {
                    return false;
                }
                boolean matches = false;
                Pattern pattern = Pattern.compile(condition.getArguments().get(argIndex));

                Value value = invokeExpr.getArg(argIndex);
                if (value instanceof Constant) {
                    Matcher matcher = pattern.matcher(value.toString());
                    if (matcher.find()) {
                        continue;
                    }
                }


                List<Pair<Object, Unit>> valuePairs = vsa.solveOf(stmt, value, true);

                for (Pair<Object, Unit> pair : valuePairs) {
                    Object maybeValue = pair.getO1();
                    String maybeValueString;
                    if (maybeValue instanceof StringConstant) {
                        maybeValueString = ((StringConstant) maybeValue).value;
                    } else if (maybeValue instanceof IntConstant) {
                        maybeValueString = String.valueOf(((IntConstant) maybeValue).value);
                    } else {
                        maybeValueString = maybeValue.toString();
                    }

                    Matcher matcher = pattern.matcher(maybeValueString);
                    matches = matcher.find();
                    if (matches) {
                        break;
                    }
                }

                if (!matches) {
                    return false;
                }
            }
        }

        return true;
    }

    public Rule[] getRules() {
        return rules;
    }

    public void setRules(Rule[] rules) {
        this.rules = rules;
    }

    public List<Pair<StmtLocation, Rule>> getResult() {
        return result;
    }

    public Map<Rule, List<StmtLocation>> getResultMap() {
        return resultMap;
    }

}

class StmtLocation {
    private Stmt stmt;
    private Body body;

    StmtLocation(Stmt stmt, Body body) {
        this.stmt = stmt;
        this.body = body;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}

import java.util.List;
import java.util.Map;

public class Rule {
    private String name;
    private List<Condition> conditions;

    public Rule() {
    }

    @Override
    public String toString() {
        return "Rule{" +
                "name='" + name + '\'' +
                ", conditions=" + conditions +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }
}


class Condition {
    private String className;
    private String methodName;
    private String classPattern;
    private String methodPattern;
    private String stringPattern;
    private int paramsCount = -1;
    private Map<Integer, String> arguments;

    public String getClassPattern() {
        return classPattern;
    }

    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Map<Integer, String> getArguments() {
        return arguments;
    }

    public void setArguments(Map<Integer, String> arguments) {
        this.arguments = arguments;
    }

    public int getParamsCount() {
        return paramsCount;
    }

    public void setParamsCount(int paramsCount) {
        this.paramsCount = paramsCount;
    }

    public String getStringPattern() {
        return stringPattern;
    }

    public void setStringPattern(String stringPattern) {
        this.stringPattern = stringPattern;
    }


    public boolean isInvokeCondition() {
        return !conditionEmpty(className) || !conditionEmpty(classPattern)
                || !conditionEmpty(methodName) || !conditionEmpty(methodPattern)
                || !conditionEmpty(paramsCount) || !conditionEmpty(arguments);
    }

    private boolean conditionEmpty(String key) {
        return key == null || key.isEmpty();
    }

    private boolean conditionEmpty(int key) {
        return key == -1;
    }


    private boolean conditionEmpty(Map key) {
        return key == null || key.size() == 0;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", classPattern='" + classPattern + '\'' +
                ", methodPattern='" + methodPattern + '\'' +
                ", stringPattern='" + stringPattern + '\'' +
                ", paramsCount=" + paramsCount +
                ", arguments=" + arguments +
                '}';
    }
}

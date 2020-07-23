import soot.JastAddJ.ExprStmt;
import soot.Local;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueSetAnalysis extends ForwardFlowAnalysis<Unit, ValueSet> {


    protected Map<Unit, FlowSet<ValueSet>> unitValueSetMap = new HashMap<>();
    ValueSet emptySet = new ValueSet();


    /**
     * Construct the analysis from a DirectedGraph representation of a Body.
     *
     * @param graph
     */
    public ValueSetAnalysis(DirectedGraph graph) {
        super(graph);

        doAnalysis();
    }

    @Override
    protected void flowThrough(ValueSet in, Unit d, ValueSet out) {
        in.copy(out);
        if (d instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) d;
            Value leftOp = defStmt.getLeftOp();
            AbstractValue value = new AbstractValue(leftOp);
            value.values.add(new Pair<>(defStmt.getRightOp(), d));
            if (out.contains(value)) {
                out.varToValues.remove(value.o);
            }
            out.add(value);

        }
    }

    public List<Pair<Object, Unit>> solveOf(Unit location, Object o) {
        return solveOf(location, o, false);
    }


    public List<Pair<Object, Unit>> solveOf(Unit location, Object o, boolean before) {
        ValueSet vs = before ? this.getFlowBefore(location) : this.getFlowAfter(location);
        if (vs.varToValues.containsKey(o)) {
            return vs.varToValues.get(o).values;
        }
        return new ArrayList<>();
    }

    @Override
    protected ValueSet newInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void merge(ValueSet in1, ValueSet in2, ValueSet out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(ValueSet source, ValueSet dest) {
        dest.varToValues = new HashMap<>();
        for (Object var : source.varToValues.keySet()) {
            dest.varToValues.put(var, source.varToValues.get(var).clone());
        }
    }
}


import soot.Unit;
import soot.toolkits.scalar.AbstractFlowSet;
import soot.toolkits.scalar.Pair;

import java.util.*;

class AbstractValue {
    Object o;
    List<Pair<Object, Unit>> values = new ArrayList<>();
    Map<String, Unit> conditions = new TreeMap<>();

    public AbstractValue() {
    }

    public AbstractValue(Object obj) {
        this.o = obj;
    }

    @Override
    public AbstractValue clone() {
        AbstractValue res = new AbstractValue(this.o);
        res.values = new ArrayList<>(values);
        res.conditions = new TreeMap<>(this.conditions);
        return res;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(values.toArray()), Arrays.hashCode(conditions.values().toArray()), this.o);
    }
}

public class ValueSet extends AbstractFlowSet<AbstractValue> {

    public Map<Object, AbstractValue> varToValues = new HashMap<>();


    public ValueSet() {
    }

    @Override
    public ValueSet clone() {
        ValueSet res = new ValueSet();
        res.varToValues = new HashMap<>();
        for (Object var : this.varToValues.keySet()) {
            res.varToValues.put(var, this.varToValues.get(var).clone());
        }
        return res;
    }

    @Override
    public boolean isEmpty() {
        return this.varToValues.isEmpty();
    }

    @Override
    public int size() {
        return this.varToValues.size();
    }

    @Override
    public void add(AbstractValue obj) {
        if (contains(obj)) {
            AbstractValue dest = this.varToValues.get(obj.o);
            for (Pair<Object, Unit> value : obj.values) {
                if (!dest.values.contains(value)) {
                    dest.values.add(value);
                }
            }
        } else {
            this.varToValues.put(obj.o, obj);
        }
    }

    @Override
    public void remove(AbstractValue obj) {
        if (contains(obj)) {
            AbstractValue dest = this.varToValues.get(obj.o);
            for (Pair<Object, Unit> value : obj.values) {
                if (!dest.values.contains(value)) {
                    dest.values.remove(value);
                }
            }
            if (dest.values.isEmpty()) {
                this.varToValues.remove(obj.o);
            }
        }
    }


    public void intersection(ValueSet other, ValueSet dest) {
        for (AbstractValue obj : this) {
            if (other.contains(obj)) {
                List<Pair<Object, Unit>> destValues = new ArrayList<>();
                List<Pair<Object, Unit>> values1 = other.varToValues.get(obj.o).values;
                List<Pair<Object, Unit>> values2 = this.varToValues.get(obj.o).values;
                for (Pair<Object, Unit> value : values1) {
                    if (values2.contains(value)) {
                        destValues.add(value);
                    }
                }

                if (!destValues.isEmpty()) {
                    AbstractValue n = new AbstractValue();
                    n.o = obj.o;
                    n.values = destValues;
                    dest.add(n);
                }
            }
        }
    }

    @Override
    public boolean contains(AbstractValue obj) {
        for (Object o : this.varToValues.keySet()) {
            if (o.equals(obj.o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<AbstractValue> iterator() {
        return this.varToValues.values().iterator();
    }

    @Override
    public List<AbstractValue> toList() {
        return (List<AbstractValue>) this.varToValues.values();
    }
}

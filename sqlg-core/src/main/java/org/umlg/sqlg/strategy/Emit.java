package org.umlg.sqlg.strategy;

import com.google.common.base.Preconditions;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.lambda.ElementValueTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.lambda.IdentityTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.SelectOneStep;
import org.javatuples.Pair;
import org.umlg.sqlg.structure.SqlgElement;

import java.util.*;

/**
 * Created by pieter on 2015/10/26.
 */
public class Emit<E extends SqlgElement> implements Comparable<Emit<E>> {

    private Path path;
    private E element;
    private Set<String> labels;
    private boolean repeat;
    private boolean repeated;
    private boolean fake;
    //This is set to true for local optional step where the query has no labels, i.e. for a single SchemaTableTree only.
    //In this case the element will already be on the traverser i.e. the incoming element.
    private boolean incomingOnlyLocalOptionalStep;
    /**
     * This is the SqlgComparatorHolder for the SqlgElement that is being emitted.
     * It represents the {@link org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderGlobalStep} for the SqlgElement that is being emitted.
     */
    private SqlgComparatorHolder sqlgComparatorHolder;
    /**
     * This represents all the {@link org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderGlobalStep}s, one for each element along the path.
     */
    private List<SqlgComparatorHolder> sqlgComparatorHolders;

    private Traverser.Admin<E> traverser;
    private List<Pair<Object, Comparator>> comparatorValues;

    /**
     * The {@link org.umlg.sqlg.sql.parse.ReplacedStep}'s depth
     */
    private int replacedStepDepth;

    public Emit() {
        this.fake = true;
        this.labels = Collections.emptySet();
    }

    public Emit(E element, Set<String> labels, int replacedStepDepth, SqlgComparatorHolder sqlgComparatorHolder) {
        this.element = element;
        this.labels = labels;
        this.replacedStepDepth = replacedStepDepth;
        this.sqlgComparatorHolder = sqlgComparatorHolder;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public E getElement() {
        return element;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public SqlgComparatorHolder getSqlgComparatorHolder() {
        return this.sqlgComparatorHolder;
    }

    public void setSqlgComparatorHolders(List<SqlgComparatorHolder> sqlgComparatorHolders) {
        this.sqlgComparatorHolders = sqlgComparatorHolders;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    public boolean isFake() {
        return fake;
    }

    boolean isIncomingOnlyLocalOptionalStep() {
        return incomingOnlyLocalOptionalStep;
    }

    public void setIncomingOnlyLocalOptionalStep(boolean incomingOnlyLocalOptionalStep) {
        this.incomingOnlyLocalOptionalStep = incomingOnlyLocalOptionalStep;
    }

    public Traverser.Admin<E> getTraverser() {
        return traverser;
    }

    public void setTraverser(Traverser.Admin<E> traverser) {
        this.traverser = traverser;
    }

    public List<Pair<Object, Comparator>> getComparatorValues() {
        return this.comparatorValues;
    }

    @Override
    public String toString() {
        String result = "";
        if (!this.fake) {
            if (this.path != null) {
                result += this.path.toString();
                result += ", ";
            }
            result += element.toString();
        } else {
            result = "fake emit";
        }
        return result;
    }

    void evaluateElementValueTraversal() {
        for (int i = this.sqlgComparatorHolders.size()  - 1; i >= 0 ; i--) {
            if (this.comparatorValues == null) {
                this.comparatorValues = new ArrayList<>();
            }
            SqlgElement sqlgElement;
            SqlgComparatorHolder comparatorHolder = this.sqlgComparatorHolders.get(i);
            if (comparatorHolder.hasPrecedingSelectOneLabel()) {
                String precedingLabel = comparatorHolder.getPrecedingSelectOneLabel();
                sqlgElement = this.traverser.path().get(precedingLabel);
            } else {
                sqlgElement = (SqlgElement) this.traverser.path().objects().get(i);
            }
            for (Pair<Traversal.Admin<?, ?>, Comparator<?>> traversalComparator : comparatorHolder.getComparators()) {
                Traversal.Admin<?, ?> traversal = traversalComparator.getValue0();
                Comparator comparator = traversalComparator.getValue1();
                ElementValueTraversal elementValueTraversal;
                if (traversal.getSteps().size() == 1 && traversal.getSteps().get(0) instanceof SelectOneStep) {
                    //xxxxx.select("a").order().by(select("a").by("name"), Order.decr)
                    SelectOneStep selectOneStep = (SelectOneStep) traversal.getSteps().get(0);
                    Preconditions.checkState(selectOneStep.getScopeKeys().size() == 1, "toOrderByClause expects the selectOneStep to have one scopeKey!");
                    Preconditions.checkState(selectOneStep.getLocalChildren().size() == 1, "toOrderByClause expects the selectOneStep to have one traversal!");
                    Preconditions.checkState(selectOneStep.getLocalChildren().get(0) instanceof ElementValueTraversal, "toOrderByClause expects the selectOneStep's traversal to be a ElementValueTraversal!");
                    String selectKey = (String) selectOneStep.getScopeKeys().iterator().next();
                    sqlgElement = this.traverser.path().get(selectKey);
                    elementValueTraversal = (ElementValueTraversal) selectOneStep.getLocalChildren().get(0);
                    this.comparatorValues.add(Pair.with(sqlgElement.value(elementValueTraversal.getPropertyKey()), comparator));
                } else if (traversal instanceof IdentityTraversal) {
                    //This is for Order.shuffle
                    this.comparatorValues.add(Pair.with(new Random().nextInt(), comparator));
                } else {
                    elementValueTraversal = (ElementValueTraversal) traversal;
                    this.comparatorValues.add(Pair.with(sqlgElement.value(elementValueTraversal.getPropertyKey()), comparator));
                }
            }
        }
    }

    @Override
    public int compareTo(Emit<E> emit) {
        if (this.replacedStepDepth != emit.replacedStepDepth)  {
            return Integer.valueOf(this.replacedStepDepth).compareTo(Integer.valueOf(emit.replacedStepDepth));
        }
        for (int i = 0; i < this.comparatorValues.size(); i++) {
            Pair<Object, Comparator> comparatorPair1 = this.comparatorValues.get(i);
            Pair<Object, Comparator> comparatorPair2 = emit.comparatorValues.get(i);
            Object value1 = comparatorPair1.getValue0();
            Comparator comparator1 = comparatorPair1.getValue1();
            Object value2 = comparatorPair2.getValue0();
            Comparator comparator2 = comparatorPair2.getValue1();
            Preconditions.checkState(comparator1.equals(comparator2));
            int compare = comparator1.compare(value1, value2);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }
}

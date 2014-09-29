package nars.plugin.mental;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import nars.core.EventEmitter.Observer;
import nars.core.Events.TaskDerived;
import nars.core.Memory;
import nars.core.NAR;
import nars.core.Param;
import nars.core.Parameters;
import nars.core.Plugin;
import nars.entity.BudgetValue;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.inference.BudgetFunctions;
import nars.io.Symbols;
import static nars.language.CompoundTerm.termArray;
import nars.language.Similarity;
import nars.language.Term;
import nars.operator.Operation;
import nars.operator.Operator;
import nars.util.meter.util.AtomicDouble;

/**
 * Abbreviation plugin.  When first enabled, creates the ^abbreviation operator
 */
public class Abbreviation implements Plugin {

    public AtomicInteger abbreviationComplexityMin = new AtomicInteger(20);
    public AtomicDouble abbreviationQualityMin = new AtomicDouble(0.95f);
    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);

    
    /**
    * Operator that give a CompoundTerm an atomic name
    */
    public static class Abbreviate extends Operator {

        public Abbreviate() {
            super("^abbreviate");
        }

        /**
         * To create a judgment with a given statement
         * @param args Arguments, a Statement followed by an optional tense
         * @param memory The memory in which the operation is executed
         * @return Immediate results as Tasks
         */
        @Override
        protected ArrayList<Task> execute(Operation operation, Term[] args, Memory memory) {
            
            Term compound = args[0];
            
            Term atomic = memory.newSerialTerm(Symbols.TERM_PREFIX);
                        
            Sentence sentence = new Sentence(
                    Similarity.make(compound, atomic, memory), 
                    Symbols.JUDGMENT_MARK, 
                    new TruthValue(1, 0.9999f),  // a naming convension
                    new Stamp(memory));
            
            float quality = BudgetFunctions.truthToQuality(sentence.truth);
            
            BudgetValue budget = new BudgetValue(
                    Parameters.DEFAULT_JUDGMENT_PRIORITY, 
                    Parameters.DEFAULT_JUDGMENT_DURABILITY, 
                    quality);

            return Lists.newArrayList( new Task(sentence, budget) );        

        }

    }

    
    
    public boolean canAbbreviate(Task task) {
        return !(task.sentence.content instanceof Operation) && 
                (task.sentence.content.getComplexity() > abbreviationComplexityMin.get()) &&
                (task.budget.getQuality() > abbreviationQualityMin.get());
    }
    
    @Override
    public boolean setEnabled(final NAR n, final boolean enabled) {
        final Memory memory = n.memory;
        
        final Operator opTerm = memory.getOperator("^abbreviate");
        if (opTerm == null) {
            memory.addOperator(new Abbreviate());
        }
        
        memory.event.set(new Observer() {            
            
            @Override public void event(Class event, Object[] a) {
                if (event != TaskDerived.class)
                    return;                    

                Param p = memory.param;

                Task task = (Task)a[0];

                //is it complex and also important? then give it a name:
                if (canAbbreviate(task)) {

                    Operation operation = Operation.make(
                            opTerm, termArray( task.sentence.content ), false, memory);

                    
                    TruthValue truth = new TruthValue(1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);

                    Stamp stampi = task.sentence.stamp.clone();
                    stampi.setOccurrenceTime(n.getTime());

                    Sentence j = new Sentence(operation,Symbols.GOAL_MARK, truth, stampi);

                    BudgetValue budg=new BudgetValue(
                            Parameters.DEFAULT_GOAL_PRIORITY /* * ((float)priorityFactor.get())*/, 
                            Parameters.DEFAULT_GOAL_DURABILITY, 
                            1);

                    Task newTask = new Task(j, budg, Parameters.INTERNAL_EXPERIENCE_FULL ? null : task);
                    
                    memory.output(newTask);
                    
                    memory.addNewTask(newTask, "Derived (Abbreviated)");
                }
                                
            }

        }, enabled, TaskDerived.class);
        
        return true;
    }
    
}
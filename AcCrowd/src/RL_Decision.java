import com.sun.org.apache.xerces.internal.dom.ParentNode;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;

/* The Reinforcement Learning module makes online decision for the task-worker selection
 * Created by Zehong on 3/17/2017 0017.
 */
abstract class RL_Decision {
    ArrayList<Action> m_available_action;
    double m_obj_value;
    int m_t;

    // Initialize the inner state
    RL_Decision(int t0, double obj_value, ArrayList<Action> available_action)
    {
        m_t = t0;
        m_obj_value = obj_value;
        m_available_action = available_action;
    }

    // Make the task-worker decision
    abstract Action getDecision(State St, Prob_Model model, Obj_Function obj, Market_Simulator simulator);

    // Get the system time
    int getT()
    {
        return m_t;
    }
}

/* The random selection algorithm always uniformly select the action.
 */
class Random_RL extends RL_Decision {

    Random_RL(int t0, double obj_value, ArrayList<Action> available_action)
    {
        super(t0, obj_value, available_action);
    }

    Action getDecision(State St, Prob_Model model, Obj_Function obj, Market_Simulator simulator)
    {
        // Uniformly select the action
        int index_of_action = ThreadLocalRandom.current().nextInt(m_available_action.size());
        // Remove the action from the action set
        Action at = m_available_action.remove(index_of_action);
        // The time plus one
        m_t++;

        return at;
    }
}

class CrowdNode {
    // Tree Structure
    public CrowdNode parentNode;
    public List<List<CrowdNode>> childNodes; // Every action may generate classNum states.

    // Node Data
    public Prob_Model crowdModel;
    public double objValue;
    public Obj_Function objFun;

    // Node Generation
    public Action parentAction;
    public int observedLabel;
    public double labelProb;

    // Initialize the Node
    CrowdNode(CrowdNode parent, Action pa_action, int ob_label, Obj_Function obj_function, double label_probability)
    {
        parentNode = parent;
        parentAction = pa_action;
        observedLabel = ob_label;
        objFun = obj_function;
        labelProb = label_probability;
    }

    // Calculate the action-value table
    private List<Double> CalActionValue(State state, List<Action> available_action_list) throws InterruptedException {
        int nThreads = 10;
        WorkerThreadFactory WT = new WorkerThreadFactory();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nThreads, nThreads,0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),WT);
        Vector<State> s_vec = new Vector<>(nThreads+1);
        s_vec.set(0,state);
        for(int i=1; i<nThreads+1; ++i)
        {
            s_vec.set(i, null);
        }
        Vector<PredictValueTask> Tasks = new Vector<>();
        for(Action a: available_action_list){
            PredictValueTask myTask = new PredictValueTask(s_vec, a, crowdModel, objFun);
            Tasks.add(myTask);
            executor.execute(myTask);
        }
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.MINUTES);
        List<Double> action_value = new ArrayList<>();
        for(PredictValueTask task: Tasks)
        {
            action_value.add(task.getObjValue());
        }
        return action_value;
    }

    private void Cal_Obj_Value(State state)
    {
        state.setEntry(parentAction.i, parentAction.j, observedLabel);
        crowdModel = parentNode.crowdModel;
        crowdModel.QuickUpdate(parentAction, state);
        objValue = objFun.getObjValue(crowdModel);
        state.setEntry(parentAction.i, parentAction.j, 0);
    }

    private List<CrowdNode> Gene_Node(State state, Action action)
    {
        List<CrowdNode> new_nodes = new ArrayList<>(crowdModel.getClass_num());
        RealVector label_prob = crowdModel.getLabelProb(action);
        for(int label = 1; label<=crowdModel.getClass_num(); ++label)
        {
            CrowdNode node = new CrowdNode(this, action, label, objFun, label_prob.getEntry(label-1));
            node.Cal_Obj_Value(state);
            new_nodes.set(label-1, node);
        }
        return new_nodes;
    }


    // Generate the children nodes
    public void geneChildren(State state, List<Action> available_action_list) throws InterruptedException {
        int num_of_children = 5;

        if(available_action_list.size()>num_of_children)
        {
            List<Double> action_value = CalActionValue(state, available_action_list);

            // Get the indexes of top num_of_children
            int[] indexes = new int[num_of_children];
            for(int i=0; i<num_of_children; ++i)
            {
                int index = -1;
                double value = -1;
                int count = 0;
                for(Double v:action_value)
                {
                    if(v>value)
                    {
                        index = count;
                    }
                    count++;
                }
                indexes[i] = index;
            }

            // Generate new nodes
            for(int i:indexes)
            {
                childNodes.add(Gene_Node(state, available_action_list.get(i)));
            }
        }
        else{
            for(Action action:available_action_list)
            {
                childNodes.add(Gene_Node(state, action));
            }
        }
    }
}


class PredictValueTask implements Runnable {
    private Vector<State> m_s_vec;
    private Action m_a;
    private double m_obj;
    private Obj_Function m_obj_fun;
    private Prob_Model m_model;

    public PredictValueTask(Vector<State> s_vec, Action a, Prob_Model model, Obj_Function obj_fun) {
        m_s_vec = s_vec;
        m_a = a;
        m_model = model;
        m_obj_fun = obj_fun;
        m_obj = 0;
    }

    private double Cal_Label_Value(State s, int ob_label)
    {
        s.setEntry(m_a.i, m_a.j, ob_label);
        Prob_Model newModel = m_model.Copy();
        newModel.QuickUpdate(m_a, s);
        double obj = m_obj_fun.getObjValue(newModel);
        s.setEntry(m_a.i, m_a.j, 0);
        return obj;
    }

    private void Cal_Action_Value()
    {
        RealVector label_prob = m_model.getLabelProb(m_a);
        m_obj = 0;
        int index_of_state = Integer.parseInt(Thread.currentThread().getName());
        State s = m_s_vec.elementAt(index_of_state+1);
        if(s == null)
        {
            s = m_s_vec.elementAt(0).copy();
            m_s_vec.set(index_of_state+1, s);
        }
        for(int i=0; i<label_prob.getDimension(); ++i) {
            m_obj += label_prob.getEntry(i) * Cal_Label_Value(s, i + 1);
        }
    }

    @Override
    public void run() {
        Cal_Action_Value();
    }

    public double getObjValue()
    {
        return m_obj;
    }
}

class WorkerThreadFactory implements ThreadFactory {
    private int counter = 0;

    public Thread newThread(Runnable r) {
        return new Thread(r, Integer.toString(counter++));
    }
}
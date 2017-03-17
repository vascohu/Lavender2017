import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

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

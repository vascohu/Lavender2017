/**
 * Created by Zehong on 3/14/2017 0014.
 */
public class Market_Simulator {
    int m_task_num, m_worker_num;

    public Market_Simulator(int task_num, int worker_num)
    {
        m_task_num = task_num;
        m_worker_num = worker_num;
    }

    public int getTask_Num()
    {
        return m_task_num;
    }

    public int getWorker_Num()
    {
        return m_worker_num;
    }
}

interface Worker {
    int labelTask(Task t);
}

interface Task {
    int getLabel();
}

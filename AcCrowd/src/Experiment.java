import org.apache.commons.math3.linear.RealMatrix;

import java.util.concurrent.*;

/* Generate the experiment instance.
 * Created by Zehong on 3/14/2017 0014.
 */
public class Experiment {

//    public static void main(String[] args) {
//        Market_Simulator simulator = new Market_Simulator(100,10);
//        Active_Mechanism mechanism = new Active_Mechanism(simulator);
//        mechanism.Run(1000);
//        System.out.println(mechanism.getFinalObjValue());
//        State result = mechanism.getFinalLabelMat();
//        result.print();
//    }

    public static void main(String[] args) throws InterruptedException {
        int nThreads = 10;
        WorkerThreadFactory2 WT = new WorkerThreadFactory2();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nThreads, nThreads,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),WT);

        for(int i=0;i<30;i++){
            MyTask myTask = new MyTask(i);
            executor.execute(myTask);
            System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+
                    executor.getQueue().size()+"，已执行玩别的任务数目："+executor.getCompletedTaskCount());
        }
        System.out.println(">>>>>>>>><<<<<<<<<<<<<");
        executor.shutdown();
        boolean finshed = executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.MINUTES);
        System.out.println("SSSSSSSSSSSSSSSSSSSSSSSS");
    }
}

class MyTask implements Runnable {
    private int taskNum;

    public MyTask(int num) {
        this.taskNum = num;
    }

    @Override
    public void run() {
        System.out.println("正在执行task "+taskNum);
        try {
            Thread.currentThread().sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("task "+taskNum+"执行完毕"+" at Thread "+ Thread.currentThread().getName());
    }
}

class WorkerThreadFactory2 implements ThreadFactory {
    private int counter = 0;

    public Thread newThread(Runnable r) {
        return new Thread(r, Integer.toString(counter++));
    }
}

        /*RealMatrix AAA = new BlockRealMatrix(4, 4);
        AAA.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
            @Override
            public double visit(int row, int column, double value)
            {
                return 1.0;
            }
        });

        AAA.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
            @Override
            public void visit(int row, int column, double value)
            {
                System.out.println(value);
            }
        });*/
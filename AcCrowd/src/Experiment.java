import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/* Generate the experiment instance.
 * Created by Zehong on 3/14/2017 0014.
 */
public class Experiment {

    public static void main(String[] args) {
        int count1=0, count2=0;
        Market_Simulator simulator = new Market_Simulator(1000,1000);
        RealMatrix label_mat = new BlockRealMatrix(simulator.getTask_Num(), simulator.getWorker_Num());
        for(int i=0; i<label_mat.getRowDimension(); ++i)
        {
            for(int j=0; j<label_mat.getColumnDimension(); ++j)
            {
                label_mat.setEntry(i,j,0.0);
            }
        }

        for(int i=0; i<simulator.getTask_Num(); ++i)
        {
            for(int j=0; j<simulator.getWorker_Num(); ++j)
            {
                int label = (int)simulator.getLabelStream(i,j);
                if(label==1)
                {
                    count1++;
                }
                else
                {
                    count2++;
                }
                // System.out.print(label+", ");
            }
            // System.out.print("\n");
        }
        System.out.println(count1+"\t"+count2);
        System.out.println(simulator.getReliability_mat().getEntry(0,0));
    }
}

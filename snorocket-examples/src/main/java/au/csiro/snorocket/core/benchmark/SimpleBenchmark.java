/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.benchmark;

/**
 * Runs both benchmarks using predefined parameters.
 * 
 * @author Alejandro Metke
 *
 */
public class SimpleBenchmark {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Benchmark.main(new String[] {"RF1", "1"});
        /*for(int i = 3; i < 12; i++) {
            int num = exp(2, i);
            System.out.println("Running incremental classification for "+num+" concepts");
            BenchmarkIncremental.main(new String[] {"RF1", "2", "20110731", 
                    "sct1_Concepts_Core_INT_20110731_base_"+num+".txt",
                    "res1_StatedRelationships_Core_INT_20110731_base_"+num+".txt",
                    "sct1_Concepts_Core_INT_20110731_inc_"+num+".txt",
                    "res1_StatedRelationships_Core_INT_20110731_inc_"+num+".txt",
                    String.valueOf(num)
                    }
            );
            System.out.println("Done.");
        }*/
    }
    
    public static int exp(int x, int y) {
        int res = 1;
        for(int i = 0; i < y; i++) {
            res = res * x;
        }
        return res;
    }

}

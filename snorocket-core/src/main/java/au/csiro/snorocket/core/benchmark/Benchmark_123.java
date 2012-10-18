/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;

import au.csiro.snorocket.core.Factory_123;
import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.core.NormalisedOntology_123;
import au.csiro.snorocket.core.NormalisedOntology_123.Classification;
import au.csiro.snorocket.core.PostProcessedData_123;
import au.csiro.snorocket.core.axioms.Inclusion_123;
import au.csiro.snorocket.core.importer.RF1Importer_123;

/**
 * Class used to measure the speed of the Snorocket classifier.
 * 
 * @author Alejandro Metke
 * 
 */
public class Benchmark_123 {

    final static String RES_DIR = "src/main/resources/";
    final static String OUT_DIR = "src/site/resources/";

    public static final String VERSION = "1.1.22_123";

    /**
     * Runs the benchmark using an RF1 file as input.
     * 
     * @param concepts
     *            The concepts file.
     * @param relationships
     *            The stated relationships file.
     */
    public static Stats runBechmarkRF1(String concepts, String relations) {
        Stats res = new Stats();

        // Classify ontology from stated form
        System.out.println("Classifying ontology from " + concepts);
        long start = System.currentTimeMillis();
        int[] conceptArray = new int[3];
        int[] roleArray = new int[3];
        IFactory_123 factory = new Factory_123(conceptArray, 2, roleArray, 0);
        System.out.println("Importing axioms");
        RF1Importer_123 imp = new RF1Importer_123(factory, concepts, relations);
        NullReasonerProgressMonitor mon = new NullReasonerProgressMonitor();
        List<Inclusion_123> axioms = imp.transform(mon);
        res.setAxiomTransformationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Loading axioms");
        NormalisedOntology_123 no = new NormalisedOntology_123(factory,
                new HashSet<>(axioms));
        res.setAxiomLoadingTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Running classification");
        Classification c = no.getClassification();
        res.setClassificationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Computing taxonomy");
        @SuppressWarnings("unused")
        PostProcessedData_123 ppd = new PostProcessedData_123(factory,
                c.getSubsumptions());
        res.setTaxonomyBuildingTimeMs(System.currentTimeMillis() - start);
        System.out.println("Done");

        return res;
    }

    public static void main(String[] args) {
        String type = args[0];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");

        if ("RF1".equals(type)) {
            String concepts = RES_DIR + args[1];
            String relations = RES_DIR + args[2];
            int numRuns = Integer.parseInt(args[3]);

            String outputFile = OUT_DIR + "benchmark_" + VERSION + "_"
                    + sdf.format(Calendar.getInstance().getTime()) + ".csv";

            StringBuilder sb = new StringBuilder();
            sb.append("Date,VM Parameters,Concepts File,Relationships File,"
                    + "Snorocket Version,Axiom Transformation Time (ms),"
                    + "Axiom Loading Time (ms),Classification Time(ms),"
                    + "Taxonomy Construction Time(ms), Total Time(ms)\n");

            for (int j = 0; j < numRuns; j++) {
                Stats stats = Benchmark_123.runBechmarkRF1(concepts, relations);

                sb.append(sdf.format(Calendar.getInstance().getTime()));
                sb.append(",");

                RuntimeMXBean RuntimemxBean = ManagementFactory
                        .getRuntimeMXBean();
                List<String> arguments = RuntimemxBean.getInputArguments();
                for (int i = 0; i < arguments.size(); i++) {
                    sb.append(arguments.get(i));
                    if (i < arguments.size())
                        sb.append(" ");
                }
                sb.append(",");
                sb.append(concepts);
                sb.append(",");
                sb.append(relations);
                sb.append(",");
                sb.append(VERSION);
                sb.append(",");
                sb.append(stats.getAxiomTransformationTimeMs());
                sb.append(",");
                sb.append(stats.getAxiomLoadingTimeMs());
                sb.append(",");
                sb.append(stats.getClassificationTimeMs());
                sb.append(",");
                sb.append(stats.getTaxonomyBuildingTimeMs());
                sb.append(",");
                sb.append(stats.getTotalTime());
                sb.append("\n");

                System.gc();
            }

            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(
                        new File(outputFile).getAbsoluteFile()));
                bw.write(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bw != null)
                    try {
                        bw.close();
                    } catch (Exception e) {
                    }
                ;
            }
        } else {
            System.out.println("Unknown input type " + type);
            System.exit(0);
        }
    }

}

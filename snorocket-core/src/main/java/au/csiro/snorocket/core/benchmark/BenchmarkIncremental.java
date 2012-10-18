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

import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.PostProcessedData;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.importer.RF1Importer;

/**
 * Class used to measure the speed of the incremental classification
 * functionality in Snorocket.
 * 
 * @author Alejandro Metke
 * 
 */
public class BenchmarkIncremental {

    final static String RES_DIR = "src/main/resources/";
    final static String OUT_DIR = "src/site/resources/";

    public static final String VERSION = "2.0.0";

    /**
     * Runs the incremental benchmark using RF1 files as input. Only the time
     * spent doing the incremental classification is reported (not the time
     * spent doing the base classification).
     * 
     * @param baseConcepts
     * @param baseRelations
     * @param incrementalConcept
     * @param incrementalRelations
     * @return
     */
    public static Stats runBechmarkRF1(String baseConcepts,
            String baseRelations, String incrementalConcepts,
            String incrementalRelations) {
        Stats res = new Stats();

        // Classify ontology from stated form
        System.out.println("Classifying base ontology from " + baseConcepts);

        IFactory factory = new Factory();
        NormalisedOntology no = new NormalisedOntology(factory);
        System.out.println("Importing axioms");
        RF1Importer imp = new RF1Importer(factory, baseConcepts, baseRelations);
        NullReasonerProgressMonitor mon = new NullReasonerProgressMonitor();
        List<Inclusion> axioms = imp.transform(mon);
        System.out.println("Loading axioms");
        no.loadAxioms(new HashSet<>(axioms));
        System.out.println("Running classification");
        no.classify();
        System.out.println("Computing taxonomy");
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, no.getSubsumptions(), null);
        System.out.println("Done");

        // If a relationship that is part of a role group is added incrementally
        // on its own then it will not be added to the correct role group
        // because that information is missing. Therefore care must be taken to
        // remove all relationships that belong to a role group when deriving
        // a test case for incremental classification using RF1.

        System.out.println("Running incremental classification");
        imp = new RF1Importer(factory, incrementalConcepts,
                incrementalRelations);

        long start = System.currentTimeMillis();
        System.out.println("Transforming axioms");
        axioms = imp.transform(mon);
        res.setAxiomTransformationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Running classification");
        no.classifyIncremental(new HashSet<>(axioms));
        res.setClassificationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Computing taxonomy");
        ppd.computeDagIncremental(factory, no.getNewSubsumptions(),
                no.getAffectedSubsumptions(), null);
        res.setTaxonomyBuildingTimeMs(System.currentTimeMillis() - start);

        return res;
    }

    public static void main(String[] args) {
        String type = args[0];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");

        if ("RF1".equals(type)) {
            String baseConcepts = RES_DIR + args[1];
            String baseRelations = RES_DIR + args[2];
            String incConcepts = RES_DIR + args[3];
            String incRelations = RES_DIR + args[4];
            int numRuns = Integer.parseInt(args[5]);
            String outputFile = OUT_DIR + "inc_benchmark_" + VERSION + "_"
                    + sdf.format(Calendar.getInstance().getTime()) + ".csv";

            StringBuilder sb = new StringBuilder();
            sb.append("Date,Threads,VM Parameters,Concepts File,"
                    + "Relationships File,Snorocket Version,Axiom "
                    + "Transformation Time (ms),Axiom Loading Time (ms),"
                    + "Classification Time(ms),Taxonomy Construction "
                    + "Time(ms), Total Time(ms), Used Memory(bytes), "
                    + "Max Memory (bytes)\n");

            for (int j = 0; j < numRuns; j++) {
                Stats stats = BenchmarkIncremental.runBechmarkRF1(baseConcepts,
                        baseRelations, incConcepts, incRelations);

                sb.append(sdf.format(Calendar.getInstance().getTime()));
                sb.append(",");
                sb.append(Runtime.getRuntime().availableProcessors());
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
                sb.append(baseConcepts);
                sb.append(",");
                sb.append(baseRelations);
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
                sb.append(",");
                sb.append(Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory());
                sb.append(",");
                sb.append(Runtime.getRuntime().maxMemory());
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

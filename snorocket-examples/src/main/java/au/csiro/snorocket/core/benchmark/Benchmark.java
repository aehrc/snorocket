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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import au.csiro.ontology.Ontology;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.importer.rf1.RF1Importer;
import au.csiro.ontology.util.NullProgressMonitor;
import au.csiro.snorocket.core.CoreFactory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;

/**
 * Class used to measure the speed of the Snorocket classifier.
 * 
 * @author Alejandro Metke
 * 
 */
public class Benchmark {

    final static String OUT_DIR = "src/site/resources/";

    public static final String VERSION = "2.2.0";

    /**
     * Runs the benchmark using an RF1 file as input.
     * 
     * @param concepts
     *            The concepts file.
     * @param relationships
     *            The stated relationships file.
     */
    public Stats runBechmarkRF1() {
        Stats res = new Stats();
        
        String version = "20110731";
        
        // Classify ontology from stated form
        System.out.println("Classifying ontology");
        long start = System.currentTimeMillis();
        IFactory factory = new CoreFactory();
        NormalisedOntology no = new NormalisedOntology(factory);
        System.out.println("Importing axioms");
        
        RF1Importer imp = new RF1Importer(
                this.getClass().getResourceAsStream(
                        "/sct1_Concepts_Core_INT_20110731.txt"), 
                this.getClass().getResourceAsStream(
                        "/res1_StatedRelationships_Core_INT_20110731.txt"), 
                version);
        
        Iterator<Ontology> it = imp.getOntologyVersions(new NullProgressMonitor());
        
        Ontology ont = null;
        while(it.hasNext()) {
            Ontology o = it.next();
            if(o.getVersion().equals("snomed")) {
                ont = o;
                break;
            }
        }

        // We can do this because we know there is only one ontology (RF1 does
        // not support multiple versions)
        if(ont == null) {
            System.out.println("Could not find version "+version+
                    " in input files");
        }
        res.setAxiomTransformationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Loading axioms");
        no.loadAxioms(new HashSet<Axiom>((Collection<? extends Axiom>) ont.getStatedAxioms()));
        res.setAxiomLoadingTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Running classification");
        no.classify();
        res.setClassificationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Computing taxonomy");
        no.buildTaxonomy();
        res.setTaxonomyBuildingTimeMs(System.currentTimeMillis() - start);
        System.out.println("Done");

        return res;
    }
    
    public static void main(String[] args) {
        String type = args[0];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");

        if ("RF1".equals(type)) {
            int numRuns = Integer.parseInt(args[1]);
            String outputFile = OUT_DIR + "benchmark_" + VERSION + "_"
                    + sdf.format(Calendar.getInstance().getTime()) + ".csv";

            StringBuilder sb = new StringBuilder();
            sb.append("Date,Threads,VM Parameters,Snomed Version," +
            		"Snorocket Version,Axiom Transformation Time (ms)," +
            		"Axiom Loading Time (ms),Classification Time(ms)," +
            		"Taxonomy Construction Time(ms),Total Time(ms)," +
            		"Used Memory(bytes),Max Memory (bytes)\n");
            
            Benchmark b = new Benchmark();
            for (int j = 0; j < numRuns; j++) {
                Stats stats = b.runBechmarkRF1();

                sb.append(sdf.format(Calendar.getInstance().getTime()));
                sb.append(",");
                sb.append(Runtime.getRuntime().availableProcessors());
                sb.append(",");

                RuntimeMXBean RuntimemxBean = 
                        ManagementFactory.getRuntimeMXBean();
                List<String> arguments = RuntimemxBean.getInputArguments();
                for (int i = 0; i < arguments.size(); i++) {
                    sb.append(arguments.get(i));
                    if (i < arguments.size())
                        sb.append(" ");
                }
                sb.append(",");
                sb.append("SNOMED_20110731");
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
                if(bw != null) {
                    try { bw.close(); } catch(Exception e) {}
                }
            }
        } else {
            System.out.println("Unknown input type " + type);
            System.exit(0);
        }
    }

}

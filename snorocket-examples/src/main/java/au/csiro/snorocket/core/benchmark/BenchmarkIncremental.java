/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import au.csiro.ontology.Ontology;
import au.csiro.ontology.importer.rf1.RF1Importer;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.util.NullProgressMonitor;
import au.csiro.snorocket.core.CoreFactory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;

/**
 * Class used to measure the speed of the incremental classification functionality in Snorocket.
 * 
 * @author Alejandro Metke
 * 
 */
public class BenchmarkIncremental {

    final static String OUT_DIR = "src/site/resources/";

    public static final String VERSION = "2.2.0";

    /**
     * Runs the incremental benchmark using RF1 files as input. Only the time
     * spent doing the incremental classification is reported (not the time
     * spent doing the base classification).
     * 
     * @param version
     * @param conceptsBase
     * @param relsBase
     * @param conceptsInc
     * @param relsInc
     * @return
     */
    public Stats runBechmarkRF1(String version, String conceptsBase, String relsBase, String conceptsInc, String relsInc) {
        Stats res = new Stats();

        // Classify ontology from stated form
        System.out.println("Classifying base ontology");

        IFactory factory = new CoreFactory();
        NormalisedOntology no = new NormalisedOntology(factory);
        System.out.println("Importing axioms");
        InputStream conceptsFile = this.getClass().getResourceAsStream("/"+conceptsBase);
        InputStream relsFile = this.getClass().getResourceAsStream("/"+relsBase);
        RF1Importer imp = new RF1Importer(conceptsFile, relsFile, version);
        
        Iterator<Ontology> it = imp.getOntologyVersions(new NullProgressMonitor());
        
        Ontology ont = null;
        while(it.hasNext()) {
            Ontology o = it.next();
            if(o.getVersion().equals("snomed")) {
                ont = o;
                break;
            }
        }
        
        if(ont == null) {
            throw new RuntimeException("Could not find version " + version + " in input files");
        }
        System.out.println("Loading axioms");
        no.loadAxioms(new HashSet<Axiom>(ont.getStatedAxioms()));
        System.out.println("Running classification");
        no.classify();
        System.out.println("Computing taxonomy");
        no.buildTaxonomy();
        System.out.println("Done");

        // If a relationship that is part of a role group is added incrementally
        // on its own then it will not be added to the correct role group
        // because that information is missing. Therefore care must be taken to
        // remove all relationships that belong to a role group when deriving
        // a test case for incremental classification using RF1.

        System.out.println("Running incremental classification");
        imp = new RF1Importer(
                this.getClass().getResourceAsStream("/"+conceptsInc), 
                this.getClass().getResourceAsStream("/"+relsInc), 
                version);

        long start = System.currentTimeMillis();
        System.out.println("Transforming axioms");
        
        it = imp.getOntologyVersions(new NullProgressMonitor());
        
        ont = null;
        while(it.hasNext()) {
            Ontology o = it.next();
            if(o.getVersion().equals("snomed")) {
                ont = o;
                break;
            }
        }
        
        if(ont == null) {
            throw new RuntimeException("Could not find version " + version + " in input files");
        }
        res.setAxiomTransformationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Running classification");
        no.loadIncremental(new HashSet<Axiom>((Collection<? extends Axiom>) ont.getStatedAxioms()));
        no.classifyIncremental();
        res.setClassificationTimeMs(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        System.out.println("Computing taxonomy");
        no.buildTaxonomy();
        res.setTaxonomyBuildingTimeMs(System.currentTimeMillis() - start);

        return res;
    }
    
    public static void main(String[] args) {
        String type = args[0];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");

        if ("RF1".equals(type)) {
            int numRuns = Integer.parseInt(args[1]);
            String outputFile = OUT_DIR + "inc_benchmark_" + VERSION + "_"
                    + sdf.format(Calendar.getInstance().getTime()) + ".csv";

            StringBuilder sb = new StringBuilder();
            sb.append("Date,Threads,VM Parameters,Snomed Version," +
                    "Snorocket Version,Axiom Transformation Time (ms)," +
                    "Axiom Loading Time (ms),Classification Time(ms)," +
                    "Taxonomy Construction Time(ms),Total Time(ms)," +
                    "Used Memory(bytes),Max Memory (bytes),Incremental Concepts\n");
            String version = args[2];
            String conceptsBase = args[3];
            String relsBase = args[4];
            String conceptsInc = args[5];
            String relsInc = args[6];
            String numIncremental = args[7];
            BenchmarkIncremental bi = new BenchmarkIncremental();
            for (int j = 0; j < numRuns; j++) {
                Stats stats = bi.runBechmarkRF1(version, conceptsBase, relsBase, conceptsInc, relsInc);

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
                sb.append(",");
                sb.append(numIncremental);
                sb.append("\n");

                System.gc();
            }

            BufferedWriter bw = null;
            try {
                File ou = new File(outputFile).getAbsoluteFile();
                System.out.println("Writing to file "+ou);
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ou), StandardCharsets.UTF_8));
                bw.write(sb.toString());
                bw.flush();
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

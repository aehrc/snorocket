/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */

package au.csiro.snorocket.snapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.Snorocket;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.util.LineReader;
import au.csiro.snorocket.snapi.I_Snorocket.I_Callback;

public class TestSnorocket {

    /**
     * Not really a test, more for debugging - traced performance discrepancy between snapper
     * and snorocket to the BATCH_MODE flag (which _was_ false by default).  This would also have
     * affected ACE integration.
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void testMain() {
        String[] args = {
                "--conceptsFile",
                "c:/rocket_concepts.txt",
                "--relationshipsFile",
                "c:/rocket_relationships.txt",
                "--Xdebug"
        };

        Main.main(args);
    }

    @Ignore
    @Test
    public void testMain_20061230_stated() {
        String[] args = {
                "--conceptsFile",
                "../ontologies/snomedct_20061230_stated_ming.krss_concepts.txt",
                "--relationshipsFile",
                "../ontologies/snomedct_20061230_stated_ming.krss_relationships.txt",
                "--Xdebug"
        };

        Main.main(args);
    }

    @Ignore
    @Test
    public void testMain_20070731() {
        String[] args = {
                "--conceptsFile",
                "../ontologies/sct_concepts_20070731.txt",
                "--relationshipsFile",
                "../ontologies/sct_relationships_20070731.txt",
                "--Xdebug"
        };
        
        Main.main(args);
    }

    @Ignore
    @Test
    public void testMain_20060131() {
        String[] args = {
                "--conceptsFile",
                "../ontologies/sct_concepts_20060131.txt",
                "--relationshipsFile",
                "../ontologies/sct_relationships_20060131.txt",
                "--Xdebug"
        };

        Main.main(args);
    }
    
    
    @Ignore
    @Test
    public void generatePreclassifiedState() throws ParseException, IOException {
        Snorocket.DEBUGGING = true;
        
        final I_Snorocket rocket = new au.csiro.snorocket.snapi.Snorocket();

        rocket.setIsa(Snorocket.ISA_ROLE);
        loadConcepts(rocket, new FileReader("../ontologies/sct_concepts_20070731.txt"));
        loadRelationships(rocket, new FileReader("../ontologies/sct_relationships_20070731.txt"));
        
        rocket.classify();
        
        final InputStream is = rocket.getStream();
        final FileOutputStream w = new FileOutputStream("src/main/resources/snomed/20070731.txt");
        int len;
        byte[] b = new byte[1024];
        while ((len = is.read(b)) >= 0) {
            w.write(b, 0, len);
        }
        w.close();
    }


    /**
     * Non-redundant defining relationships == distribution form
     * 
     */
	@Ignore
    @Test
    public void definingNonRedundantRelationships() throws FileNotFoundException, ParseException {
        Snorocket.DEBUGGING = true;
        
        final I_Snorocket rocket = new au.csiro.snorocket.snapi.Snorocket();

        rocket.setIsa(Snorocket.ISA_ROLE);
        loadConcepts(rocket, new FileReader("src/test/files/almrf_concepts_stated.txt"));
        loadRelationships(rocket, new FileReader("src/test/files/almrf_relationships_stated.txt"));
        
        rocket.classify();

        final Set<String> result = new HashSet<String>();
     
        rocket.getDistributionFormRelationships(new I_Callback() {

            public void addRelationship(String conceptId1, String roleId,
                                        String conceptId2, int group) {
                final String string = conceptId1 + " " + roleId + " " + conceptId2;

                // The ALMRF documentation only specifes the hierarchy for a subset
                // of the concepts in the example
                if (Integer.valueOf(conceptId1) < 222212) {
                    result.add(string);
                }
                assertEquals(0, group);
            }
            
        });

        // 16 ISAs
        assertTrue(result.contains("222201 116680003 222200")); // B [ A
        assertTrue(result.contains("222202 116680003 222200")); // C [ A
        assertTrue(result.contains("222203 116680003 222201")); // D [ B
        assertTrue(result.contains("222203 116680003 222202")); // D [ C
        assertTrue(result.contains("222204 116680003 222202")); // E [ C
        assertTrue(result.contains("222205 116680003 222202")); // F [ C
        assertTrue(result.contains("222206 116680003 222203")); // G [ D
        assertTrue(result.contains("222207 116680003 222203")); // H [ D
        assertTrue(result.contains("222207 116680003 222204")); // H [ E
        assertTrue(result.contains("222208 116680003 222204")); // J [ E
        assertTrue(result.contains("222208 116680003 222205")); // J [ F
        assertTrue(result.contains("222209 116680003 222205")); // K [ F
        assertTrue(result.contains("222210 116680003 222206")); // L [ G
        assertTrue(result.contains("222210 116680003 222207")); // L [ H
        assertTrue(result.contains("222211 116680003 222207")); // M [ H
        assertTrue(result.contains("222211 116680003 222208")); // M [ J

        // C
        assertTrue(result.contains("222202 222221 222215")); // morph=inj
        
        // D
        assertTrue(result.contains("222203 222220 222212")); // site=upper limb
        assertTrue(result.contains("222203 222221 222215")); // morph=inj

        // E
        assertTrue(result.contains("222204 222221 222215")); // morph=inj
        
        // F
        assertTrue(result.contains("222205 222221 222215")); // morph=inj
        
        // G
        assertTrue(result.contains("222206 222220 222212")); // site=upper limb
        assertTrue(result.contains("222206 222221 222215")); // morph=inj
        assertTrue(result.contains("222206 222222 222214")); // severity=sever

        // H
        assertFalse(result.contains("222207 222220 222212")); // !site=upper limb
        assertTrue(result.contains("222207 222220 222213")); // site=hand
        assertTrue(result.contains("222207 222221 222215")); // morph=inj
        
        // J
        assertTrue(result.contains("222208 222220 222217")); // site=bone
        assertTrue(result.contains("222208 222221 222218")); // morph=fracture
        
        // K
        assertTrue(result.contains("222209 222220 222216")); // site=joint
        assertTrue(result.contains("222209 222221 222215")); // morph=inj
        
        // L
        assertTrue(result.contains("222210 222220 222213")); // site=hand
        assertTrue(result.contains("222210 222221 222215")); // morph=inj
        assertTrue(result.contains("222210 222222 222214")); // severity=sever
        
        // M
        assertTrue(result.contains("222211 222220 222219")); // site=scaphoid bone
        assertTrue(result.contains("222211 222221 222218")); // morph=fracture
        
//        assertEquals(35, result.size());	FIXME
    }

    private void loadConcepts(final I_Snorocket rocket, final Reader concepts) throws ParseException {
        final LineReader lineReader = new LineReader(concepts);

        try {
            String line;
            
            lineReader.readLine();
            
            while (null != (line = lineReader.readLine())) {
                if (line.trim().length() < 1) {
                    continue;
                }
                
                int idx1 = line.indexOf('\t');          // 0..idx1 == conceptid
                int idx2 = line.indexOf('\t', idx1+1);  // idx1+1..idx2 == status
                int idx3 = line.indexOf('\t', idx2+1);  // idx2+1..idx3 == fully specified name
                int idx4 = line.indexOf('\t', idx3+1);  // idx3+1..idx4 == CTV3ID
                int idx5 = line.indexOf('\t', idx4+1);  // idx4+1..idx5 == SNOMEDID
                int idx6 = line.indexOf('\t', idx5+1);  // idx5+1..idx6 == isPrimitive

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0) {
                    throw new ParseException("Concepts: Mis-formatted line, expected at least 6 tab-separated fields, got: " + line, lineReader);
                }

                final String status = line.substring(idx1+1, idx2);
                
                if ("0".equals(status) || "6".equals(status) || "11".equals(status)) {
                    // status one of 0, 6, 11 means the concept is active; we skip inactive concepts

                    final String conceptId = line.substring(0, idx1);

                    final int isPrimitive = idx6 < 0
                            ? Integer.parseInt(line.substring(idx5+1))
                            : Integer.parseInt(line.substring(idx5+1, idx6));
                    
                    rocket.addConcept(conceptId, 0 == isPrimitive);
                }
            }
        } catch (IOException e) {
            throw new ParseException("Concepts: Problem reading concepts file", lineReader, e);
        }
    }
    
    private void loadRelationships(final I_Snorocket rocket, final Reader relationships) throws ParseException {
        final LineReader lineReader = new LineReader(relationships);

        try {
            String line;
            
            lineReader.readLine();
            
            while (null != (line = lineReader.readLine())) {
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');          // 0..idx1 == relationshipid
                int idx2 = line.indexOf('\t', idx1+1);  // idx1+1..idx2 == conceptid1
                int idx3 = line.indexOf('\t', idx2+1);  // idx2+1..idx3 == RELATIONSHIPTYPE
                int idx4 = line.indexOf('\t', idx3+1);  // idx3+1..idx4 == conceptid2
                int idx5 = line.indexOf('\t', idx4+1);  // idx4+1..idx5 == CHARACTERISTICTYPE
                int idx6 = line.indexOf('\t', idx5+1);  // idx5+1..idx6 == REFINABILITY
                int idx7 = line.indexOf('\t', idx6+1);  // idx6+1..idx7 == RELATIONSHIPGROUP

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0 || idx6 < 0) {
                    throw new ParseException("Relationships: Mis-formatted line, expected at least 7 tab-separated fields, got: " + line, lineReader);
                }

                final String concept1 = line.substring(idx1+1, idx2);
                final String role = line.substring(idx2+1, idx3);
                final String concept2 = line.substring(idx3+1, idx4);
                final String characteristicType = line.substring(idx4+1, idx5);

                // only process active concepts and defining relationships
                if ("0".equals(characteristicType)) {
                    final int group = idx7 < 0
                            ? Integer.parseInt(line.substring(idx6+1))
                            : Integer.parseInt(line.substring(idx6+1, idx7));

                    rocket.addRelationship(concept1, role, concept2, group);
                }
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Relationships: Malformed number.", lineReader, e);
        } catch (IOException e) {
            throw new ParseException("Relationships: Problem reading relationships file.", lineReader, e);
        }
    }
    
}

class Main implements Runnable {

    private final static Logger LOGGER = Snorocket.getLogger();
    private final static String VALID_ARGS = "[--conceptsFile filename --relationshipsFile filename|--krssFile filename|--snorocketFile filename] [--outputFile filename] [--inputState filename] [--outputState filename] [--noHeader] [--hideEquivalents] [--virtualConcepts]";
    private final static String X_ARGS = "[--X|--Xhelp] [--Xdebug] [--XvirtualConcepts] [--Xbatch|--Xnobatch]";

    public enum FileFormat {
        SNOMED, KRSS, NATIVE, KRSS2
    }

    private String conceptsFile;
    private String relationshipsFile;
    private String krssFile;
    private String snorocketFile;
    private String outputFile;
    private String inputState;
    private String outputState;
    private boolean showEquivalents = true;
    private boolean skipHeader = true;
    private boolean includeVirtualConcepts = false;
    private boolean usingCompression = false;

    private FileFormat _fileFormat = null;
    private I_Snorocket rocket;

    public Main(String[] args) {
//        Snorocket.installLoggingHander();
        if (null != args) {
            processArgs(args);
        }
    }

    /**
     * 
     * @param args are of form args[i] = --flag or args[i] = --flag, args[i+1] = value
     * @throws IOException 
     */
    private void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--conceptsFile")) {
                i++;
                if (i < args.length) {
                    setConceptsFile(args[i]);
                } else {
                    throw new IllegalArgumentException("--conceptsFile requires an value");
                }
            } else if (arg.equals("--relationshipsFile")) {
                i++;
                if (i < args.length) {
                    setRelationshipsFile(args[i]);
                } else {
                    throw new IllegalArgumentException("--relationshipsFile requires an value");
                }
            } else if (arg.equals("--krssFile")) {
                i++;
                if (i < args.length) {
                    setKRSSFile(args[i]);
                } else {
                    throw new IllegalArgumentException("--krssFile requires an value");
                }
            } else if (arg.equals("--krss2File")) {
                i++;
                if (i < args.length) {
                    setKRSS2File(args[i]);
                } else {
                    throw new IllegalArgumentException("--krss2File requires an value");
                }
            } else if (arg.equals("--snorocketFile")) {
                i++;
                if (i < args.length) {
                    setSnorocketFile(args[i]);
                } else {
                    throw new IllegalArgumentException("--snorocketFile requires an value");
                }
            } else if (arg.equals("--outputFile")) {
                i++;
                if (i < args.length) {
                    setOutputFile(args[i]);
                } else {
                    throw new IllegalArgumentException("--outputFile requires an value");
                }
            } else if (arg.equals("--inputState")) {
                i++;
                if (i < args.length) {
                    setInputState(args[i]);
                } else {
                    throw new IllegalArgumentException("--inputState requires an value");
                }
            } else if (arg.equals("--outputState")) {
                i++;
                if (i < args.length) {
                    setOutputState(args[i]);
                } else {
                    throw new IllegalArgumentException("--outputState requires an value");
                }
            } else if (arg.equals("--hideEquivalents")) {
                setShowEquivalents(false);
            } else if (arg.equals("--noHeader")) {
                setSkipHeader(false);
//              } else if (arg.equals("")) {
//              } else if (arg.equals("")) {
//              } else if (arg.equals("")) {
            } else if (arg.startsWith("--X")) {
                if (arg.equals("--X") || arg.equals("--Xhelp")) {
                    System.err.println("Valid extended args are: " + X_ARGS);
                    System.exit(0);
                } else if (arg.equals("--Xdebug")) {
                    Snorocket.DEBUGGING = true;
//                    Snorocket.uninstallLoggingHander();
                } else if (arg.equals("--XvirtualConcepts")) {
                    setIncludeVirtualConcepts(true);
                } else if (arg.equals("--Xcompress")) {
                    setUsingCompression(true);
                } else {
                    LOGGER.warning("Unknown extended argument: " + arg + ", valid extended args are: " + X_ARGS);
                }
            } else {
                LOGGER.severe("Unknown argument: " + arg + ", valid args are: " + VALID_ARGS);
                System.exit(1);
            }
        }
    }

    private void setFileFormat(FileFormat format) {
        if (null != _fileFormat) {
            throw new IllegalArgumentException("Only one input file may be specified.");
        }
        _fileFormat = format;
    }

    public boolean isShowEquivalents() {
        return showEquivalents;
    }

    public void setShowEquivalents(boolean show) {
        showEquivalents = show;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String filename) {
        outputFile = filename;
    }

    public String getConceptsFile() {
        return conceptsFile;
    }

    public void setConceptsFile(final String filename) {
        conceptsFile = filename;
    }

    public String getRelationshipsFile() {
        return relationshipsFile;
    }

    public void setRelationshipsFile(final String filename) {
        relationshipsFile = filename;
        setFileFormat(FileFormat.SNOMED);
    }

    public String getKRSSFile() {
        return krssFile;
    }

    public void setKRSSFile(final String filename) {
        krssFile = filename;
        setFileFormat(FileFormat.KRSS);
    }

    public void setKRSS2File(final String filename) {
        krssFile = filename;
        setFileFormat(FileFormat.KRSS2);
    }

    public String getSnorocketFile() {
        return snorocketFile;
    }

    public void setSnorocketFile(final String filename) {
        snorocketFile = filename;
        setFileFormat(FileFormat.NATIVE);
    }

    public String getOutputState() {
        return outputState;
    }

    public void setOutputState(String filename) {
        outputState = filename;
    }

    public String getInputState() {
        return inputState;
    }

    public void setInputState(final String filename) {
        inputState = filename;
    }

    private void dontOverwriteInputFile(String filename) {
        if (filename.equals(getOutputFile()) || filename.equals(getOutputState())) {
            throw new AssertionError("Can not overwrite an input file: " + filename);
        }
    }
    
    /*
    protected NormalisedOntology loadOntology() {
        long start = System.currentTimeMillis();
        
        final IFactory factory;
        final Classification classification;

        // check for existing state (implies incremental mode)
        if (null != getInputState()) {
            dontOverwriteInputFile(getInputState());
            try {
                final Reader reader;
                if (isUsingCompression()) {
                    reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(getInputState())));
                } else {
                    reader = new FileReader(getInputState());
                }
                classification = NormalisedOntology.loadClassification(new BufferedReader(reader));
                factory = classification.getExtensionFactory();
                LOGGER.info("Load state: " + (System.currentTimeMillis()-start)/1000.0 + "s");
                start = System.currentTimeMillis();
            } catch (FileNotFoundException e) {
                handleFileNotFoundException(e, getInputState());
                return null;
            } catch (IOException e) {
                handleIOException(e, getInputState());
                return null;
            } catch (ParseException e) {
                handleParseException(e, getInputState());
                return null;
            }
        } else {
            factory = new Factory();
            classification = null;
        }

        final Set<Inclusion> ontology;

        if (null == _fileFormat) {
            throw new IllegalStateException("No input files specified.");
        }

        switch (_fileFormat) {
        case SNOMED:
            ontology = null;
            rocket = loadSnomedOntology(factory);
            break;
        case KRSS:
        case KRSS2:
            ontology = loadKRSSOntology(factory);
            break;
        case NATIVE:
            ontology = loadSnorocketOntology(factory);
            break;
        default:
            throw new AssertionError("Unknown file format requested: " + _fileFormat);  
        }

        if (null == ontology) {
            return null;
        }

        final NormalisedOntology normalisedOntology;
        if (null == classification) {
            normalisedOntology = new NormalisedOntology(factory, ontology);
        } else {
            //normalisedOntology = classification.getExtensionOntology(factory, ontology);
        	normalisedOntology = null;
        }

        LOGGER.info("Load + normalise time: " + (System.currentTimeMillis()-start)/1000.0 + "s");
        
        return normalisedOntology;
    }
    */

    private Set<Inclusion> loadSnorocketOntology(final IFactory factory) {
        if (null != getConceptsFile()) {
            LOGGER.warning("Concepts file ignored: " + getConceptsFile());
        }

        if (null == getSnorocketFile()) {
            LOGGER.severe("No snorocket file specified.");
            return null;
        }
        
        dontOverwriteInputFile(getSnorocketFile());

        Set<Inclusion> ontology = null;
        try {
            final Reader reader = new FileReader(getSnorocketFile());
//            ontology = new SnorocketParser().parse(factory, reader);	FIXME
        } catch (FileNotFoundException e) {
            handleFileNotFoundException(e, getSnorocketFile());
//        } catch (ParseException e) {
//            handleParseException(e, getSnorocketFile());
        }
        return ontology;
    }

    private Set<Inclusion> loadKRSSOntology(final IFactory factory) {
        if (null != getConceptsFile()) {
            LOGGER.warning("Concepts file ignored: " + getConceptsFile());
        }

        if (null == getKRSSFile()) {
            LOGGER.severe("No KRSS file specified.");
            return null;
        }
        
        dontOverwriteInputFile(getKRSSFile());

        Set<Inclusion> ontology = null;
        try {
            final Reader reader = new FileReader(getKRSSFile());
            if (_fileFormat == FileFormat.KRSS) {
//                ontology = new KRSSParser().parse(factory, reader);	FIXME
            } else if (_fileFormat == FileFormat.KRSS2) {
//                ontology = new KRSSParserMeng().parse(factory, reader);	FIXME
            } else {
                throw new AssertionError("File format expected to be one of " + FileFormat.KRSS + " or " + FileFormat.KRSS2 + ", not " + _fileFormat);
            }
        } catch (FileNotFoundException e) {
            handleFileNotFoundException(e, getKRSSFile());
//        } catch (ParseException e) {
//            handleParseException(e, getKRSSFile());
        }
        return ontology;
    }

    private I_Snorocket loadSnomedOntology(IFactory factory) {
        if (null == getConceptsFile()) {
            LOGGER.severe("No concepts file specified.");
            return null;
        }
        if (null == getRelationshipsFile()) {
            LOGGER.severe("No relationships file specified.");
            return null;
        }
        dontOverwriteInputFile(getConceptsFile());
        dontOverwriteInputFile(getRelationshipsFile());

        final Reader concepts;
        final Reader relationships;

        try {
            concepts = new FileReader(getConceptsFile());
        } catch (FileNotFoundException e) {
            handleFileNotFoundException(e, getConceptsFile());
            return null;
        }

        try {
            relationships = new FileReader(getRelationshipsFile());
        } catch (FileNotFoundException e) {
            handleFileNotFoundException(e, getRelationshipsFile());
            return null;
        }

        try {
            final MockFileTableParser mockFileTableParser = new MockFileTableParser();
            return mockFileTableParser.parse(factory, isSkipHeader(), concepts, relationships);
        } catch (ParseException e) {
            handleParseException(e, getConceptsFile() + " or " + getRelationshipsFile());
            return null;
        }
    }

    private void handleParseException(ParseException e, String file) {
        LOGGER.log(Level.SEVERE, "Parse error for file: " + file + ", " + e.getMessage(), e);
    }

    private void handleIOException(IOException e, String file) {
        LOGGER.log(Level.SEVERE, "Problem reading input file: " + file + ", " + e.getMessage(), e);
    }

    private void handleFileNotFoundException(FileNotFoundException e, String file) {
        LOGGER.log(Level.SEVERE, "File not found: " + file + ", " + e.getMessage(), e);
    }
    
    public void run() {
    	/*
        long start = System.currentTimeMillis();
        final NormalisedOntology normalisedOntology = loadOntology();

        if (null == normalisedOntology) {
            start = System.currentTimeMillis();
            rocket.classify();
            LOGGER.info("Classification time: " + (System.currentTimeMillis()-start)/1000.0 + "s");
            return;
        }

        if (Snorocket.DEBUGGING) {
            final IFactory factory = normalisedOntology.getFactory();
            LOGGER.info(factory.getTotalConcepts() + " concepts, " + factory.getTotalRoles() + " roles.");
        }
        
        start = System.currentTimeMillis();
        Classification classification = normalisedOntology.getClassification();
        LOGGER.info("Classification time: " + (System.currentTimeMillis()-start)/1000.0 + "s");

        if (null != getOutputState()) {
            try {
                final Writer writer;
                if (isUsingCompression()) {
                    writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(getOutputState())));
                } else {
                    writer = new FileWriter(getOutputState());
                }
                final PrintWriter printWriter = new PrintWriter(writer);
                classification.printClassification(printWriter);
                printWriter.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Problem writing file: " + getOutputState() + ", " + e.getMessage(), e);
            }
        }

//        PostProcessedData ppd = null;		FIXME
        
        try {
            if (null != getOutputFile()) {
                final PrintWriter printWriter;
                if ("-".equals(getOutputFile())) {
                    printWriter = new PrintWriter(System.out);
                } else {
                    printWriter = new PrintWriter(new FileWriter(getOutputFile()));
                }
//                ppd = classification.getPostProcessedData();	FIXME
//                new FileTablePrinter().printRelationshipTable(normalisedOntology.getFactory(), classification, ppd, printWriter, isIncludeVirtualConcepts());
                if (!"-".equals(getOutputFile())) {
                    printWriter.close();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Problem writing file: " + getOutputFile() + ", " + e.getMessage(), e);
        }

        if (isShowEquivalents()) {
            start = System.currentTimeMillis();
            final IFactory factory = normalisedOntology.getFactory();
//            if (null == ppd) {	FIXME
//                ppd = classification.getPostProcessedData();
//            }
//            final IConceptMap<IConceptSet> eq = ppd.getEquivalents();		FIXME
            LOGGER.info("Compute equivalents time: " + (System.currentTimeMillis()-start)/1000.0 + "s");

            System.out.println("-- Equivalents");
//            for (final IntIterator itr = eq.keyIterator(); itr.hasNext(); ) {	FIXME
//                final int c = itr.next();
//                final IConceptSet set = eq.get(c);
//                if (set.size() > 0) {
//                    final IntIterator sItr = set.iterator();
//                    System.out.print(factory.lookupConceptId(c) + " == " + factory.lookupConceptId(sItr.next()));
//                    while (sItr.hasNext()) {
//                        System.out.print(", " + factory.lookupConceptId(sItr.next()));
//                    }
//                    System.out.println();
//                }
//            }
        }
		*/
    }

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public void setSkipHeader(boolean skipHeader) {
        this.skipHeader = skipHeader;
    }

    public boolean isIncludeVirtualConcepts() {
        return includeVirtualConcepts;
    }

    public void setIncludeVirtualConcepts(boolean includeVirtualConcepts) {
        this.includeVirtualConcepts = includeVirtualConcepts;
    }
    
    void setUsingCompression(boolean useCompression) {
        this.usingCompression  = useCompression;
    }
    
    private boolean isUsingCompression() {
        return usingCompression;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.severe("Usage: snorocket " + VALID_ARGS);
            return;
        }
        try {
            new Main(args).run();
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Caught unexpected exception", e);
            System.exit(1);
        }
    }

}

class MockFileTableParser {
    private I_Snorocket rocket;

    public I_Snorocket parse(final IFactory factory, final boolean skipHeader, final Reader concepts, final Reader relationships) throws ParseException {
        try {
            rocket = new au.csiro.snorocket.snapi.Snorocket();
            rocket.setIsa(Snorocket.ISA_ROLE);

            loadConcepts(skipHeader, concepts);
            loadRelationships(skipHeader, relationships);

            return rocket;
        } finally {
            // avoid mem leak in parser
            rocket = null;
        }
    }

    public static int limit = Integer.MAX_VALUE;

    private void loadRelationships(final boolean skipHeader, final Reader relationships) throws ParseException {
        final BufferedReader br = new BufferedReader(relationships);

        int lineNumber = 0;

        try {
            if (skipHeader) {
                br.readLine();  // skip header line
                lineNumber++;
            }

            String line;
            while (null != (line = br.readLine()) && lineNumber < limit) {
                lineNumber++;
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');          // 0..idx1 == relationshipid
                int idx2 = line.indexOf('\t', idx1+1);  // idx1+1..idx2 == conceptid1
                int idx3 = line.indexOf('\t', idx2+1);  // idx2+1..idx3 == RELATIONSHIPTYPE
                int idx4 = line.indexOf('\t', idx3+1);  // idx3+1..idx4 == conceptid2
                int idx5 = line.indexOf('\t', idx4+1);  // idx4+1..idx5 == CHARACTERISTICTYPE
                int idx6 = line.indexOf('\t', idx5+1);  // idx5+1..idx6 == REFINABILITY
                int idx7 = line.indexOf('\t', idx6+1);  // idx6+1..idx7 == RELATIONSHIPGROUP

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0 || idx6 < 0) {
                    throw new RuntimeException("Relationships: Mis-formatted line, expected at least 7 tab-separated fields, got: " + line + " on line " + lineNumber);
                }

                final String concept1 = line.substring(idx1+1, idx2);
                final String role = line.substring(idx2+1, idx3);
                final String concept2 = line.substring(idx3+1, idx4);
                final String characteristicType = line.substring(idx4+1, idx5);

                // only process active concepts and defining relationships
                if ("0".equals(characteristicType)) {
                    final int group = idx7 < 0
                            ? Integer.parseInt(line.substring(idx6+1))
                            : Integer.parseInt(line.substring(idx6+1, idx7));

                    rocket.addRelationship(concept1, role, concept2, group);
//                  if (Snorocket.DEBUGGING && rowList.size() > 1480842) {
//                      System.err.println("#rows = " + rowList.size());
//                  }
//                    if (Snorocket.DEBUGGING && "109917000".equals(concept1)) {
//                        System.err.println(line);
//                    }
                } else {
//                    if (Snorocket.DEBUGGING) {
//                        if (!_factory.conceptExists(concept1)) {
//                            Snorocket.getLogger().warning("Skipping unknown concept1: " + concept1);
//                        } else if (!_factory.conceptExists(concept2)) {
//                            Snorocket.getLogger().warning("Skipping unknown concept2: " + concept2);
//                        } else if (!_factory.conceptExists(role)) {
//                            Snorocket.getLogger().warning("Skipping unknown role: " + role);
//                        } else if (false) {
//                            System.err.println("Skipping inactive " + characteristicType +"\t"+concept1+"\t"+_factory.conceptExists(concept1) + "\t"+concept2+"\t"+_factory.conceptExists(concept2) + "\t"+role+"\t"+_factory.conceptExists(role));
//                        }
//                    }
                }
            }

            if (null != line) {
                System.err.println("Line limit reached while loading relationships file, read " + limit + " lines.");
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Relationships: Malformed number.", lineNumber, e);
        } catch (IOException e) {
            throw new ParseException("Relationships: Problem reading relationships file.", lineNumber, e);
        }
//        if (Snorocket.DEBUGGING) System.err.println("Number of rows = " + rowList.size());

    }

    private void loadConcepts(final boolean skipHeader, final Reader concepts) throws ParseException {
        final BufferedReader br = new BufferedReader(concepts);
        int lineNumber = 0;

        try {
            if (skipHeader) {
                br.readLine();  // skip header line
                lineNumber++;
            }

            String line;
            while (null != (line = br.readLine())) {
                lineNumber++;
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');          // 0..idx1 == conceptid
                int idx2 = line.indexOf('\t', idx1+1);  // idx1+1..idx2 == status
                int idx3 = line.indexOf('\t', idx2+1);  // idx2+1..idx3 == fully specified name
                int idx4 = line.indexOf('\t', idx3+1);  // idx3+1..idx4 == CTV3ID
                int idx5 = line.indexOf('\t', idx4+1);  // idx4+1..idx5 == SNOMEDID
                int idx6 = line.indexOf('\t', idx5+1);  // idx5+1..idx6 == isPrimitive

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0) {
                    throw new RuntimeException("Concepts: Mis-formatted line, expected at least 6 tab-separated fields, got: " + line + " on line " + lineNumber);
                }

                final String status = line.substring(idx1+1, idx2);
                
                if ("0".equals(status) || "6".equals(status) || "11".equals(status)) {
                    // status one of 0, 6, 11 means the concept is active; we skip inactive concepts

//                    c.setLabel(line.substring(idx2+1, idx3));

                    final int isPrimitive = idx6 < 0
                            ? Integer.parseInt(line.substring(idx5+1))
                            : Integer.parseInt(line.substring(idx5+1, idx6));

                     rocket.addConcept(line.substring(0, idx1), 0 == isPrimitive);
                }
            }
        } catch (IOException e) {
            throw new ParseException("Concepts: Problem reading concepts file", lineNumber, e);
        }
    }

    static class Row implements Comparable<Row> {

        final int concept1;
        final int role;
        final int concept2;
        final int group;

        Row(final int concept1, final int role, final int concept2, final int group) {
            this.concept1 = concept1;
            this.role = role;
            this.concept2 = concept2;
            this.group = group;
        }

        public int compareTo(Row other) {
//            return (int) Math.signum(
//                    concept1 == other.concept1
//                    ? (group == other.group
//                            ? (role == other.role ? concept2 - other.concept2 : role - other.role)
//                                    : (group - other.group))
//                                    : (concept1 - other.concept1)
//            );
            return concept1 == other.concept1
            ? (group == other.group
                ? (role == other.role
                    ? (compareTo(concept2, other.concept2))
                    : compareTo(role, other.role))
                : compareTo(group, other.group))
            : compareTo(concept1, other.concept1);
        }

        private static int compareTo(int lhs, int rhs) {
            return lhs < rhs ? -1 : (lhs > rhs ? 1 : 0);
        }

        @Override
        public String toString() {
            return concept1 + ",\t" + role + ",\t" + concept2 + ",\t" + group;
        }
        
        // The following are implemented to satisfy FindBugs
        
        @Override
        public boolean equals(Object other) {
            assert false: "equals not designed";
        throw new AssertionError();
//            return super.equals(other);
        }
        
        @Override
        public int hashCode() {
            assert false: "hashCode not designed";
        throw new AssertionError();
//            return 42;
        }

    }

}

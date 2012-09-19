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

package au.csiro.snorocket;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.parser.FileTableParser;
import au.csiro.snorocket.parser.KRSSParser;
import au.csiro.snorocket.parser.KRSSParserMeng;
import au.csiro.snorocket.parser.SnorocketParser;
import au.csiro.snorocket.printer.FileTablePrinter;
import au.csiro.snorocket.snapi.I_Snorocket;
import au.csiro.snorocket.snapi.SnomedMetadata;
import au.csiro.snorocket.snapi.Snorocket;

public class Main implements Runnable {

	private final static Logger LOGGER = au.csiro.snorocket.core.Snorocket
			.getLogger();
	private final static String VALID_ARGS = "[--conceptsFile filename --relationshipsFile filename|--krssFile filename|--snorocketFile filename] [--outputFile filename] [--inputState filename] [--outputState filename] [--noHeader] [--hideEquivalents] [--allRelationships]";
	private final static String X_ARGS = "[--X|--Xhelp] [--Xdebug] [--XvirtualConcepts] [--Xbatch|--Xnobatch] [--XisaId isaId]";

	public enum FileFormat {
		SNOMED, KRSS, NATIVE, KRSS2, UNSET
	}

	private String conceptsFile;
	private String relationshipsFile;
	private String krssFile;
	private String snorocketFile;
	private String outputFile;
	private String inputState;
	private String outputState;
	private String isaId = au.csiro.snorocket.core.Snorocket.ISA_ROLE;
	private boolean allRelationships = false;
	private boolean showEquivalents = true;
	private boolean skipHeader = true;
	private boolean includeVirtualConcepts = false;
	private boolean usingCompression = false;

	private boolean shouldExit = false;

	private FileFormat _fileFormat = FileFormat.UNSET;

	public Main(String[] args) {
		au.csiro.snorocket.core.Snorocket.installLoggingHander();
		//NormalisedOntology.setBatchMode(true);
		if (null != args) {
			processArgs(args);
		}
	}

	private interface ArgHandler {
	}

	private interface NoParamArgHandler extends ArgHandler {
		void handleArg();
	}

	private interface ParamArgHandler extends ArgHandler {
		void handleParam(String param);
	}

	private Map<String, ArgHandler> argHandlers = new HashMap<String, ArgHandler>();
	{
		argHandlers.put("--conceptsFile", new ParamArgHandler() {
			public void handleParam(String param) {
				setConceptsFile(param);
			}
		});
		argHandlers.put("--relationshipsFile", new ParamArgHandler() {
			public void handleParam(String param) {
				setRelationshipsFile(param);
			}
		});
		argHandlers.put("--krssFile", new ParamArgHandler() {
			public void handleParam(String param) {
				setKRSSFile(param);
			}
		});
		argHandlers.put("--krss2File", new ParamArgHandler() {
			public void handleParam(String param) {
				setKRSS2File(param);
			}
		});
		argHandlers.put("--snorocketFile", new ParamArgHandler() {
			public void handleParam(String param) {
				setSnorocketFile(param);
			}
		});
		argHandlers.put("--outputFile", new ParamArgHandler() {
			public void handleParam(String param) {
				setOutputFile(param);
			}
		});
		argHandlers.put("--inputState", new ParamArgHandler() {
			public void handleParam(String param) {
				setInputState(param);
			}
		});
		argHandlers.put("--outputState", new ParamArgHandler() {
			public void handleParam(String param) {
				setOutputState(param);
			}
		});

		argHandlers.put("--hideEquivalents", new NoParamArgHandler() {
			public void handleArg() {
				setShowEquivalents(false);
			}
		});
		argHandlers.put("--noHeader", new NoParamArgHandler() {
			public void handleArg() {
				setSkipHeader(false);
			}
		});
		argHandlers.put("--allRelationships", new NoParamArgHandler() {
			public void handleArg() {
				setAllRelationships(true);
			}
		});

		final NoParamArgHandler xUsage = new NoParamArgHandler() {
			public void handleArg() {
				System.err.println("Valid extended args are: " + X_ARGS);
				shouldExit = true;
			}
		};
		argHandlers.put("--X", xUsage);
		argHandlers.put("--Xhelp", xUsage);
		argHandlers.put("--Xdebug", new NoParamArgHandler() {
			public void handleArg() {
				au.csiro.snorocket.core.Snorocket.DEBUGGING = true;
				au.csiro.snorocket.core.Snorocket.uninstallLoggingHander();
			}
		});
		argHandlers.put("--XvirtualConcepts", new NoParamArgHandler() {
			public void handleArg() {
				setIncludeVirtualConcepts(true);
			}
		});
		/*
		argHandlers.put("--Xbatch", new NoParamArgHandler() {
			public void handleArg() {
				NormalisedOntology.setBatchMode(true);
			}
		});
		argHandlers.put("--Xnobatch", new NoParamArgHandler() {
			public void handleArg() {
				NormalisedOntology.setBatchMode(false);
			}
		});
		*/
		argHandlers.put("--Xcompress", new NoParamArgHandler() {
			public void handleArg() {
				setUsingCompression(true);
			}
		});
		argHandlers.put("--XisaId", new ParamArgHandler() {
			public void handleParam(String param) {
				isaId = param;
			}
		});
	}

	/**
	 * 
	 * @param args
	 *            are of form args[i] = --flag or args[i] = --flag, args[i+1] =
	 *            value
	 * @return
	 * @throws IOException
	 */
	private void processArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			final String arg = args[i];

			final ArgHandler handler = argHandlers.get(arg);
			if (handler instanceof ParamArgHandler) {
				i++;
				if (i < args.length) {
					final String param = args[i];
					if (argHandlers.containsKey(param)) {
						throw new ReportErrorAndExitException(
								"Missing parameter for " + arg);
					}
					((ParamArgHandler) handler).handleParam(param);
				} else {
					throw new ReportErrorAndExitException(arg
							+ " requires a value");
				}
			} else if (handler instanceof NoParamArgHandler) {
				((NoParamArgHandler) handler).handleArg();
			} else if (arg.startsWith("--X")) {
				LOGGER.warning("Unknown extended argument: " + arg
						+ ", valid extended args are: " + X_ARGS);
			} else {
				throw new ReportErrorAndExitException(("Unknown argument: "
						+ arg + ", valid args are: " + VALID_ARGS));
			}
		}
	}

	private void setAllRelationships(boolean allRelationships) {
		this.allRelationships = allRelationships;
	}

	private boolean getAllRelationships() {
		return allRelationships;
	}

	private void setFileFormat(FileFormat format) {
		if (!FileFormat.UNSET.equals(_fileFormat)) {
			throw new ReportErrorAndExitException(
					"Only one input file may be specified.");
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
		if (filename.equals(getOutputFile())
				|| filename.equals(getOutputState())) {
			throw new ReportErrorAndExitException(
					"Can not overwrite an input file: " + filename);
		}
	}

	class Snorocket2 extends Snorocket {

		private Set<Inclusion> ontology;
		final private boolean incremental;
		final private PrintWriter printWriter;

		public Snorocket2(PrintWriter printWriter) {
			super();
			this.incremental = false;
			this.printWriter = printWriter;
			init();
		}

		public Snorocket2(final InputStream state, PrintWriter printWriter) {
			//super(state);
			this.incremental = true;
			this.printWriter = printWriter;
		}

		// called by createExtension
		/*
		protected Snorocket2(final Classification classification,
				final String isaId, PrintWriter printWriter) {
			//super(classification, isaId);
			this.incremental = true;
			this.printWriter = printWriter;
			init();
		}

		@Override
		protected I_Snorocket createExtension(Classification classification,
				String isaId) {
			return new Snorocket2(classification, isaId, printWriter);
		}
		*/

		private void init() {

			if (!incremental) {
				setIsa(isaId);
				SnomedMetadata.configureSnorocket(this, "20080731");
			}

			switch (_fileFormat) {
			case SNOMED:
				loadSnomedOntology(this, incremental, printWriter);
				ontology = super.getInclusions();
				break;
			case KRSS:
			case KRSS2:
				ontology = loadKRSSOntology(factory);
				break;
			case NATIVE:
				ontology = loadSnorocketOntology(factory);
				break;
			case UNSET:
				throw new ReportErrorAndExitException(
						"No input files specified.");
			default:
				throw new ReportErrorAndExitException(
						"Unknown file format requested: " + _fileFormat);
			}
		}

		@Override
		protected Set<Inclusion> getInclusions() {
			return ontology;
		}
	}

	protected I_Snorocket loadOntology(PrintWriter printWriter) {
		long start = System.currentTimeMillis();

		if (FileFormat.UNSET.equals(_fileFormat)) {
			throw new ReportErrorAndExitException("No input files specified.");
		}

		I_Snorocket rocket;

		// check for existing state (implies incremental mode)
		if (null != getInputState()) {
			dontOverwriteInputFile(getInputState());
			try {
				final InputStream input;
				if (isUsingCompression()) {
					input = new GZIPInputStream(new FileInputStream(
							getInputState()));
				} else {
					input = new FileInputStream(getInputState());
				}
				rocket = new Snorocket2(input, printWriter).createExtension();
				if (au.csiro.snorocket.core.Snorocket.DEBUGGING)
					LOGGER.info("Load state time (s): "
							+ (System.currentTimeMillis() - start) / 1000.0);
				start = System.currentTimeMillis();
			} catch (FileNotFoundException e) {
				throw handleFileNotFoundException(e, getInputState());
			} catch (IOException e) {
				throw handleIOException(e, getInputState());
			}
		} else {
			rocket = new Snorocket2(printWriter);
		}

		if (au.csiro.snorocket.core.Snorocket.DEBUGGING)
			LOGGER.info("Load time: " + (System.currentTimeMillis() - start)
					/ 1000.0 + "s");

		return rocket;
	}

	private Set<Inclusion> loadSnorocketOntology(final IFactory factory) {
		if (null != getConceptsFile()) {
			LOGGER.warning("Concepts file ignored: " + getConceptsFile());
		}

		if (null == getSnorocketFile()) {
			throw new ReportErrorAndExitException(
					"No snorocket file specified.");
		}

		dontOverwriteInputFile(getSnorocketFile());

		Set<Inclusion> ontology = null;
		try {
			final Reader reader = new FileReader(getSnorocketFile());
			ontology = new SnorocketParser().parse(factory, reader);
		} catch (FileNotFoundException e) {
			throw handleFileNotFoundException(e, getSnorocketFile());
		} catch (ParseException e) {
			throw handleParseException(e, getSnorocketFile());
		}
		return ontology;
	}

	private Set<Inclusion> loadKRSSOntology(final IFactory factory) {
		if (null != getConceptsFile()) {
			LOGGER.warning("Concepts file ignored: " + getConceptsFile());
		}

		if (null == getKRSSFile()) {
			throw new ReportErrorAndExitException("No KRSS file specified.");
		}

		dontOverwriteInputFile(getKRSSFile());

		Set<Inclusion> ontology = null;
		try {
			final Reader reader = new FileReader(getKRSSFile());
			if (_fileFormat == FileFormat.KRSS) {
				ontology = new KRSSParser().parse(factory, reader);
			} else if (_fileFormat == FileFormat.KRSS2) {
				ontology = new KRSSParserMeng().parse(factory, reader);
			} else {
				throw new ReportErrorAndExitException(
						"File format expected to be one of " + FileFormat.KRSS
								+ " or " + FileFormat.KRSS2 + ", not "
								+ _fileFormat);
			}
		} catch (FileNotFoundException e) {
			throw handleFileNotFoundException(e, getKRSSFile());
		} catch (ParseException e) {
			throw handleParseException(e, getKRSSFile());
		}
		return ontology;
	}

	private void loadSnomedOntology(final I_Snorocket rocket,
			boolean incremental, PrintWriter printWriter) {
		if (null == getConceptsFile()) {
			throw new ReportErrorAndExitException("No concepts file specified.");
		}
		if (null == getRelationshipsFile()) {
			throw new ReportErrorAndExitException(
					"No relationships file specified.");
		}
		dontOverwriteInputFile(getConceptsFile());
		dontOverwriteInputFile(getRelationshipsFile());

		final Reader concepts;
		final Reader relationships;

		if (!incremental) {
			rocket.setIsa(isaId);
		}

		try {
			concepts = new FileReader(getConceptsFile());
		} catch (FileNotFoundException e) {
			throw handleFileNotFoundException(e, getConceptsFile());
		}

		LOGGER.info("Concepts file: " + getConceptsFile());

		try {
			relationships = new FileReader(getRelationshipsFile());
		} catch (FileNotFoundException e) {
			throw handleFileNotFoundException(e, getRelationshipsFile());
		}

		LOGGER.info("Relationships file: " + getRelationshipsFile());

		try {
			new FileTableParser().parse(rocket, isSkipHeader(), concepts,
					relationships, printWriter);
		} catch (ParseException e) {
			throw handleParseException(e, getConceptsFile() + " or "
					+ getRelationshipsFile());
		}

	}

	private ReportErrorAndExitException handleParseException(ParseException e,
			String file) {
		return new ReportErrorAndExitException("Parse error for file: " + file
				+ ", " + e.getMessage(), e);
	}

	private ReportErrorAndExitException handleIOException(IOException e,
			String file) {
		return new ReportErrorAndExitException("Problem reading/writing file: "
				+ file + ", " + e.getMessage());
	}

	private ReportErrorAndExitException handleFileNotFoundException(
			FileNotFoundException e, String file) {
		return new ReportErrorAndExitException("File not found: " + file + ", "
				+ e.getMessage());
	}

	// EKM
	private boolean log_memory_p = false;

	private long start_time = 0;

	private void logMemory(String tag) {
		if (start_time == 0)
			start_time = System.currentTimeMillis();
		if (!log_memory_p)
			return;
		long gc_start = System.currentTimeMillis();
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		long gc_time = System.currentTimeMillis() - gc_start;
		LOGGER.info("Used memory @ " + tag + ": "
				+ ((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)));
		LOGGER.info("Time @ " + tag + ": "
				+ ((System.currentTimeMillis() - start_time) / 1000) + " (GC "
				+ gc_time + " ms)");
	}

	public void run() {
		String log_memory_p_str = System.getProperty("log.memory");
		if (log_memory_p_str != null) {
			log_memory_p_str = log_memory_p_str.toUpperCase();
			log_memory_p = log_memory_p_str.startsWith("T")
					|| log_memory_p_str.startsWith("Y");
		}
		final PrintWriter printWriter;
		final FileTablePrinter callback;

		if (null != getOutputFile()) {
			if ("-".equals(getOutputFile())) {
				printWriter = new PrintWriter(System.out);
			} else {
				try {
					printWriter = new PrintWriter(new FileWriter(
							getOutputFile()));
				} catch (IOException e) {
					throw handleIOException(e, getOutputFile());
				}
			}
			// Need to create the callback object here since, as a side effect,
			// it prints head information
			// to the PrintWriter and this needs to be done *before* the call to
			// loadOntology below.
			callback = new FileTablePrinter(printWriter);
		} else {
			printWriter = null;
			callback = null;
		}

		long start = System.currentTimeMillis();
		logMemory("Pre loadOntology");
		final I_Snorocket rocket = loadOntology(printWriter);
		logMemory("Post loadOntology");

		// if (null == normalisedOntology) {
		// return;
		// }

		// if (Snorocket.DEBUGGING) {
		// final IFactory factory = normalisedOntology.getFactory();
		// LOGGER.info(factory.getTotalConcepts() + " concepts, " +
		// factory.getTotalRoles() + " roles.");
		// }

		start = System.currentTimeMillis();
		logMemory("Pre classify");
		rocket.classify();
		logMemory("Post classify");
		// final Classification classification =
		// normalisedOntology.getClassification();
		if (au.csiro.snorocket.core.Snorocket.DEBUGGING)
			LOGGER.info("Classification time: "
					+ (System.currentTimeMillis() - start) / 1000.0 + "s");

		if (null != getOutputState()) {
			try {
				final OutputStream output;
				if (isUsingCompression()) {
					output = new GZIPOutputStream(new FileOutputStream(
							getOutputState()));
				} else {
					output = new FileOutputStream(getOutputState());
				}
				// final PrintWriter printWriter = new PrintWriter(output);
				// normalisedOntology.printClassification(printWriter);
				final BufferedInputStream is = new BufferedInputStream(rocket
						.getStream());
				final byte[] buffer = new byte[1024];
				int len;
				while ((len = is.read(buffer)) >= 0) {
					output.write(buffer, 0, len);
				}
				output.close();
			} catch (IOException e) {
				throw handleIOException(e, getOutputState());
			}
		}

		logMemory("Pre xxxRelationships");
		if (null != getOutputFile()) {
			if (getAllRelationships()) {
				rocket.getRelationships(callback);
			} else {
				rocket.getDistributionFormRelationships(callback);
			}
			printWriter.flush();
			if (!"-".equals(getOutputFile())) {
				printWriter.close();
			}
		}
		logMemory("Post xxxRelationships");

		// if (isShowEquivalents()) { FIXME
		// final IConceptMap<IConceptSet> eq =
		// getPostProcessedData(normalisedOntology.getFactory(),
		// classification).getEquivalents();
		//
		// System.out.println("-- Equivalents");
		// final IFactory factory = normalisedOntology.getFactory();
		// for (final IntIterator itr = eq.keyIterator(); itr.hasNext(); ) {
		// final int c = itr.next();
		// final IConceptSet set = eq.get(c);
		// if (set.size() > 0) {
		// final IntIterator sItr = set.iterator();
		// System.out.print(factory.lookupConceptId(c) + " == " +
		// factory.lookupConceptId(sItr.next()));
		// while (sItr.hasNext()) {
		// System.out.print(", " + factory.lookupConceptId(sItr.next()));
		// }
		// System.out.println();
		// }
		// }
		// }

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
		this.usingCompression = useCompression;
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
			final Main main = new Main(args);
			if (!main.shouldExit) {
				main.run();
			}
		} catch (ReportErrorAndExitException e) {
			if (null != e.getCause()) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e.getCause());
			} else {
				LOGGER.severe(e.getMessage());
			}
		} catch (RuntimeException e) {
			if (au.csiro.snorocket.core.Snorocket.DEBUGGING) {
				LOGGER.log(Level.SEVERE, "Caught unexpected exception", e);
			}
			throw e;
		}
	}

	static class ReportErrorAndExitException extends RuntimeException {

		public ReportErrorAndExitException(String message) {
			super(message);
		}

		public ReportErrorAndExitException(String message, Exception e) {
			super(message, e);
		}

	}

}

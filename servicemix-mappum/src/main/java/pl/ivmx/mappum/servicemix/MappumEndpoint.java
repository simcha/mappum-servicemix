package pl.ivmx.mappum.servicemix;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.jruby.exceptions.RaiseException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @org.apache.xbean.XBean element="endpoint"
 */
public class MappumEndpoint extends ProviderEndpoint {

	public static final String LANGUAGE_RUBY = "jruby";

	public static final String DEFAULT_MAP_FOLDER = "map";
	public static final String DEFAULT_SCHEMA_FOLDER = "schema";
	public static final String DEFAULT_GENERATED_CLASSES_FOLDER = "generated_classes";

	private String mapFolder;
	private String schemaFolder;
	private String generatedClassesFolder;

	private ScriptEngine engine;
	private ScriptEngineManager manager;
	private String language = LANGUAGE_RUBY;

	private String xsd2rubyScriptCode;
	private Resource xsd2rubyScript;

	public void validate() throws DeploymentException {
		super.validate();

		logger.info("MappumEndpoint.validate() called");
	}

	protected void processInOut(MessageExchange exchange, NormalizedMessage in,
			NormalizedMessage out) throws Exception {

		if (exchange.getStatus() == ExchangeStatus.DONE) {
			return;
		} else if (exchange.getStatus() == ExchangeStatus.ERROR) {
			// Exchange has been aborted with an exception
			return;
		} else if (exchange.getFault() != null) {
			// Fault message
			done(exchange);
		} else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {

			if (engine != null) {

				SourceTransformer sourceTransformer = new SourceTransformer();
				String inMessage = sourceTransformer.toString(in.getContent());

				String mappumRubyScript = "def mappum(xml)\n"
						+ "rt = Mappum::XmlTransform.new()\n"
						+ "content = rt.transform(xml)\n" + "end";

				try {
					logger.debug("IN MESSAGE:\n" + inMessage);

					engine.eval(mappumRubyScript);
					Invocable invEngine = (Invocable) engine;
					String outMessage = (String) invEngine.invokeFunction(
							"mappum", inMessage);

					out.setContent(new StringSource(outMessage));

					logger.debug("OUT MESSAGE:\n"
							+ sourceTransformer.toString(out.getContent()));

					getChannel().send(exchange);
				} catch (ScriptException e) {
					RaiseException re = (RaiseException) e.getCause();
					logger.error(re.getException());
					throw re;
				}

			} else {
				throw new MessagingException(
						"ScriptingEngine for jruby not initialized.");
			}
		}
	}

	@Override
	public void start() throws Exception {
		super.start();

		logger.debug("MappumEndpoint.start() called");
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		logger.debug("MappumEndpoint.stop() called");
	}

	@Override
	public synchronized void activate() throws Exception {
		super.activate();

		logger.debug("MappumEndpoint.activate() called");

		setFullPaths();

		logger.info("\nMappumEndpoint information:\n\tendpoint = "
				+ getEndpoint() + "\n\tendpoint install root = "
				+ getContext().getInstallRoot() + "\n\tservice unit = "
				+ getServiceUnit().getName() + "\n\tservice unit root path = "
				+ getServiceUnit().getRootPath() + "\n\tmap folder = "
				+ getMapFolder() + "\n\tschema folder = " + getSchemaFolder()
				+ "\n\tgenerated classes folder = "
				+ getGeneratedClassesFolder());

		// checkConfiguration();

		try {
			manager = new ScriptEngineManager(serviceUnit
					.getConfigurationClassLoader());
			engine = manager.getEngineByName(language);
		} catch (Exception e) {
			logger.fatal(
					"Unable to instantiate ScriptEngineManager or ScriptEngine for "
							+ language, e);
			throw new DeploymentException(e);
		}

		if (engine == null) {
			logger.fatal("There is no script engine for language " + language);
			throw new DeploymentException(
					"There is no script engine for language " + language);
		}

		// traverseAndGenerateClassesFromXSD(new File(getSchemaFolder()), null);

		generateAndRequire();
	}

	@Override
	public synchronized void deactivate() throws Exception {
		super.deactivate();

		logger.debug("MappumEndpoint.deactivate() called");
	}

	@Override
	protected void done(MessageExchange messageExchange)
			throws MessagingException {
		super.done(messageExchange);
	}

	@Override
	protected void fail(MessageExchange messageExchange, Exception e)
			throws MessagingException {
		super.fail(messageExchange, e);
	}

	@Override
	protected void send(MessageExchange messageExchange)
			throws MessagingException {
		super.send(messageExchange);
	}

	@Override
	protected void sendSync(MessageExchange messageExchange)
			throws MessagingException {
		super.sendSync(messageExchange);
	}

	public String getMapFolder() {
		return mapFolder;
	}

	public void setMapFolder(String mapFolder) {
		this.mapFolder = mapFolder;
	}

	public String getSchemaFolder() {
		return schemaFolder;
	}

	public void setSchemaFolder(String schemaFolder) {
		this.schemaFolder = schemaFolder;
	}

	public String getGeneratedClassesFolder() {
		return generatedClassesFolder;
	}

	public void setGeneratedClassesFolder(String generatedClassesFolder) {
		this.generatedClassesFolder = generatedClassesFolder;
	}

	private void setFullPaths() throws Exception {

		if (getMapFolder() == null || "".equals(getMapFolder().trim())) {
			setMapFolder(DEFAULT_MAP_FOLDER);
		}
		if (getSchemaFolder() == null || "".equals(getSchemaFolder().trim())) {
			setSchemaFolder(DEFAULT_SCHEMA_FOLDER);
		}
		if (getGeneratedClassesFolder() == null
				|| "".equals(getGeneratedClassesFolder().trim())) {
			setGeneratedClassesFolder(DEFAULT_GENERATED_CLASSES_FOLDER);
		}
		logger.debug("getServiceUnit()=" + getServiceUnit());
		if (getServiceUnit().getRootPath() != null) {
			setMapFolder(getServiceUnit().getRootPath() + File.separator
					+ getMapFolder());
			setSchemaFolder(getServiceUnit().getRootPath() + File.separator
					+ getSchemaFolder());
			setGeneratedClassesFolder(getServiceUnit().getRootPath()
					+ File.separator + getGeneratedClassesFolder());
		}
	}

	private void generateAndRequire() throws DeploymentException {

		try {

			StringBuffer params = new StringBuffer();
			params.append("\n");
			params.append("require 'xml_transform'");
			params.append("\n\n");
			params.append("wl = Mappum::WorkdirLoader.new(\"");
			params.append(getSchemaFolder());
			params.append("\", \"");
			params.append(getGeneratedClassesFolder());
			params.append("\", \"");
			params.append(getMapFolder());
			params.append("\")");
			params.append("\n");
			params.append("wl.generate_and_require");
			params.append("\n");

			xsd2rubyScriptCode = params.toString();

			logger.debug(xsd2rubyScriptCode);

			xsd2rubyScript = new ByteArrayResource(xsd2rubyScriptCode
					.getBytes());

			// execute the script (non-compiled!)

			engine.eval(new InputStreamReader(xsd2rubyScript.getInputStream()));

		} catch (IOException e) {
			logger.fatal("Unable to load the script "
					+ xsd2rubyScript.getFilename(), e);
			throw new DeploymentException("Unable to load the script "
					+ xsd2rubyScript.getFilename());
		} catch (ScriptException e) {
			RaiseException re = (RaiseException) e.getCause();
			logger.fatal(re.getException());
			throw new DeploymentException(re.getException().getBacktrace()
					.toString(), e);
		}

	}

	private void checkConfiguration() throws DeploymentException {

		List<String> xsdFiles = Collections
				.synchronizedList(new ArrayList<String>());

		traverse(new File(getSchemaFolder()), xsdFiles);

		if (xsdFiles.isEmpty()) {
			logger.fatal("No XSD file found in folder " + getSchemaFolder()
					+ " or its subfolders");
			throw new DeploymentException("No XSD file found in folder "
					+ getSchemaFolder() + " or its subfolders");
		}

		// TODO: check if mapFolder contains any map

		// TODO: check if all files in mapSchema folder and subfolders have
		// distinct base names
		// (there is no file abc.xsd in more than one folder at the same time)
	}

	private void generateClassesFromXSD(File xsdFile, String module)
			throws DeploymentException {

		File classes = new File(getGeneratedClassesFolder());

		if (!classes.exists()) {
			classes.mkdir();
		}

		String classesFolder = classes.getAbsolutePath() + File.separator;

		logger.debug("Processing XML Schema file " + xsdFile.getAbsolutePath()
				+ " ...");

		String xsdName = xsdFile.getName().replaceAll("\\.xsd$", "");

		try {

			StringBuffer params = new StringBuffer();
			params.append("'--xsd', 'file://");
			params.append(xsdFile.getAbsolutePath());
			params.append("', ");
			params.append("'--classdef', '");
			params.append(classesFolder);
			params.append(xsdName);
			params.append("', ");
			params.append("'--mapping_registry', ");
			params.append("'--mapper', ");
			if (module != null && !"".equals(module.trim())) {
				params.append("'--module_path', '");
				params.append(module);
				params.append("', ");
			}
			// params.append("'--quiet', ");
			// params.append("'--force' ");

			xsd2rubyScriptCode = "ARGV=[" + params.toString()
					+ "]; load 'xsd2ruby.rb'";

			logger.debug(xsd2rubyScriptCode);

			xsd2rubyScript = new ByteArrayResource(xsd2rubyScriptCode
					.getBytes());

			// execute the script (non-compiled!)

			engine.eval(new InputStreamReader(xsd2rubyScript.getInputStream()));

		} catch (IOException e) {
			logger.fatal("Unable to load the script "
					+ xsd2rubyScript.getFilename(), e);
			throw new DeploymentException("Unable to load the script "
					+ xsd2rubyScript.getFilename());
		} catch (ScriptException e) {
			RaiseException re = (RaiseException) e.getCause();
			logger.fatal(re.getException().getBacktrace());
			throw new DeploymentException(re.getException().getBacktrace()
					.toString(), e);
		}

	}

	private void traverseAndGenerateClassesFromXSD(File path, String module)
			throws DeploymentException {

		if (path.isDirectory()) {

			// first, check XML Schema files from the current directory
			FilenameFilter filterXSD = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xsd");
				}
			};
			File[] filesXSD = path.listFiles(filterXSD);

			if (filesXSD != null) {
				for (int i = 0; i < filesXSD.length; i++) {
					generateClassesFromXSD(filesXSD[i], module);
				}
			}

			// next, recursively check subdirectories
			FileFilter filterDir = new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}

			};
			File[] subdirs = path.listFiles(filterDir);
			if (subdirs != null) {
				for (int i = 0; i < subdirs.length; i++) {

					String submodule = (module == null ? "" : module + "::")
							+ subdirs[i].getName().substring(0, 1)
									.toUpperCase()
							+ subdirs[i].getName().substring(1);

					traverseAndGenerateClassesFromXSD(subdirs[i], submodule);
				}
			}

		}
	}

	private void traverse(File path, List<String> xsdFiles) {

		if (path.isDirectory()) {

			// first, check XML Schema files from the current directory
			FilenameFilter filterXSD = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xsd");
				}
			};
			File[] filesXSD = path.listFiles(filterXSD);

			if (filesXSD != null) {
				for (int i = 0; i < filesXSD.length; i++) {
					xsdFiles.add(filesXSD[i].getAbsolutePath());
				}
			}

			// next, recursively check subdirectories
			FileFilter filterDir = new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}

			};
			File[] subdirs = path.listFiles(filterDir);
			if (subdirs != null) {
				for (int i = 0; i < subdirs.length; i++) {
					traverse(subdirs[i], xsdFiles);
				}
			}

		}
	}
}

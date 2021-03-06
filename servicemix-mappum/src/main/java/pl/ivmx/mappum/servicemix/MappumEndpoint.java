package pl.ivmx.mappum.servicemix;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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

				String mapName = null;

				if (exchange.getOperation() != null) {
					mapName = (String) exchange.getOperation().getLocalPart();
				}

				String mappumRubyScript = "def mappum(xml,map)\n"
						+ "rt = Mappum::XmlTransform.new()\n"
						+ "content = rt.transform(xml,map)\n" + "end";

				try {
					logger.debug("IN MESSAGE:\n" + inMessage);

					engine.eval(mappumRubyScript);
					Invocable invEngine = (Invocable) engine;
					String outMessage = (String) invEngine.invokeFunction(
							"mappum", inMessage, mapName);

					out.setContent(new StringSource(outMessage));

					logger.debug("OUT MESSAGE:\n"
							+ sourceTransformer.toString(out.getContent()));
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

		logger.info("MappumEndpoint started");
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		logger.info("MappumEndpoint stopped");
	}

	@Override
	public synchronized void activate() throws Exception {
		super.activate();

		setFullPaths();

		logger.info("\nMappumEndpoint information:\n\tendpoint = "
				+ getEndpoint() + "\n\tendpoint install root = "
				+ getContext().getInstallRoot() + "\n\tservice unit = "
				+ getServiceUnit().getName() + "\n\tservice unit root path = "
				+ getServiceUnit().getRootPath() + "\n\tmap folder = "
				+ getMapFolder() + "\n\tschema folder = " + getSchemaFolder()
				+ "\n\tgenerated classes folder = "
				+ getGeneratedClassesFolder());

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

		logger.debug("MappumEndpoint activated");
	}

	@Override
	public synchronized void deactivate() throws Exception {
		super.deactivate();

		logger.debug("MappumEndpoint deactivated");
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
			params.append("require 'mappum/xml_transform'");
			params.append("\n\n");
			params.append("wl = Mappum::WorkdirLoader.new(\"");
			params.append(getSchemaFolder());
			params.append("\", \"");
			params.append(getMapFolder());
			params.append("\", \"");
			params.append(getGeneratedClassesFolder());
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
}

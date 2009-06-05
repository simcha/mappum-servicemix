package pl.ivmx.mappum.servicemix;

import java.util.List;

import javax.jbi.JBIException;

import org.apache.servicemix.common.DefaultComponent;

/**
 * @org.apache.xbean.XBean element="component" description="Mappum Component"
 */
public class MappumComponent extends DefaultComponent {

	private MappumEndpoint[] endpoints;

	@Override
	public void shutDown() throws JBIException {
		super.shutDown();

		logger.info("MappumComponent shut down.");
	}

	@Override
	public void start() throws JBIException {
		super.start();

		logger.info("MappumComponent started.");
		logger.info("MappumComponent install root = "
				+ getComponentContext().getInstallRoot());
	}

	@Override
	public void stop() throws JBIException {
		super.stop();

		logger.debug("MappumComponent stopped.");
	}

	public MappumEndpoint[] getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(MappumEndpoint[] endpoints) {
		this.endpoints = endpoints;
	}

	protected List getConfiguredEndpoints() {
		return asList(endpoints);
	}

	protected Class[] getEndpointClasses() {
		return new Class[] { MappumEndpoint.class };
	}
}

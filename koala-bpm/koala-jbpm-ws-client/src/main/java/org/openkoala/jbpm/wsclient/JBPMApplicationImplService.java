package org.openkoala.jbpm.wsclient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.6 in JDK 6 Generated
 * source version: 2.1
 * 
 */
@WebServiceClient(name = "JBPMApplicationImplService", targetNamespace = "http://jbpm.applicationImpl.jbpm.openkoala.org/", wsdlLocation = "http://localhost:8180/ws/jbpmService?wsdl")
public class JBPMApplicationImplService extends Service {

	private final static URL JBPMAPPLICATIONIMPLSERVICE_WSDL_LOCATION;
	private final static Logger logger = Logger
			.getLogger(org.openkoala.jbpm.wsclient.JBPMApplicationImplService.class
					.getName());

	static {
		URL url = null;
		try {
			URL baseUrl;
			baseUrl = org.openkoala.jbpm.wsclient.JBPMApplicationImplService.class
					.getResource(".");
			url = new URL(baseUrl, "http://localhost:8180/ws/jbpmService?wsdl");
		} catch (MalformedURLException e) {
			logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:8180/ws/jbpmService?wsdl', retrying as a local file");
			logger.warning(e.getMessage());
		}
		JBPMAPPLICATIONIMPLSERVICE_WSDL_LOCATION = url;
	}

	public JBPMApplicationImplService(URL wsdlLocation, QName serviceName) {
		super(wsdlLocation, serviceName);
	}

	public JBPMApplicationImplService() {
		super(JBPMAPPLICATIONIMPLSERVICE_WSDL_LOCATION, new QName(
				"http://jbpm.applicationImpl.jbpm.openkoala.org/",
				"JBPMApplicationImplService"));
	}

	/**
	 * 
	 * @return returns JBPMApplication
	 */
	@WebEndpoint(name = "JBPMApplicationImplPort")
	public JBPMApplication getJBPMApplicationImplPort() {
		return super.getPort(new QName(
				"http://jbpm.applicationImpl.jbpm.openkoala.org/",
				"JBPMApplicationImplPort"), JBPMApplication.class);
	}

	/**
	 * 
	 * @param features
	 *            A list of {@link javax.xml.ws.WebServiceFeature} to configure
	 *            on the proxy. Supported features not in the
	 *            <code>features</code> parameter will have their default
	 *            values.
	 * @return returns JBPMApplication
	 */
	@WebEndpoint(name = "JBPMApplicationImplPort")
	public JBPMApplication getJBPMApplicationImplPort(
			WebServiceFeature... features) {
		return super.getPort(new QName(
				"http://jbpm.applicationImpl.jbpm.openkoala.org/",
				"JBPMApplicationImplPort"), JBPMApplication.class, features);
	}

	public JBPMApplicationImplService(URL url) {
		super(url, new QName(
				"http://jbpm.applicationImpl.jbpm.openkoala.org/",
				"JBPMApplicationImplService"));
	}

	private static JBPMApplication jbpmApplication;

	public static JBPMApplication getJBPMApplication() {
		if (jbpmApplication == null) {
			jbpmApplication = new JBPMApplicationImplService()
					.getJBPMApplicationImplPort();
		}
		return jbpmApplication;
	}

	public JBPMApplicationImplService(String url) throws MalformedURLException {
		super(new URL(url), new QName(
				"http://jbpm.applicationImpl.jbpm.openkoala.org/",
				"JBPMApplicationImplService"));
	}

}
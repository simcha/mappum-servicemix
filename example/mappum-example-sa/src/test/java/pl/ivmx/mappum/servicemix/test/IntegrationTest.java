package pl.ivmx.mappum.servicemix.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.activemq.broker.BrokerService;
import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SuppressWarnings("deprecation")
public class IntegrationTest extends TestCase {

	private Log logger = new Log4JLogger(IntegrationTest.class.getName());;

	protected static ConfigurableApplicationContext applicationContext;

	private static Properties settings = new Properties();

	private static int counter = 0;

	public void testHttpMappum() throws Exception {

		logger.info("PROCESSING... testHttpMappum()");

		counter++;

		try {
			String endpoint = "http://localhost:8192/MappumService";

			Service service = new Service();
			Call call = (Call) service.createCall();

			call.setTargetEndpointAddress(new java.net.URL(endpoint));
			call.setOperationName("person");

			// create XML file in the input directory
			InputStream is = this.getClass().getClassLoader()
					.getResourceAsStream("data/person.xml");
			assertNotNull("Missing input file 'data/person.xml'", is);

			SOAPEnvelope retVal = call.invoke(new Message(is, true));

			assertNotNull("No value returned from web service", retVal);
			assertNotNull("No soap-body in message returned from web service",
					retVal.getBody());
			assertNotNull("Child nodes list in soap-body is null", retVal
					.getBody().getChildNodes());
			assertEquals("Soap-body should have only one child element", 1,
					retVal.getBody().getChildNodes().getLength());
			assertEquals("Mappum not working - xml not translated", "client",
					retVal.getBody().getChildNodes().item(0).getLocalName());

		} catch (Exception e) {
			logger.error("Error when calling web service", e);
			fail("Error when calling web service");
		}
	}

	public void testFileCamelMappum() throws Exception {

		logger.info("PROCESSING... testFileCamelMappum()");

		counter++;

		settings = (Properties) applicationContext.getBean("settings");
		String inputDir = settings.getProperty("filePoller.dir");
		String outputDir = settings.getProperty("fileSender.dir");

		File inputDirFile = new File(inputDir);
		File outputDirFile = new File(outputDir);

		String[] inFiles = inputDirFile.list();
		String[] outFiles = outputDirFile.list();

		assertNotNull("Input directory '" + inputDirFile.getAbsolutePath()
				+ "' should already exist", inFiles);
		assertNotNull("Output directory '" + outputDirFile.getAbsolutePath()
				+ "'should already exist", outFiles);

		assertTrue("Input directory '" + inputDirFile.getAbsolutePath()
				+ "' should be empty", inFiles.length == 0);
		assertTrue("Output directory '" + outputDirFile.getAbsolutePath()
				+ "' should be empty", outFiles.length == 0);

		// create XML file in the input directory
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(
				"data/person.xml");
		assertNotNull("Missing input file 'data/person.xml'", is);

		File inFile = new File(inputDirFile.getAbsolutePath() + File.separator
				+ "person.xml");

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		Writer w = new FileWriter(inFile);

		String line = br.readLine();
		while (line != null) {
			w.write(line);
			w.write("\n");
			line = br.readLine();
		}
		w.flush();
		w.close();
		br.close();

		inFiles = inputDirFile.list();
		assertTrue("Input directory '" + inputDirFile.getAbsolutePath()
				+ "' should not be empty", inFiles.length > 0);

		logger
				.info("\n\t==========================\n\t    WAITING 65 SECONDS\n\t==========================\n");

		Thread.sleep(65000); // wait 1 m 05 sec

		// test if the converted XML appeared in the output directory
		outFiles = outputDirFile.list();
		assertTrue("Output directory '" + outputDirFile.getAbsolutePath()
				+ "' should not be empty", outFiles.length > 0);

		File outFile = new File(outputDirFile.getAbsolutePath()
				+ File.separator + "client.xml");
		br = new BufferedReader(new FileReader(outFile));
		StringBuffer sb = new StringBuffer();
		line = br.readLine();
		while (line != null) {
			sb.append(line);
			sb.append("\n");
			line = br.readLine();
		}
		br.close();

		assertTrue("'person' not converted to 'client'", sb.toString()
				.contains("<client>"));

	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		logger.info("counter = " + counter);

		if (counter == 0) {
			logger.info("STARTING UP APPLICATION CONTEXT...");
			FileUtil.deleteFile(new File("rootDir"));
			applicationContext = createApplicationContext();
			assertNotNull("Could not create the applicationContext!",
					applicationContext);
			applicationContext.start();
			logger.info("...APPLICATION CONTEXT STARTED");
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		logger.info("counter = " + counter);

		if (counter == 2) {
			logger.info("STOPPING APPLICATION CONTEXT...");
			if (applicationContext != null) {
				applicationContext.stop();

				Map brokers = applicationContext
						.getBeansOfType(BrokerService.class);
				if (!brokers.isEmpty()) {
					Set keys = brokers.keySet();
					Iterator it = keys.iterator();
					Object key = it.next();
					BrokerService broker = (BrokerService) brokers.get(key);
					logger.info("\tSTOPPING BROKER " + broker + "...");
					broker.stop();
					logger.info("\t...BROKER " + broker + " STOPPED");
				}

				logger.info("...APPLICATION CONTEXT STOPPED");
			}
		}
	}

	protected ConfigurableApplicationContext createApplicationContext()
			throws Exception {
		return new ClassPathXmlApplicationContext("integrationTest.xml");
	}
}

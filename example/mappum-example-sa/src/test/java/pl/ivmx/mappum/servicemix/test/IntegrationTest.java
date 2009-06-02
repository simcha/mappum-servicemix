package pl.ivmx.mappum.servicemix.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SuppressWarnings("deprecation")
public class IntegrationTest extends TestCase {

	private Log logger = new Log4JLogger(IntegrationTest.class.getName());;

	protected ConfigurableApplicationContext applicationContext;

	private Properties settings = new Properties();

	public void testDeploy() throws Exception {
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
		FileUtil.deleteFile(new File("rootDir"));
		super.setUp();
		applicationContext = createApplicationContext();
		assertNotNull("Could not create the applicationContext!",
				applicationContext);
		applicationContext.start();
	}

	@Override
	protected void tearDown() throws Exception {
		if (applicationContext != null) {
			applicationContext.stop();
		}
		super.tearDown();
	}

	protected ConfigurableApplicationContext createApplicationContext()
			throws Exception {
		return new ClassPathXmlApplicationContext("integrationTest.xml");
	}
}

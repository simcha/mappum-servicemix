package pl.ivmx.mappum.servicemix.test;

import java.io.File;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SuppressWarnings("deprecation")
public class IntegrationTest extends TestCase {

	protected ConfigurableApplicationContext applicationContext;

    public void testDeploy() throws Exception {
//        Thread.sleep(5000);
    }
    
	@Override
    protected void setUp() throws Exception {
        FileUtil.deleteFile(new File("rootDir"));
        super.setUp();
        applicationContext = createApplicationContext();
        assertNotNull("Could not create the applicationContext!", applicationContext);
        applicationContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (applicationContext != null) {
            applicationContext.stop();
        }
        super.tearDown();
    }

    protected ConfigurableApplicationContext createApplicationContext() throws Exception {
        return new ClassPathXmlApplicationContext("integrationTest.xml");
    }
}


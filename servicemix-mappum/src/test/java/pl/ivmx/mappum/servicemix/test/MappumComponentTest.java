package pl.ivmx.mappum.servicemix.test;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class MappumComponentTest extends SpringTestSupport {

	private Log logger = new Log4JLogger(MappumComponentTest.class.getName());;

	public void testMissingMap() throws Exception {

		DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
		InOut me = client.createInOutExchange();
		me.setService(new QName("urn:test", "service"));

		me.getInMessage().setContent(
				new StringSource("<hello>mappum rulez!</hello>"));

		client.sendSync(me);

		logger.debug(me.getError().getMessage());

		if (me.getStatus() != ExchangeStatus.ERROR) {
			fail("Should return error.");
		}
		assertTrue("Should return error about missing map.", me.getError()
				.getMessage().contains("not registered for xml mapping"));

		client.close();
	}

	public void testPerson2Client() throws Exception {

		SourceTransformer sourceTransformer = new SourceTransformer();
		Source src = getSourceFromClassPath("/data/person.xml");

		if (src != null) {

			DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
			InOut me = client.createInOutExchange();
			me.setService(new QName("urn:test", "service"));

			me.getInMessage().setContent(src);

			client.sendSync(me);

			String dst = sourceTransformer.toString(me.getOutMessage()
					.getContent());

			assertTrue("'person' not converted to 'client'", dst
					.contains("<client>"));

			client.done(me);

		} else {
			fail("Missing example xml file with person data.");
		}
	}

	public void testClient2Person() throws Exception {

		SourceTransformer sourceTransformer = new SourceTransformer();
		Source src = getSourceFromClassPath("/data/client.xml");

		if (src != null) {

			DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
			InOut me = client.createInOutExchange();
			me.setService(new QName("urn:test", "service"));

			me.getInMessage().setContent(src);

			client.sendSync(me);

			String dst = sourceTransformer.toString(me.getOutMessage()
					.getContent());

			assertTrue("'client' not converted to 'person'", dst
					.contains("<person>"));

			client.done(me);

		} else {
			fail("Missing example xml file with client data.");
		}
	}

	protected AbstractXmlApplicationContext createBeanFactory() {
		// System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
		// SpringInitialContextFactory.class.getName());

		return new ClassPathXmlApplicationContext("spring.xml");
	}

}

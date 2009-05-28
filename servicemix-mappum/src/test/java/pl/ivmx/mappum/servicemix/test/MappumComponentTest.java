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

		if (me.getStatus() != ExchangeStatus.ERROR) {
			fail("Should return error.");
		}

		assertNull("Output message must be NULL", me.getOutMessage()
				.getContent());

		assertTrue("Should return error about missing map.", me.getError()
				.getMessage().matches(".*Map for .* not found.*"));

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

	public void testAddress2Adresse() throws Exception {

		SourceTransformer sourceTransformer = new SourceTransformer();
		Source src = getSourceFromClassPath("/data/address.xml");

		if (src != null) {

			DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
			InOut me = client.createInOutExchange();
			me.setService(new QName("urn:test", "service"));

			me.getInMessage().setContent(src);
			me.setProperty("map_name", "address");

			client.sendSync(me);

			assertNotNull("Output message can't be NULL", me.getOutMessage()
					.getContent());

			String dst = sourceTransformer.toString(me.getOutMessage()
					.getContent());

			assertTrue("'address' not converted to 'adresse'", dst
					.contains("<adresse>"));

			client.done(me);

		} else {
			fail("Missing example xml file with 'Address' data.");
		}
	}

	protected AbstractXmlApplicationContext createBeanFactory() {
		// System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
		// SpringInitialContextFactory.class.getName());

		return new ClassPathXmlApplicationContext("spring.xml");
	}

}

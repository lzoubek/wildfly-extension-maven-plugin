package org.jboss.plugins;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class XmlConfigBuilderTest {
	
	final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
	final XPath xpath = XPathFactory.newInstance().newXPath();
	
	public XmlConfigBuilderTest() {
		try {
			factory.setNamespaceAware(true);
			dBuilder = factory.newDocumentBuilder();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void printTempFile() {
		try {
			System.out.println(IOUtil.toString(new FileReader(getTempFile())));
		} catch (Exception e) {
			
		}
	}
	
	private File getResourceFile(String name) {
		return new File("src/test/resources/"+name);
	}
	
	private File getTempFile() {
		return new File(System.getProperty("java.io.tmpdir"),"test.xml");
	}
	
	private Document xml(String xml) throws Exception {
	    InputSource is = new InputSource(new StringReader(xml));
	    return dBuilder.parse(is);
	}
	
	private void assertXpath(String expression, Document doc, int expectedCount) throws Exception {
		XPathExpression expr = xpath.compile(expression);
		NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		Assert.assertEquals(expectedCount, nl.getLength());
	}
	
	@Test
	public void findNamespaceFromXpath() {
		Assert.assertEquals("test", XmlConfigBuilder.findRecentNamespaceFromXpath("/*[namespace-uri()='test']/test"));
		Assert.assertEquals("test", XmlConfigBuilder.findRecentNamespaceFromXpath("/*[namespace-uri()='foo']/test/*[namespace-uri()='test']"));
	}
	
	@Test
	public void testXpath2Namespaced() {
		Assert.assertEquals("", XmlConfigBuilder.xpath2Namespaced("",""));
		Assert.assertEquals("x:test/x:test1", XmlConfigBuilder.xpath2Namespaced("test/test1","x"));
		Assert.assertEquals("/x:test[@a='x']/*[@b='y']", XmlConfigBuilder.xpath2Namespaced("/test[@a='x']/*[@b='y']","x"));
	}
	
	@Test
	public void testElement2Xpath() throws Exception {
		// element without ns
		Assert.assertEquals("/test", XmlConfigBuilder.element2Xpath(xml("<test></test>").getDocumentElement(),"x",null));
		// element without ns default prefix passed
		Assert.assertEquals("/y:test", XmlConfigBuilder.element2Xpath(xml("<test></test>").getDocumentElement(),"x","y"));
		// element with ns
		Assert.assertEquals("/x:test", XmlConfigBuilder.element2Xpath(xml("<test xmlns=\"foo\"></test>").getDocumentElement(),"x",null));
		// element with attributes
		Assert.assertEquals("/test[@attr1='val1' and @attr2='val2']", XmlConfigBuilder.element2Xpath(xml("<test attr1=\"val1\"  attr2=\"val2\"></test>").getDocumentElement(),"x",null));
		// element with ns and attributes
		Assert.assertEquals("/x:test[@attr1='val1' and @attr2='val2']", XmlConfigBuilder.element2Xpath(xml("<test attr1=\"val1\" xmlns=\"foo\"  attr2=\"val2\"></test>").getDocumentElement(),"x",null));

	}
	
	@Test
	public void testRootAppend() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("root.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1Append.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		assertXpath("/server/subsystem[@name='foobar']/child", doc, 2);
		assertXpath("/server/subsystem[@name='foo']/child", doc,1); // subsystem without with different name unchanged
	}
	
	@Test
	public void testRootAppendKeepNestedNS() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("root.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1AppendNestedNS.xml")));
		builder.build();
		Document doc = dBuilder.parse(builder.getTargetFile());
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", "keepme"));
		assertXpath("/server/subsystem[@name='foobar']/child", doc, 2);
		assertXpath("/server/subsystem[@name='foo']/child", doc,1); // subsystem without with different name unchanged		
		assertXpath("/server/subsystem[@name='foobar']/x:child2/x:keepme",doc,1); //verify we did not overwrite nested namespace
	}
	
	@Test
	public void testRootReplace() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("root.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1Replace.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		assertXpath("/server/subsystem[@name='foo']/child", doc,2); // subsystem modified
	}
	
	
	@Test
	public void testNSRrootAppend() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1Append.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		String xmlns = doc.getDocumentElement().getAttribute("xmlns");
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", xmlns));	
		assertXpath("/x:server/x:subsystem[@name='foobar']/x:child", doc, 2);
		assertXpath("/x:server/x:subsystem[@name='foo']/x:child", doc,1); // subsystem without with different name unchanged
	}
	@Test
	public void testNSRootReplace() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1Replace.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		String xmlns = doc.getDocumentElement().getAttribute("xmlns");
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", xmlns).mapping("ns", "foo"));
		assertXpath("/x:server/ns:subsystem/ns:child", doc, 5); // subsystem without name attribute unchaned (4 +1 already present)
		assertXpath("/x:server/x:subsystem[@name='foo']/x:child", doc,2); // subsystem without ns replaced
	}
	
	@Test
	public void testNSRootNSAppend() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1NSAppend.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		String xmlns = doc.getDocumentElement().getAttribute("xmlns");
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", xmlns).mapping("ns", "something"));	
		assertXpath("/*[local-name()='server']/*[local-name()='subsystem' and namespace-uri()='something']/*[local-name()='child']", doc, 2);
		// query using namespaces
		assertXpath("/x:server/ns:subsystem/ns:child", doc, 2);
		assertXpath("/x:server/x:subsystem[@name='foo']/x:child", doc,1); // subsystem without ns unchanged
	}
	
	
	@Test
	public void testNSRootNSReplace() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("content1NSReplace.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		String xmlns = doc.getDocumentElement().getAttribute("xmlns");
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", xmlns).mapping("ns", "foo"));
		assertXpath("/*[local-name()='server']/*[local-name()='subsystem' and namespace-uri()='foo' and @name='bar']/*[local-name()='child']", doc, 2);
		// query using namespaces
		assertXpath("/x:server/ns:subsystem[@name='bar']/ns:child", doc, 2); // replaced
		assertXpath("/x:server/ns:subsystem/ns:child", doc,6); // subsystem without name attribute unchaned (4 already present + 2 added)
		assertXpath("/x:server/x:subsystem[@name='foo']/x:child", doc,1); // subsystem without ns unchanged
	}
	
	@Test
	public void testNSRootAppendUnderNS() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server/*[local-name()='subsystem' and namespace-uri()='foo' and @name='bar']", getResourceFile("content1Append.xml")));
		builder.build();		
		Document doc = dBuilder.parse(builder.getTargetFile());
		String xmlns = doc.getDocumentElement().getAttribute("xmlns");
		xpath.setNamespaceContext(new NamespaceContextImpl().mapping("x", xmlns).mapping("ns", "foo"));
		assertXpath("/x:server/ns:subsystem[@name='bar']/ns:subsystem[@name='foobar']", doc, 1);
		assertXpath("/x:server/ns:subsystem[@name='bar']/ns:subsystem[@name='foobar']/ns:child", doc, 2);
	}
	
	@Test(expected = XPathExpressionException.class)
	public void invalidSelect() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rotNS.xml"), getTempFile());
		builder.insert(new Insert("/serv hello?", getResourceFile("content1Append.xml")));
		builder.build();		
	}
	@Test(expected = IllegalArgumentException.class)
	public void contentDoesNotExist() throws Exception {
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rotNS.xml"), getTempFile());
		builder.insert(new Insert("/server", getResourceFile("foo.xml")));
		builder.build();		
	}
	
	//@Test()
	public void invalidSelectNoNodeset() throws Exception {
		// TODO expect exception
		XmlConfigBuilder builder = new XmlConfigBuilder(getResourceFile("rootNS.xml"), getTempFile());
		builder.insert(new Insert("/server/@attr", getResourceFile("content1.xml")));
		builder.build();		
	}
}

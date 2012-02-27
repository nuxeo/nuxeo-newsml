package org.nuxeo.newsml.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.newsml.utils.NewsMLCodec;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", user = "Administrator", cleanup = Granularity.CLASS)
public class TestNewsMLCode {

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected RuntimeHarness harness;

    protected NewsMLCodec codec;

    protected LSSerializer writer;

    @Before
    public void setupCodec() throws Exception {
        codec = new NewsMLCodec();

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        writer = impl.createLSSerializer();
    }

    @Test
    public void testNoteToNewsML() throws Exception {
        DocumentModel note = coreSession.createDocumentModel("Note");

        // note contains HTML wrapping boilerplate:
        note.setPropertyValue("note:note", "<html>"
                + "<head><title>Title Stuff</title></head>"
                + "<body><p>Content stuff</p></body>" + "</html>");
        Document newsMLDom = codec.getDefaultNewMLDomDocument();
        assertNotNull(newsMLDom);

        assertTrue(codec.bodyToXML(note, "note:note", newsMLDom));
        String updatedNewsML = writer.writeToString(newsMLDom);
        assertTrue(updatedNewsML.contains("<body.content><p>Content stuff</p>"
                + "</body.content>"));

        // let's try without the boilerplate:
        note.setPropertyValue("note:note", "<p>Content stuff</p>");
        newsMLDom = codec.getDefaultNewMLDomDocument();
        assertTrue(codec.bodyToXML(note, "note:note", newsMLDom));
        updatedNewsML = writer.writeToString(newsMLDom);
        assertTrue(updatedNewsML.contains("<body.content><p>Content stuff</p>"
                + "</body.content>"));

        // NewsML doc is untouched if the body is not valid
        note.setPropertyValue("note:note", "<p>Invalid content</div>");
        newsMLDom = codec.getDefaultNewMLDomDocument();
        assertFalse(codec.bodyToXML(note, "note:note", newsMLDom));
        updatedNewsML = writer.writeToString(newsMLDom);
        assertTrue(updatedNewsML.contains("<body.content/>"));
    }

    @Test
    public void testNewsMLToNote() throws Exception {
        DocumentModel note = coreSession.createDocumentModel("Note");
        Document newsMLDom = codec.getDefaultNewMLDomDocument();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpath.compile("//body/body.content");
        // XXX: assume that there is only one ContentItem in the NewsML document
        Node bodyContentNode = (Node) expr.evaluate(newsMLDom,
                XPathConstants.NODE);
        bodyContentNode.appendChild(newsMLDom.createTextNode("Some NewsML text"));

        // use the code to extract the body as the HTML payload of a note
        codec.bodyFromXML(note, "note:note", newsMLDom);
        assertTrue(note.getPropertyValue("note:note").toString().contains(
                "Some NewsML text"));
    }
}

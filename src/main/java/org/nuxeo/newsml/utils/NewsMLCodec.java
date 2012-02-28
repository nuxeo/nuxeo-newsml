package org.nuxeo.newsml.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * Parse or Serialize a NewsML XML to / from a host DocumentModel properties.
 */
public class NewsMLCodec {

    private static final String DEFAULT_NEWSML_MATRIX_XML = "/newsml/newsml-1.2-matrix.xml";

    protected DocumentBuilder builder;

    protected XPath xpath;

    protected LSSerializer writer;

    private String newsmlBlobProperty;

    protected String htmlBodyProperty;

    public NewsMLCodec() throws ParserConfigurationException,
            ClassCastException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        this(null, "file:content", "note:note");
    }

    public NewsMLCodec(Map<String, String> mapping, String newMLBlobProperty,
            String htmlBodyProperty) throws ParserConfigurationException,
            ClassCastException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();

        XPathFactory xFactory = XPathFactory.newInstance();
        xpath = xFactory.newXPath();

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        writer = impl.createLSSerializer();
        if (mapping == null) {
            // TODO build a default mapping here
        }
        this.newsmlBlobProperty = newMLBlobProperty;
        this.htmlBodyProperty = htmlBodyProperty;
    }

    public void propertiesFromXML(DocumentModel doc, InputStream xml)
            throws IOException, SAXException, XPathExpressionException,
            PropertyException, ClientException {
        if (xml == null) {
            xml = getDefaultNewMLStream();
        }
        Document domDoc = builder.parse(xml);

        // TODO: impement regular properies here

        bodyFromXML(doc, htmlBodyProperty, domDoc);
    }

    public String propertiesToXML(DocumentModel doc, InputStream xmlMatrixStream)
            throws IOException, SAXException, XPathExpressionException,
            PropertyException, ClientException {
        if (xmlMatrixStream == null) {
            xmlMatrixStream = getDefaultNewMLStream();
        }
        Document domDoc = builder.parse(xmlMatrixStream);

        // TODO: implement regular properties here

        bodyToXML(doc, htmlBodyProperty, domDoc);
        return writer.writeToString(domDoc);
    }

    public Document getDefaultNewMLDomDocument() throws IOException,
            SAXException {
        return builder.parse(getDefaultNewMLStream());
    }

    protected InputStream getDefaultNewMLStream() throws IOException {
        InputStream xmlMatrixStream;
        xmlMatrixStream = getClass().getResourceAsStream(
                DEFAULT_NEWSML_MATRIX_XML);
        if (xmlMatrixStream == null) {
            throw new IOException("Could not find " + DEFAULT_NEWSML_MATRIX_XML
                    + " in classpath");
        }
        return xmlMatrixStream;
    }

    public String propertiesToXML(DocumentModel doc)
            throws XPathExpressionException, IOException, SAXException,
            PropertyException, ClientException {
        return propertiesToXML(doc, getDefaultNewMLStream());
    }

    /**
     * @param bodyPath of a String or Blob property holding the HTML source to
     *            parse and insert as a body of the NewsML DOM document.
     * @return true is the body property of doc is valid XHTML, else false
     */
    public boolean bodyToXML(DocumentModel doc, String bodyPath, Document domDoc)
            throws XPathExpressionException, PropertyException,
            ClientException, IOException {
        // fetch the String representation from the document
        NodeList newBodyNodeList = domDoc.createElement("tmp").getChildNodes();
        if (doc.getPropertyValue(bodyPath) != null) {
            InputStream bodyStream;
            Property property = doc.getProperty(bodyPath);
            if ("content".equals(property.getType().getName())) {
                Blob blob = property.getValue(Blob.class);
                bodyStream = blob.getStream();
            } else {
                bodyStream = new ByteArrayInputStream(doc.getPropertyValue(
                        bodyPath).toString().getBytes("utf-8"));
            }
            // parse the string body as a DOM node
            try {
                Document bodyDoc = builder.parse(bodyStream);
                NodeList bodyNodes = bodyDoc.getChildNodes();
                newBodyNodeList = cleanHtmlWrapperElements(bodyNodes, bodyDoc);
            } catch (SAXException e) {
                return false;
            }
        }
        XPathExpression expr = xpath.compile("//body");
        // XXX: assume that there is only one ContentItem in the NewsML document
        Node bodyNode = (Node) expr.evaluate(domDoc, XPathConstants.NODE);

        // remove any previous "body.content" tag
        NodeList bodyNodeChildren = bodyNode.getChildNodes();
        for (int i = 0; i < bodyNodeChildren.getLength(); i++) {
            Node child = bodyNodeChildren.item(i);
            if ("body.content".equals(child.getLocalName())) {
                bodyNode.removeChild(child);
                break;
            }
        }
        // insert the new "body.content" tag with the parsed nodes as children
        Element bodyContent = domDoc.createElement("body.content");
        for (int i = 0; i < newBodyNodeList.getLength(); i++) {
            Node importedNode = domDoc.importNode(newBodyNodeList.item(i), true);
            bodyContent.appendChild(importedNode);
        }
        bodyNode.appendChild(bodyContent);
        return true;
    }

    protected NodeList cleanHtmlWrapperElements(NodeList bodyNodes, Document doc) {
        if (bodyNodes.getLength() == 0) {
            return bodyNodes;
        }
        if ("html".equals(bodyNodes.item(0).getNodeName())) {
            bodyNodes = bodyNodes.item(0).getChildNodes();
        }
        if (bodyNodes.getLength() > 0
                && "head".equals(bodyNodes.item(0).getNodeName())) {
            // hack to filter a NodeList...
            Element container = doc.createElement("container");
            for (int i = 1; i < bodyNodes.getLength(); i++) {
                container.appendChild(bodyNodes.item(i));
            }
            bodyNodes = container.getChildNodes();
        }
        if (bodyNodes.getLength() > 0
                && "body".equals(bodyNodes.item(0).getNodeName())) {
            bodyNodes = bodyNodes.item(0).getChildNodes();
        }
        return bodyNodes;
    }

    public void bodyFromXML(DocumentModel doc, String bodyPath, Document domDoc)
            throws XPathExpressionException, PropertyException, ClientException {
        XPathExpression expr = xpath.compile("//body/body.content");
        // XXX: assume that there is only one ContentItem in the NewsML document
        Node bodyContentNode = (Node) expr.evaluate(domDoc, XPathConstants.NODE);
        Document htmlDoc = builder.newDocument();
        Element htmlNode = htmlDoc.createElement("html");
        htmlDoc.appendChild(htmlNode);
        Element htmlBodyNode = htmlDoc.createElement("body");
        htmlNode.appendChild(htmlBodyNode);

        if (bodyContentNode != null) {
            NodeList children = bodyContentNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node importedNode = htmlDoc.importNode(children.item(i), true);
                htmlBodyNode.appendChild(importedNode);
            }
        }
        String htmlString = writer.writeToString(htmlNode);

        // check whether this payload should be wrapped into a blob or not
        Property property = doc.getProperty(bodyPath);
        if ("content".equals(property.getType().getName())) {
            Blob blob = StreamingBlob.createFromString(htmlString);
            blob.setMimeType("text/html");
            doc.setPropertyValue(bodyPath, (Serializable) blob);
        } else {
            doc.setPropertyValue(bodyPath, htmlString);
        }
    }

    public void synchronizeProperties(DocumentModel doc)
            throws PropertyException, ClientException, IOException,
            XPathExpressionException, SAXException {
        if (!doc.hasFacet("HasNewsMLWriteBack")) {
            return;
        }
        Blob content = (Blob) doc.getPropertyValue(newsmlBlobProperty);
        if (content != null && content.getMimeType().equals("text/xml")
                && doc.getProperty(newsmlBlobProperty).isDirty()) {
            // update the properties by parsing the blob content if not null
            propertiesFromXML(doc, content.getStream());
        } else if (hasDirtyNewsMLProperties(doc)) {
            if (content != null) {
                String newsMLContent = propertiesToXML(doc, content.getStream());
                Blob newsMLBlob = StreamingBlob.createFromString(newsMLContent);
                newsMLBlob.setFilename(content.getFilename());
                newsMLBlob.setMimeType("application/xml");
                doc.setPropertyValue(newsmlBlobProperty,
                        (Serializable) newsMLBlob);
            } else {
                String newsMLContent = propertiesToXML(doc);
                Blob newsMLBlob = StreamingBlob.createFromString(newsMLContent);
                newsMLBlob.setFilename("newsml-" + doc.getName() + ".xml");
                newsMLBlob.setMimeType("application/xml");
                doc.setPropertyValue(newsmlBlobProperty,
                        (Serializable) newsMLBlob);
            }
        }
    }

    /**
     * Checked whether at least on document property mapped to NewsML has been
     * modified.
     */
    public boolean hasDirtyNewsMLProperties(DocumentModel doc) {
        // TODO implement me
        return true;
    }

}

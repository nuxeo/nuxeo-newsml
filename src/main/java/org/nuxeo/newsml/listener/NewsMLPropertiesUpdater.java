/**
 * 
 */

package org.nuxeo.newsml.listener;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.newsml.utils.NewsMLCodec;

/**
 * Ensure that properties and attached NewML file properties are in sync.
 * 
 * @author ogrisel
 * 
 */
public class NewsMLPropertiesUpdater implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext context;
        if (event.getContext() instanceof DocumentEventContext) {
            context = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = context.getSourceDocument();
        Blob content = (Blob) doc.getPropertyValue("file:content");
        try {
            NewsMLCodec codec = new NewsMLCodec();
            if (content != null && content.getMimeType().equals("text/xml")
                    && doc.getProperty("file:content").isDirty()) {
                // update the properties by parsing the blob content if not null
                codec.propertiesFromXML(doc, content.getStream());
            }
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

}

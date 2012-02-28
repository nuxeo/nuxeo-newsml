/**
 * 
 */

package org.nuxeo.newsml.listener;

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
        try {
            NewsMLCodec codec = new NewsMLCodec();
            codec.synchronizeProperties(doc);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

}

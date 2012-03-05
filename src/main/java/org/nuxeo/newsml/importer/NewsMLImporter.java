package org.nuxeo.newsml.importer;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

public class NewsMLImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;
    
    private static final Log log = LogFactory.getLog(NewsMLImporter.class);

    @Override
    public DocumentModel create(CoreSession documentManager, Blob input,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {
        path = getNearestContainerPath(documentManager, path);
        String typeName = "NewsML";

        doSecurityCheck(documentManager, path, typeName, typeService);

        String filename = FileManagerUtils.fetchFileName(fullname);
        input.setFilename(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);

        // Determining if we need to create or update an existing one
        if (overwrite && docModel != null) {

            // save changes the user might have made to the current version
            documentManager.saveDocument(docModel);
            documentManager.save();

            docModel.setProperty("file", "content", input);
            docModel = overwriteAndIncrementversion(documentManager, docModel);

        } else {
            // new
            String title = FileManagerUtils.fetchTitle(filename);

            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            docModel = documentManager.createDocumentModel(typeName);

            // Updating known attributes (title, filename, content)
            docModel.setProperty("dublincore", "title", title);
            docModel.setProperty("file", "filename", filename);
            docModel.setProperty("file", "content", input);

            // writing the new document to the repository
            docModel.setPathInfo(path, pss.generatePathSegment(docModel));
            docModel = documentManager.createDocument(docModel);
        }

        documentManager.save();

        log.debug("imported the document: " + docModel.getName()
                + " with icon: " + docModel.getProperty("common", "icon")
                + " and type: " + typeName);
        return docModel;
    }

}

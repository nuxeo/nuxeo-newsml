# NewsML

This addon makes it possible to edit the content of a NewsML file using
the same wysiwyg widget as for the Node document type.

The Nuxeo document and NewsML file are kept in sync whenever one of the
two is updated.


## Status

This addon is **experimental** and not ready for production: only the
headline and body are kept in sync. All other metadata (e.g. modification
dates, source...) are not editable through the Nuxeo UI.


## Building the addon

Using maven 2.2.1 or later, from root of the `nuxeo-newsml` folder:

    $ mvn install

Then copy the jar `target/nuxeo-newsml-*-SNAPSHOT.jar` into the
`nxserver/bundles` folder of your Nuxeo DM or DAM instance (assuming
the default tomcat package).

Put the
[htmlcleaner-2.2.jar](http://repo1.maven.org/maven2/net/sourceforge/htmlcleaner/htmlcleaner/2.2/htmlcleaner-2.2.jar)
jar into the `nxserver/lib` folder of Tomcat.

Restart Tomcat.


## Using the addon

To test the addon, login and go to a workspace such as your personal
workspace and create a new document with type "NewsML Article". You can
attach a NewsML file or type the body of the article. The two fields
will keep in sync during later edits.

The addon also contributes a custom importer for the FileManagerService
so that any XML imported through drag and drop will be automatically
interpreted as a NewsML document with sync mode enabled.


## About Nuxeo

Nuxeo provides a modular, extensible Java-based
[open source software platform for enterprise content
management](http://www.nuxeo.com/en/products/ep)
and packaged applications for [document
management](http://www.nuxeo.com/en/products/document-management),
[digital asset management](http://www.nuxeo.com/en/products/dam) and [case
management](http://www.nuxeo.com/en/products/case-management). Designed
by developers for developers, the Nuxeo platform offers a modern
architecture, a powerful plug-in model and extensive packaging
capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

#!/bin/bash
echo "Creating dnnt cores ..."
/opt/docker-solr/scripts/precreate-core  autority /autority
/opt/docker-solr/scripts/precreate-core  catalog /catalog
/opt/docker-solr/scripts/precreate-core  history /history
/opt/docker-solr/scripts/precreate-core  imports /imports
/opt/docker-solr/scripts/precreate-core  imports_documents /imports_documents
/opt/docker-solr/scripts/precreate-core  notifications /notifications
/opt/docker-solr/scripts/precreate-core  shibusers /shibusers
/opt/docker-solr/scripts/precreate-core  users /users
/opt/docker-solr/scripts/precreate-core  zadost /zadost


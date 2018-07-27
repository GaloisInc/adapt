#!/usr/bin/env bash

wget http://downloads.dbpedia.org/2015-10/core/article_categories_en.ttl.bz2
wget http://downloads.dbpedia.org/2015-10/core/instance_types_en.ttl.bz2
wget http://downloads.dbpedia.org/2015-10/core/instance_types_transitive_en.ttl.bz2
wget http://downloads.dbpedia.org/2015-10/core/mappingbased_objects_en.ttl.bz2
wget http://downloads.dbpedia.org/2015-10/core/skos_categories_en.ttl.bz2

bzip2 -d article_categories_en.ttl.bz2
bzip2 -d instance_types_en.ttl.bz2
bzip2 -d instance_types_transitive_en.ttl.bz2
bzip2 -d mappingbased_objects_en.ttl.bz2
bzip2 -d skos_categories_en.ttl.bz2

#!/usr/bin/bash

get_jar ()
{
   rm ./$PROJECT-pg-bin.jar 2>/dev/null
   cp ../../../$PROJECT/dist/$PROJECT-pg-bin.jar .
}

#############################################################################

PROJECT=assrulex
get_jar
#=====
PROJECT=coron
get_jar
#=====
#PROJECT=guis
#get_jar
#=====
PROJECT=leco
get_jar
#=====
PROJECT=postproc
get_jar
#=====
PROJECT=preproc
get_jar
#=====
PROJECT=tools
get_jar
#=====


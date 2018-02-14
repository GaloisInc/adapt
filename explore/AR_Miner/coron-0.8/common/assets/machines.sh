# This file is sourced into every launch script.
# Put your machine info in the list in alphabetical order.

if [ $AUTO_MEM = "1" ]; then
   case "$HOSTNAME" in
      "BARRACUDA"            )   JAVA_MAX_HEAP_SIZE=16384m;;
      "coutures"             )   JAVA_MAX_HEAP_SIZE=16384m;;
      "debrecen"             )   JAVA_MAX_HEAP_SIZE=16384m;;
      "hagrid.loria.fr"      )   JAVA_MAX_HEAP_SIZE=16384m;;
      "hoth"                 )   JAVA_MAX_HEAP_SIZE=16384m;;
      "kraken"	    	        )   JAVA_MAX_HEAP_SIZE=16384m;;
      "servbioinfo.loria.fr" )   JAVA_MAX_HEAP_SIZE=16384m;;
      *                      )   JAVA_MAX_HEAP_SIZE=16384m;;
   esac
else
   JAVA_MAX_HEAP_SIZE=16384m
fi

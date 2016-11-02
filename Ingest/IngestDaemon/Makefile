all:
	java -jar /opt/avro/avro-tools-1.8.1.jar compile schema ../Ingest/data/TCCDMDatum13.avsc .
	javac -cp /opt/titan/lib/*:/opt/avro/*:. Ingest.java

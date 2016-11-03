package com.galois.adapt;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

import java.util.ArrayList;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.bbn.tc.schema.avro.*;
import java.io.File;
import java.io.IOException;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import com.google.common.collect.Lists;
import java.util.List;

public class Ingest {
    public static void main(String[] args) throws IOException {
        System.out.println(readAvroFile("src/test/resources/test.avro"));

        System.exit(0);
    }

    private static List<TCCDMDatum> readAvroFile(String file) throws IOException {
        List<TCCDMDatum> results = Lists.newArrayList();
        File f = new File(file);
        DatumReader<TCCDMDatum> tcDatumReader = new SpecificDatumReader<TCCDMDatum>(TCCDMDatum.class);
        DataFileReader<TCCDMDatum> tcFileReader = new DataFileReader<TCCDMDatum>(f, tcDatumReader);
        TCCDMDatum tcd = null;
        while(tcFileReader.hasNext()) {
            tcd = tcFileReader.next();
            results.add(tcd);
        }

        return results;
    }

    private static void insertIntoTitan(List<TCCDMDatum> data) {
        TitanGraph graph = TitanFactory.open("/opt/titan/conf/gremlin-server/titan-cassandra-server.properties");

        try {
            AbstractObject o = (AbstractObject) data;
        }
        catch (Exception e)
        {
        }
        try {
            FileObject o = (FileObject) data;
        }
        catch (Exception e)
        {
        }
        try {
            MemoryObject o = (MemoryObject) data;
        }
        catch (Exception e)
        {
        }
        try {
            NetFlowObject o = (NetFlowObject) data;
        }
        catch (Exception e)
        {
        }
        try {
            Principal o = (Principal) data;
        }
        catch (Exception e)
        {
        }
        try {
            SrcSinkObject o = (SrcSinkObject) data;
        }
        catch (Exception e)
        {
        }
        try {
            Subject o = (Subject) data;
        }
        catch (Exception e)
        {
        }

        // see org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory.generateClassic()
        //f1.addEdge("EDGE_EVENT_AFFECTS_FILE out", f2);

/*        GraphTraversalSource g = graph.traversal();
        Vertex fromNode = g.V().has("ident", 1).next();
        Vertex toNode = g.V().has("ident", 4).next();
        ArrayList list = new ArrayList();
        g.V(fromNode).repeat(both().simplePath()).until(is(toNode)).limit(1).path().fill(list);
        System.out.println("\n\n\nFOUND PATH:");
        System.out.println(list);
        System.out.println("\n\n\n");*/
    }

    private static void insertEntity(TitanGraph g, TCCDMDatum d) {
        final Vertex v = g.addVertex("Entity");
        Object o = d.getDatum();
        v.property("ident", 1);
    }

    /*
       AbstractObject -> Entity
       ConfidentialityTag ->
       EdgeType ->
       Event ->
       EventType ->
       FileObject -> Entity-File
       InstrumentationSource ->
       IntegrityTag ->
       MemoryObject -> Entity-Memory
       NetFlowObject -> Entity-NetFlow
       Principal -> Agent
       PrincipalType ->
       ProvenanceTagNode ->
       RegistryKeyObject ->
       SHORT ->
       SimpleEdge ->
       SrcSinkObject -> Resource
       SrcSinkType ->
       Subject -> Subject
       SubjectType ->
       TagEntity ->
       TagOpCode ->
       TCCDMDatum ->
       UUID ->
       ValueDataType ->
       Value ->
       ValueType ->

Unmapped: Host
    */
}

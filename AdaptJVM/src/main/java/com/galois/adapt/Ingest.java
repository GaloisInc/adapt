package com.galois.adapt;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

import java.util.ArrayList;

import com.thinkaurelius.titan.diskstorage.configuration.SystemConfiguration;
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
import java.lang.RuntimeException;


public class Ingest {
    public static void main(String[] args) throws IOException {
        TitanGraph graph = TitanFactory.build().set("storage.backend", "inmemory").open();

        long startTime = System.currentTimeMillis();

        List<TCCDMDatum> cdmData = readAvroFile("src/test/resources/Avro_datasets_with_units_CDM13/infoleak_small_units.avro");
        long stopTime = System.currentTimeMillis();
        insertIntoTitan(cdmData, graph);
        System.out.println(startTime);
        System.out.println("Time: " + ((stopTime-startTime)) + "\nStatements: " + cdmData.size());

        System.exit(0);
    }

    private static class CDMData {
        public List<FileObject> fos;
        public List<MemoryObject> mos;
        public List<NetFlowObject> nfos;
        public List<AbstractObject> aos;
        public List<Principal> ps;
        public List<SrcSinkObject> ssos;
        public List<Subject> ss;
        public List<SimpleEdge> edges;

        public CDMData() {
            fos = Lists.newArrayList();
            mos = Lists.newArrayList();
            nfos = Lists.newArrayList();
            aos = Lists.newArrayList();
            ps = Lists.newArrayList();
            ssos = Lists.newArrayList();
            ss = Lists.newArrayList();
            edges = Lists.newArrayList();
        }
    }

    static List<TCCDMDatum> readAvroFile(String filePath) throws IOException {
        //private static CDMData readAvroFile(String filePath) throws IOException {
//        CDMData cdata = new CDMData();
        List<TCCDMDatum> results = Lists.newArrayList();
        File f = new File(filePath);
        DatumReader<TCCDMDatum> tcDatumReader = new SpecificDatumReader<TCCDMDatum>(TCCDMDatum.class);
        DataFileReader<TCCDMDatum> tcFileReader = new DataFileReader<TCCDMDatum>(f, tcDatumReader);
        TCCDMDatum tcd = null;

        int counter = 0;
        while(tcFileReader.hasNext()) {
            tcd = tcFileReader.next();
            results.add(tcd);
//            tcFileReader.next();
            counter++;
//            System.out.println(counter);
        }
//        System.out.println("Count in file: " + counter);
        /*

        DatumReader<FileObject> foReader = new SpecificDatumReader<FileObject>(FileObject.class);
        DataFileReader<FileObject> foFileReader = new DataFileReader<FileObject>(f, foReader);
        FileObject fo = null;
        while(foFileReader.hasNext()) {
            fo = foFileReader.next();
            cdata.fos.add(fo);
        }

        DatumReader<MemoryObject> moReader = new SpecificDatumReader<MemoryObject>(MemoryObject.class);
        DataFileReader<MemoryObject> moFileReader = new DataFileReader<MemoryObject>(f, moReader);
        MemoryObject mo = null;
        while(moFileReader.hasNext()) {
            mo = moFileReader.next();
            cdata.mos.add(mo);
        }

        DatumReader<NetFlowObject> nfoReader = new SpecificDatumReader<NetFlowObject>(NetFlowObject.class);
        DataFileReader<NetFlowObject> nfoFileReader = new DataFileReader<NetFlowObject>(f, nfoReader);
        NetFlowObject nfo = null;
        while(nfoFileReader.hasNext()) {
            nfo = nfoFileReader.next();
            cdata.nfos.add(nfo);
        }

        DatumReader<AbstractObject> aoReader = new SpecificDatumReader<AbstractObject>(AbstractObject.class);
        DataFileReader<AbstractObject> aoFileReader = new DataFileReader<AbstractObject>(f, aoReader);
        AbstractObject ao = null;
        while(aoFileReader.hasNext()) {
            ao = aoFileReader.next();
            cdata.aos.add(ao);
        }

        DatumReader<Principal> pReader = new SpecificDatumReader<Principal>(Principal.class);
        DataFileReader<Principal> pFileReader = new DataFileReader<Principal>(f, pReader);
        Principal p = null;
        while(pFileReader.hasNext()) {
            p = pFileReader.next();
            cdata.ps.add(p);
        }

        DatumReader<SrcSinkObject> ssoReader = new SpecificDatumReader<SrcSinkObject>(SrcSinkObject.class);
        DataFileReader<SrcSinkObject> ssoFileReader = new DataFileReader<>(f, ssoReader);
        SrcSinkObject sso = null;
        while(ssoFileReader.hasNext()) {
            sso = ssoFileReader.next();
            cdata.ssos.add(sso);
        }

        DatumReader<Subject> sReader = new SpecificDatumReader<Subject>(Subject.class);
        DataFileReader<Subject> sFileReader = new DataFileReader<Subject>(f, sReader);
        Subject s = null;
        while(sFileReader.hasNext()) {
            s = sFileReader.next();
            cdata.ss.add(s);
        }

        DatumReader<SimpleEdge> seReader = new SpecificDatumReader<SimpleEdge>(SimpleEdge.class);
        DataFileReader<SimpleEdge> seFileReader = new DataFileReader<SimpleEdge>(f, seReader);
        SimpleEdge se = null;
        while(seFileReader.hasNext()) {
            se = seFileReader.next();
            cdata.edges.add(se);
        }

        return cdata;*/

        return results;
    }

    private static void insertIntoTitan(List<TCCDMDatum> dataList, TitanGraph graph) {
//        TitanGraph graph = TitanFactory.open("/opt/titan/conf/gremlin-server/titan-cassandra-server.properties");
//
//
//        // TOOD: Actually iterate over list

        for(TCCDMDatum data : dataList) {

            String dataString = data.getDatum().toString();
            long startTime = System.currentTimeMillis();

            Vertex v = graph.addVertex("Entity");
            v.property("properties", dataString);
            long stopTime = System.currentTimeMillis();
//            System.out.println("time: " + (stopTime - startTime));// + "  for:\n" + dataString);
        }

        System.out.println(graph.traversal().V().count().toList());
        /*if(data instanceof AbstractObject)
        {
            if(data instanceof FileObject)
            {
                Vertex v = graph.addVertex("Entity-File");
                insertEntityFileProperties(v, (FileObject) data);
            } else if(data instanceof MemoryObject)
            {
                Vertex v = graph.addVertex("Entity-Memory");
                insertEntityMemoryProperties(v, (MemoryObject) data);
            } else if(data instanceof NetFlowObject)
            {
                Vertex v = graph.addVertex("Entity-NetFlow");
                insertEntityNetFlowProperties(v, (NetFlowObject) data);
            } else
            {
                Vertex v = graph.addVertex("Entity");
                insertEntityProperties(v, (AbstractObject) data);
            }
        } else if(data instanceof Principal)
        {
            Vertex v = graph.addVertex("Agent");
            insertAgentProperties(v, (Principal) data);
        } else if(data instanceof SrcSinkObject)
        {
            Vertex v = graph.addVertex("Resource");
            insertResourceProperties(v, (SrcSinkObject) data);
        } else if(data instanceof Subject)
        {
            Vertex v = graph.addVertex("Subject");
            insertSubjectProperties(v, (Subject) data);
        } else if(data instanceof SimpleEdge)
        {
            insertEdge(graph, (SimpleEdge) data);
        } else
        {
            throw new RuntimeException("Unknown CDM Type: " + data);
        }*/
    }

    private static void insertEdge(TitanGraph tg, SimpleEdge e) {
        GraphTraversalSource g = tg.traversal();
        Vertex from = g.V().has("ident", e.getFromUuid()).next();
        Vertex to = g.V().has("ident", e.getToUuid()).next();
        String type = "";
        switch(e.getType()) {
            case EDGE_EVENT_AFFECTS_MEMORY:
                type = "EDGE_EVENT_AFFECTS_MEMORY";
                break;
            case EDGE_EVENT_AFFECTS_FILE:
                type = "EDGE_EVENT_AFFECTS_FILE";
                break;
            case EDGE_EVENT_AFFECTS_NETFLOW:
                type = "EDGE_EVENT_AFFECTS_NETFLOW";
                break;
            case EDGE_EVENT_AFFECTS_SUBJECT:
                type = "EDGE_EVENT_AFFECTS_SUBJECT";
                break;
            case EDGE_EVENT_AFFECTS_SRCSINK:
                type = "EDGE_EVENT_AFFECTS_SRCSINK";
                break;
            case EDGE_EVENT_HASPARENT_EVENT:
                type = "EDGE_EVENT_HASPARENT_EVENT";
                break;
            case EDGE_EVENT_CAUSES_EVENT:
                type = "EDGE_EVENT_CAUSES_EVENT";
                break;
            case EDGE_EVENT_ISGENERATEDBY_SUBJECT:
                type = "EDGE_EVENT_ISGENERATEDBY_SUBJECT";
                break;
            case EDGE_SUBJECT_AFFECTS_EVENT:
                type = "EDGE_SUBJECT_AFFECTS_EVENT";
                break;
            case EDGE_SUBJECT_HASPARENT_SUBJECT:
                type = "EDGE_SUBJECT_HASPARENT_SUBJECT";
                break;
            case EDGE_SUBJECT_HASLOCALPRINCIPAL:
                type = "EDGE_SUBJECT_HASLOCALPRINCIPAL";
                break;
            case EDGE_SUBJECT_RUNSON:
                type = "EDGE_SUBJECT_RUNSON";
                break;
            case EDGE_FILE_AFFECTS_EVENT:
                type = "EDGE_FILE_AFFECTS_EVENT";
                break;
            case EDGE_NETFLOW_AFFECTS_EVENT:
                type = "EDGE_NETFLOW_AFFECTS_EVENT";
                break;
            case EDGE_MEMORY_AFFECTS_EVENT:
                type = "EDGE_MEMORY_AFFECTS_EVENT";
                break;
            case EDGE_SRCSINK_AFFECTS_EVENT:
                type = "EDGE_SRCSINK_AFFECTS_EVENT";
                break;
            case EDGE_OBJECT_PREV_VERSION:
                type = "EDGE_OBJECT_PREV_VERSION";
                break;
            case EDGE_FILE_HAS_TAG:
                type = "EDGE_FILE_HAS_TAG";
                break;
            case EDGE_NETFLOW_HAS_TAG:
                type = "EDGE_NETFLOW_HAS_TAG";
                break;
            case EDGE_MEMORY_HAS_TAG:
                type = "EDGE_MEMORY_HAS_TAG";
                break;
            case EDGE_SRCSINK_HAS_TAG:
                type = "EDGE_SRCSINK_HAS_TAG";
                break;
            case EDGE_SUBJECT_HAS_TAG:
                type = "EDGE_SUBJECT_HAS_TAG";
                break;
            case EDGE_EVENT_HAS_TAG:
                type = "EDGE_EVENT_HAS_TAG";
                break;
            case EDGE_EVENT_AFFECTS_REGISTRYKEY:
                type = "EDGE_EVENT_AFFECTS_REGISTRYKEY";
                break;
            case EDGE_REGISTRYKEY_AFFECTS_EVENT:
                type = "EDGE_REGISTRYKEY_AFFECTS_EVENT";
                break;
            case EDGE_REGISTRYKEY_HAS_TAG:
                type = "EDGE_REGISTRYKEY_HAS_TAG";
                break;
            default:
                throw new RuntimeException("Unknown Edge Event Type");
        }
        Vertex mid = tg.addVertex(type);
        mid.property("time", e.getTimestamp());
        mid.property("properties", e.getProperties());
        from.addEdge(type+" out", mid);
        mid.addEdge(type+" in", to);
    }

    private static void insertEntityProperties(Vertex v, AbstractObject d) {
        v.property("source", d.getSource());
        v.property("permissions", d.getPermission());
        v.property("time", d.getLastTimestampMicros());
        v.property("properties", d.getProperties());
    }

    private static void insertEntityFileProperties(Vertex v, FileObject d) {
        insertEntityProperties(v, d.getBaseObject());
        v.property("ident", d.getUuid());
        // TODO: Are we storing isPipe?
        v.property("url", d.getUrl());
        v.property("file-version", d.getVersion());
        v.property("size", d.getSize());
    }

    private static void insertEntityMemoryProperties(Vertex v, MemoryObject d) {
        insertEntityProperties(v, d.getBaseObject());
        v.property("ident", d.getUuid());
        v.property("address", d.getMemoryAddress());
        v.property("pageNumber", d.getPageNumber());
    }

    private static void insertEntityNetFlowProperties(Vertex v, NetFlowObject d) {
        insertEntityProperties(v, d.getBaseObject());
        v.property("ident", d.getUuid());
        v.property("srcAddress", d.getSrcAddress());
        v.property("srcPort", d.getSrcPort());
        v.property("dstAddress", d.getDestAddress());
        v.property("dstPort", d.getDestPort());
        v.property("ipProtocol", d.getIpProtocol());
    }

    private static void insertAgentProperties(Vertex v, Principal d) {
        v.property("ident", d.getUuid());
        v.property("agentType", d.getType());
        v.property("userID", d.getUserId());
        v.property("gid", d.getGroupIds());
        v.property("source", d.getSource());
        v.property("properties", d.getProperties());
    }

    private static void insertResourceProperties(Vertex v, SrcSinkObject d) {
        insertEntityProperties(v, d.getBaseObject());
        v.property("ident", d.getUuid());
        v.property("srcSinkType", d.getType());
    }

    private static void insertSubjectProperties(Vertex v, Subject d) {
        v.property("ident", d.getUuid());
        v.property("subjectType", d.getType());
        v.property("pid", d.getPid());
        v.property("ppid", d.getPpid());
        v.property("source", d.getSource());
        v.property("startedAtTime", d.getStartTimestampMicros());
        v.property("commandLine", d.getCmdLine());
        v.property("importLibs", d.getImportedLibraries());
        v.property("exportLibs", d.getExportedLibraries());
        v.property("pInfo", d.getPInfo());
        v.property("properties", d.getProperties());
    }

    /*
       ConfidentialityTag ->
       EdgeType ->
       Event ->
       EventType ->
       InstrumentationSource ->
       IntegrityTag ->
       PrincipalType ->
       ProvenanceTagNode ->
       RegistryKeyObject ->
       SHORT ->
       SrcSinkType ->
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

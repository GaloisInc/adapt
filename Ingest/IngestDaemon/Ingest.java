import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

import java.util.ArrayList;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class Ingest {
    public static void main(String[] args) {
        TitanGraph graph = TitanFactory.open("/opt/titan/conf/gremlin-server/titan-cassandra-server.properties");

        // see org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory.generateClassic()
        final Vertex f1 = graph.addVertex("Entity-File");
        f1.property("ident", 1);
        final Vertex f2 = graph.addVertex("Entity-File");
        f2.property("ident", 2);
        final Vertex f3 = graph.addVertex("Entity-File");
        f3.property("ident", 3);
        final Vertex f4 = graph.addVertex("Entity-File");
        f4.property("ident", 4);
        f1.addEdge("EDGE_EVENT_AFFECTS_FILE out", f2);
        f2.addEdge("EDGE_EVENT_AFFECTS_FILE out", f3);
        f3.addEdge("EDGE_EVENT_AFFECTS_FILE out", f4);

        GraphTraversalSource g = graph.traversal();
        Vertex fromNode = g.V().has("ident", 1).next();
        Vertex toNode = g.V().has("ident", 4).next();
        ArrayList list = new ArrayList();
        g.V(fromNode).repeat(both().simplePath()).until(is(toNode)).limit(1).path().fill(list);
        System.out.println("\n\n\nFOUND PATH:");
        System.out.println(list);
        System.out.println("\n\n\n");
        System.exit(0);
    }
}

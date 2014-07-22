package org.umlg.sqlgraph.test;

import com.tinkerpop.gremlin.AbstractGremlinSuite;
import com.tinkerpop.gremlin.LoadGraphWith;
import com.tinkerpop.gremlin.process.Path;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.io.GraphReader;
import com.tinkerpop.gremlin.structure.io.graphml.GraphMLReader;
import com.tinkerpop.gremlin.util.function.FunctionUtils;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.umlg.sqlgraph.structure.SqlGraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.tinkerpop.gremlin.LoadGraphWith.GraphData.CLASSIC;
import static com.tinkerpop.gremlin.structure.Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES;
import static org.junit.Assert.*;

/**
 * Date: 2014/07/13
 * Time: 6:32 PM
 */
public class TinkerpopTest extends BaseTest {

    @Test
    @FeatureRequirement(featureClass = Graph.Features.GraphFeatures.class, feature = Graph.Features.GraphFeatures.FEATURE_TRANSACTIONS)
    public void shouldSupportTransactionOneAndDone() {
        final Graph graph = this.sqlGraph;

        // first fail the tx
        try {
            this.sqlGraph.tx().submit(FunctionUtils.wrapFunction(grx -> {
                grx.addVertex();
                throw new Exception("fail");
            })).oneAndDone();
        } catch (Exception ex) {
            assertEquals("fail", ex.getCause().getCause().getMessage());
        }

        AbstractGremlinSuite.assertVertexEdgeCounts(0, 0);

        // this tx will work
        this.sqlGraph.tx().submit(grx -> graph.addVertex()).oneAndDone();
        AbstractGremlinSuite.assertVertexEdgeCounts(1, 0);

        // make sure a commit happened and a new tx started
        this.sqlGraph.tx().rollback();
        AbstractGremlinSuite.assertVertexEdgeCounts(1, 0);
    }

    public Traversal<Vertex, String> get_g_E_subgraphXcreatedX(final Graph subgraph) {
        return this.sqlGraph.V().inE().subgraph(subgraph, e -> e.label().equals("created")).value("name");
    }

    public Traversal<Vertex, Path> get_g_V_asXxX_both_simplePath_jumpXx_loops_lt_3X_path() {
        return this.sqlGraph.V().as("x").both().simplePath().jump("x", t -> t.getLoops() < 3).path();
    }

//    @Test
//    @LoadGraphWith(CLASSIC)
//    public void g_e7_hasXlabelXknowsX() throws IOException {
//        readGraphMLIntoGraph(this.sqlGraph);
//        //System.out.println(convertToEdgeId("marko", "knows", "vadas"));
//        Iterator<Edge> traversal = get_g_e7_hasXlabelXknowsX(convertToEdgeId("marko", "knows", "vadas"));
//        System.out.println("Testing: " + traversal);
//        int counter = 0;
//        while (traversal.hasNext()) {
//            counter++;
//            assertEquals("knows", traversal.next().label());
//        }
//        assertEquals(1, counter);
//    }

    public Traversal<Edge, Edge> get_g_e7_hasXlabelXknowsX(final Object e7Id) {
        return this.sqlGraph.e(e7Id).has("label", "knows");
    }

    protected Object convertToEdgeId(final String identifier1, String edgeLabel, final String identifier2) {
        return convertToEdgeId(this.sqlGraph, identifier1, edgeLabel, identifier2);
    }

    protected Object convertToEdgeId(final Graph g, final String identifier1, String edgeLabel, final String identifier2) {
        return ((Edge) g.V().has("name", identifier1).outE(edgeLabel).as("e").inV().has("name", identifier2).back("e").next()).id();
    }


//        @Test
    public void g_v1_outXcreatedX_inXcreatedX_rangeX1_2X() throws IOException {
        readGraphMLIntoGraph(this.sqlGraph);

        Object id = convertToVertexId("josh");
//        Traversal<Vertex, Vertex> t = this.sqlGraph.v(id).out("created").in("created");
        Traversal<Vertex, Vertex> t = this.sqlGraph.v(id).both("knows");
        while (t.hasNext()) {
            System.out.println(t.next().property("name").value());
        }

//        final Iterator<Vertex> traversal = get_g_v1_outXcreatedX_inXcreatedX_rangeX1_2X(convertToVertexId("marko"));
//        System.out.println("Testing: " + traversal);
//        int counter = 0;
//        while (traversal.hasNext()) {
//            counter++;
//            final String name = traversal.next().value("name");
//            assertTrue(name.equals("marko") || name.equals("josh") || name.equals("peter"));
//        }
//        assertEquals(2, counter);
    }

    public Traversal<Vertex, Vertex> get_g_v1_outXcreatedX_inXcreatedX_rangeX1_2X(final Object v1Id) {
        return this.sqlGraph.v(v1Id).out("created").in("created").range(1, 2);
    }

    //    @Test
    public void g_v1_outXcreatedX_inXcreatedX_cyclicPath_path() throws IOException {
        readGraphMLIntoGraph(this.sqlGraph);
        final Traversal<Vertex, Path> traversal = get_g_v1_outXcreatedX_inXcreatedX_cyclicPath_path(convertToVertexId("marko"));
        System.out.println("Testing: " + traversal);
        int counter = 0;
        while (traversal.hasNext()) {
            counter++;
            Path path = traversal.next();
            assertFalse(path.isSimple());
        }
        assertEquals(1, counter);
        assertFalse(traversal.hasNext());
    }

    public Traversal<Vertex, Path> get_g_v1_outXcreatedX_inXcreatedX_cyclicPath_path(final Object v1Id) {
        return this.sqlGraph.v(v1Id).out("created").in("created").cyclicPath().path();
    }

    /**
     * Looks up the identifier as generated by the current source graph being tested.
     *
     * @param identifier a unique string that will identify a graph element within a graph
     * @return the id as generated by the graph
     */
    protected Object convertToVertexId(final String identifier) {
        return convertToVertexId(this.sqlGraph, identifier);
    }

    /**
     * Looks up the identifier as generated by the current source graph being tested.
     *
     * @param g          the graph to get the element id from
     * @param identifier a unique string that will identify a graph element within a graph
     * @return the id as generated by the graph
     */
    protected Object convertToVertexId(final Graph g, final String identifier) {
        // all test graphs have "name" as a unique id which makes it easy to hardcode this...works for now
        return ((Vertex) g.V().has("name", identifier).next()).id();
    }

    //    @Test
    public void shouldSupportTransactionIsolationWithSeparateThreads() throws Exception {
        // one thread modifies the graph and a separate thread reads before the transaction is committed.
        // the expectation is that the changes in the transaction are isolated to the thread that made the change
        // and the second thread should not see the change until commit() in the first thread.
        final Graph graph = this.sqlGraph;

        final CountDownLatch latchCommit = new CountDownLatch(1);
        final CountDownLatch latchFirstRead = new CountDownLatch(1);
        final CountDownLatch latchSecondRead = new CountDownLatch(1);

        final Thread threadMod = new Thread() {
            public void run() {
                graph.addVertex();

                latchFirstRead.countDown();

                try {
                    latchCommit.await();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }

                graph.tx().commit();

                latchSecondRead.countDown();
            }
        };

        threadMod.start();

        final AtomicLong beforeCommitInOtherThread = new AtomicLong(0);
        final AtomicLong afterCommitInOtherThread = new AtomicLong(0);
        final Thread threadRead = new Thread() {
            public void run() {
                try {
                    latchFirstRead.await();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }

                // reading vertex before tx from other thread is committed...should have zero vertices
                beforeCommitInOtherThread.set(graph.V().count().next());

                latchCommit.countDown();

                try {
                    latchSecondRead.await();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }

                // tx in other thread is committed...should have one vertex
                afterCommitInOtherThread.set(graph.V().count().next());
            }
        };

        threadRead.start();

        threadMod.join();
        threadRead.join();

        assertEquals(0l, beforeCommitInOtherThread.get());
        assertEquals(1l, afterCommitInOtherThread.get());
    }

    //    @Test
    public void shouldReadGraphML() throws IOException {
        readGraphMLIntoGraph(this.sqlGraph);
        assertClassicGraph(this.sqlGraph, true, true);
    }

    private static void readGraphMLIntoGraph(final Graph g) throws IOException {
        final GraphReader reader = GraphMLReader.create().build();
        try (final InputStream stream = new FileInputStream(new File("sqlgraph-test/src/main/resources/tinkerpop-classic.xml"))) {
            reader.readGraph(stream, g);
        }
//        try (final InputStream stream = TinkerpopTest.class.getResourceAsStream("tinkerpop-classic.xml")) {
//            reader.readGraph(stream, g);
//        }
    }

    public static void assertClassicGraph(final Graph g1, final boolean lossyForFloat, final boolean lossyForId) {
        assertEquals(new Long(6), g1.V().count().next());
        assertEquals(new Long(6), g1.E().count().next());

        final Vertex v1 = (Vertex) g1.V().has("name", "marko").next();
        assertEquals(29, v1.<Integer>value("age").intValue());
        assertEquals(2, v1.keys().size());
        assertClassicId(g1, lossyForId, v1, 1);

        final List<Edge> v1Edges = v1.bothE().toList();
        assertEquals(3, v1Edges.size());
        v1Edges.forEach(e -> {
            if (e.inV().value("name").next().equals("vadas")) {
                assertEquals("knows", e.label());
                if (lossyForFloat)
                    assertEquals(0.5d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.5f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 7);
            } else if (e.inV().value("name").next().equals("josh")) {
                assertEquals("knows", e.label());
                if (lossyForFloat)
                    assertEquals(1.0, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 8);
            } else if (e.inV().value("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 9);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v2 = (Vertex) g1.V().has("name", "vadas").next();
        assertEquals(27, v2.<Integer>value("age").intValue());
        assertEquals(2, v2.keys().size());
        assertClassicId(g1, lossyForId, v2, 2);

        final List<Edge> v2Edges = v2.bothE().toList();
        assertEquals(1, v2Edges.size());
        v2Edges.forEach(e -> {
            if (e.outV().value("name").next().equals("marko")) {
                assertEquals("knows", e.label());
                if (lossyForFloat)
                    assertEquals(0.5d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.5f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 7);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v3 = (Vertex) g1.V().has("name", "lop").next();
        assertEquals("java", v3.<String>value("lang"));
        assertEquals(2, v2.keys().size());
        assertClassicId(g1, lossyForId, v3, 3);

        final List<Edge> v3Edges = v3.bothE().toList();
        assertEquals(3, v3Edges.size());
        v3Edges.forEach(e -> {
            if (e.outV().value("name").next().equals("peter")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.2d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.2f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 12);
            } else if (e.outV().next().value("name").equals("josh")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 11);
            } else if (e.outV().value("name").next().equals("marko")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 9);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v4 = (Vertex) g1.V().has("name", "josh").next();
        assertEquals(32, v4.<Integer>value("age").intValue());
        assertEquals(2, v4.keys().size());
        assertClassicId(g1, lossyForId, v4, 4);

        final List<Edge> v4Edges = v4.bothE().toList();
        assertEquals(3, v4Edges.size());
        v4Edges.forEach(e -> {
            if (e.inV().value("name").next().equals("ripple")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 10);
            } else if (e.inV().value("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.4d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.4f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 11);
            } else if (e.outV().value("name").next().equals("marko")) {
                assertEquals("knows", e.label());
                if (lossyForFloat)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 8);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v5 = (Vertex) g1.V().has("name", "ripple").next();
        assertEquals("java", v5.<String>value("lang"));
        assertEquals(2, v5.keys().size());
        assertClassicId(g1, lossyForId, v5, 5);

        final List<Edge> v5Edges = v5.bothE().toList();
        assertEquals(1, v5Edges.size());
        v5Edges.forEach(e -> {
            if (e.outV().value("name").next().equals("josh")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(1.0d, e.value("weight"), 0.0001d);
                else
                    assertEquals(1.0f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 10);
            } else {
                fail("Edge not expected");
            }
        });

        final Vertex v6 = (Vertex) g1.V().has("name", "peter").next();
        assertEquals(35, v6.<Integer>value("age").intValue());
        assertEquals(2, v6.keys().size());
        assertClassicId(g1, lossyForId, v6, 6);

        final List<Edge> v6Edges = v6.bothE().toList();
        assertEquals(1, v6Edges.size());
        v6Edges.forEach(e -> {
            if (e.inV().value("name").next().equals("lop")) {
                assertEquals("created", e.label());
                if (lossyForFloat)
                    assertEquals(0.2d, e.value("weight"), 0.0001d);
                else
                    assertEquals(0.2f, e.value("weight"), 0.0001f);
                assertEquals(1, e.keys().size());
                assertClassicId(g1, lossyForId, e, 12);
            } else {
                fail("Edge not expected");
            }
        });
    }

    private static void assertClassicId(final Graph g, final boolean lossyForId, final Element e, final Object expected) {
        if (g.getFeatures().edge().supportsUserSuppliedIds()) {
            if (lossyForId)
                assertEquals(expected.toString(), e.id().toString());
            else
                assertEquals(expected, e.id());
        }
    }

    protected void tryCommit(final SqlGraph g, final Consumer<Graph> assertFunction) {
        assertFunction.accept(g);
        if (g.getFeatures().graph().supportsTransactions()) {
            g.tx().commit();
            assertFunction.accept(g);
        }
    }

    protected void tryCommit(final Graph g) {
        if (g.getFeatures().graph().supportsTransactions())
            g.tx().commit();
    }

    protected void tryRollback(final Graph g) {
        if (g.getFeatures().graph().supportsTransactions())
            g.tx().rollback();
    }

}
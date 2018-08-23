package com.galois.dbpedia

import com.rrwright.quine.language.EdgeDirections._
import com.rrwright.quine.language._
import scala.pickling.Defaults._
import scala.pickling.json.pickleFormat
import akka.actor.ActorSystem
import akka.util.Timeout
import com.rrwright.quine.runtime.{GraphService, MapDBMultimap}
import shapeless._
import shapeless.syntax.singleton._
import scala.concurrent.duration._
import java.io._
import scala.util.matching._
import scala.concurrent._
import scala.util._
import java.util.UUID


object Application extends App {

  implicit val system = ActorSystem("test")
  implicit val graph = GraphService(system)(MapDBMultimap())
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(300 seconds)

  // Open up for SSH ammonite shelling via `ssh repl@localhost -p22222`
  import ammonite.sshd._
  import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator
  val replServer = new SshdRepl(
    SshServerConfig(
      address = "localhost", // or "0.0.0.0" for public-facing shells
      port = 22222, // Any available port
      passwordAuthenticator = Some(AcceptAllPasswordAuthenticator.INSTANCE)
    ),
    predef = "repl.frontEnd() = ammonite.repl.FrontEnd.JLineUnix"
  )
  replServer.start()
 
  // These files can be downloaded from http://downloads.dbpedia.org/2015-10/core/
  val ingest = for {
    _ <- TurtleTriple.loadFileTriples("article_categories_en.ttl"); //, Some(100));
    _ <- TurtleTriple.loadFileTriples("instance_types_en.ttl"); //, Some(100));
  //_ <- TurtleTriple.loadFileTriples("instance_types_transitive_en.ttl");
    _ <- TurtleTriple.loadFileTriples("mappingbased_objects_en.ttl"); //, Some(100));
    _ <- TurtleTriple.loadFileTriples("skos_categories_en.ttl") //, Some(100))
  } yield ()

  ingest.onComplete {
    case Failure(e) =>
      println("Ingest failed:")
      e.printStackTrace
    case Success(s) =>
      println("Done ingesting.")
  }
}

/** See <https://www.w3.org/TeamSubmission/turtle/> for proper grammar/parsing */
object TurtleTriple {

  def uuidFromUri(uri: String): QuineId = UUID.nameUUIDFromBytes(uri.getBytes)

  /** Load into the graph a series of triples read from a .ttl file */
  def loadFileTriples(
    path: String,
    limit: Option[Int] = None
  )(implicit
    graph: GraphService,
    timeout: Timeout,
    ec: ExecutionContext
  ): Future[Unit] = {
    
    // Regular expressions for matching one line of a .ttl file
    val nodeEdgeNode: Regex = raw"""<([^>]*)>\s+<([^>]*)>\s+<([^>]*)>\s*\.\s*""".r
    val nodeKeyValue: Regex = raw"""<([^>]*)>\s+<([^>]*)>\s+"([^"]*)"(@[a-zA-Z]+)?\s*\.\s*""".r
    val ignoreLine: Regex = raw"#.*|\s*".r
    
    val br: BufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path),"UTF-8"))
    
    // mutable state
    var line: String = ""
    var fut: Future[Unit] = Future.successful(())
    var count: Int = 0

    var predicates: Set[String] = Set()

    while (line != null && limit.fold(true)(l => count < l)) {
      
      // Parse the line
      line match {
        case nodeEdgeNode(n1, e, n2) =>
          val qId1 = uuidFromUri(n1) 
          val qId2 = uuidFromUri(n2)  
          
          fut = fut
            .flatMap(_ => graph.dumbOps.addEdge(qId1,qId2,e))
            .flatMap(_ => graph.dumbOps.setProp(qId1,"uri",n1))
            .flatMap(_ => graph.dumbOps.setProp(qId2,"uri",n2))

        case nodeKeyValue(n, k, v, _) => 
          val qId = uuidFromUri(n)

          fut = fut.flatMap(_ => graph.dumbOps.setProp(qId,k,v))
        
        case ignoreLine() => println(s"Skipping line: $line")
        case l => println(s"Couldn't parse line: $line")
      }
      line = br.readLine()
      
      // Report progress
      count += 1
      if (count % 1000 == 0) {
        Await.ready(fut, 30 seconds).onComplete { _ =>
          println(s"Ingested from $path: $count")
        }
      }
    }

    println(predicates)
    println(predicates.size)
    println(s"Done ingesting from $path: $count")
    fut
  }
}



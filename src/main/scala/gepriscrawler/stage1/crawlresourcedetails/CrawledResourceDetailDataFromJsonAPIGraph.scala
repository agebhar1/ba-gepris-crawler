package gepriscrawler.stage1.crawlresourcedetails

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.{FlowShape, Graph}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object CrawledResourceDetailDataFromJsonAPIGraph {

  def graph(resourceType: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[String, (String, String)], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
    val resourceIdToCrawl = b.add(Flow[String])

    val resourceDetailJsonHttpRequest: FlowShape[String, (String, String)] = b.add(Flow[String]
      .throttle(90, 60.seconds)
      .mapAsync(1) { resourceId =>
        Http().singleRequest(
          HttpRequest(
            uri = s"https://gepris-extern.dfg.de/$resourceNameForUrlQuery/$resourceId"
          )
        ).flatMap { response =>
          val (status, headers) = (response.status, response.headers)
          status.intValue() match {
            case 200 =>
              response.entity.dataBytes
                .runReduce(_ ++ _)
                .map(body => resourceId -> body.utf8String)
            case _ =>
              println(s"Got non 200 status code for resource with id $resourceId; resp status: $status; resp headers: $headers")
              throw new Exception("Got non-200 HTTP response from Gepris server")
          }
        }
      }
    )

    resourceIdToCrawl ~> resourceDetailJsonHttpRequest
    FlowShape(resourceIdToCrawl.in, resourceDetailJsonHttpRequest.out)
  }
}

package gepriscrawler.stage1.crawlresourcedetails

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.{FlowShape, Graph}
import gepriscrawler.CrawledResourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object CrawledResourceDetailDataGraph {

  def graph(resourceType: String, crawledLanguage: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[(Cookie, String), CrawledResourceData], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val resourceNameForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
    val cookieAndUrlToCrawl = b.add(Flow[(Cookie, String)])

    val resourceDetailPageHttpRequest: FlowShape[(Cookie, String), (String, Element)] = b.add(Flow[(Cookie, String)]
      .throttle(90, 60.seconds)
      .mapAsync(1) { case (cookie, resourceId) =>
        Http().singleRequest(
          HttpRequest(
            uri = s"https://gepris.dfg.de/gepris/$resourceNameForUrlQuery/$resourceId?language=$crawledLanguage",
            headers = List(cookie)
          )
        ).flatMap {
          response: HttpResponse =>
            val (status, headers) = (response.status, response.headers)
            status.intValue() match {
              case 200 =>
                print(s"\rReceived the detail page for the following resource for resource type '${resourceType}': id: $resourceId                                                   ")
                response.entity.dataBytes
                  .runReduce(_ ++ _)
                  .map(body => (resourceId, Jsoup.parse(body.utf8String).body()))
              case _ =>
                println(s"Got non 200 status code for resource with id $resourceId; resp status: $status; resp headers: $headers")
                throw new Exception("Got non-200 HTTP response from Gepris server")
            }
        }
      }
    )

    val crawledResourceDetailData: FlowShape[(String, Element), CrawledResourceData] = b.add(Flow[(String, Element)]
      .mapConcat { case (resourceId: String, body: Element) =>
        List(CrawledResourceData(resourceType, resourceId, crawledLanguage, body.toString))
      }
    )

    cookieAndUrlToCrawl ~> resourceDetailPageHttpRequest ~> crawledResourceDetailData
    FlowShape(cookieAndUrlToCrawl.in, crawledResourceDetailData.out)
  }
}

package gepriscrawler.stage0.resourceidstocrawl

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.{FlowShape, Graph}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ResourceCatalogPagesToResourceIdsToCrawlGraph {

  def graph(resourceType: String, resourceLinkCssSelector: String)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[FlowShape[(Cookie, String), (String, String)], NotUsed] = GraphDSL.create() { implicit b =>
    val resourceIdToName = b.add(Flow[(Cookie, String)]
      .throttle(90, 60.seconds)
      .mapAsync(1) { case (cookie, url) =>
        val indexRegex = ".*index=(\\d*)&.*".r
        val indexRegex(index) = url
        Http().singleRequest(
          request = HttpRequest(
            uri = url,
            headers = List(cookie)
          )
        ).flatMap {
          response: HttpResponse =>
            response.status.intValue() match {
              case 200 =>
                val indexRegex = ".*index=(\\d*)&.*".r
                val indexRegex(index) = url
                response.entity.dataBytes
                  .runReduce(_ ++ _)
                  .map(body => Jsoup.parse(body.utf8String).body())
              case _ =>
                println(s"Got non 200 status code for $resourceType for resp")
                throw new Exception("Got non-200 HTTP response from Gepris server")

            }
        }
      }
      .mapConcat { body: Element =>
        val resourceLinks = body.select(resourceLinkCssSelector).asScala.to[collection.immutable.Seq]
        val resourceIdsToNames = resourceLinks.map { resourceLink =>

          val resourceName = resourceLink.text()
          val resourceTypeForUrlQuery = gepriscrawler.GeprisResources.resourceList(resourceType).resourceTyppeForUrlQuery
          val resourceHref = resourceLink.attr("href")
          val resourceRegex = raw"""\/gepris\/$resourceTypeForUrlQuery/(\d*)""".r
          val resourceId = resourceHref match {
            case resourceRegex(id) => id
            case _ => ""
          }

          print(s"\rReceived from catalog page the following resource for resource type '${resourceType}': id: $resourceId, name: $resourceName                         ")
          (resourceId -> resourceName)
        }

        resourceIdsToNames
      }
    )

    FlowShape(resourceIdToName.in, resourceIdToName.out)
  }

}

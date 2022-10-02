package gepriscrawler.stage0

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import gepriscrawler._
import gepriscrawler.helpers.CrawlerHelpers.CSVRow
import gepriscrawler.helpers.{CookieFlowGraph, CrawlerHelpers}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import java.nio.file.{Path, Paths}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object CrawlAndExtractSubjectAreasGraph {

  def graph(exportRootPath: Path)(implicit actorSystem: akka.actor.ActorSystem, streamMaterializer: akka.stream.Materializer, executionContext: ExecutionContext): Graph[SourceShape[ResearchArea], NotUsed] = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val exportPath = Paths.get(exportRootPath.toString, "stage0")
    val subjectAreasCsvWriterSink: Sink[CSVRow, Unit] = CrawlerHelpers.createCsvFileWriterSink(
      exportPath, "subject_areas",
      Seq("subject_area", "review_board", "research_area")
    )

    val cookiePinger: SourceShape[Boolean] = b.add(Source.repeat(false))
    val cookieFlow: FlowShape[Boolean, Cookie] = b.add(CookieFlowGraph.graph)

    // TODO: multi langauge support
    val initialSubjectAreaUrl = b.add(Source.single("https://www.dfg.de/en/dfg_profile/statutory_bodies/review_boards/subject_areas/index.jsp"))
    //    val initialSubjectAreaUrl = b.add(Source.single("https://www.dfg.de/dfg_profil/gremien/fachkollegien/faecher/"))

    val subjectAreaRequestAndExtractorBC = b.add(Broadcast[ResearchArea](2))

    val subjectAreaRequestAndExtractor: FlowShape[(String, Cookie), ResearchArea] = b.add(Flow[(String, Cookie)]
      .throttle(90, 60.seconds)
      .mapAsync(1) { case (subjectAreaUrl, cookie) =>
        Http().singleRequest(
          HttpRequest(
            uri = subjectAreaUrl,
            headers = List(cookie)
          )
        ).flatMap {
          response: HttpResponse =>
            val (status, headers) = (response.status, response.headers)
            status.intValue() match {
              case 200 =>
                response.entity.dataBytes
                  .runReduce(_ ++ _)
                  .map(body => Jsoup.parse(body.utf8String).body())
              case _ =>
                println(s"Got non 200 status code for subject area crawling; resp status: $status; resp headers: $headers")
                throw new Exception("Got non-200 HTTP response from Gepris server")
            }
        }
      }
      .mapConcat { body: Element =>
        val researchAreasElements = body.select("#main-content > div:nth-child(2) > div > div > div > div > div > table").asScala
        researchAreasElements.map { researchAreaElement =>
          val researchAreaTitle = researchAreaElement.select("caption").text()
          val reviewBoardElements = researchAreaElement.select(".fachkolleg").asScala

          val reviewBoards = reviewBoardElements.map { reviewBoardElement =>
            val reviewBoardTitle = reviewBoardElement.select("td > a:not(.toggle_fk)").text()
            val subjectAreas = reviewBoardElement.select(".fachInhalt > a").eachText().asScala.map(_.trim)
            ReviewBoard(reviewBoardTitle.trim, subjectAreas)
          }
          ResearchArea(researchAreaTitle.trim, reviewBoards)
        }.toList
      }
    )

    val cookieWithsubjectAreaUrlIdZipper = b.add(Zip[String, Cookie])

    initialSubjectAreaUrl ~> cookieWithsubjectAreaUrlIdZipper.in0
    cookiePinger ~> cookieFlow ~> cookieWithsubjectAreaUrlIdZipper.in1
    cookieWithsubjectAreaUrlIdZipper.out ~> subjectAreaRequestAndExtractor

    subjectAreaRequestAndExtractor ~> subjectAreaRequestAndExtractorBC

    subjectAreaRequestAndExtractorBC.out(0)
      .mapConcat { researchArea: ResearchArea =>
        researchArea.reviewBoards.flatMap { reviewBoard: ReviewBoard =>
          reviewBoard.subjectAreas.map { subjectArea: String =>
            Seq(subjectArea, reviewBoard.name, researchArea.name)
          }
        }.toList
      } ~> subjectAreasCsvWriterSink

    SourceShape(subjectAreaRequestAndExtractorBC.out(1))
  }

}

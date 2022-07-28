package gepriscrawler.stage2.CrawlAndExtractPersons.PersonExtractor

import akka.NotUsed
import akka.stream.scaladsl.Flow
import gepriscrawler.{CrawledResourceData, Person}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object PersonExtractorGraph {

  val graph: Flow[CrawledResourceData, Person, NotUsed] = Flow[CrawledResourceData]
    .map { crawledPersonData: CrawledResourceData =>

      val body: Document = Jsoup.parse(crawledPersonData.html)
      val detailSection = body.select("#detailseite > div > div > div.content_frame > div.content_inside.detailed")

      val institutionNameAndAddress = detailSection
//        TODO: Mehrsprachigkeit
//        .select("span.name:matches(Adresse) + span")
        .select("span.name:matches(Address) + span")
        .html()
        .split("<br>")
        .map(_.trim)

      val institutionName = institutionNameAndAddress.head

      val address = institutionNameAndAddress.mkString("\n")

      Person(
        crawledPersonData.resourceId,
        name = detailSection
          .select("h1")
          .text(),
        institutionName,
        address = address
      )
    }
}

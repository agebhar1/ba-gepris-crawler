package gepriscrawler.stage2.CrawlProjects.ProjectExtractor

import gepriscrawler.helpers.ExtractorHelpers
import org.jsoup.nodes.Element


object ProjectPersonRelationsExtractors {


  //TODO: hier eventuell ueber cases die field-names und extractor-regexes sauberer in liste von cases class instanzen speichern
  //  case class ProjectPersonRelationExtractorRules(fieldName: String, extractorRege x: String)
  def extractProjectPersonRelations(allNameFields: Seq[Element]) = {

    type ProjectPersonRelationType = String
    type FieldLabelVariations = Seq[String]

    def extractPersonIdsFromLinksByRegex = ExtractorHelpers.extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("person")(_)

    val projectPersonRelationTypesToFieldLabelVariations: Seq[(ProjectPersonRelationType, FieldLabelVariations)] = Seq(
      "APPLICANT" -> Seq("Applicant", "Applicants"),
      "HEAD" -> Seq("Head", "Heads", "Project Head", "Project Heads"),
      "PROJECT_LEADER" -> Seq("Project leader", "Project leaders", "Leader", "Leaders"),
      "PARTICIPATING_SCIENTIST" -> Seq("Participating scientist", "Participating scientists"),
      "COAPPLICANT" -> Seq("Co-Applicant", "Co-Applicants", "Co-applicant", "Co-applicants"),
      "FORMER_APPLICANT" -> Seq("Former applicant", "Former applicants", "Ehemaliger Antragsteller","Ehemalige Antragstellerin","Ehemalige Antragstellerinnen / Ehemalige Antragsteller", "Ehemalige Antragstellerinnen","Ehemalige Antragsteller"),
      "PARTICIPATING_PERSON" -> Seq("Participating Person", "Participating Persons"),
      "SPOKESPERSON" -> Seq("Spokesperson", "Spokespersons"),
      "FOREIGN_SPOKESPERSON" -> Seq("Foreign spokesperson", "Foreign spokespeople"),
      "DEPUTY_SPOKESPERSON" -> Seq("Deputy spokesperson", "Deputy spokespeople"),
      "INTERNATIONAL_CO_APPLICANT" -> Seq("International Co-Applicant", "International Co-Applicants"),
      "COOPERATION_PARTNER" -> Seq("Cooperation partner", "Cooperation partners"),
      "PARTICIPATING_RESEARCHER" -> Seq("Participating Researchers", "Participating Researcher"),
      "CO_INVESTIGATOR" -> Seq("Co-Investigators", "Co-Investigator"),
      "HOST" -> Seq("Host", "Hosts"),
      "IRTG_PARTNER_SPOKESPERSON" ->Seq("IRTG-Partner: Spokesperson", "IRTG-Partner: Spokespersons")
    )

    projectPersonRelationTypesToFieldLabelVariations
      .flatMap(singleProjectPersonRelationTypeToFieldLabelVariations =>
        extractPersonIdsFromLinksByRegex(singleProjectPersonRelationTypeToFieldLabelVariations._2)
          .map(personId => (personId -> singleProjectPersonRelationTypeToFieldLabelVariations._1))
      )
  }

}

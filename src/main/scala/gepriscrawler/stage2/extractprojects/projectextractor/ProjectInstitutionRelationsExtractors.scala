package gepriscrawler.stage2.extractprojects.projectextractor

import gepriscrawler.helpers.ExtractorHelpers
import org.jsoup.nodes.Element


object ProjectInstitutionRelationsExtractors {

  def extractProjectInstitutionRelations(allNameFields: Seq[Element]) = {

    type ProjectInstitutionRelationType = String
    type FieldLabelVariations = Seq[String]

    def extractInstitutionIdsFromLinksByRegex = ExtractorHelpers.extractResourceIdsFromLinkByResourceTypeAndRegex(allNameFields)("institution")(_)

    val projectInstitutionRelationTypesToFieldLabelVariations: Seq[(ProjectInstitutionRelationType, FieldLabelVariations)] = Seq(
      "APPLICANT_INSTITUTION" -> Seq("Applicant Institution"),
      "CO_APPLICANT_INSTITUTION" -> Seq("Co-Applicant Institution"),
      "FOREIGN_INSTITUTION" -> Seq("Foreign Institution"),
      "PARTICIPATING_INSTITUTION" -> Seq("Participating Institution"),
      "PARTICIPATING_UNIVERSITY" -> Seq("Participating University"),
      "PARTNER_ORGANISATION" -> Seq("Partner Organisation"),
      "APPLICATION_PARTNER" -> Seq("Application Partner"),
      "INDUSTRY" -> Seq("Business and Industry"),
      "IRTG_PARTNER_INSTITUTION" -> Seq("IRTG-Partner Institution")
    )

    projectInstitutionRelationTypesToFieldLabelVariations
      .flatMap(singleProjectInstitutionRelationTypeToFieldLabelVariations =>
        extractInstitutionIdsFromLinksByRegex(singleProjectInstitutionRelationTypeToFieldLabelVariations._2)
          .map(institutionId => (institutionId -> singleProjectInstitutionRelationTypeToFieldLabelVariations._1))
      )
  }
}

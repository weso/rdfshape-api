package es.weso.html2rdf
import es.weso.rdf.jena.RDFAsJenaModel
import org.scalatest._

class HTML2RDFTest extends FunSpec with Matchers {
  describe(s"Extract RDF data from HTML") {

    shouldExtract(
      """|<body prefix = "xsd: http://www.w3.org/2001/XMLSchema#"
         |      vocab = "http://schema.org/" >
         |<div resource="http://example.org/post" typeOf="Blog">
         |  <p>Post created
         |     <span content="2018-10-30"
         |           property="created"
         |           datatype="xsd:date">last saturday</span>.
         |  </p>
         |</div>
         |</body>
      """.stripMargin,
      """|prefix rdfa:   <http://www.w3.org/ns/rdfa#>
         |prefix schema: <http://schema.org/>
         |prefix xsd:    <http://www.w3.org/2001/XMLSchema#>
         |
         |<http://example.org>  rdfa:usesVocabulary schema: .
         |
         |<http://example.org/post> a schema:Blog ;
         |         schema:created "2018-10-30"^^xsd:date .
      """.stripMargin, "html-rdfa11"
    )

    shouldExtract(
      """|<div itemscope itemid="http://example.org/eliza">
       | <p>My name is <span itemprop="name">Elizabeth</span>.</p>
       |</div>""".stripMargin,
      """|prefix md:   <http://www.w3.org/1999/xhtml/microdata#>
       |prefix schema: <http://schema.org/>
       |prefix xsd:    <http://www.w3.org/2001/XMLSchema#>
       |prefix : <http://example.org/>
       |
       |<http://example.org>  md:item :eliza .
       |:eliza schema:name "Elizabeth" .
    """.stripMargin, "html-microdata"
    )

    shouldExtract(
      """|<div itemscope
         |    itemtype="https://vocab.example.net/book">
         |</div>
         |""".stripMargin,
      """|prefix md:   <http://www.w3.org/1999/xhtml/microdata#>
         |
         |<http://example.org>  md:item [
         |  a  <https://vocab.example.net/book>
         |] .
      """.stripMargin, "html-microdata"
    )

/* TODO: Checkwhy this test fails   shouldExtract(
      """|<dl itemscope
         |    itemtype="https://vocab.example.net/book">
         |</dl>
         |""".stripMargin,
      """|prefix md:   <http://www.w3.org/1999/xhtml/microdata#>
         |
         |<http://example.org>  md:item [
         |  a  <https://vocab.example.net/book>
         |] .
      """.stripMargin, "html-microdata"
    ) */

    def shouldExtract(html: String, expected: String, extractorName: String): Unit = {
      it(s"Should extract from $html and obtain $expected with extractor $extractorName") {
        val r = for {
          rdf          <- HTML2RDF.extractFromString(html,extractorName)
          expected     <- RDFAsJenaModel.fromChars(expected, "TURTLE")
          isIsomorphic <- rdf.isIsomorphicWith(expected)
        } yield (isIsomorphic, rdf)

        r.fold(
          e => fail(s"Error: $e"),
          pair => {
            val (ok, rdf) = pair
            info(s"Model:\n${rdf.serialize("Turtle").getOrElse("")}")
            ok should be(true)
          }
        )
      }
    }

  }
}
package es.weso.html2rdf
import es.weso.rdf.jena.RDFAsJenaModel
import org.scalatest.funspec._
import org.scalatest.matchers.should._
import es.weso.utils.IOUtils._
import es.weso.rdf.RDFReader

class HTML2RDFTest extends AnyFunSpec with Matchers {
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

    shouldExtract(
      """|<div itemscope
         |     itemtype="http://schema.org/Person"
         |     itemid="http://person.info/alice" style="font-size:25pt;">
         |  My name is <span itemprop="name">Alice</span>.
         |</div>
        |
      """.stripMargin,
      """|prefix md:   <http://www.w3.org/1999/xhtml/microdata#>
         |prefix person: <http://person.info/>
         |<http://example.org>  md:item person:alice .
         |person:alice a <http://schema.org/Person> ;
         |  <http://schema.org/Person/name> "Alice" .
         |""".stripMargin,
      "html-microdata")

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
        val r: ESIO[(Boolean,String,String)] = for {
          expected     <- {
            println(s"## Before parsing RDF\n${expected}\n---")
            io2es(RDFAsJenaModel.fromChars(expected, "TURTLE"))
          }
          expectedStr <- io2es(expected.serialize("TURTLE"))
          rdf          <- {
            println(s"Expected: \n ${expectedStr}")
            println(s"## Before extraction with $extractorName")
            HTML2RDF.extractFromString(html,extractorName)
          }
          rdfObtained <- io2es(rdf.serialize("TURTLE"))
          isIsomorphic <- {
            println(s"RDF extracted: \n ${rdfObtained}")
            io2es(rdf.isIsomorphicWith(expected))
          }
        } yield (isIsomorphic, rdfObtained, expectedStr)

        run_es(r).unsafeRunSync.fold(
          e => fail(s"Error extracting: $e"),
          pair => {
            val (ok, rdf, expected) = pair
            if (ok) {
              info(s"Model extracted isomorphic with expected")
            } else {
              fail(
                s"""Model extracted is not isomorphic with expected one:
                   |Model extracted
                   |${expected}
                   |Model expected
                   |${rdf}
                   |""".stripMargin)
            }
          }
        )
      }
    }

  }
}
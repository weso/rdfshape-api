package es.weso.rdfshape.server.html2rdf

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.utils.IOUtils._
import es.weso.rdf.RDFReader
import cats.implicits._
import cats.effect.IO
import munit._

class HTML2RDFTest extends CatsEffectSuite {

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
         |<http://example.org/>  rdfa:usesVocabulary schema: .
         |
         |<http://example.org/post> a schema:Blog ;
         |         schema:created "2018-10-30"^^xsd:date .
      """.stripMargin,
    "html-rdfa11"
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
       |<http://example.org/>  md:item :eliza .
       |:eliza schema:name "Elizabeth" .
    """.stripMargin,
    "html-microdata"
  )

  shouldExtract(
    """|<div itemscope
         |    itemtype="https://vocab.example.net/book">
         |</div>
         |""".stripMargin,
    """|prefix md:   <http://www.w3.org/1999/xhtml/microdata#>
         |
         |<http://example.org/>  md:item [
         |  a  <https://vocab.example.net/book>
         |] .
      """.stripMargin,
    "html-microdata"
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
         |<http://example.org/>  md:item person:alice .
         |person:alice a <http://schema.org/Person> ;
         |  <http://schema.org/Person/name> "Alice" .
         |""".stripMargin,
    "html-microdata"
  )

  /* TODO: Check why this test fails shouldExtract( """|<dl itemscope |
   * itemtype="https://vocab.example.net/book"> |</dl> |""".stripMargin,
   * """|prefix md: <http://www.w3.org/1999/xhtml/microdata#> |
   * |<http://example.org/> md:item [ | a <https://vocab.example.net/book> |] .
   * """.stripMargin, "html-microdata" ) */

  def shouldExtract(
      html: String,
      expected: String,
      extractorName: String
  ): Unit = {
    test(
      s"Should extract from \n$html\n and obtain\n$expected\nExtractor $extractorName"
    ) {
      val r: IO[(Boolean, String, String)] = for {
        res1 <- IO(HTML2RDF.extractFromString(html, extractorName))
        res2 <- RDFAsJenaModel.fromChars(expected, "TURTLE")
        vv <- (res1, res2).tupled.use { case (rdf, expected) =>
          for {
            //_ <- { pprint.log(rdf); IO(())}
            expectedStr <- expected.serialize("TURTLE")
            //_ <- { pprint.log(expectedStr); IO(())}
            rdfObtained <- rdf.serialize("TURTLE")
            //_ <- { pprint.log(rdfObtained); IO(())}
            isIsomorphic <- rdf.isIsomorphicWith(expected)
          } yield (isIsomorphic, rdfObtained, expectedStr)
        }
      } yield vv

      r.map(t => {
        val (ok, rdf, expected) = t
        if(ok) {
          assertEquals(ok, true)
        } else {
          fail(s"""Model extracted is not isomorphic with expected one:
                   |Model extracted
                   |${expected}
                   |Model expected
                   |${rdf}
                   |""".stripMargin)
        }
      })
    }
  }
}

package hydra.notifications.converters

import org.scalatest.{FlatSpec, Matchers}

class CsvToHtmlConverterSpec extends FlatSpec with Matchers {

  "CsvToHtmlConverter" should
    "correctly convert CSV to HTML with headers and data" in {
    val csv =
      """Topics,JobId,ConsumerGroupName,StartTime,TotalOffsetLag,LagPercentage,LargestOffset
        |dvs.test.critical,1b70054f-c560-3251-8564-92e81455f213,dvs-critical1,2024-08-14T14:16:39.406+05:30,0,0.0,9
        |dvs.test.critical,a971e760-f985-3e7a-a774-4367af885fda,dvs-critical3,2024-08-14T14:23:47.380+05:30,0,0.0,9""".stripMargin

    val expectedHtml =
      """
        |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>Topics</th><th>JobId</th><th>ConsumerGroupName</th><th>StartTime</th><th>TotalOffsetLag</th><th>LagPercentage</th><th>LargestOffset</th></tr></thead><tbody><tr><td>dvs.test.critical</td><td>1b70054f-c560-3251-8564-92e81455f213</td><td>dvs-critical1</td><td>2024-08-14T14:16:39.406+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr><tr><td>dvs.test.critical</td><td>a971e760-f985-3e7a-a774-4367af885fda</td><td>dvs-critical3</td><td>2024-08-14T14:23:47.380+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr></tbody></table>
        |""".stripMargin.trim

    val result = CsvToHtmlConverter.convertToHtml(csv).trim
    result shouldEqual expectedHtml
  }

  it should "align numeric cells to the right" in {
    val csv =
      """A,B,C
        |1,Hello,2.5
        |World,3,42""".stripMargin

    val expectedHtml =
      """
        |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>A</th><th>B</th><th>C</th></tr></thead><tbody><tr><td align="right">1</td><td>Hello</td><td align="right">2.5</td></tr><tr><td>World</td><td align="right">3</td><td align="right">42</td></tr></tbody></table>
        |""".stripMargin.trim

    val result = CsvToHtmlConverter.convertToHtml(csv).trim
    result shouldEqual expectedHtml
  }

  it should "handle empty CSV correctly" in {
    val csv = ""
    val expectedHtml = ""

    val result = CsvToHtmlConverter.convertToHtml(csv).trim
    result shouldEqual expectedHtml
  }

  it should "handle non CSV string correctly" in {
    val csv = "Hello World"
    val expectedHtml = ""

    val result = CsvToHtmlConverter.convertToHtml(csv)
    result shouldBe expectedHtml
  }
}

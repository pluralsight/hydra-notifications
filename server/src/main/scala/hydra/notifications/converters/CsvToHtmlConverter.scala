package hydra.notifications.converters

object CsvToHtmlConverter {

  def isCsv(input: String, delimiter: Char = ','): Boolean = {
    // Split the input into lines
    val lines = input.split("\n").filter(_.nonEmpty)

    // Check if there are at least two lines (header + at least one data row)
    if (lines.length < 2) return false

    // Split each line by the delimiter and check that all rows have the same number of columns
    val columnCounts = lines.map(_.split(delimiter).length)
    columnCounts.distinct.length == 1
  }

  def convertToHtml(inputString: String): String = {
    if (!isCsv(inputString)) {
      return ""
    }

    // Split the CSV into rows based on line breaks
    val rows = inputString.trim.split("\n").map(_.split(","))

    // Extract the header and data rows
    val header = rows.head.filter(_.nonEmpty).toSeq
    val dataRows = rows.tail

    // Convert the header to an HTML table row
    val headerHtml = header match {
      case Nil => ""
      case _ => header.map(cell => s"<th>$cell</th>").mkString("\n    <tr>", "", "</tr>\n  ")
    }

    // Convert each data row to an HTML table row
    val dataHtml = if (dataRows.nonEmpty) {
      dataRows.map { row =>
        row.map { cell =>
          // Check if the cell is a number and align it to the right if true
          if (isNumeric(cell.trim)) {
            s"""<td align="right">$cell</td>"""
          }
          else {
            s"<td>$cell</td>"
          }
        }.mkString("\n    <tr>", "", "</tr>")
      }.mkString("", "", "\n  ")
    } else {
      ""
    }

    // Wrap the rows in <table> tags
    val htmlString = s"""
                        |<table class="table table-bordered table-hover table-condensed">
                        |  <thead>$headerHtml</thead>
                        |  <tbody>$dataHtml</tbody>
                        |</table>
     """.stripMargin.trim

    htmlString.replaceAll("\\n\\s*", "")
  }

  private def isNumeric(value: String): Boolean = value.matches("""^-?\d+(\.\d+)?$""")
}

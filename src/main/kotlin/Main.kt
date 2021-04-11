import dev.gitlive.difflib.DiffUtils
import dev.gitlive.difflib.patch.AbstractDelta
import dev.gitlive.difflib.patch.DeltaType
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.*
import java.lang.Integer.min

fun DIV.originalTable(tableName: String, lines: List<String>) {
    table {
        caption { +tableName }
        lines.forEach {
            tr {
                td {
                    +it
                }
            }
        }
    }
}

fun DIV.diffTable(tableName: String, rowsList: List<Pair<String, DeltaType?>>) {
    table {
        caption { +tableName }
        try {
            rowsList.forEach {
                diffTableRow(it.first, it.second)
            }
        } catch (e: NullPointerException) {
            tr {
                td { +"ERROR OCUSED WHILE CALCULATING DIFF" }
            }
        }
    }
}

fun TABLE.diffTableRow(line: String, deltaType: DeltaType? = null) {
    tr {
        if (deltaType != null) {
            when (deltaType) {
                DeltaType.CHANGE -> {
                    diffTableCell(line, setOf("diffChanged"))
                }
                DeltaType.DELETE -> {
                    diffTableCell(line, setOf("diffDeleted"))
                }
                else -> {
                    diffTableCell(line, setOf("diffAdded"))
                }
            }
        } else {
            diffTableCell(line)
        }
    }
}

fun TR.diffTableCell(text: String, classes_arg: Set<String>? = null) {
    td {
        if (classes_arg != null) {
            classes = classes_arg
        }
        +text
    }
}

fun DIV.legendTable() {
    table {
        classes = setOf("legend")
        caption { +"Legend" }
        legendTableRow("Changed", setOf("rect", "diffChanged"))
        legendTableRow("Deleted", setOf("rect", "diffDeleted"))
        legendTableRow("Added", setOf("rect", "diffAdded"))
    }
}

fun TABLE.legendTableRow(text: String, classes_arg: Set<String>? = null) {
    tr {
        td {
            div {
                if (classes_arg != null) {
                    classes = classes_arg
                }
                +text
            }
        }
    }
}

fun addChangedLinesToRowsList(
    rowsList: MutableList<Pair<String, DeltaType?>>,
    position: Int,
    fromDeltaLines: List<String>,
    toDeltaLines: List<String>
) {
    val fromDeltaLinesNumber = fromDeltaLines.size
    val toDeltaLinesNumber = toDeltaLines.size
    val deltaLinesNumberMin = min(fromDeltaLinesNumber, toDeltaLinesNumber)
    for (i in 0 until deltaLinesNumberMin) {
        rowsList[position + i] = rowsList[position + i].first to DeltaType.CHANGE
    }
    if (fromDeltaLinesNumber > toDeltaLinesNumber) {
        for (i in deltaLinesNumberMin until fromDeltaLinesNumber) {
            rowsList[position + i] = rowsList[position + i].first to DeltaType.DELETE
        }
    } else if (toDeltaLinesNumber > fromDeltaLinesNumber) {
        for (i in deltaLinesNumberMin until toDeltaLinesNumber) {
            rowsList.add(position + i, toDeltaLines[i] to DeltaType.INSERT)
        }
    }
}

fun getDiffRowsList(
    original: List<String>,
    diffResult: MutableList<AbstractDelta<String>>
): MutableList<Pair<String, DeltaType?>> {
    val rowsList: MutableList<Pair<String, DeltaType?>> = original.map { it to null }.toMutableList()
    diffResult.reversed().forEach {
        val position = it.source.position
        val fromDeltaLines = it.source.lines!!
        val toDeltaLines = it.target.lines!!
        when (it.type) {
            DeltaType.DELETE -> {
                for (i in fromDeltaLines.indices) {
                    rowsList[position + i] = rowsList[position + i].first to DeltaType.DELETE
                }
            }
            DeltaType.INSERT -> {
                for (i in toDeltaLines.indices) {
                    rowsList.add(position + i, toDeltaLines[i] to DeltaType.INSERT)
                }
            }
            else -> {
                addChangedLinesToRowsList(rowsList, position, fromDeltaLines, toDeltaLines)
            }
        }
    }
    return rowsList
}

fun getFileLines(fileName: String): List<String>? {
    return try {
        val fileLines = File(fileName).readLines()
        fileLines
    } catch (e: FileNotFoundException) {
        println("File $fileName not found")
        null
    }
}

fun generateHtml(
    originalFileName: String,
    revisedFileName: String,
    originalFileLines: List<String>,
    revisedFileLines: List<String>,
    originalFileRows: List<Pair<String, DeltaType?>>,
    revisedFileRows: List<Pair<String, DeltaType?>>
) {
    val resultFileStream = PrintStream(File("test-result.html"))
    val console = System.out
    System.setOut(resultFileStream)
    System.out.appendHTML().html {
        head {
            link {
                rel = "stylesheet"
                href = "style.css"
            }
        }
        body {
            div {
                classes = setOf("holeDiv")
                originalTable("Original $originalFileName", originalFileLines)
                originalTable("Original $revisedFileName", revisedFileLines)
                diffTable(originalFileName, originalFileRows)
                diffTable(revisedFileName, revisedFileRows)
                legendTable()
            }
        }
    }
    System.setOut(console)
}

fun compareFiles(
    originalFilePath: String,
    revisedFilePath: String
): Pair<List<Pair<String, DeltaType?>>, List<Pair<String, DeltaType?>>>? {
    val originalFileLines = getFileLines(originalFilePath)
    val revisedFileLines = getFileLines(revisedFilePath)
    if (originalFileLines == null || revisedFileLines == null) {
        return null
    }

    val originalDiff = DiffUtils.diff(originalFileLines, revisedFileLines).getDeltas()
    val originalRowsList: List<Pair<String, DeltaType?>> = getDiffRowsList(originalFileLines, originalDiff)
    val revisedDiff = DiffUtils.diff(revisedFileLines, originalFileLines).getDeltas()
    val revisedRowsList: List<Pair<String, DeltaType?>> = getDiffRowsList(revisedFileLines, revisedDiff)

    val originalFileName = originalFilePath.split('/').last()
    val revisedFileName = revisedFilePath.split('/').last()

    generateHtml(originalFileName, revisedFileName, originalFileLines, revisedFileLines, originalRowsList, revisedRowsList)
    return originalRowsList to revisedRowsList
}

fun main(args: Array<String>) {

//    Uncomment this line if you want to generate html on test file
//    compareFiles("./src/main/resources/original.txt", "./src/main/resources/revised.txt")

    when {
        args.size == 2 -> {
            compareFiles(args[0], args[1])
        }
        args.size > 2 -> {
            print("Too many arguments. Syntax: compare.py [FIRST_FILE_PATH] [SECOND_FILE_PATH]")
        }
        else -> {
            print("Specify the path to the files")
        }
    }
}
import dev.gitlive.difflib.DiffUtils
import dev.gitlive.difflib.patch.AbstractDelta
import dev.gitlive.difflib.patch.DeltaType
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.*
import java.lang.Integer.min

/**
 * Generate table with passed file lines
 *
 * @param tableName the table name
 * @param lines lines of file nedded to put in table
 */
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

/**
 * Generate table with passed file lines + changes
 *
 * @param tableName the table name
 * @param rowsList rows with information about line delta status
 */
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

/**
 * Generate diffTable row
 *
 * @param line the line that should be added
 * @param deltaType deltaType of line to find it's color
 */
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

/**
 * Generate diffTable row cell
 *
 * @param line the line that should be added
 * @param classes_arg css classes of adding line
 */
fun TR.diffTableCell(line: String, classes_arg: Set<String>? = null) {
    td {
        if (classes_arg != null) {
            classes = classes_arg
        }
        +line
    }
}

/**
 * Generate legend table that tells info about diff table colors
 *
 */
fun DIV.legendTable() {
    table {
        classes = setOf("legend")
        caption { +"Legend" }
        legendTableRow("Changed", setOf("rect", "diffChanged"))
        legendTableRow("Deleted", setOf("rect", "diffDeleted"))
        legendTableRow("Added", setOf("rect", "diffAdded"))
    }
}

/**
 * Generate diffTable row
 *
 * @param line the line that should be added
 * @param classes_arg css classes of adding line
 */
fun TABLE.legendTableRow(line: String, classes_arg: Set<String>? = null) {
    tr {
        td {
            div {
                if (classes_arg != null) {
                    classes = classes_arg
                }
                +line
            }
        }
    }
}

/**
 * Subfunction of getDiffRowsList function. Handle information about CHANGED lines
 *
 * @param rowsList [List] of [Pair]: line from file to it's delta information
 * @param position index of calculated diff in [rowsList]
 * @param originalDeltaLines [List] of lines that should be changed
 * @param revisedDeltaLines [List] of lines to that lines should be changed
 */
fun addChangedLinesToRowsList(
    rowsList: MutableList<Pair<String, DeltaType?>>,
    position: Int,
    originalDeltaLines: List<String>,
    revisedDeltaLines: List<String>
) {
    val fromDeltaLinesNumber = originalDeltaLines.size
    val toDeltaLinesNumber = revisedDeltaLines.size
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
            rowsList.add(position + i, revisedDeltaLines[i] to DeltaType.INSERT)
        }
    }
}

/**
 * Get delta information about file's lines
 *
 * @param original Lines from file that should be changed
 * @param diffResult result from diff util
 * @return [List] of [Pair]: line from file to it's delta information (stay unchanged - null, change, delete, insert)
 */
fun getDiffRowsList(
    original: List<String>,
    diffResult: List<AbstractDelta<String>>
): List<Pair<String, DeltaType?>> {
    val rowsList: MutableList<Pair<String, DeltaType?>> = original.map { it to null }.toMutableList()
    diffResult.reversed().forEach {
        val position = it.source.position
        val originalDeltaLines = it.source.lines!!
        val revisedDeltaLines = it.target.lines!!
        when (it.type) {
            DeltaType.DELETE -> {
                for (i in originalDeltaLines.indices) {
                    rowsList[position + i] = rowsList[position + i].first to DeltaType.DELETE
                }
            }
            DeltaType.INSERT -> {
                for (i in revisedDeltaLines.indices) {
                    rowsList.add(position + i, revisedDeltaLines[i] to DeltaType.INSERT)
                }
            }
            else -> {
                addChangedLinesToRowsList(rowsList, position, originalDeltaLines, revisedDeltaLines)
            }
        }
    }
    return rowsList.toList()
}

/**
 * Get lines from file or return null if file not found
 *
 * @param filePath path to file
 * @return [List] of file lines
 */
fun getFileLines(filePath: String): List<String>? {
    return try {
        val fileLines = File(filePath).readLines()
        fileLines
    } catch (e: FileNotFoundException) {
        println("File $filePath not found")
        null
    }
}

/**
 * Generate html file from two files diff information
 *
 * @param originalFileName original File name
 * @param revisedFileName revised File name
 * @param originalFileLines [List] of lines from original file
 * @param revisedFileLines [List]  of lines from revised file
 * @param originalFileRows [List] of [Pair]: line from original file to it's delta information
 * @param revisedFileRows [List] of [Pair]: line from revised file to it's delta information
 */
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

/**
 * Compare files
 *
 * @param originalFilePath path to original file
 * @param revisedFilePath path to revised file
 * @return pair of Lists containing information calculated by diff util
 */
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
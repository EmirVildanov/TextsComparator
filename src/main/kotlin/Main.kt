import dev.gitlive.difflib.DiffUtils
import dev.gitlive.difflib.patch.AbstractDelta
import dev.gitlive.difflib.patch.DeltaType
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.*
import java.lang.Integer.min

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
        }
    }
    return rowsList
}

fun DIV.diffTable(
    tableName: String,
    original: List<String>,
    revised: List<String>
) {
    table {
        caption { +tableName }
        val diffResult = DiffUtils.diff(original, revised).getDeltas()
        val rowsList: MutableList<Pair<String, DeltaType?>>
        try {
            rowsList = getDiffRowsList(original, diffResult)
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

fun TABLE.legendTableCell(text: String, classes_arg: Set<String>? = null) {
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

fun DIV.legendTable() {
    table {
        classes = setOf("legend")
        caption { +"Legend" }
        legendTableCell("Changed", setOf("rect", "diffChanged"))
        legendTableCell("Deleted", setOf("rect", "diffDeleted"))
        legendTableCell("Added", setOf("rect", "diffAdded"))
    }
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

fun compare(firstFilePath: String, secondFilePath: String) {
    val firstFileLines = getFileLines(firstFilePath)
    val secondFileLines = getFileLines(secondFilePath)
    if (firstFileLines == null || secondFileLines == null) {
        return
    }
    val resultFileStream = PrintStream(File("result.html"))
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
                val firstFileName = firstFilePath.split('/').last()
                val secondFileName = secondFilePath.split('/').last()
                originalTable("Original $firstFileName", firstFileLines)
                originalTable("Original $secondFileName", secondFileLines)
                diffTable(firstFileName, firstFileLines, secondFileLines)
                diffTable(secondFileName, secondFileLines, firstFileLines)
                legendTable()
            }
        }
    }
    System.setOut(console)
}

fun main(args: Array<String>) {
    compare("/home/emir/Desktop/Programming/TextsComparator/src/main/resources/original.txt", "/home/emir/Desktop/Programming/TextsComparator/src/main/resources/revised.txt")
    compare("t1.txt", "t2.txt")
    when {
        args.size == 2 -> {
            compare(args[0], args[1])
        }
        args.size > 2 -> {
            print("Too many arguments. Syntax: compare.py [FIRST_FILE_PATH] [SECOND_FILE_PATH]")
        }
        else -> {
            print("Specify the path to the files")
        }
    }
}
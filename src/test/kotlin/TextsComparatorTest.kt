import dev.gitlive.difflib.patch.DeltaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ComputerInfoTest {
    private val resourcesPath = "./src/test/resources/"

    @Test
    fun shouldReturnNullBecauseOfNonExistingFile() {
        val lines = getFileLines(fileName = resourcesPath + "testFileThatDoNotExist.txt")
        assertNull(lines)
    }

    @Test
    fun shouldReadLinesOfTestFile() {
        val lines = getFileLines(fileName = resourcesPath + "testToRead.txt")
        assertArrayEquals(arrayOf("line one", "line two", "line three"), lines!!.toTypedArray())
    }

    @Test
    fun shouldCompareTwoFiles() {
        val originalFilePath = resourcesPath + "testFileToCompare1.txt"
        val revisedFilePath = resourcesPath + "testFileToCompare2.txt"
        val diffPair = compareFiles(originalFilePath, revisedFilePath)
        val firstDiffDeltaTypes = diffPair!!.first.map { it -> it.second }
        val assertionArray = arrayOf(
            null,
            DeltaType.CHANGE,
            null,
            DeltaType.DELETE,
            null,
            DeltaType.CHANGE,
            null,
            DeltaType.INSERT,
            DeltaType.INSERT
        )
        assertArrayEquals(assertionArray, firstDiffDeltaTypes.toTypedArray())
    }

}
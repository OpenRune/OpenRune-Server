package dtx.util
public class SparseMatrix<T : Any>(
    public val rows: Int,
    public val columns: Int,
    public val defaultValue: T,
    public val returnDefaultOnOob: Boolean = false,
) {
    private val nonDefaultValues: MutableList<T> = mutableListOf()

    private val columnIndices: MutableList<Int> = mutableListOf()

    private val rowPointers: MutableList<Int> = MutableList(rows + 1) { 0 }

    private var nonDefaultCount = 0

    init {

        require(rows > 0 && columns > 0) {
            "Matrix dimensions must be at least 1x1 (passed ${rows}x$columns)."
        }
    }

    private fun boundCheck(row: Int, col: Int) = row < 0 || row >= rows || col < 0 || col >= columns

    private fun colIndexOf(row: Int, col: Int): Int {

        val rowStart = rowPointers[row]
        val rowEnd = rowPointers[row + 1]

        return columnIndices.binarySearch(col, fromIndex = rowStart, toIndex = rowEnd)
    }

    public operator fun get(row: Int, col: Int): T {

        val isOob = boundCheck(row, col)

        if (isOob) {

            if (returnDefaultOnOob) {
                return defaultValue
            }

            throw error("Out of Bounds for [$row:$col] (matrix size [$rows:$columns]")
        }

        val index = colIndexOf(row, col)

        return if (index >= 0) {
            nonDefaultValues[index]
        } else {
            defaultValue
        }
    }

    public operator fun set(row: Int, col: Int, value: T) {

        val isOob = boundCheck(row, col)

        if (isOob) {

            if (returnDefaultOnOob) {
                return
            }

            throw error("Out of Bounds for [$row:$col] (matrix size [$rows:$columns]")
        }

        val index = colIndexOf(row, col)
        val isDefaultValue = (value == defaultValue)

        if (index >= 0) {

            if (isDefaultValue) {
                removeElementAt(index, row)
            } else {
                nonDefaultValues[index] = value
            }
        } else {

            if (!isDefaultValue) {

                val insertionPoint = -(index + 1)
                insertAt(insertionPoint, row, col, value)
            }
        }
    }

    private fun insertAt(index: Int, row: Int, col: Int, value: T) {

        if (value == defaultValue) {
            return
        }

        nonDefaultValues.add(index, value)
        columnIndices.add(index, col)
        nonDefaultCount++

        for (r in (row + 1)..rows) {
            rowPointers[r]++
        }
    }

    private fun removeElementAt(index: Int, row: Int) {

        nonDefaultValues.removeAt(index)
        columnIndices.removeAt(index)
        nonDefaultCount--

        for (r in (row + 1)..rows) {
            rowPointers[r]--
        }
    }

    public fun nonDefaults(): Iterator<Triple<Int, Int, T>> {

        val elements = mutableListOf<Triple<Int, Int, T>>()
        var currentRow = 0

        for (i in 0 until nonDefaultCount) {

            while (rowPointers[currentRow + 1] <= i) {
                currentRow++
            }

            elements.add(Triple(currentRow, columnIndices[i], nonDefaultValues[i]))
        }

        return elements.iterator()
    }


    public fun nonDefaultString(): String = buildString {

        append("nondefaults (row, col, value):\n")

        nonDefaults().forEach { triple ->
            append("  (${triple.first}, ${triple.second}) -> ${triple.third}\n")
        }
    }


    override fun toString(): String = buildString {

        append("Matrix: \n")

        for (rowIndex in 0 until rows) {
            append("[ ")

            for (colIndex in 0 until columns) {
                append(get(rowIndex, colIndex).toString().padEnd(5))

                if (colIndex < columns - 1) {
                    append(", ")
                }
            }

            append(" ]\n")
        }

        return toString()
    }

    public fun copy(defaultValue: T = this.defaultValue): SparseMatrix<T> {

        val matrix = SparseMatrix<T>(rows, columns, defaultValue)

        nonDefaults().forEach { (row, column, nonDefaultValue) ->
            matrix[row, column] = nonDefaultValue
        }

        return matrix
    }
}

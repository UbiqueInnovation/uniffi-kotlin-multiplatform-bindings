import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.asTomlArray
import net.peanuuutz.tomlkt.asTomlTable
import java.io.File

data class BindgenInfo(
    val name: String,
    val version: String,
    val binName: String,
)

fun parseBindgenCargoToml(file: File): BindgenInfo {
    val tomlString = file.readText()
    val tomlTable = Toml.parseToTomlTable(tomlString)

    val packageData = tomlTable["package"]!!.asTomlTable()
    val allBinData = tomlTable["bin"]!!.asTomlArray()
    val binData = allBinData[0].asTomlTable()

    return BindgenInfo(
        name = packageData["name"].toString(),
        version = packageData["version"].toString(),
        binName = binData["name"].toString(),
    )
}

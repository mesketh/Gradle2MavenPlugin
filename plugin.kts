import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.ide.CopyPasteManager
import liveplugin.*
import java.awt.datatransfer.DataFlavor
import java.util.*
import java.util.regex.Pattern

/**
 * Jetbrains implementing this after 5yrs...
 * https://youtrack.jetbrains.com/issue/IDEA-188753/Copy-action-on-Gradle-Maven-dependency-in-project-view-should-copy-artifact-definition
 */

if (!isIdeStartup) show("Loaded gradle 2 maven dependency copier... ^ ⌥ ⬆️G to run it")

registerAction(
    id = "Paste maven dependency",
    keyStroke = "ctrl alt shift G"
) { event: AnActionEvent ->
    if (validatePreconditions(event)) {

        val contents = CopyPasteManager.getInstance()
            .getContents<String>(DataFlavor.stringFlavor)

        if (contents != null) {
            show(String.format("Contents: %s", contents))

            if (isValidGradleDependency(contents)) {
                val mvnDeps = captureMavenDependencies()

                executeCommand(project) {
                    val caretModel = event.editor?.caretModel
                    val lineStartOffset =
                        event.document!!.getLineStartOffset(caretModel!!.logicalPosition.line)
                    val caretOffset = event.editor!!.caretModel.offset
                    event.document?.insertString(caretOffset, mvnDeps)

                    caretModel.moveToOffset(caretModel.offset + 1)
                    PluginUtil.log("copied dependency $mvnDeps")
                }

            } else {
                PluginUtil.log(
                    "No gradle dependency found!",
                    com.intellij.notification.NotificationType.WARNING
                )
            }

        }

    } else {
        show("Pasting gradle dependencies requires an open pom.xml file")
    }
}

fun validatePreconditions(event: AnActionEvent): Boolean {
    return (event.project != null && event.editor != null && event.document != null)
}

fun captureMavenDependencies(): CharSequence {

    val regex = "\\s+(?<type>\\w+)\\s+'(?<dep>[A-Za-z0-9-:.]+)'+"
    val mavenDepTemplate =
        "<dependency>\n<groupId>%1%</groupId>\n<artifactId>%2%</artifactId>\n<scope>%3%</scope>\n</dependency>\n"

    val gradleDepCopy = CopyPasteManager.getInstance()
        .getContents<String>(DataFlavor.stringFlavor)

    val pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.MULTILINE)
    val matcher = pattern.matcher(gradleDepCopy!!)

    val mavenBuilder = StringBuilder()

    while (matcher.find()) {
        val type = matcher.group("type")
        val dependency = matcher.group("dep")

        val tokenizer = StringTokenizer(dependency, ":")

        var mvnDep = mavenDepTemplate.replace("%1%", tokenizer.nextToken())
        mvnDep = mvnDep.replace("%2%", tokenizer.nextToken())
        var scope = "compile"
        when (type) {
            "api", "implementation" -> scope = "runtime"
            "testImplementation",
            "testCompileOnly",
            "testCompileClasspath",
            "testRuntimeClasspath" -> {
                scope = "test"
            }
        }
        mavenBuilder.append(mvnDep.replace("%3%", scope))

        PluginUtil.log("maven dependency added $mvnDep")
    }

    if (mavenBuilder.isNotEmpty())
        return mavenBuilder.toString()
    else
        return mavenDepTemplate

}

fun isValidGradleDependency(contents: String): Boolean {
    var checkPattern = Pattern.compile("(\\s*(\\w+)\\s+'([A-Za-z0-9-:.]+)')+")
    return contents.matches(checkPattern.toRegex())
}

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.ide.CopyPasteManager
import liveplugin.*
import java.awt.datatransfer.DataFlavor
import java.util.*
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

/**
 * Jetbrains implementing this after 5yrs...
 * https://youtrack.jetbrains.com/issue/IDEA-188753/Copy-action-on-Gradle-Maven-dependency-in-project-view-should-copy-artifact-definition
 */

if (!isIdeStartup) show("Loaded gradle 2 maven dependency paster... ^ ⌥ ⬆️G to run it")

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

                PluginUtil.log("mvn deps captured: $mvnDeps")

                ApplicationManager.getApplication().runWriteAction {
                    executeCommand(project) {
                        val caretModel = event.editor?.caretModel
                        val caretOffset = event.editor!!.caretModel.offset
                        event.document?.insertString(caretOffset, mvnDeps)
                        caretModel!!.moveToOffset(caretModel.offset + 1)
                        PluginUtil.log("copied dependency $mvnDeps")
                    }
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

    val regex = "\\s*(?<type>\\w+)\\s+'(?<dep>[A-Za-z0-9-:.]+)'"
    val mavenDepTemplate =
        "<dependency>\n<groupId>%1%</groupId>\n<artifactId>%2%</artifactId>\n<version>%3%</version>\n<scope>%4%</scope>\n</dependency>\n"

    val gradleDepCopy = CopyPasteManager.getInstance()
        .getContents<String>(DataFlavor.stringFlavor)

    val pattern = compile(regex, MULTILINE)
    val matcher = pattern.matcher(gradleDepCopy!!)

    val mavenBuilder = StringBuilder()

    while (matcher.find()) {
        val type = matcher.group("type")
        val dependency = matcher.group("dep")

        val tokenizer = StringTokenizer(dependency, ":")

        var mvnDep = mavenDepTemplate.replace("%1%", tokenizer.nextToken())
        mvnDep = mvnDep.replace("%2%", tokenizer.nextToken())
        mvnDep = mvnDep.replace("%3%", tokenizer.nextToken())

        PluginUtil.log("mvn dep = $mvnDep")

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
        mavenBuilder.append(mvnDep.replace("%4%", scope))

        PluginUtil.log("maven dependency found: $mvnDep")
    }

    if (mavenBuilder.isNotEmpty())
        return mavenBuilder.toString()
    else
        return mavenDepTemplate

}

fun isValidGradleDependency(contents: String): Boolean {
    val checkPattern = "\\s*(\\w+)\\s+([\"|'][A-Za-z0-9-:.]+[\"|'])"
    return checkPattern.toRegex(RegexOption.MULTILINE).containsMatchIn(contents)
}

//import liveplugin.PluginUtil.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import liveplugin.PluginUtil.show
import liveplugin.document
import liveplugin.editor
import liveplugin.registerAction
import java.awt.datatransfer.DataFlavor

/**
 * Jetbrains implementing this after 5yrs...
 * https://youtrack.jetbrains.com/issue/IDEA-188753/Copy-action-on-Gradle-Maven-dependency-in-project-view-should-copy-artifact-definition
 */

/**
 * Rules:
 * 1. Only show action popup when clipboard has valid gradle deps to translate
 */

registerAction(
    id = "Paste maven dependency",
    keyStroke = "ctrl alt shift G"
) { event: AnActionEvent ->
    if (validatePreconditions(event)) {

        show("Preconditions are valid")

        val contents = CopyPasteManager.getInstance()
            .getContents<String>(DataFlavor.stringFlavor)

        if (contents != null) {
            show(String.format("Contents: %s", contents))
            // TODO - check contents against Regex to ensure valid groovy/gradle dependencies

        }

//        val project =
//            event.project ?: return@registerAction // Can be null if there are no open projects.
//        val editor = event.editor
//            ?: return@registerAction // Can be null if focus is not in the editor or no editors are open.

        // validate the

        show("TODO: translate + paste to current project editor doc", "", NotificationType.WARNING)
        // Document modifications must be done inside "commands" which will support undo/redo functionality.
//        event.editor!!.document.executeCommand(event.project!!, description = "Paste dependency") {
//            insertString(event.editor!!.caretModel.offset, translateDependency(event))
//        }
    } else {
        // TODO: disable action - log event to tell user of issue
    }
}

//class Gradle2MavenDependencyTranslator : CopyPastePostProcessor<TextBlockTransferableData>() {
//    override fun collectTransferableData(
//        psiFile: PsiFile,
//        editor: Editor,
//        p2: IntArray?,
//        p3: IntArray?
//    ): MutableList<TextBlockTransferableData> {
//
//    }
//
//}

fun validatePreconditions(event: AnActionEvent): Boolean {
    return (event.project != null && event.editor != null && event.document != null)
}

fun translateDependency(event: AnActionEvent): CharSequence {
    TODO("Not yet implemented")
}

if (!isIdeStartup) show("Loaded 'TODO' <br/>Use ctrl+alt+shift+P to run it")


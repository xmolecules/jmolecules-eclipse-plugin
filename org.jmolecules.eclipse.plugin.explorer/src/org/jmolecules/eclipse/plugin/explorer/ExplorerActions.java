package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.ui.ISharedImages.IMG_ELCL_COLLAPSEALL;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

class ExplorerActions {

    private final ImageProvider imageProvider;

    ExplorerActions(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    Action collapseAllAction(TreeViewer treeViewer) {
        Action action = new Action("Collapse All") {

            public void run() {
                treeViewer.collapseAll();
            }
        };

        action.setToolTipText("Collapse All");
        imageProvider.getImageDescriptor(IMG_ELCL_COLLAPSEALL).ifPresent(action::setImageDescriptor);

        return action;
    }

    Action showInEditorAction(IJavaElement source) {
        Action action = new Action("Show in Editor") {

            public void run() {
                try {
                    JavaUI.openInEditor(source, false, true);
                } catch (PartInitException | JavaModelException e) {
                    // FIXME should we not propagate the exception and only log debug?
                    throw new RuntimeException(e);
                }
            }
        };

        action.setToolTipText("Show in Editor");
        if (!ISourceReference.class.isInstance(source)) {
            action.setEnabled(false);
        }

        return action;
    }
}

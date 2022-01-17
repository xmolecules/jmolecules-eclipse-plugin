/*-
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

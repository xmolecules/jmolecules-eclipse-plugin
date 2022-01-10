package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.ui.ISharedImages.IMG_ELCL_COLLAPSEALL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

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
}

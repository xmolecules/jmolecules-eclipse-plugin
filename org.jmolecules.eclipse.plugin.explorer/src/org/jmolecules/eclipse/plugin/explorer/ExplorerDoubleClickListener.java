package org.jmolecules.eclipse.plugin.explorer;

import static java.util.Optional.of;

import java.util.Optional;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

class ExplorerDoubleClickListener implements IDoubleClickListener {

	@Override
	public void doubleClick(DoubleClickEvent event) {
		TreeViewer treeViewer = (TreeViewer) event.getViewer();

		Optional<TreeNode> treeNode = of(event) //
				.map(e -> e.getSelection()) //
				.filter(IStructuredSelection.class::isInstance) //
				.map(IStructuredSelection.class::cast) //
				.filter(s -> s.size() == 1) //
				.map(s -> s.getFirstElement()) //
				.filter(TreeNode.class::isInstance) //
				.map(TreeNode.class::cast);

		treeNode.ifPresent(n -> doubleClick(treeViewer, n));
	}

	private static void doubleClick(TreeViewer treeViewer, TreeNode treeNode) {
		IJavaElement source = treeNode.getSource();
		if (source instanceof ISourceReference) {
			tryToOpen(source);
		} else {
			if (treeNode.hasChildren()) {
				treeViewer.setExpandedState(treeNode, !treeViewer.getExpandedState(treeNode));
			}
		}
	}

	private static void tryToOpen(IJavaElement source) {
		try {
			JavaUI.openInEditor(source, false, true);
		} catch (PartInitException | JavaModelException e) {
			// FIXME should we not propagate the exception and only log debug?
			throw new RuntimeException(e);
		}
	}
}

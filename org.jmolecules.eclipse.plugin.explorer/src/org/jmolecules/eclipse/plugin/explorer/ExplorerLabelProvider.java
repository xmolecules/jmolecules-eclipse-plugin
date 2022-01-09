package org.jmolecules.eclipse.plugin.explorer;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept;

class ExplorerLabelProvider extends LabelProvider {

	private final ImageProvider imageProvider;

	ExplorerLabelProvider(ImageProvider imageProvider) {
		this.imageProvider = imageProvider;
	}

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof TreeNode)) {
			return null;
		}

		TreeNode treeNode = (TreeNode) element;
		return imageProvider.get(treeNode).orElse(null);
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof TreeNode)) {
			return null;
		}

		TreeNode treeNode = (TreeNode) element;
		return getText(treeNode);
	}

	private static String getText(TreeNode treeNode) {
		IJavaElement source = treeNode.getSource();

		StringBuilder sb = new StringBuilder();
		if (source instanceof IPackageFragmentRoot) {
			sb.append(toString((IPackageFragmentRoot) source));
		} else if (source instanceof IPackageFragment) {
			sb.append(toString((IPackageFragment) source));
		} else {
			sb.append(source.getElementName());
		}

		if (treeNode.hasConcepts()) {
			sb.append(" ").append(toString(treeNode.getConcepts()));
		}
		return sb.toString();
	}

	private static String toString(IPackageFragmentRoot source) {
		IPath path = source.getPath();

		StringBuilder sb = new StringBuilder();
		if (source.getJavaProject().getElementName().equals(path.segment(0))) {
			if (path.segmentCount() == 1) {
				sb.append("<project root>");
			} else {
				sb.append(path.removeFirstSegments(1).makeRelative());
			}
		} else {
			sb.append(path);
		}

		return sb.toString();
	}

	private static String toString(IPackageFragment source) {
		String name = source.getElementName();
		name = name.isEmpty() ? "(default package)" : name;
		return name;
	}

	private static String toString(List<Concept> concepts) {
		return concepts.stream() //
				.sorted() //
				.map(c -> c.getName()) //
				.collect(joining(", ", "<", ">"));
	}
}

package org.jmolecules.eclipse.plugin.explorer;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

class ExplorerContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        List<TreeNode> children = null;
        if (parentElement instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) parentElement;
            children = treeNode.getChildren();
        }

        return ArrayContentProvider.getInstance().getElements(children);
    }

    @Override
    public Object getParent(Object element) {
        if (!(element instanceof TreeNode)) {
            return null;
        }

        TreeNode child = (TreeNode) element;
        return child.getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        if (!(element instanceof TreeNode)) {
            return false;
        }

        return ((TreeNode) element).hasChildren();
    }
}

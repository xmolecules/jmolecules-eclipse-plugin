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

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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

class ExplorerComparator extends ViewerComparator {

    private final JavaElementComparator delegate;

    ExplorerComparator() {
        delegate = new JavaElementComparator(true);
    }

    @Override
    public int category(Object element) {
        return ofNullable(element) //
            .filter(Objects::nonNull) //
            .filter(TreeNode.class::isInstance) //
            .map(TreeNode.class::cast) //
            .map(n -> delegate.category(n.getSource())) //
            .orElse(0);
    }

    @Override
    public boolean isSorterProperty(Object element, String property) {
        return ofNullable(element) //
            .filter(Objects::nonNull) //
            .filter(TreeNode.class::isInstance) //
            .map(TreeNode.class::cast) //
            .map(n -> delegate.isSorterProperty(n.getSource(), property)) //
            .orElse(false);
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof TreeNode) {
            e1 = ((TreeNode) e1).getSource();
        }
        if (e2 instanceof TreeNode) {
            e2 = ((TreeNode) e2).getSource();
        }
        return delegate.compare(viewer, e1, e2);
    }

    @Override
    public void sort(Viewer viewer, Object[] elements) {
        IJavaElement[] sources = Arrays.stream(elements) //
            .filter(TreeNode.class::isInstance) //
            .map(TreeNode.class::cast) //
            .map(n -> n.getSource()) //
            .collect(Collectors.toList()) //
            .toArray(new IJavaElement[0]);

        delegate.sort(viewer, sources);
        Arrays.sort(elements, treeNodeBySourceComparator(asList(sources)));
    }

    private static Comparator<Object> treeNodeBySourceComparator(List<IJavaElement> sources) {
        return (a, b) -> {
            IJavaElement aSource = ((TreeNode) a).getSource();
            IJavaElement bSource = ((TreeNode) b).getSource();
            return sources.indexOf(aSource) - sources.indexOf(bSource);
        };
    }
}

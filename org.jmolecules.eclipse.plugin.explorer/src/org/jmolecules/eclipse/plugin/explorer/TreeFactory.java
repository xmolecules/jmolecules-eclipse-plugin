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

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getChildren;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getFields;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getMethods;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isPackageInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.jmolecules.eclipse.plugin.explorer.JMolecules.Concepts;

class TreeFactory {

    private final JMolecules jMolecules;

    TreeFactory(JMolecules jMolecules) {
        this.jMolecules = jMolecules;
    }

    TreeNode create(IJavaProject project) {
        List<TreeNode> children = stream(getChildren(project)) //
            .filter(JavaModelUtils::isSourcePackageFragmentRoot) //
            .map(IPackageFragmentRoot.class::cast) //
            .map(this::treeNode) //
            .flatMap(Optional::stream) //
            .collect(toList());

        TreeNode root = new TreeNode(children, project);
        return new TreeNode(List.of(root));
    }

    private Optional<TreeNode> treeNode(IPackageFragmentRoot source) {
        List<TreeNode> children = stream(getChildren(source)) //
            .filter(IPackageFragment.class::isInstance) //
            .map(IPackageFragment.class::cast) //
            .map(this::treeNode) //
            .flatMap(Optional::stream) //
            .collect(toList());

        return createIf(children, source, Concepts.empty());
    }

    private Optional<TreeNode> treeNode(IPackageFragment source) {
        IJavaElement[] sourceChildren = getChildren(source);

        List<TreeNode> children = stream(sourceChildren) //
            .filter(ICompilationUnit.class::isInstance) //
            .map(ICompilationUnit.class::cast) //
            .filter(u -> !isPackageInfo(u)) //
            .map(this::treeNode) //
            .flatMap(Optional::stream) //
            .collect(toList());

        Optional<IPackageDeclaration> declaration = stream(sourceChildren) //
            .filter(ICompilationUnit.class::isInstance) //
            .map(ICompilationUnit.class::cast) //
            .filter(JavaModelUtils::isPackageInfo) //
            .limit(1) //
            .flatMap(u -> stream(getChildren(u))) //
            .filter(IPackageDeclaration.class::isInstance) //
            .map(IPackageDeclaration.class::cast) //
            .limit(1) //
            .findAny();

        return createIf(children, source, declaration.map(jMolecules::expresses).orElse(Concepts.empty()));
    }

    private Optional<TreeNode> treeNode(ICompilationUnit source) {
        List<TreeNode> children = stream(getChildren(source)) //
            .filter(IType.class::isInstance) //
            .map(IType.class::cast) //
            .map(this::treeNode) //
            .flatMap(Optional::stream) //
            .collect(toList());

        return createIf(children, source, Concepts.empty());
    }

    private Optional<TreeNode> treeNode(IType source) {
        Stream<TreeNode> sourceChildren = stream(getChildren(source)) //
            .filter(IType.class::isInstance) //
            .map(IType.class::cast) //
            .map(this::treeNode) //
            .flatMap(Optional::stream);

        Stream<TreeNode> fields = stream(getFields(source)) //
            .map(this::treeNode) //
            .flatMap(Optional::stream);

        Stream<TreeNode> methods = stream(getMethods(source)) //
            .map(this::treeNode) //
            .flatMap(Optional::stream);

        List<TreeNode> children = concat(sourceChildren, concat(fields, methods)).collect(toList());
        Concepts concepts = jMolecules.expresses(source);

        return createIf(children, source, concepts);
    }

    private Optional<TreeNode> treeNode(IField source) {
        return createIf(emptyList(), source, jMolecules.expresses(source));
    }

    private Optional<TreeNode> treeNode(IMethod source) {
        return createIf(emptyList(), source, jMolecules.expresses(source));
    }

    private static Optional<TreeNode> createIf(List<TreeNode> children, IJavaElement source, Concepts concepts) {
        if (children.isEmpty() && concepts.isEmpty()) {
            return empty();
        }
        return of(new TreeNode(children, source, concepts));
    }
}

class TreeNode {

    private TreeNode parent;
    private final List<TreeNode> children;

    private final IJavaElement source;
    private final Concepts concepts;

    TreeNode(List<TreeNode> children) {
        this(children, null, null);
    }

    TreeNode(List<TreeNode> children, IJavaElement source) {
        this(children, source, null);
    }

    TreeNode(List<TreeNode> children, IJavaElement source, Concepts concepts) {
        children.forEach(c -> c.parent = this);
        this.children = children != null ? children : new ArrayList<>();
        this.source = source;
        this.concepts = concepts != null ? concepts : Concepts.empty();
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this, new String[] { "parent" });
    }

    @Override
    public boolean equals(Object obj) {
        return reflectionEquals(this, obj, new String[] { "parent" });
    }

    Optional<TreeNode> findNode(IJavaElement source) {
        if (source.equals(this.source)) {
            return Optional.of(this);
        } else {
            return children.stream().map(n -> n.findNode(source)).flatMap(Optional::stream).limit(1).findAny();
        }
    }

    IJavaElement getSource() {
        return source;
    }

    Concepts getConcepts() {
        return concepts;
    }

    Concepts collectConcepts() {
        return concepts.merge(children.stream().map(TreeNode::collectConcepts).collect(toSet()).toArray(new Concepts[0]));
    }

    List<TreeNode> getChildren() {
        return unmodifiableList(children);
    }

    boolean hasChildren() {
        return !children.isEmpty();
    }

    TreeNode getParent() {
        return parent;
    }

    boolean hasParent() {
        return parent != null;
    }
}

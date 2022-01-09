package org.jmolecules.eclipse.plugin.explorer;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getChildren;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getFields;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getMethods;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isPackageInfo;

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
import org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept;

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

		TreeNode root = new TreeNode(project, emptyList(), children);
		return new TreeNode(null, emptyList(), List.of(root));
	}

	private Optional<TreeNode> treeNode(IPackageFragmentRoot source) {
		List<TreeNode> children = stream(getChildren(source)) //
				.filter(IPackageFragment.class::isInstance) //
				.map(IPackageFragment.class::cast) //
				.map(this::treeNode) //
				.flatMap(Optional::stream) //
				.collect(toList());

		return createIf(source, emptyList(), children);
	}

	private Optional<TreeNode> treeNode(IPackageFragment source) {
		IJavaElement[] sourceChildren = getChildren(source);

		List<Concept> concepts = stream(sourceChildren) //
				.filter(ICompilationUnit.class::isInstance) //
				.map(ICompilationUnit.class::cast) //
				.filter(JavaModelUtils::isPackageInfo) //
				.limit(1) //
				.flatMap(u -> stream(getChildren(u))) //
				.filter(IPackageDeclaration.class::isInstance) //
				.map(IPackageDeclaration.class::cast) //
				.limit(1) //
				.map(u -> jMolecules.expresses(u)) //
				.findAny() //
				.orElse(emptyList());

		List<TreeNode> children = stream(sourceChildren) //
				.filter(ICompilationUnit.class::isInstance) //
				.map(ICompilationUnit.class::cast) //
				.filter(u -> !isPackageInfo(u)) //
				.map(this::treeNode) //
				.flatMap(Optional::stream) //
				.collect(toList());

		return createIf(source, concepts, children);
	}

	private Optional<TreeNode> treeNode(ICompilationUnit source) {
		List<TreeNode> children = stream(getChildren(source)) //
				.filter(IType.class::isInstance) //
				.map(IType.class::cast) //
				.map(this::treeNode) //
				.flatMap(Optional::stream) //
				.collect(toList());

		return createIf(source, emptyList(), children);
	}

	private Optional<TreeNode> treeNode(IType source) {
		List<Concept> concepts = jMolecules.expresses(source);

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
		return createIf(source, concepts, children);
	}

	private Optional<TreeNode> treeNode(IField source) {
		return createIf(source, jMolecules.expresses(source), emptyList());
	}

	private Optional<TreeNode> treeNode(IMethod source) {
		return createIf(source, jMolecules.expresses(source), emptyList());
	}

	private static Optional<TreeNode> createIf(IJavaElement source, List<Concept> concepts, List<TreeNode> children) {
		if (concepts.isEmpty() && children.isEmpty()) {
			return empty();
		}
		return of(new TreeNode(source, concepts, children));
	}
}

class TreeNode {

	private final IJavaElement source;
	private final List<Concept> concepts;
	private final List<TreeNode> children;
	private TreeNode parent;

	TreeNode(IJavaElement source, List<Concept> concepts, List<TreeNode> children) {
		this.source = source;
		this.concepts = concepts;
		this.children = children;
		children.forEach(c -> c.parent = this);
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

	List<Concept> getConcepts() {
		return unmodifiableList(concepts);
	}

	boolean hasConcepts() {
		return !concepts.isEmpty();
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

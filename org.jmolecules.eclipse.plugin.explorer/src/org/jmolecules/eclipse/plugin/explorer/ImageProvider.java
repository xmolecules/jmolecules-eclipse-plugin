package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.jdt.core.Flags.isPackageDefault;
import static org.eclipse.jdt.core.Flags.isPrivate;
import static org.eclipse.jdt.core.Flags.isProtected;
import static org.eclipse.jdt.core.Flags.isPublic;
import static org.eclipse.jdt.core.IJavaElement.ANNOTATION;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_DEFAULT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PRIVATE;
import static org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PROTECTED;
import static org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PUBLIC;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ANNOTATION;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS_DEFAULT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CUNIT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_DEFAULT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ENUM;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_INTERFACE;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKFRAG_ROOT;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PRIVATE;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PROTECTED;
import static org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC;
import static org.eclipse.ui.ISharedImages.IMG_OBJ_PROJECT;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getFlags;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isAnnotation;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isClass;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isEnum;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

class ImageProvider {
	private final Map<String, Image> images = new HashMap<>();
	private final List<Function<String, Image>> imageSuppliers;

	ImageProvider() {
		imageSuppliers = List.of( //
				(n) -> JavaUI.getSharedImages().getImage(n), //
				(n) -> PlatformUI.getWorkbench().getSharedImages().getImage(n) //
		);
	}

	Optional<Image> get(TreeNode treeNode) {
		return new ImageNameBuilder(treeNode).build().map(n -> getOrCreate(n));
	}

	void dispose() {
		images.clear();
	}

	private Image getOrCreate(String imageName) {
		Image image = images.get(imageName);

		if (image == null) {
			image = obtainImage(imageName);
			if (image != null) {
				images.put(imageName, image);
			}
		}

		return image;
	}

	private Image obtainImage(String imageName) {
		return imageSuppliers //
				.stream() //
				.map(s -> s.apply(imageName)) //
				.filter(Objects::nonNull) //
				.limit(1) //
				.findAny() //
				.orElse(null);
	}

	static class ImageNameBuilder {

		private final List<Function<TreeNode, String>> mappings = init();
		private final TreeNode treeNode;

		ImageNameBuilder(TreeNode treeNode) {
			this.treeNode = treeNode;
		}

		Optional<String> build() {
			return mappings.stream().map(f -> f.apply(treeNode)).filter(Objects::nonNull).limit(1).findAny();
		}

		private static List<Function<TreeNode, String>> init() {
			List<Function<TreeNode, String>> mappings = new ArrayList<>();
			mappings.add(projectMapping());
			mappings.add(sourceFolderMapping());
			mappings.add(packageMapping());
			mappings.add(compilationUnitMapping());
			mappings.add(annotationMapping());
			mappings.add(interfaceMapping());
			mappings.add(classMapping());
			mappings.add(enumMapping());
			mappings.add(fieldMapping());
			mappings.add(methodMapping());
			return mappings;
		}

		@SuppressWarnings("deprecation")
		private static Function<TreeNode, String> projectMapping() {
			return n -> n.getSource().getElementType() == IJavaElement.JAVA_PROJECT ? IMG_OBJ_PROJECT : null;
		}

		private static Function<TreeNode, String> sourceFolderMapping() {
			return n -> n.getSource().getElementType() == PACKAGE_FRAGMENT_ROOT ? IMG_OBJS_PACKFRAG_ROOT : null;
		}

		private static Function<TreeNode, String> packageMapping() {
			return n -> n.getSource().getElementType() == PACKAGE_FRAGMENT ? IMG_OBJS_PACKAGE : null;
		}

		private static Function<TreeNode, String> compilationUnitMapping() {
			return n -> n.getSource().getElementType() == COMPILATION_UNIT ? IMG_OBJS_CUNIT : null;
		}

		private static Function<TreeNode, String> annotationMapping() {
			return n -> {
				if (n.getSource().getElementType() == ANNOTATION) {
					return IMG_OBJS_ANNOTATION;
				}

				IJavaElement source = n.getSource();
				if (source instanceof IType) {
					IType type = (IType) source;
					return isAnnotation(type) ? IMG_OBJS_ANNOTATION : null;
				}

				return null;
			};
		}

		private static Function<TreeNode, String> interfaceMapping() {
			return n -> {
				IJavaElement source = n.getSource();
				if (!(source instanceof IType)) {
					return null;
				}
				IType type = (IType) source;
				return isInterface(type) ? IMG_OBJS_INTERFACE : null;
			};
		}

		private static Function<TreeNode, String> classMapping() {
			return n -> {
				IJavaElement source = n.getSource();
				if (!(source instanceof IType)) {
					return null;
				}

				IType type = (IType) source;
				if (!isClass(type)) {
					return null;
				}

				return isPackageDefault(getFlags(type)) ? IMG_OBJS_CLASS_DEFAULT : IMG_OBJS_CLASS;
			};
		}

		private static Function<TreeNode, String> enumMapping() {
			return n -> {
				IJavaElement source = n.getSource();
				if (!(source instanceof IType)) {
					return null;
				}

				IType type = (IType) source;
				return isEnum(type) ? IMG_OBJS_ENUM : null;
			};
		}

		private static Function<TreeNode, String> fieldMapping() {
			return n -> {
				IJavaElement source = n.getSource();
				if (!(source instanceof IField)) {
					return null;
				}

				IField field = (IField) source;
				Integer flags = getFlags(field);

				if (isPrivate(flags)) {
					return IMG_FIELD_PRIVATE;
				} else if (isProtected(flags)) {
					return IMG_FIELD_PROTECTED;
				} else if (isPackageDefault(flags)) {
					return IMG_FIELD_DEFAULT;
				} else if (isPublic(flags)) {
					return IMG_FIELD_PUBLIC;
				}
				return null;
			};
		}

		private static Function<TreeNode, String> methodMapping() {
			return n -> {
				IJavaElement source = n.getSource();
				if (!(source instanceof IMethod)) {
					return null;
				}

				IMethod method = (IMethod) source;
				Integer flags = getFlags(method);

				if (isPrivate(flags)) {
					return IMG_OBJS_PRIVATE;
				} else if (isProtected(flags)) {
					return IMG_OBJS_PROTECTED;
				} else if (isPackageDefault(flags)) {
					return IMG_OBJS_DEFAULT;
				} else if (isPublic(flags)) {
					return IMG_OBJS_PUBLIC;
				}
				return null;
			};
		}
	}
}

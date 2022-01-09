package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.jdt.core.IPackageFragmentRoot.K_SOURCE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

final class JavaModelUtils {

	static final Object PACKAGE_INFO_FILENAME = "package-info.java";

	private JavaModelUtils() {
	}

	static Integer getKind(IPackageFragmentRoot model) {
		try {
			return model.getKind();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IJavaElement[] getChildren(IParent model) {
		try {
			return model.getChildren();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IAnnotation[] getAnnotations(IAnnotatable model) {
		try {
			return model.getAnnotations();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IPackageDeclaration[] getPackageDeclarations(ICompilationUnit model) {
		try {
			return model.getPackageDeclarations();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IImportDeclaration[] getImports(ICompilationUnit model) {
		try {
			return model.getImports();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static Integer getFlags(IMember model) {
		try {
			return model.getFlags();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IField[] getFields(IType source) {
		try {
			return source.getFields();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static IMethod[] getMethods(IType source) {
		try {
			return source.getMethods();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isSourcePackageFragmentRoot(IJavaElement model) {
		return model instanceof IPackageFragmentRoot && getKind((IPackageFragmentRoot) model) == K_SOURCE;
	}

	static boolean isAnnotation(IType model) {
		try {
			return model.isAnnotation();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isInterface(IType model) {
		try {
			return model.isInterface();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isClass(IType model) {
		try {
			return model.isClass();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isEnum(IType model) {
		try {
			return model.isEnum();
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isPackageInfo(ICompilationUnit model) {
		return PACKAGE_INFO_FILENAME.equals(model.getElementName());
	}

	static ITypeHierarchy supertypeHierarchy(IType model, IProgressMonitor progressMonitor) {
		try {
			return model.newSupertypeHierarchy(progressMonitor);
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}
}

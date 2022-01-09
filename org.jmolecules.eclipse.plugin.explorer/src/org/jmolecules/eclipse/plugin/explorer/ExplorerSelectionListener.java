package org.jmolecules.eclipse.plugin.explorer;

import static java.util.Optional.of;
import static org.eclipse.jdt.core.JavaCore.NATURE_ID;
import static org.jmolecules.eclipse.plugin.explorer.ProjectUtils.hasNature;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.texteditor.ITextEditor;

class ExplorerSelectionListener implements ISelectionListener {

	private final List<BiFunction<IWorkbenchPart, ISelection, IJavaElement>> functions;
	private final ExplorerView explorerView;

	ExplorerSelectionListener(ExplorerView explorerView) {
		this.explorerView = explorerView;
		this.functions = List.of(fromTextEditor(), fromPackagesView(), fromProjectExplorer());
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part.equals(explorerView)) {
			return;
		}

		if (selection.isEmpty()) {
			return;
		}

		functions.stream().map(f -> f.apply(part, selection)).filter(Objects::nonNull).limit(1).findAny()
				.ifPresent(explorerView::update);
	}

	private static BiFunction<IWorkbenchPart, ISelection, IJavaElement> fromTextEditor() {
		return (part, selection) -> {
			return of(part) //
					.filter(ITextEditor.class::isInstance) //
					.map(ITextEditor.class::cast) //
					.map(ITextEditor::getEditorInput) //
					.map(JavaUI::getEditorInputJavaElement) //
					.orElse(null);
		};
	}

	private static BiFunction<IWorkbenchPart, ISelection, IJavaElement> fromPackagesView() {
		return (part, selection) -> {
			if (!IPackagesViewPart.class.isInstance(part)) {
				return null;
			}

			return of(selection) //
					.filter(IStructuredSelection.class::isInstance) //
					.map(IStructuredSelection.class::cast) //
					.filter(s -> (s.size() == 1)) //
					.map(IStructuredSelection::getFirstElement) //
					.filter(IJavaElement.class::isInstance) //
					.map(IJavaElement.class::cast) //
					.orElse(null);
		};
	}

	private static BiFunction<IWorkbenchPart, ISelection, IJavaElement> fromProjectExplorer() {
		return (part, selection) -> {
			if (!ProjectExplorer.class.isInstance(part)) {
				return null;
			}

			Optional<Object> selected = of(selection) //
					.filter(ITreeSelection.class::isInstance) //
					.map(ITreeSelection.class::cast) //
					.filter(s -> (s.size() == 1)) //
					.map(ITreeSelection::getFirstElement);

			Optional<IJavaElement> element = selected //
					.filter(IProject.class::isInstance) //
					.map(IProject.class::cast) //
					.filter(p -> hasNature(p, NATURE_ID)) //
					.map(JavaCore::create);

			return element.orElseGet(
					() -> selected.filter(IJavaElement.class::isInstance).map(IJavaElement.class::cast).orElse(null));
		};
	}
}

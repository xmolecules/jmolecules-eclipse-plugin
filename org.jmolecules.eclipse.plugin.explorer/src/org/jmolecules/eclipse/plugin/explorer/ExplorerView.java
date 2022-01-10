package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.swt.SWT.H_SCROLL;
import static org.eclipse.swt.SWT.MULTI;
import static org.eclipse.swt.SWT.NONE;
import static org.eclipse.swt.SWT.V_SCROLL;
import static org.eclipse.swt.layout.GridData.FILL_BOTH;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ExplorerView extends ViewPart {

	private ImageProvider imageProvider;
	private TreeFactory treeFactory;

	private ExplorerSelectionListener selectionListener;
	private ExplorerActions explorerActions;
	private TreeViewer treeViewer;
	private Composite container;
	private Label label;

	@PostConstruct
	void postConstruct() {
		imageProvider = new ImageProvider();
		treeFactory = new TreeFactory(new JMolecules());
		explorerActions = new ExplorerActions(imageProvider);
	}

	@Override
	public void createPartControl(Composite parent) {
		createControls(parent);
		registerSelectionListener();
	}

	private void createControls(Composite parent) {
		container = new Composite(parent, NONE);
		container.setLayoutData(new GridData(FILL_BOTH));
		container.setLayout(new StackLayout());

		label = new Label(container, NONE);
		label.setText("Please select a Java project or a file contained in it to have this project analyzed.");

		treeViewer = new TreeViewer(container, MULTI | H_SCROLL | V_SCROLL);
		treeViewer.setContentProvider(new ExplorerContentProvider());
		treeViewer.setLabelProvider(new ExplorerLabelProvider(imageProvider));
		treeViewer.addDoubleClickListener(new ExplorerDoubleClickListener());
		treeViewer.setComparator(new ExplorerComparator());
		treeViewer.setUseHashlookup(true);
		treeViewer.setInput(null);
		getSite().setSelectionProvider(treeViewer);

		((StackLayout) container.getLayout()).topControl = label;

		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(explorerActions.collapseAllAction(treeViewer));
	}

	@Override
	public void setFocus() {
		if (container != null) {
			container.setFocus();
		}
	}

	@Override
	public void dispose() {
		deregisterSelectionListener();
		super.dispose();
	}

	void update(IJavaElement element) {
		IJavaProject project = element.getJavaProject();

		TreeNode tree = (TreeNode) treeViewer.getInput();
		if (tree == null || !tree.findNode(project).isPresent()) {
			treeViewer.setInput(treeFactory.create(project));
			tree = (TreeNode) treeViewer.getInput();
		}

		Optional<TreeNode> treeNode = tree.findNode(element);
		treeNode.ifPresent(n -> {
			treeViewer.expandToLevel(n, 0);
			treeViewer.setSelection(new StructuredSelection(n), true);
		});

		show(treeViewer.getControl());
	}

	void reset() {
		treeViewer.setInput(null);
		show(label);
	}

	private void show(Control control) {
		((StackLayout) container.getLayout()).topControl = control;
		container.layout();
	}

	private void registerSelectionListener() {
		selectionListener = new ExplorerSelectionListener(this);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

	private void deregisterSelectionListener() {
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}
}

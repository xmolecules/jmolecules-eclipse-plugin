package org.jmolecules.eclipse.plugin.explorer;

import static org.eclipse.swt.SWT.H_SCROLL;
import static org.eclipse.swt.SWT.MULTI;
import static org.eclipse.swt.SWT.NONE;
import static org.eclipse.swt.SWT.V_SCROLL;
import static org.eclipse.swt.SWT.WRAP;
import static org.eclipse.swt.layout.GridData.FILL_BOTH;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.jmolecules.eclipse.plugin.explorer.JMolecules.Concepts;

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
        initializeInteractions(parent);
        registerSelectionListener();
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

        updateStatusLine(tree);
        show(treeViewer.getControl());
    }

    void reset() {
        treeViewer.setInput(null);
        updateStatusLine(null);
        show(label);
    }

    private void createControls(Composite parent) {
        container = new Composite(parent, NONE);
        container.setLayoutData(new GridData(FILL_BOTH));
        container.setLayout(new StackLayout());

        label = new Label(container, WRAP);
        label.setText("Please select a Java project or a file contained in it to have this project analyzed.");

        treeViewer = new TreeViewer(container, MULTI | H_SCROLL | V_SCROLL);
        treeViewer.setContentProvider(new ExplorerContentProvider());
        treeViewer.setLabelProvider(new ExplorerLabelProvider(imageProvider));
        treeViewer.addDoubleClickListener(new ExplorerDoubleClickListener());
        treeViewer.setComparator(new ExplorerComparator());
        treeViewer.setUseHashlookup(true);
        getSite().setSelectionProvider(treeViewer);

        show(label);
    }

    private void initializeInteractions(Composite parent) {
        IActionBars actionBars = getViewSite().getActionBars();
        Action collapseAllAction = explorerActions.collapseAllAction(treeViewer);

        IToolBarManager toolbarManager = actionBars.getToolBarManager();
        toolbarManager.add(collapseAllAction);

        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(collapseAllAction);

        actionBars.updateActionBars();

        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(mgr);
            }
        });

        Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, treeViewer);
    }

    private void fillContextMenu(IMenuManager menuManager) {
        Optional<IJavaElement> selection = Optional.of(treeViewer.getSelection()) //
            .filter(IStructuredSelection.class::isInstance) //
            .map(IStructuredSelection.class::cast) //
            .filter(s -> s.size() == 1) //
            .map(s -> s.getFirstElement()) //
            .filter(TreeNode.class::isInstance) //
            .map(TreeNode.class::cast) //
            .map(TreeNode::getSource);

        selection.ifPresent(s -> menuManager.add(explorerActions.showInEditorAction(s)));
    }

    private void updateStatusLine(TreeNode tree) {
        IActionBars actionBars = getViewSite().getActionBars();
        IStatusLineManager statusLineManager = actionBars.getStatusLineManager();
        if (tree != null) {
            statusLineManager.setMessage(new StatusLineMessageBuilder(tree).build());
        } else {
            statusLineManager.setMessage(null);
        }
        actionBars.updateActionBars();
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

    private static class StatusLineMessageBuilder {

        private final TreeNode tree;

        StatusLineMessageBuilder(TreeNode tree) {
            this.tree = tree;
        }

        String build() {
            TreeNode node = tree.getChildren().iterator().next();
            Concepts concepts = node.collectConcepts();
            int conceptCount = concepts.get().size();

            StringBuilder sb = new StringBuilder(node.getSource().getElementName()).append(" [expresses ");
            if (conceptCount == 0) {
                sb.append("no concepts]");
            } else {
                int categoryCount = concepts.getCategories().size();
                sb.append(conceptCount) //
                    .append(" concept") //
                    .append(conceptCount == 1 ? "" : "s") //
                    .append(" from ") //
                    .append(categoryCount) //
                    .append(" categor") //
                    .append(categoryCount == 1 ? "y" : "ies") //
                    .append("]");
            }
            return sb.toString();
        }
    }
}

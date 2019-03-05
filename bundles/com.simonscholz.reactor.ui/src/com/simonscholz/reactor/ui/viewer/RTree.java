package com.simonscholz.reactor.ui.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

public class RTree<M, I> {

	private Tree tree;
	private RTreeContentProvider<M, I> treeContentProvider;
	private RLabelProvider<M> labelProvider;
	private List<Predicate<M>> filters;
	private ResourceManager resourceManager;

	public RTree(Composite parent) {
		this(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
	}

	/**
	 * Creates a tree viewer on a newly-created tree control under the given parent.
	 * The tree control is created using the given SWT style bits. The viewer has no
	 * input, no content provider, a default label provider, no sorter, and no
	 * filters.
	 *
	 * @param parent the parent control
	 * @param style  the SWT style bits used to create the tree.
	 */
	public RTree(Composite parent, int style) {
		this(new Tree(parent, style));
	}

	/**
	 * Creates a tree viewer on the given tree control. The viewer has no input, no
	 * content provider, a default label provider, no sorter, and no filters.
	 *
	 * @param tree the tree control
	 */
	public RTree(Tree tree) {
		this.tree = tree;
		resourceManager = new LocalResourceManager(JFaceResources.getResources(), tree);
		filters = new ArrayList<>();
	}

	public void setContentProvider(RTreeContentProvider<M, I> treeContentProvider) {
		this.treeContentProvider = treeContentProvider;
	}

	public void setLabelProvider(RLabelProvider<M> labelProvider) {
		this.labelProvider = labelProvider;
	}

	public void setInput(I input) {
		Publisher<Collection<M>> elementPublisher = treeContentProvider.getElements(input);
		Mono.from(elementPublisher).subscribe(elements -> {
			elements.stream().filter(filters.stream().reduce(Predicate::or).orElse(t -> true)).forEach(m -> {
				createTreeItem(m);
			});
		}, Throwable::printStackTrace);
	}

	public void addFilter(Predicate<M> filter) {
		filters.add(filter);
	}

	public void removeFilter(Predicate<M> filter) {
		filters.remove(filter);
	}

	private void createTreeItem(M m) {
		TreeItem treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setData(m);

		if (labelProvider != null) {
			Publisher<String> textPublisher = labelProvider.getText(m);
			Mono.from(textPublisher).subscribe(text -> {
				treeItem.setText(text);
			});
			Publisher<ImageDescriptor> imgPublisher = labelProvider.getImage(m);
			Mono.from(imgPublisher).subscribe(img -> {
				Image image = resourceManager.createImage(img);
				treeItem.setImage(image);
			});
		} else {
			treeItem.setText(m.toString());
		}
	}
}

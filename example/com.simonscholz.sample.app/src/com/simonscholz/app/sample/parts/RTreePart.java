 
package com.simonscholz.app.sample.parts;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.reactivestreams.Publisher;

import com.simonscholz.reactor.ui.viewer.RLabelProvider;
import com.simonscholz.reactor.ui.viewer.RTree;
import com.simonscholz.reactor.ui.viewer.RTreeContentProvider;

import reactor.core.publisher.Mono;

public class RTreePart {
	@PostConstruct
	public void postConstruct(Composite parent) {
		RTree<File,File> rTree = new RTree<>(parent);
		rTree.setContentProvider(new RTreeContentProvider<File, File>() {
			
			@Override
			public Publisher<Collection<File>> getElements(File inputElement) {
				File[] listFiles = inputElement.listFiles();
				List<File> asList = Arrays.asList(listFiles);
				return Mono.just(asList);
			}
			
			@Override
			public boolean hasChildren(File element) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Publisher<Collection<File>> getParent(File element) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Publisher<Collection<File>> getChildren(File parentElement) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL homeImgUrl = FileLocator.find(bundle, new Path("/icons/home.png"));
		rTree.setLabelProvider(new RLabelProvider<File>() {
			
			@Override
			public Publisher<String> getText(File element) {
				return Mono.just(String.format("%s (%s)", element.getName(), element.getFreeSpace()));
			}
			
			@Override
			public Publisher<ImageDescriptor> getImage(File element) {
				return Mono.just(ImageDescriptor.createFromURL(homeImgUrl));
			}
			
			@Override
			public Publisher<ColorDescriptor> getForeground(File element) {
				return Mono.empty();
			}
			
			@Override
			public Publisher<ColorDescriptor> getBackground(File element) {
				return Mono.empty();
			}
		});
		rTree.setInput(File.listRoots()[0]);
	}
}
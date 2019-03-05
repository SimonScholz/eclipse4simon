package com.simonscholz.reactor.ui.viewer;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.reactivestreams.Publisher;

public interface RLabelProvider<E> {

	Publisher<String> getText(E element);

	Publisher<ImageDescriptor> getImage(E element);

	Publisher<ColorDescriptor> getForeground(E element);

	Publisher<ColorDescriptor> getBackground(E element);
}

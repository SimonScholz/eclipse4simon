package com.simonscholz.reactor.ui.viewer;

import java.util.Collection;

import org.reactivestreams.Publisher;

public interface RTreeContentProvider<E, I> extends RContentProvider<E,I> {

	public Publisher<Collection<E>> getChildren(E parentElement);

	public Publisher<Collection<E>> getParent(E element);

	public boolean hasChildren(E element);
}

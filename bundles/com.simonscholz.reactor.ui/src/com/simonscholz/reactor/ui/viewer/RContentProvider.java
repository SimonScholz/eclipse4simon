package com.simonscholz.reactor.ui.viewer;

import java.util.Collection;

import org.reactivestreams.Publisher;

public interface RContentProvider<E, I> {

	public Publisher<Collection<E>> getElements(I inputElement);
}

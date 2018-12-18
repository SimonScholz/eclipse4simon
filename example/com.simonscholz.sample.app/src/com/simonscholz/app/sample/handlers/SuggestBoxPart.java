
package com.simonscholz.app.sample.handlers;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.nebula.widgets.proposal.ContentProposalAdapter;
import org.eclipse.nebula.widgets.proposal.DirectInputProposalConfigurator;
import org.eclipse.nebula.widgets.proposal.IProposalChangedListener;
import org.eclipse.nebula.widgets.proposal.controladapter.TextControlAdapter;
import org.eclipse.nebula.widgets.suggestbox.ClosableSuggestBoxEntry;
import org.eclipse.nebula.widgets.suggestbox.SuggestBox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class SuggestBoxPart {

	@PostConstruct
	public void postConstruct(Composite parent) {
		SuggestBox<String> suggestBox = new SuggestBox<>(parent, SWT.BORDER);
		suggestBox.addBox(new ClosableSuggestBoxEntry<String>("Simon"));
		TextControlAdapter textControlAdapter = new TextControlAdapter(suggestBox.getTextControl());
		DirectInputProposalConfigurator<List<String>> proposalConfigurator = new DirectInputProposalConfigurator<>(
				Arrays.asList("Simon Scholz", "Dirk Fauth", "Lars Vogel", "Wim Jongman"));
		ContentProposalAdapter<List<String>> proposalAdapter = new ContentProposalAdapter<>(textControlAdapter,
				proposalConfigurator);
		proposalAdapter.addSelectionChangedListener(new IProposalChangedListener() {

			@Override
			public void proposalChanged(ISelection selection) {
				IStructuredSelection sSel = (IStructuredSelection) selection;
				if (!selection.isEmpty()) {
					suggestBox.addBox(new ClosableSuggestBoxEntry<String>(sSel.getFirstElement().toString()));
					suggestBox.getTextControl().setText("");
				}
			}
		});
	}

}
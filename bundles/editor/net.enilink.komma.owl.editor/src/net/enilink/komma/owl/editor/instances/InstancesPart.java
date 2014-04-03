/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.owl.editor.instances;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.StatementPattern;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.provider.reflective.StatementPatternContentProvider;
import net.enilink.komma.edit.ui.util.SearchWidget;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IObject;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.vocab.rdf.RDF;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;

public class InstancesPart extends AbstractEditingDomainPart {
	private StructuredViewer viewer;
	private IAdapterFactory adapterFactory;
	private IClass input;
	Action deleteItemAction, addItemAction;

	@Override
	public void createContents(Composite parent) {
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);

		createActions(parent);
		createIndividualsPart(parent);
	}

	private void createIndividualsPart(Composite parent) {
		viewer = createViewer(parent);
		viewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer.setComparator(new ObjectComparator());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getForm().fireSelectionChanged(InstancesPart.this,
						event.getSelection());
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (deleteItemAction != null) {
					deleteItemAction
							.setEnabled(!event.getSelection().isEmpty());
				}
			}
		});

		SearchWidget searchWidget = new SearchWidget();
		searchWidget.setViewer(viewer);
		searchWidget.createControl(parent);
		searchWidget.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.END, false, false));
	}

	protected StructuredViewer createViewer(Composite parent) {
		Table table = getWidgetFactory().createTable(parent,
				SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);

		viewer = new TableViewer(table) {
			@SuppressWarnings("rawtypes")
			@Override
			protected void setSelectionToWidget(List list, boolean reveal) {
				// FIX - do only select existing items to prevent instantiation
				// of all virtual elements
				List<Object> existing = null;
				if (list != null) {
					existing = new ArrayList<Object>(list.size());
					for (Object toSelect : list) {
						if (doFindItem(toSelect) != null) {
							existing.add(toSelect);
						}
					}
				}
				super.setSelectionToWidget(existing, reveal);
			}
		};
		viewer.setContentProvider(new StatementPatternContentProvider());

		return viewer;
	}

	protected StructuredViewer getViewer() {
		return viewer;
	}

	public void createActions(Composite parent) {
		IToolBarManager toolBarManager = (IToolBarManager) getForm()
				.getAdapter(IToolBarManager.class);
		ToolBarManager ownManager = null;
		if (toolBarManager == null) {
			toolBarManager = ownManager = new ToolBarManager(SWT.HORIZONTAL);
			ToolBar toolBar = ownManager.createControl(parent);
			getWidgetFactory().adapt(toolBar);
			toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.DEFAULT, true,
					false));
		}

		addItemAction = new Action("Add") {
			public void run() {
				addItem();
			}
		};
		addItemAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.ADD)));
		toolBarManager.add(addItemAction);

		deleteItemAction = new Action("Remove") {
			public void run() {
				deleteItem();
			}
		};
		deleteItemAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.REMOVE)));
		deleteItemAction.setEnabled(false);
		toolBarManager.add(deleteItemAction);

		IAction refreshAction = new Action("Refresh") {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.REFRESH)));
		toolBarManager.add(refreshAction);

		if (ownManager != null) {
			ownManager.update(true);
		}
	}

	void addItem() {
		if (input instanceof IClass) {
			final IClass parent = (IClass) input;
			NewObjectWizard wizard = new NewObjectWizard(
					((IObject) parent).getModel()) {
				@Override
				public boolean performFinish() {
					final URI resourceName = getObjectName();
					try {
						ICommand cmd = new SimpleCommand("Create instance") {
							@Override
							protected CommandResult doExecuteWithResult(
									IProgressMonitor progressMonitor,
									IAdaptable info) throws ExecutionException {
								final IResource individual = parent
										.newInstance(resourceName);
								return CommandResult
										.newOKCommandResult(individual);
							}
						};
						getEditingDomain().getCommandStack().execute(cmd, null,
								null);
						if (cmd.getCommandResult().getStatus().isOK()) {
							viewer.setSelection(new StructuredSelection(cmd
									.getCommandResult().getReturnValue()));
						}
					} catch (ExecutionException e) {
						OWLEditorPlugin.INSTANCE.log(e);
					}

					return true;
				}
			};

			WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
			wizardDialog.open();
		}
	}

	void editItem() {
		// TODO
	}

	void deleteItem() {
		try {
			getEditingDomain().getCommandStack().execute(
					new SimpleCommand("Delete instance") {
						@Override
						protected CommandResult doExecuteWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
							final Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
							if (selected instanceof IResource) {
								((IResource) selected).getEntityManager()
										.remove((IResource) selected);
								return CommandResult
										.newOKCommandResult(selected);
							}
							return CommandResult.newCancelledCommandResult();
						}
					}, null, null);
		} catch (ExecutionException e) {
			OWLEditorPlugin.INSTANCE.log(e);
		}
	}

	@Override
	public boolean setFocus() {
		if (viewer != null && viewer.getControl().setFocus()) {
			return true;
		}
		return super.setFocus();
	}

	public void reveal(IResource resource) {
		if (resource instanceof IObject) {
			viewer.setInput(resource);
			refresh();
		}
	}

	@Override
	public boolean setEditorInput(Object input) {
		if (input == null || input instanceof IClass) {
			if (this.input != input) {
				this.input = (IClass) input;
				setStale(true);
			}
			return true;
		} else if (input instanceof IReference) {
			if (viewer != null) {
				viewer.setSelection(new StructuredSelection(input), true);
			}
		}
		return false;
	}

	@Override
	public void refresh() {
		IAdapterFactory newAdapterFactory = getAdapterFactory();
		if (adapterFactory == null || !adapterFactory.equals(newAdapterFactory)) {
			adapterFactory = newAdapterFactory;
			adapterFactoryChanged();
		}

		setInputToViewer(viewer, input);
		super.refresh();
	}

	protected void setInputToViewer(StructuredViewer viewer, IClass input) {
		if (input == null) {
			viewer.setInput(null);
		} else {
			viewer.setInput(new StatementPattern(null, RDF.PROPERTY_TYPE, input));
		}
	}

	protected void adapterFactoryChanged() {
		viewer.setLabelProvider(new AdapterFactoryLabelProvider(
				getAdapterFactory()));

		createContextMenuFor(viewer);
	}
}

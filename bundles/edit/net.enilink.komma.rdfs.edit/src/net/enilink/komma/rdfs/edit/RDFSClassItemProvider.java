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
package net.enilink.komma.rdfs.edit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.DragAndDropCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.provider.ViewerNotification;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

public class RDFSClassItemProvider extends ReflectiveItemProvider {
	public RDFSClassItemProvider(RDFSItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	protected Collection<IViewerNotification> addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification, boolean contentRefresh,
			boolean labelUpdate) {
		if (RDFS.PROPERTY_SUBCLASSOF.equals(notification.getPredicate())) {
			Object element = notification.getObject();

			IObject object;
			if (element instanceof IObject) {
				object = (IObject) element;
			} else if (element instanceof IReference) {
				object = resolveReference((IReference) element);
			} else {
				return null;
			}

			if (object != null) {
				if (viewerNotifications == null) {
					viewerNotifications = createViewerNotificationList();
				}
				viewerNotifications.add(new ViewerNotification(object,
						contentRefresh, labelUpdate));
			}
			return viewerNotifications;
		}
		return super.addViewerNotifications(viewerNotifications, notification,
				contentRefresh, labelUpdate);
	}

	@Override
	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
	}

	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		if (object instanceof IClass) {
			newChildDescriptors.add(createChildParameter(
					((IObject) object).getModel().resolve(
							RDFS.PROPERTY_SUBCLASSOF),
					new ChildDescriptor(Arrays
							.asList((IClass) ((IObject) object).getModel()
									.resolve(OWL.TYPE_CLASS)), true)));
		}
		newChildDescriptors.done();
	}

	@Override
	protected ICommand createCreateChildCommand(IEditingDomain domain,
			IObject owner, IReference property, Object value, int index,
			Collection<?> collection) {
		if (RDFS.PROPERTY_SUBCLASSOF.equals(property)) {
			return new CreateChildCommand(domain, owner, property, value,
					index, collection, this) {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					child = helper.createChild(owner, property,
							childDescription);

					if (child != null) {
						addAndExecute(AddCommand.create(domain, child,
								property, owner, index), progressMonitor, info);
					}

					affectedObjects = helper.getCreateChildResult(child);

					Collection<?> result = helper.getCreateChildResult(child);

					return CommandResult
							.newOKCommandResult(result == null ? Collections.EMPTY_LIST
									: result);
				}

				@Override
				public boolean prepare() {
					if (owner == null || property == null
							|| childDescription == null) {
						return false;
					}
					return true;
				}

			};
		}
		return super.createCreateChildCommand(domain, owner, property, value,
				index, collection);
	}

	@Override
	protected ICommand createDragAndDropCommand(IEditingDomain domain,
			Object owner, float location, int operations, int operation,
			Collection<?> collection) {
		return new DragAndDropCommand(domain, owner, location, operations,
				operation, collection) {
			@Override
			protected boolean isNonContainment(IReference property) {
				if (RDFS.PROPERTY_SUBCLASSOF.equals(property)) {
					return false;
				}
				return super.isNonContainment(property);
			}

			@Override
			protected boolean prepareDropCopyOn() {
				// simply link dropped class to parent class by rdfs:subClassOf
				dragCommand = IdentityCommand.INSTANCE;
				dropCommand = AddCommand
						.create(domain, owner, null, collection);

				return dropCommand.canExecute();
			}

			@Override
			protected boolean prepareDropLinkOn() {
				return prepareDropCopyOn();
			}
		};
	}

	@Override
	protected ICommand factorAddCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		if (commandParameter.getCollection() == null
				|| commandParameter.getCollection().isEmpty()) {
			return UnexecutableCommand.INSTANCE;
		}
		CompositeCommand addCommand = new CompositeCommand();

		Object owner = commandParameter.getOwner();
		for (Object value : commandParameter.getCollection()) {
			if (owner instanceof IClass && value instanceof IClass
					&& !owner.equals(value)
					&& !((IClass) owner).getRdfsSubClassOf().contains(value)) {
				addCommand.add(new AddCommand(domain, (IObject) value,
						((IObject) value).getModel().resolve(
								RDFS.PROPERTY_SUBCLASSOF),
						Arrays.asList(owner), CommandParameter.NO_INDEX) {
					@Override
					public Collection<?> doGetAffectedObjects() {
						if (affectedObjects.contains(owner)) {
							return collection;
						} else {
							return owner == null ? Collections.emptySet()
									: Collections.singleton(owner);
						}

					}
				});
			} else {
				addCommand.dispose();
				return UnexecutableCommand.INSTANCE;
			}
		}
		return addCommand.reduce();
	}

	@Override
	protected ICommand factorMoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	protected ICommand factorRemoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		final IObject owner = commandParameter.getOwnerObject();
		CompositeCommand removeCommand = new CompositeCommand();
		for (Object value : commandParameter.getCollection()) {
			if (owner.equals(value)) {
				removeCommand.dispose();
				return UnexecutableCommand.INSTANCE;
			}
			removeCommand.add(createRemoveCommand(
					domain,
					(IObject) value,
					((IObject) value).getModel().resolve(
							RDFS.PROPERTY_SUBCLASSOF),
					Arrays.asList(commandParameter.getOwner())));
		}
		return removeCommand.reduce();
	}

	@Override
	public Collection<?> getChildren(Object object) {
		if (object instanceof IClass) {
			Set<IClass> subClasses = ((IClass) object)
					.getDirectNamedSubClasses().toSet();
			if (RDFS.TYPE_RESOURCE.equals(object)) {
				if (!subClasses.contains(OWL.TYPE_THING)) {
					subClasses.add((IClass) ((IClass) object)
							.getEntityManager().find(OWL.TYPE_THING));
				}
			}
			// avoid recursive containment
			if (OWL.TYPE_THING.equals(object)) {
				subClasses.remove(RDFS.TYPE_RESOURCE);
			}
			return subClasses;
		}
		return super.getChildren(object);
	}

	@Override
	protected ISearchableItemProvider getSearchableItemProvider() {
		return new SparqlSearchableItemProvider() {
			@Override
			protected String getSparqlFindPatterns(Object parent) {
				return "?s rdfs:subClassOf ?parent";
			}
		};
	}

	@Override
	public Object getParent(Object object) {
		if (object instanceof IClass) {
			if (RDFS.TYPE_RESOURCE.equals(object)) {
				return null;
			}
			
			// always return rdfs:Resource as parent of owl:Thing
			if (OWL.TYPE_THING.equals(object)) {
				return ((IEntity) object).getEntityManager().find(
						RDFS.TYPE_RESOURCE);
			}

			IExtendedIterator<?> it = ((IClass) object)
					.getDirectNamedSuperClasses();
			try {
				if (it.hasNext()) {
					return it.next();
				}
			} finally {
				it.close();
			}
		}
		return super.getParent(object);
	}

	@Override
	protected Collection<? extends IClass> getTypes(Object object) {
		if (object instanceof IClass
				&& (((IClass) object).equals(OWL.TYPE_OBJECTPROPERTY) || ((IClass) object)
						.equals(OWL.TYPE_DATATYPEPROPERTY))) {
			return Arrays.asList((IClass) object);
		}
		return super.getTypes(object);
	}

	@Override
	public boolean hasChildren(Object object) {
		if (object instanceof IClass) {
			return ((IClass) object).hasNamedSubClasses(true);
		}
		return hasChildren(object, false);
	}
}

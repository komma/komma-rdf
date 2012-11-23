/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.UnitOfWork;

public class ThreadLocalDataManager implements IDataManager {
	@Inject
	protected IDataManagerFactory dmFactory;

	@Inject
	protected UnitOfWork uow;

	private ThreadLocal<IDataManager> delegate = new ThreadLocal<IDataManager>();

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements,
			IReference... contexts) {
		getDelegate().add(statements, contexts);
		return this;
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements,
			IReference[] readContexts, IReference... addContexts) {
		getDelegate().add(statements, readContexts, addContexts);
		return this;
	}

	@Override
	public IDataManager clearNamespaces() {
		getDelegate().clearNamespaces();
		return this;
	}

	@Override
	public void close() {
		IDataManager manager = delegate.get();
		if (manager != null) {
			manager.close();
			delegate.remove();
		}
	}

	@Override
	public <R> IDataManagerQuery<R> createQuery(String query, String baseURI,
			boolean includeInferred, IReference... contexts) {
		return getDelegate().createQuery(query, baseURI, includeInferred,
				contexts);
	}

	public IDataManager getDelegate() {
		IDataManager manager = delegate.get();
		if (manager == null || !manager.isOpen()) {
			if (!uow.isActive()) {
				throw new KommaException("No active unit of work found.");
			}
			manager = dmFactory.get();
			uow.addManager(manager);
			delegate.set(manager);
		}
		return manager;
	}
	
	@Override
	public IDialect getDialect() {
		return getDelegate().getDialect();
	}

	@Override
	public InferencingCapability getInferencing() {
		return getDelegate().getInferencing();
	}

	@Override
	public URI getNamespace(String prefix) {
		return getDelegate().getNamespace(prefix);
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		return getDelegate().getNamespaces();
	}

	@Override
	public ITransaction getTransaction() {
		return getDelegate().getTransaction();
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
			IValue object, boolean includeInferred, IReference... contexts) {
		return getDelegate().hasMatch(subject, predicate, object,
				includeInferred, contexts);
	}

	@Override
	public boolean isOpen() {
		IDataManager manager = delegate.get();
		return (manager != null) ? manager.isOpen() : false;
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject,
			IReference predicate, IValue object, boolean includeInferred,
			IReference... contexts) {
		return getDelegate().match(subject, predicate, object, includeInferred,
				contexts);
	}

	@Override
	public IReference newBlankNode() {
		return getDelegate().newBlankNode();
	}

	@Override
	public IDataManager remove(
			Iterable<? extends IStatementPattern> statements,
			IReference... contexts) {
		getDelegate().remove(statements, contexts);
		return this;
	}

	@Override
	public IDataManager removeNamespace(String prefix) {
		getDelegate().removeNamespace(prefix);
		return this;
	}

	@Override
	public IDataManager setNamespace(String prefix, URI uri) {
		getDelegate().setNamespace(prefix, uri);
		return this;
	}

	@Override
	public String toString() {
		return getDelegate().toString();
	}
}

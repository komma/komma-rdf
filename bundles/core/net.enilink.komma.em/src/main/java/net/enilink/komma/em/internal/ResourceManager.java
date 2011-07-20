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
package net.enilink.komma.em.internal;

import java.util.Arrays;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * Manage the statements about a resource.
 * 
 */
public class ResourceManager {
	IDataManager dm;

	public ResourceManager(IDataManager dm) {
		this.dm = dm;
	}

	public IReference createResource(URI uri) {
		if (uri == null) {
			return dm.newBlankNode();
		}
		return uri;
	}

	public void removeResource(IReference resource) {
		boolean active = dm.getTransaction().isActive();
		if (!active) {
			dm.getTransaction().begin();
		}
		dm.remove(Arrays.asList( //
				new Statement(resource, null, null), //
				new Statement(null, null, resource)));
		if (!active) {
			dm.getTransaction().commit();
		}
	}

	public void renameResource(IReference before, IReference after) {
		try {
			boolean active = dm.getTransaction().isActive();
			if (!active) {
				dm.getTransaction().begin();
			}
			IExtendedIterator<IStatement> stmts = dm.matchAsserted(
					before, null, null);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference pred = stmt.getPredicate();
					Object obj = stmt.getObject();
					dm.remove(new Statement(before, pred, obj));
					dm.add(new Statement(after, pred, obj));
				}
			} finally {
				stmts.close();
			}
			stmts = dm.matchAsserted(null, before, null);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					Object obj = stmt.getObject();
					dm.remove(new Statement(subj, before, obj));
					dm.add(new Statement(subj, after, obj));
				}
			} finally {
				stmts.close();
			}
			stmts = dm.matchAsserted(null, null, before);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					IReference pred = stmt.getPredicate();
					dm.remove(new Statement(subj, pred, before));
					dm.add(new Statement(subj, pred, after));
				}
			} finally {
				stmts.close();
			}
			if (!active) {
				dm.getTransaction().commit();
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}
}

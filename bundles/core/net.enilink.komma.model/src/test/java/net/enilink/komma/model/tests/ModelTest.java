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
package net.enilink.komma.model.tests;

import java.util.Collection;

import junit.framework.Test;

import com.google.inject.Guice;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.tests.KommaTestCase;

public class ModelTest extends KommaTestCase {
	IModelSet modelSet;

	protected void setUp() throws Exception {
		super.setUp();

		KommaModule module = ModelCore.createModelSetModule(getClass()
				.getClassLoader());

		IModelSetFactory factory = Guice.createInjector(
				new ModelSetModule(module)).getInstance(IModelSetFactory.class);

		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI
				.appendFragment("MemoryModelSet"));
	}

	protected void tearDown() throws Exception {
		modelSet.dispose();

		super.tearDown();
	}

	public void testModel() throws Exception {
		IModel model = modelSet.createModel(URIImpl
				.createURI("http://iwu.fraunhofer.de/test/model1"));
		final boolean[] notified = new boolean[] { false };
		final Object[] subject = new Object[1];

		modelSet.addListener(new INotificationListener<INotification>() {
			@Override
			public NotificationFilter<INotification> getFilter() {
				return NotificationFilter
						.instanceOf(IStatementNotification.class);
			}

			@Override
			public void notifyChanged(
					Collection<? extends INotification> notifications) {
				for (INotification notification : notifications) {
					subject[0] = ((IStatementNotification) notification)
							.getSubject();

					System.out.println("changed: " + notification);
				}
				notified[0] = true;
			}
		});
		IClass resource = (IClass) model.getManager().create(
				net.enilink.vocab.owl.Class.class);

		assertEquals("Reference is unequal to resource", subject[0], resource);
		assertEquals("Resource is unequal to reference", resource, subject[0]);

		assertTrue(notified[0]);
	}

	public static Test suite() throws Exception {
		return suite(ModelTest.class);
	}
}
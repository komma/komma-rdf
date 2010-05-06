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
package net.enilink.vocab.owl;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

@Iri("http://www.w3.org/2002/07/owl#ObjectProperty")
public interface ObjectProperty extends OwlProperty {

	/** http://www.w3.org/2002/07/owl#inverseOf */
	@Iri("http://www.w3.org/2002/07/owl#inverseOf")
	public abstract Set<ObjectProperty> getOwlInverseOf();

	/** http://www.w3.org/2002/07/owl#inverseOf */
	public abstract void setOwlInverseOf(Set<? extends ObjectProperty> value);

	/** http://www.w3.org/2002/07/owl#propertyChain */
	@Iri("http://www.w3.org/2002/07/owl#propertyChain")
	public abstract Set<ObjectProperty> getOwlPropertyChain();

	/** http://www.w3.org/2002/07/owl#propertyChain */
	public abstract void setOwlPropertyChain(Set<? extends ObjectProperty> value);

}

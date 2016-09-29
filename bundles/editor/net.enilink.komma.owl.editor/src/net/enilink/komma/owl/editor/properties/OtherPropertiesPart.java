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
package net.enilink.komma.owl.editor.properties;

import net.enilink.komma.core.URI;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.rdf.RDF;

public class OtherPropertiesPart extends AbstractPropertiesPart {
	@Override
	protected String getName() {
		return "Properties";
	}

	@Override
	protected URI getPropertyType() {
		return RDF.TYPE_PROPERTY;
	}

	@Override
	protected URI getRootProperty() {
		return KOMMA.PROPERTY_ROOTPROPERTY;
	}
}
/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.parser.sparql.tree;

public class PrefixDecl {
	protected String prefix;
	protected IriRef Iri;

	public PrefixDecl(String prefix, IriRef Iri) {
		this.prefix = prefix;
		this.Iri = Iri;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public IriRef getIri() {
		return Iri;
	}
}
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
package net.enilink.komma.model;

import org.eclipse.core.runtime.IConfigurationElement;

import net.enilink.komma.core.URIImpl;

public class ModelDescription {
	private String prefix;
	private String namespace;
	private IConfigurationElement configElement;

	public ModelDescription(IConfigurationElement configElement) {
		this.configElement = configElement;
	}

	public ModelDescription(String prefix, String namespace) {
		this.prefix = prefix;
		this.namespace = namespace;
	}

	public String getNamespace() {
		if (namespace != null) {
			return namespace;
		}
		return configElement.getAttribute("namespace");
	}

	public String getUri() {
		String ns = getNamespace();
		return ns != null ? URIImpl.createURI(ns).trimFragment().toString()
				: null;
	}

	public String getPrefix() {
		if (prefix != null) {
			return prefix;
		}
		if (configElement != null) {
			return configElement.getAttribute("prefix");
		}
		return null;
	}

	public IConfigurationElement getConfigurationElement() {
		return configElement;
	}
}

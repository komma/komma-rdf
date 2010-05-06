/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.composition.mappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.exceptions.CompositionException;

/**
 * Tracks recorded roles and maps them to their subject type.
 * 
 * @author James Leigh
 * 
 */
public class HierarchicalRoleMapper<T> implements Cloneable {
	private DirectMapper<T> directMapper = new DirectMapper<T>();

	private TypeMapper<T> typeMapper = new TypeMapper<T>();

	private SimpleRoleMapper<T> simpleRoleMapper = new SimpleRoleMapper<T>();

	private Map<Class<?>, Set<Class<?>>> subclasses = new HashMap<Class<?>, Set<Class<?>>>(
			256);

	public HierarchicalRoleMapper<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			HierarchicalRoleMapper<T> cloned = (HierarchicalRoleMapper<T>) super
					.clone();
			cloned.directMapper = directMapper.clone();
			cloned.typeMapper = typeMapper.clone();
			cloned.simpleRoleMapper = simpleRoleMapper.clone();
			cloned.subclasses = clone(subclasses);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	private <K, V> Map<K, Set<V>> clone(Map<K, Set<V>> map) {
		Map<K, Set<V>> cloned = new HashMap<K, Set<V>>(map);
		for (Map.Entry<K, Set<V>> e : cloned.entrySet()) {
			e.setValue(new HashSet<V>(e.getValue()));
		}
		return cloned;
	}

	public void setTypeFactory(TypeFactory<T> typeFactory) {
		simpleRoleMapper.setTypeFactory(typeFactory);
	}

	public Collection<Class<?>> findAllRoles() {
		return simpleRoleMapper.findAllRoles();
	}

	public Collection<Class<?>> findRoles(T type) {
		return simpleRoleMapper.findRoles(type);
	}

	public Collection<Class<?>> findRoles(Collection<T> types,
			Collection<Class<?>> classes) {
		return simpleRoleMapper.findRoles(types, classes);
	}

	public boolean isTypeRecorded(T type) {
		return simpleRoleMapper.isTypeRecorded(type);
	}

	/**
	 * Finds the rdf:Class<?> for this Java Class<?>.
	 * 
	 * @param javaClass
	 * @return T of the rdf:Class<?> for this Java Class<?> or null.
	 */
	public T findType(Class<?> role) {
		return typeMapper.findType(role);
	}

	public Collection<T> findSubTypes(Class<?> role, Collection<T> rdfTypes) {
		T type = findType(role);
		if (type == null)
			throw new CompositionException("Concept not registered: "
					+ role.getSimpleName());
		rdfTypes.add(type);
		Set<Class<?>> subset = subclasses.get(role);
		if (subset == null)
			return rdfTypes;
		for (Class<?> c : subset) {
			findSubTypes(c, rdfTypes);
		}
		return rdfTypes;
	}

	public synchronized void recordConcept(Class<?> role, T type) {
		recordClassHierarchy(role);
		typeMapper.recordRole(role, type);
		if (!recordRole(role, type)) {
			Set<Class<?>> superRoles = getSuperRoles(role);
			Set<Class<?>> newRoles = new HashSet<Class<?>>(
					superRoles.size() + 1);
			newRoles.addAll(superRoles);
			newRoles.add(role);
			newRoles = simpleRoleMapper.recordRoles(newRoles, type);
			for (Class<?> r : directMapper.getDirectRoles(type)) {
				addRolesInSubclasses(r, newRoles);
			}
		}
	}

	public synchronized void recordBehaviour(Class<?> role, T type) {
		if (!recordRole(role, type)) {
			Set<Class<?>> newRoles = new HashSet<Class<?>>();
			newRoles.add(role);
			newRoles = simpleRoleMapper.recordRoles(newRoles, type);
			for (Class<?> r : directMapper.getDirectRoles(type)) {
				addRolesInSubclasses(r, newRoles);
			}
		}
	}

	private boolean recordRole(Class<?> role, T type) {
		assert type != null;
		directMapper.recordRole(role, type);

		if (simpleRoleMapper.getBaseType().equals(type)) {
			directMapper.recordRole(role, type);
			recordClassHierarchy(role);
			simpleRoleMapper.recordBaseRole(role);
			return true;
		}
		return false;
	}

	/**
	 * Record the class hierarchy of the concept to looks subclasses of related
	 * subject types.
	 * 
	 * @param concept
	 */
	private void recordClassHierarchy(Class<?> concept) {
		for (Class<?> sup : concept.getInterfaces()) {
			Set<Class<?>> set = subclasses.get(sup);
			if (set == null)
				subclasses.put(sup, set = new HashSet<Class<?>>());
			if (!set.contains(concept)) {
				set.add(concept);
				recordClassHierarchy(sup);
			}
		}
		Class<?> sup = concept.getSuperclass();
		if (sup != null) {
			Set<Class<?>> set = subclasses.get(sup);
			if (set == null)
				subclasses.put(sup, set = new HashSet<Class<?>>());
			if (!set.contains(concept)) {
				set.add(concept);
				recordClassHierarchy(sup);
			}
		}
	}

	private Set<Class<?>> getSuperRoles(Class<?> role) {
		Set<Class<?>> superRoles = new HashSet<Class<?>>();
		for (Class<?> sup : role.getInterfaces()) {
			Set<Class<?>> sr = getSuperRoles(sup);
			addRelatedRoles(sr, sup, superRoles);
		}
		Class<?> sup = role.getSuperclass();
		if (sup != null) {
			Set<Class<?>> sr = getSuperRoles(sup);
			addRelatedRoles(sr, sup, superRoles);
		}
		return superRoles;
	}

	private void addRolesInSubclasses(Class<?> role, Set<Class<?>> newRoles) {
		Set<Class<?>> subset = subclasses.get(role);
		if (subset == null)
			return; // no subclasses
		for (Class<?> sub : subset) {
			Set<Class<?>> subRoles = new HashSet<Class<?>>();
			subRoles = addRelatedRoles(newRoles, sub, subRoles);
			addRolesInSubclasses(sub, subRoles);
		}
	}

	private Set<Class<?>> addRelatedRoles(Set<Class<?>> existing,
			Class<?> role, Set<Class<?>> roles) {
		roles.addAll(existing);
		Set<T> set = directMapper.getDirectTypes(role);
		if (set != null) {
			for (T uri : set) {
				simpleRoleMapper.recordRoles(existing, uri);
				for (Class<?> c : simpleRoleMapper.findRoles(uri)) {
					roles.add(c);
				}
			}
		}
		return roles;
	}
}
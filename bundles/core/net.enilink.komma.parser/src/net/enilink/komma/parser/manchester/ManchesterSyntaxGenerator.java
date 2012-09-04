package net.enilink.komma.parser.manchester;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.enilink.vocab.owl.DataRange;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.rdfs.Datatype;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

/**
 * Generator for the <a
 * href="http://www.w3.org/2007/OWL/wiki/ManchesterSyntax">Manchester OWL
 * Syntax</a>.
 */
public class ManchesterSyntaxGenerator {
	static final String FACET_QUERY = createFacetQuery();

	private static String createFacetQuery() {
		StringBuilder sb = new StringBuilder("PREFIX xsd: <"
				+ XMLSCHEMA.NAMESPACE + ">\n");
		sb.append("select ?facet ?value where {\n");
		Iterator<String> facets = Arrays.asList("length", "minLength",
				"maxLength", "pattern", "langPattern", "minInclusive",
				"minExclusive", "maxInclusive", "maxExclusive").iterator();
		while (facets.hasNext()) {
			String facet = "xsd:" + facets.next();
			sb.append("\t{ ?s ").append(facet).append(" ?value\n");
			sb.append("\tbind ( ").append(facet).append(" as ?facet ) }\n");
			if (facets.hasNext()) {
				sb.append("\tunion\n");
			}
		}
		sb.append("} limit 1");
		return sb.toString();
	}

	// maps XSD facets to shorthand notations
	static final Map<String, String> FACET_SHORTHANDS = new HashMap<String, String>();
	static {
		FACET_SHORTHANDS.put("minInclusive", "<=");
		FACET_SHORTHANDS.put("minExclusive", "<");
		FACET_SHORTHANDS.put("maxInclusive", ">=");
		FACET_SHORTHANDS.put("maxExclusive", ">");
	}

	public String generateText(Object object) {
		if (object instanceof Class) {
			return clazz((Class) object, 0).toString();
		}
		return value(object).toString();
	}

	protected ManchesterSyntaxGenerator append(Object token) {
		sb.append(token);
		return this;
	}

	private StringBuilder sb = new StringBuilder();

	private ManchesterSyntaxGenerator clazz(Class clazz, int prio) {
		if (clazz.getURI() == null) {
			if (clazz instanceof Restriction) {
				return restriction((Restriction) clazz);
			} else if (clazz instanceof Datatype) {
				append(toString(((Datatype) clazz).getOwlOnDatatype()));
				return datatypeRestrictions(((Datatype) clazz)
						.getOwlWithRestrictions());
			} else if (clazz instanceof net.enilink.vocab.owl.Class) {
				net.enilink.vocab.owl.Class owlClass = (net.enilink.vocab.owl.Class) clazz;
				if (owlClass.getOwlUnionOf() != null) {
					return setOfClasses(owlClass.getOwlUnionOf(), "or", 1, prio);
				} else if (owlClass.getOwlIntersectionOf() != null) {
					return setOfClasses(owlClass.getOwlIntersectionOf(), "and", 2, prio);
				} else if (owlClass.getOwlComplementOf() != null) {
					append("not ");
					return clazz(owlClass.getOwlComplementOf(), 3);
				} else if (owlClass.getOwlOneOf() != null) {
					return list(owlClass.getOwlOneOf());
				}
			}
		}

		return value(clazz);
	}

	/**
	 * Converts a list of datatype restrictions to a Manchester expression in
	 * the form [facet1 value1, facet2 value2, ...]
	 * 
	 * Example: xsd:int[>=18]
	 * 
	 * @param list
	 *            The list of datatype restrictions which are expressed using
	 *            XML Schmema facets.
	 * 
	 * @return The generator instance.
	 */
	private ManchesterSyntaxGenerator datatypeRestrictions(List<?> list) {
		append("[");
		Iterator<? extends Object> it = list.iterator();
		while (it.hasNext()) {
			IEntity dtRestriction = (IEntity) it.next();
			for (IBindings<?> bindings : dtRestriction.getEntityManager()
					.createQuery(FACET_QUERY).setParameter("s", dtRestriction)
					.evaluate(IBindings.class)) {
				IReference facet = (IReference) bindings.get("facet");
				String facetShortHand = FACET_SHORTHANDS.get(facet.getURI()
						.localPart());
				if (facetShortHand == null) {
					facetShortHand = facet.getURI().localPart();
				}
				append(facetShortHand).append(toString(bindings.get("value")));
				if (it.hasNext()) {
					append(", ");
				}
			}
		}
		append("]");
		return this;
	}

	private ManchesterSyntaxGenerator dataRange(DataRange dataRange) {
		return clazz(dataRange, 0);
	}

	private ManchesterSyntaxGenerator list(List<? extends Object> list) {
		append("{");
		Iterator<? extends Object> it = list.iterator();
		while (it.hasNext()) {
			value(it.next());
			if (it.hasNext()) {
				append(" ").append(", ").append(" ");
			}
		}
		append("}");
		return this;
	}

	private ManchesterSyntaxGenerator onClassOrDataRange(Restriction restriction) {
		if (restriction.getOwlOnClass() != null) {
			return clazz(restriction.getOwlOnClass(), 0);
		} else if (restriction.getOwlOnDataRange() != null) {
			return dataRange(restriction.getOwlOnDataRange());
		}
		return this;
	}

	public ManchesterSyntaxGenerator restriction(Restriction restriction) {
		if (restriction.getURI() == null) {
			if (restriction.getOwlOnProperty() != null) {
				value(restriction.getOwlOnProperty());
			} else if (restriction.getOwlOnProperties() != null) {
				// TODO How is this correctly represented as manchester syntax?
				list(restriction.getOwlOnProperties());
			} else {
				// this is an invalid restriction, since target properties are
				// missing, so just return the name of this restriction
				return value(restriction);
			}

			append(" ");

			if (restriction.getOwlAllValuesFrom() != null) {
				append("only").append(" ");
				clazz(restriction.getOwlAllValuesFrom(), 0);
			} else if (restriction.getOwlSomeValuesFrom() != null) {
				append("some").append(" ");
				clazz(restriction.getOwlSomeValuesFrom(), 0);
			} else if (restriction.getOwlMaxCardinality() != null) {
				append("max").append(" ");
				append(restriction.getOwlMaxCardinality());
			} else if (restriction.getOwlMinCardinality() != null) {
				append("min").append(" ");
				append(restriction.getOwlMinCardinality());
			} else if (restriction.getOwlCardinality() != null) {
				append("exactly").append(" ");
				append(restriction.getOwlCardinality());
			} else if (restriction.getOwlMaxQualifiedCardinality() != null) {
				append("max").append(" ");
				append(restriction.getOwlMaxQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlMinQualifiedCardinality() != null) {
				append("min").append(" ");
				append(restriction.getOwlMinQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlQualifiedCardinality() != null) {
				append("exactly").append(" ");
				append(restriction.getOwlQualifiedCardinality()).append(" ");
				onClassOrDataRange(restriction);
			} else if (restriction.getOwlHasValue() != null) {
				append("value").append(" ");
				value(restriction.getOwlHasValue());
			}

			return this;
		}
		return value(restriction);
	}
	
	private ManchesterSyntaxGenerator setOfClasses(List<? extends Class> set,
			String operator, int operatorPrio, int prio) {
		Iterator<? extends Class> it = set.iterator();
		if (operatorPrio < prio && set.size() > 1) {
			append("(");
		}
		while (it.hasNext()) {
			clazz(it.next(), operatorPrio);
			if (it.hasNext()) {
				append(" ").append(operator).append(" ");
			}
		}
		if (operatorPrio < prio && set.size() > 1) {
			append(")");
		}
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	protected ManchesterSyntaxGenerator value(Object value) {
		if (value instanceof ILiteral) {
			ILiteral literal = (ILiteral) value;
			boolean quoted = XMLSCHEMA.TYPE_STRING
					.equals(literal.getDatatype())
					|| literal.getDatatype() == null;
			if (quoted) {
				append("\"");
			}
			append(literal.getLabel());
			if (quoted) {
				append("\"");
			}
			if (literal.getDatatype() != null) {
				append("^^").append(toString(literal.getDatatype()));
			}
		} else {
			append(toString(value));
		}
		return this;
	}

	protected String getPrefix(IReference reference) {
		if (reference instanceof IEntity) {
			return ((IEntity) reference).getEntityManager().getPrefix(
					reference.getURI().namespace());
		}
		return null;
	}

	protected String toString(Object value) {
		if (value instanceof IReference) {
			URI uri = ((IReference) value).getURI();
			if (uri != null) {
				String prefix = getPrefix((IReference) value);
				String localPart = uri.localPart();
				boolean hasLocalPart = localPart != null
						&& localPart.length() > 0;
				StringBuilder text = new StringBuilder();
				if (prefix != null && prefix.length() > 0 && hasLocalPart) {
					text.append(prefix).append(":");
				}
				if (hasLocalPart && prefix != null) {
					text.append(localPart);
				} else {
					text.append("<").append(uri.toString()).append(">");
				}
				return text.toString();
			}
		}
		return String.valueOf(value);
	}
}

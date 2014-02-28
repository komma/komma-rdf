package net.enilink.komma.edit.ui.properties.internal.parts;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.ui.assist.ContentProposals;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Literal;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.properties.IResourceProposal;
import net.enilink.komma.edit.properties.PropertyEditingHelper;
import net.enilink.komma.edit.properties.PropertyEditingHelper.Type;
import net.enilink.komma.edit.ui.assist.JFaceContentProposal;
import net.enilink.komma.edit.ui.assist.JFaceProposalProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainView;
import net.enilink.komma.model.ModelUtil;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class StatementView extends AbstractEditingDomainView {
	class StatementPart extends AbstractEditingDomainPart {
		class MyPropertyEditingHelper extends PropertyEditingHelper {
			MyPropertyEditingHelper(Type type) {
				super(type);
			}

			@Override
			protected IStatement getStatement(Object element) {
				return (IStatement) element;
			}

			@Override
			protected IEditingDomain getEditingDomain() {
				return StatementPart.this.getEditingDomain();
			}
		};

		IAdapterFactory adapterFactory;
		AdapterFactoryLabelProvider labelProvider;

		CLabel sLabel;
		Label pIcon;

		Text pText;
		Text valueText;
		Text langText;
		Text typeText;

		Button okButton;

		IStatement stmt;

		IResourceProposal acceptedResourceProposal;

		MyPropertyEditingHelper propertyHelper = new MyPropertyEditingHelper(
				Type.PROPERTY);
		MyPropertyEditingHelper valueHelper = new MyPropertyEditingHelper(
				Type.VALUE);

		@Override
		public void createContents(Composite parent) {
			parent.setLayout(new GridLayout(2, true));

			sLabel = getWidgetFactory().createCLabel(parent, "");
			sLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
					false));

			Composite pComposite = getWidgetFactory().createComposite(parent);
			pComposite.setLayout(new GridLayout(2, false));
			pComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
					true, false));
			pIcon = getWidgetFactory().createLabel(pComposite, "");
			GridData data = new GridData(SWT.RIGHT, SWT.BEGINNING, false, false);
			data.widthHint = 20;
			pIcon.setLayoutData(data);

			pText = getWidgetFactory().createText(pComposite, "");
			pText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
					false));
			createProposalAdapter(pText, propertyHelper);

			valueText = getWidgetFactory().createText(parent, "",
					SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.verticalIndent = 10;
			data.horizontalSpan = 2;
			valueText.setLayoutData(data);
			valueText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					acceptedResourceProposal = null;
					setDirty(true);
				}
			});

			ContentProposalAdapter valueProposalAdapter = createProposalAdapter(
					valueText, valueHelper);
			valueProposalAdapter
					.addContentProposalListener(new IContentProposalListener() {
						@Override
						public void proposalAccepted(IContentProposal proposal) {
							if (((JFaceContentProposal) proposal).getDelegate() instanceof IResourceProposal) {
								IResourceProposal resourceProposal = (IResourceProposal) ((JFaceContentProposal) proposal)
										.getDelegate();
								acceptedResourceProposal = resourceProposal
										.getUseAsValue() ? resourceProposal
										: null;
								setDirty(true);
							}
						}
					});
			Composite footer = getWidgetFactory().createComposite(parent);
			footer.setLayout(new GridLayout(5, false));
			data = new GridData(SWT.FILL, SWT.END, true, false);
			data.horizontalSpan = 2;
			footer.setLayoutData(data);

			getWidgetFactory().createLabel(footer, "@");
			langText = getWidgetFactory().createText(footer, "",
					SWT.SINGLE | SWT.H_SCROLL);
			data = new GridData();
			data.widthHint = 50;
			langText.setLayoutData(data);
			langText.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					if (!langText.getText().isEmpty()
							&& !typeText.getText().isEmpty()) {
						typeText.setText("");
					}
				}
			});
			langText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					setDirty(true);
				}
			});
			Label typeLabel = getWidgetFactory().createLabel(footer, "^^");
			data = new GridData();
			data.horizontalIndent = 10;
			typeLabel.setLayoutData(data);
			typeText = getWidgetFactory().createText(footer, "",
					SWT.SINGLE | SWT.H_SCROLL);
			createProposalAdapter(typeText, new MyPropertyEditingHelper(
					Type.LITERAL_LANG_TYPE));
			typeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
					false));
			typeText.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					if (!typeText.getText().isEmpty()
							&& !langText.getText().isEmpty()) {
						langText.setText("");
					}
				}
			});
			typeText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					setDirty(true);
				}
			});
			okButton = getWidgetFactory().createButton(footer, "OK", SWT.PUSH);
			data = new GridData();
			data.horizontalIndent = 10;
			okButton.setLayoutData(data);
			okButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					commit(false);
				}
			});
			getWidgetFactory().paintBordersFor(parent);
		}

		protected ContentProposalAdapter createProposalAdapter(Text text,
				final PropertyEditingHelper editingHelper) {
			return ContentProposals.enableContentProposal(text,
					new IContentProposalProvider() {
						@Override
						public IContentProposal[] getProposals(String contents,
								int position) {
							if (stmt != null) {
								IPropertyEditingSupport.ProposalSupport proposalSupport = editingHelper
										.getProposalSupport(stmt);
								if (proposalSupport != null) {
									return JFaceProposalProvider.wrap(
											proposalSupport
													.getProposalProvider())
											.getProposals(contents, position);
								}
							}
							return new IContentProposal[0];
						}
					}, null);
		}

		@Override
		public boolean setEditorInput(Object input) {
			if (input instanceof IStatement) {
				this.stmt = (IStatement) input;
				setStale(true);
				return true;
			} else if (input instanceof StatementNode) {
				this.stmt = ((StatementNode) input).getStatement();
				setStale(true);
				return true;
			}
			return false;
		}

		@Override
		public void commit(boolean onSave) {
			if (isDirty() && stmt != null) {
				String lang = langText.getText().trim();
				String type = typeText.getText().trim();
				Object newValue = acceptedResourceProposal;
				if (!lang.isEmpty()) {
					newValue = new Literal(valueText.getText(), lang);
				} else if (!type.isEmpty()) {
					IEntityManager em = ((IEntity) stmt.getSubject())
							.getEntityManager();
					URI literalType = null;
					if (type.matches("<.*>")) {
						try {
							literalType = URIImpl.createURI(type.substring(1,
									type.length() - 1));
						} catch (IllegalArgumentException e) {
							// should be shown in UI
						}
					} else {
						String prefix = "";
						int colon = type.indexOf(':');
						if (colon >= 0) {
							prefix = type.substring(0, colon);
							type = type.substring(colon + 1, type.length());
						}
						URI ns = em.getNamespace(prefix);
						if (ns != null) {
							literalType = ns.appendLocalPart(type);
						}
					}
					if (literalType != null) {
						newValue = new Literal(valueText.getText(), literalType);
					} else {
						// unknown literal type
					}
				}
				if (newValue == null) {
					newValue = valueText.getText();
				}
				CommandResult result = valueHelper.setValue(stmt, newValue);
				if (result.getStatus().isOK()
						&& result.getReturnValue() != null) {
					stmt = new Statement(stmt.getSubject(),
							stmt.getPredicate(), result.getReturnValue());
				}
			}
			setDirty(false);
		}

		@Override
		public void refresh() {
			IAdapterFactory newAdapterFactory = getAdapterFactory();
			if (adapterFactory == null
					|| !adapterFactory.equals(newAdapterFactory)) {
				adapterFactory = newAdapterFactory;
				if (labelProvider != null) {
					labelProvider.dispose();
					labelProvider = null;
				}
				if (adapterFactory != null) {
					labelProvider = new AdapterFactoryLabelProvider(
							adapterFactory);
				}
			}
			if (labelProvider != null && stmt != null) {
				sLabel.setText(ModelUtil.getLabel(stmt.getSubject()));
				sLabel.setImage(labelProvider.getImage(stmt.getSubject()));
				if (stmt.getPredicate() != null) {
					pText.setText(labelProvider.getText(stmt.getPredicate()));
					pIcon.setImage(labelProvider.getImage(stmt.getPredicate()));
				} else {
					pText.setText("");
					pIcon.setImage(null);
				}
				Object value = stmt.getObject();
				valueText.setText(labelProvider.getText(value));
				if (value instanceof ILiteral) {
					ILiteral literal = (ILiteral) value;
					langText.setText(literal.getLanguage() == null ? ""
							: literal.getLanguage());
					typeText.setText(literal.getDatatype() == null ? ""
							: labelProvider.getText(((IEntity) stmt
									.getSubject()).getEntityManager().find(
									literal.getDatatype())));
				} else {
					langText.setText("");
					typeText.setText("");
				}
				langText.setEnabled(!(value instanceof IReference));
				typeText.setEnabled(!(value instanceof IReference));
			}
			acceptedResourceProposal = null;
			super.refresh();
		}

		@Override
		public void setDirty(boolean dirty) {
			if (okButton != null) {
				okButton.setEnabled(dirty);
			}
			super.setDirty(dirty);
		}
	}

	public StatementView() {
		setEditPart(new StatementPart());
	}
}
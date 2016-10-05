/**
 *  Copyright (c) 2015-2016 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package ts.eclipse.ide.json.ui.internal.tsconfig;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.wst.json.core.databinding.ExtendedJSONPath;
import org.eclipse.wst.json.core.databinding.JSONProperties;

import ts.eclipse.ide.core.utils.TypeScriptResourceUtil;
import ts.eclipse.ide.core.utils.WorkbenchResourceUtil;
import ts.eclipse.ide.json.ui.internal.AbstractFormPage;
import ts.eclipse.ide.json.ui.internal.FormLayoutFactory;
import ts.eclipse.ide.ui.TypeScriptUIImageResource;
import ts.eclipse.ide.ui.utils.DialogUtils;
import ts.eclipse.ide.ui.utils.EditorUtils;

/**
 * Scope (files, include, exclude) page for tsconfig.json editor.
 *
 */
public class FilesPage extends AbstractFormPage {

	private static final String ID = "files";
	private TableViewer filesViewer;
	private TableViewer includeViewer;
	private TableViewer excludeViewer;

	private Button filesOpenButton;
	private Button filesRemoveButton;
	private Button includeRemoveButton;
	private Button excludeRemoveButton;

	public FilesPage(TsconfigEditor editor) {
		super(editor, ID, TsconfigEditorMessages.FilesPage_title);
	}

	private class FilesLabelProvider extends LabelProvider implements ILabelDecorator {

		@Override
		public Image getImage(Object element) {
			if (TypeScriptResourceUtil.isTsxOrJsxFile(element)) {
				return TypeScriptUIImageResource.getImage(TypeScriptUIImageResource.IMG_JSX);
			} else if (TypeScriptResourceUtil.isTsOrTsxFile(element)) {
				return TypeScriptUIImageResource.getImage(TypeScriptUIImageResource.IMG_TS);
			}
			return super.getImage(element);
		}

		@Override
		public Image decorateImage(Image image, Object object) {
			return null;
		}

		@Override
		public String decorateText(String label, Object object) {
			if (!fileExists((String) label)) {
				return label + " (not found)";
			}
			return null;
		}
	}

	@Override
	protected String getFormTitleText() {
		return TsconfigEditorMessages.FilesPage_title;
	}

	@Override
	protected void createUI(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 2));
		createLeftContent(body);
		createRightContent(body);
		updateButtons();
	}

	private void updateButtons() {
		ISelection selection = filesViewer.getSelection();
		boolean hasSelectedFile = !selection.isEmpty();
		updateFilesOpenButton(selection);
		filesRemoveButton.setEnabled(hasSelectedFile);
		includeRemoveButton.setEnabled(!includeViewer.getSelection().isEmpty());
		excludeRemoveButton.setEnabled(!excludeViewer.getSelection().isEmpty());
	}

	private void updateFilesOpenButton(ISelection selection) {
		if (filesOpenButton != null) {
			filesOpenButton.setEnabled(
					!selection.isEmpty() && fileExists((String) ((IStructuredSelection) selection).getFirstElement()));
		}
	}

	private void createLeftContent(Composite parent) {
		FormToolkit toolkit = super.getToolkit();
		Composite left = toolkit.createComposite(parent);
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		left.setLayoutData(new GridData(GridData.FILL_BOTH));
		createFilesSection(left);
	}

	/**
	 * Create Files section.
	 * 
	 * @param parent
	 */
	private void createFilesSection(Composite parent) {
		final IFile tsconfigFile = getTsconfigFile();
		FormToolkit toolkit = super.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
		section.setDescription(TsconfigEditorMessages.FilesPage_FilesSection_desc);
		section.setText(TsconfigEditorMessages.FilesPage_FilesSection_title);

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		client.setLayout(layout);

		Table table = toolkit.createTable(client, SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.minimumHeight = 100;
		gd.widthHint = 100;
		table.setLayoutData(gd);

		// Buttons
		Composite buttonsComposite = toolkit.createComposite(client);
		buttonsComposite.setLayout(new GridLayout());
		buttonsComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		if (tsconfigFile != null) {
			final Button addButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_add,
					SWT.PUSH);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			addButton.setLayoutData(gd);
			addButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// Get existing ts files
					Collection<IResource> existingFiles = getExistingFiles(tsconfigFile.getParent());
					Object[] resources = DialogUtils.openTypeScriptResourcesDialog(tsconfigFile.getProject(),
							existingFiles, addButton.getShell());
					if (resources != null && resources.length > 0) {
						IPath path = null;
						Collection<String> elements = new ArrayList<String>(resources.length);
						for (int i = 0; i < resources.length; i++) {
							path = WorkbenchResourceUtil.getRelativePath((IResource) resources[i],
									tsconfigFile.getParent());
							elements.add(path.toString());
						}
						IObservableList list = ((IObservableList) filesViewer.getInput());
						list.addAll(elements);
					}
				}

				private Collection<IResource> getExistingFiles(IContainer parent) {
					if (filesViewer.getSelection().isEmpty()) {
						return null;
					}
					Collection<IResource> resources = new ArrayList<IResource>();
					Object[] files = filesViewer.getStructuredSelection().toArray();
					for (int i = 0; i < files.length; i++) {
						IResource f = parent.getFile(new Path((String) files[i]));
						if (f.exists()) {
							resources.add(f);
						}
					}
					return resources;
				}
			});
		}

		filesRemoveButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_remove, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		filesRemoveButton.setLayoutData(gd);
		filesRemoveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IObservableList list = ((IObservableList) filesViewer.getInput());
				list.removeAll(filesViewer.getStructuredSelection().toList());
			}
		});

		if (tsconfigFile != null) {
			filesOpenButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_open, SWT.PUSH);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			filesOpenButton.setLayoutData(gd);
			filesOpenButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openFile(filesViewer.getSelection());
				}
			});
		}

		// Files table
		filesViewer = new TableViewer(table);
		FilesLabelProvider labelProvider = new FilesLabelProvider();
		filesViewer.setLabelProvider(new DecoratingLabelProvider(labelProvider, labelProvider));
		filesViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent e) {
				openFile(filesViewer.getSelection());
			}
		});
		filesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateFilesOpenButton(event.getSelection());
				filesRemoveButton.setEnabled(true);
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		IObservableList files = JSONProperties.list(new ExtendedJSONPath("files[*]"))
				.observe(getEditor().getDocument());
		filesViewer.setContentProvider(new ObservableListContentProvider());
		filesViewer.setInput(files);

	}

	/**
	 * Open in an editor the selected file of the table files.
	 * 
	 * @param selection
	 */
	private void openFile(ISelection selection) {
		if (selection.isEmpty()) {
			return;
		}
		String file = (String) ((IStructuredSelection) selection).getFirstElement();
		IFile tsconfigFile = getTsconfigFile();
		if (tsconfigFile != null) {
			IFile tsFile = tsconfigFile.getParent().getFile(new Path(file));
			if (tsFile.exists()) {
				EditorUtils.openInEditor(tsFile, true);
			}
		}
	}

	private IFile getTsconfigFile() {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			return ((IFileEditorInput) input).getFile();
		}
		return null;
	}

	private void createExcludeSection(Composite parent) {
		final IFile tsconfigFile = getTsconfigFile();
		FormToolkit toolkit = super.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
		section.setDescription(TsconfigEditorMessages.FilesPage_ExcludeSection_desc);
		section.setText(TsconfigEditorMessages.FilesPage_ExcludeSection_title);

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		client.setLayout(layout);

		Table table = toolkit.createTable(client, SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 20;
		gd.widthHint = 100;
		table.setLayoutData(gd);

		Composite buttonsComposite = toolkit.createComposite(client);
		buttonsComposite.setLayout(new GridLayout());
		buttonsComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		if (tsconfigFile != null) {
			final Button addButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_add,
					SWT.PUSH);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			addButton.setLayoutData(gd);
			addButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					MessageDialog.openInformation(addButton.getShell(), "TODO!", "TODO!");
				}
			});
		}

		excludeRemoveButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_remove, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		excludeRemoveButton.setLayoutData(gd);
		excludeRemoveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MessageDialog.openInformation(excludeRemoveButton.getShell(), "TODO!", "TODO!");
			}
		});

		excludeViewer = new TableViewer(table);
		excludeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				excludeRemoveButton.setEnabled(true);
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		IObservableList exclude = JSONProperties.list(new ExtendedJSONPath("exclude[*]"))
				.observe(getEditor().getDocument());
		excludeViewer.setContentProvider(new ObservableListContentProvider());
		excludeViewer.setInput(exclude);

	}

	private void createIncludeSection(Composite parent) {
		final IFile tsconfigFile = getTsconfigFile();
		FormToolkit toolkit = super.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
		section.setDescription(TsconfigEditorMessages.FilesPage_IncludeSection_desc);
		section.setText(TsconfigEditorMessages.FilesPage_IncludeSection_title);

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		client.setLayout(layout);

		Table table = toolkit.createTable(client, SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 20;
		gd.widthHint = 100;
		table.setLayoutData(gd);

		Composite buttonsComposite = toolkit.createComposite(client);
		buttonsComposite.setLayout(new GridLayout());
		buttonsComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		if (tsconfigFile != null) {
			final Button addButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_add,
					SWT.PUSH);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			addButton.setLayoutData(gd);
			addButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					MessageDialog.openInformation(addButton.getShell(), "TODO!", "TODO!");
				}
			});
		}

		includeRemoveButton = toolkit.createButton(buttonsComposite, TsconfigEditorMessages.Button_remove, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		includeRemoveButton.setLayoutData(gd);
		includeRemoveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MessageDialog.openInformation(includeRemoveButton.getShell(), "TODO!", "TODO!");
			}
		});

		includeViewer = new TableViewer(table);
		includeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				includeRemoveButton.setEnabled(true);
			}
		});
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		IObservableList include = JSONProperties.list(new ExtendedJSONPath("include[*]"))
				.observe(getEditor().getDocument());
		includeViewer.setContentProvider(new ObservableListContentProvider());
		includeViewer.setInput(include);

	}

	private void createRightContent(Composite parent) {
		FormToolkit toolkit = super.getToolkit();
		Composite right = toolkit.createComposite(parent);
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		right.setLayoutData(new GridData(GridData.FILL_BOTH));
		createExcludeSection(right);
		createIncludeSection(right);
		// createScopeSection(right);
	}

	private void createScopeSection(Composite parent) {
		FormToolkit toolkit = super.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
		section.setDescription(TsconfigEditorMessages.FilesPage_ScopeSection_desc);
		section.setText(TsconfigEditorMessages.FilesPage_ScopeSection_title);
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		client.setLayout(layout);

		Table table = toolkit.createTable(client, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 100;
		table.setLayoutData(gd);
	}

	@Override
	protected void updateUIBindings() {
		super.updateUIBindings();
		excludeViewer.refresh();
		includeViewer.refresh();
		filesViewer.refresh();
		updateButtons();
	}

	public boolean fileExists(String file) {
		IFile tsconfigFile = getTsconfigFile();
		if (tsconfigFile == null) {
			return true;
		}
		return tsconfigFile.getParent().exists(new Path(file));
	}

}

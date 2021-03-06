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
package ts.eclipse.ide.jsdt.internal.ui.validation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import ts.eclipse.ide.core.utils.TypeScriptResourceUtil;
import ts.eclipse.ide.jsdt.internal.ui.JSDTTypeScriptUIPlugin;
import ts.eclipse.ide.ui.utils.EditorUtils;

/**
 * JavaScript editor tracker.
 */
public class JSDTEditorTracker implements IWindowListener, IPageListener, IPartListener {
	static JSDTEditorTracker INSTANCE;

	Map<IEditorPart, TypeScriptDocumentRegionProcessor> fAsYouTypeValidators = new HashMap<IEditorPart, TypeScriptDocumentRegionProcessor>();

	private JSDTEditorTracker() {
		init();
	}

	public static JSDTEditorTracker getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new JSDTEditorTracker();
		}
		return INSTANCE;
	}

	private void init() {
		if (PlatformUI.isWorkbenchRunning()) {
			IWorkbench workbench = JSDTTypeScriptUIPlugin.getDefault().getWorkbench();
			if (workbench != null) {
				IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
				for (IWorkbenchWindow window : windows) {
					windowOpened(window);
				}
				JSDTTypeScriptUIPlugin.getDefault().getWorkbench().addWindowListener(this);
			}
		}
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		IWorkbenchPage[] pages = window.getPages();
		for (IWorkbenchPage page : pages) {
			pageClosed(page);
		}
		window.removePageListener(this);
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		if (window.getShell() != null) {
			IWorkbenchPage[] pages = window.getPages();
			for (IWorkbenchPage page : pages) {
				pageOpened(page);
			}
			window.addPageListener(this);
		}
	}

	@Override
	public void pageActivated(IWorkbenchPage page) {
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		IEditorReference[] rs = page.getEditorReferences();
		for (IEditorReference r : rs) {
			IEditorPart part = r.getEditor(false);
			if (part != null) {
				editorClosed(part);
			}
		}
		page.removePartListener(this);
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		IEditorReference[] rs = page.getEditorReferences();
		for (IEditorReference r : rs) {
			IEditorPart part = r.getEditor(false);
			if (part != null) {
				editorOpened(part);
			}
		}
		page.addPartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorClosed((IEditorPart) part);
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorOpened((IEditorPart) part);
		}
	}

	private void editorOpened(IEditorPart part) {
		if (part instanceof ITextEditor) {
			IResource resource = EditorUtils.getResource(part);
			if (resource != null && TypeScriptResourceUtil.canConsumeTsserver(resource)) {
				ISourceViewer viewer = EditorUtils.getSourceViewer(part);
				if (viewer != null) {
					TypeScriptDocumentRegionProcessor processor = fAsYouTypeValidators.get(part);
					if (processor != null) {
						// Emulate editor closed due to uninstall the old
						// processor
						editorClosed(part);
						Assert.isTrue(null == fAsYouTypeValidators.get(part),
								"An old TypeScriptDocumentRegionProcessor is not un-installed on Java Editor instance");
					}

					// try {
					processor = new TypeScriptDocumentRegionProcessor(resource);
					processor.install(viewer);
					processor.setDocument(viewer.getDocument());
					processor.startReconciling();
					fAsYouTypeValidators.put(part, processor);
					/*
					 * } catch (CoreException e) { Trace.trace( Trace.SEVERE,
					 * "Error while getting tern project for validation.", e); }
					 */
				}
			}
		}
	}

	private void editorClosed(IEditorPart part) {
		if (part instanceof ITextEditor) {
			TypeScriptDocumentRegionProcessor processor = fAsYouTypeValidators.remove(part);
			if (processor != null) {
				processor.uninstall();
				Assert.isTrue(null == fAsYouTypeValidators.get(part),
						"An old TypeScriptDocumentRegionProcessor is not un-installed on Java Editor instance");
			}
		}
	}
}
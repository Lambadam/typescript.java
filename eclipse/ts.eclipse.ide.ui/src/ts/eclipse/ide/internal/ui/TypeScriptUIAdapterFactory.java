package ts.eclipse.ide.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.ILaunchable;

import ts.eclipse.ide.core.resources.buildpath.ITypeScriptRootContainer;

public class TypeScriptUIAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.equals(ILaunchable.class)) {
			if (adaptableObject instanceof ITypeScriptRootContainer) {
				return ((ITypeScriptRootContainer) adaptableObject);
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[] { ILaunchable.class };
	}
}

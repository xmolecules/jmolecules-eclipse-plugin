package org.jmolecules.eclipse.plugin.explorer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

final class ProjectUtils {

    private ProjectUtils() {
    }

    static boolean hasNature(IProject project, String id) {
        try {
            return project.hasNature(id);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
}

package org.jmolecules.eclipse.plugin.explorer;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

class ImageProvider {

    private final List<Function<String, ImageDescriptor>> imageDescriptorSuppliers;
    private final List<Function<String, Image>> imageSuppliers;

    ImageProvider() {
        imageDescriptorSuppliers = List.of( //
            (n) -> JavaUI.getSharedImages().getImageDescriptor(n), //
            (n) -> PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(n) //
        );
        imageSuppliers = List.of( //
            (n) -> JavaUI.getSharedImages().getImage(n), //
            (n) -> PlatformUI.getWorkbench().getSharedImages().getImage(n) //
        );
    }

    Optional<Image> getImage(String name) {
        return ofNullable(obtainImage(name));
    }

    Optional<ImageDescriptor> getImageDescriptor(String name) {
        return ofNullable(obtainImageDescriptor(name));
    }

    private Image obtainImage(String name) {
        return imageSuppliers //
            .stream() //
            .map(s -> s.apply(name)) //
            .filter(Objects::nonNull) //
            .limit(1) //
            .findAny() //
            .orElse(null);
    }

    private ImageDescriptor obtainImageDescriptor(String name) {
        return imageDescriptorSuppliers //
            .stream() //
            .map(s -> s.apply(name)) //
            .filter(Objects::nonNull) //
            .limit(1) //
            .findAny() //
            .orElse(null);
    }
}

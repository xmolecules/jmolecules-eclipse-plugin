/*-
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

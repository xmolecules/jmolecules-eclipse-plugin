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

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.DDD;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.EVENTS;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.ONION_ARCHITECTURE;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getAnnotations;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getImports;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isAnnotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category;

class JMolecules {

    private final List<Concept> concepts;

    public JMolecules() {
        concepts = init();
    }

    <T extends IJavaElement> Concepts expresses(T source) {
        return new Concepts(concepts.stream().filter(c -> c.test(source)).collect(toList()));
    }

    private static List<Concept> init() {
        List<Concept> concepts = new ArrayList<>();
        // DDD based concepts
        concepts.add(new AggregateRoot());
        concepts.add(new BoundedContext());
        concepts.add(new Entity());
        concepts.add(new Identity());
        concepts.add(new Service());
        concepts.add(new DomainEvent());
        concepts.add(new DomainEventHandler());
        concepts.add(new DomainRing());
        return concepts;
    }

    static class Concepts {

        private final List<Concept> concepts;

        Concepts(List<Concept> concepts) {
            this.concepts = concepts;
        }

        List<Concept> get() {
            return unmodifiableList(concepts);
        }

        Set<Category> getCategories() {
            return concepts.stream().map(Concept::getCategory).collect(toSet());
        }

        boolean isEmpty() {
            return concepts.isEmpty();
        }

        boolean contains(Concept concept) {
            return concepts.contains(concept);
        }

        Concepts merge(Concepts... sources) {
            Set<Concept> collected = new HashSet<>(concepts);
            stream(sources).forEach(s -> collected.addAll(s.get()));
            return new Concepts(copyOf(collected));
        }

        static Concepts empty() {
            return new Concepts(List.of());
        }
    }

    static interface Concept extends Predicate<IJavaElement>, Comparable<Concept> {

        @Override
        default int compareTo(Concept other) {
            int result = getCategory().compareTo(other.getCategory());
            return result == 0 ? getName().compareTo(other.getName()) : result;
        }

        default String getName() {
            return getClass().getSimpleName();
        }

        Category getCategory();

        enum Category {
                DDD, EVENTS, CQRS_ARCHITECTURE, LAYERED_ARCHITECTURE, ONION_ARCHITECTURE
        }
    }

    static interface AnnotationBasedConcept extends Concept {

        default boolean test(String fcqn, IImportDeclaration[] imports, IAnnotation[] annotations) {
            if (stream(annotations).anyMatch(a -> a.getElementName().equals(fcqn))) {
                return true;
            }

            String pckg = substringBeforeLast(fcqn, ".").concat(".");
            String name = substringAfterLast(fcqn, ".");

            return stream(imports).anyMatch(i -> i.getElementName().startsWith(pckg))
                    && (stream(annotations).anyMatch(a -> a.getElementName().equals(name)));
        }
    }

    static interface TypeBasedConcept extends Concept {
        // FIXME implement me... :)
    }

    static class AggregateRoot implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.ddd.annotation.AggregateRoot";
            return test(fcqn, imports, annotations);
        }
    }

    static class BoundedContext implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.ddd.annotation.BoundedContext";
            return test(fcqn, imports, annotations);
        }
    }

    static class Entity implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.ddd.annotation.Entity";
            return test(fcqn, imports, annotations);
        }
    }

    static class Identity implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IField)) {
                return false;
            }

            IField field = (IField) source;
            IAnnotation[] annotations = getAnnotations(field);
            ICompilationUnit compilationUnit = (ICompilationUnit) field.getCompilationUnit();
            IImportDeclaration[] imports = getImports(compilationUnit);

            String fcqn = "org.jmolecules.ddd.annotation.Identity";
            return test(fcqn, imports, annotations);
        }
    }

    static class Service implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            ICompilationUnit compilationUnit = (ICompilationUnit) type.getCompilationUnit();
            IImportDeclaration[] imports = getImports(compilationUnit);

            String fcqn = "org.jmolecules.ddd.annotation.Service";
            return test(fcqn, imports, annotations);
        }
    }

    static class DomainEvent implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.event.annotation.DomainEvent";
            return test(fcqn, imports, annotations);
        }
    }

    static class DomainEventHandler implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            ICompilationUnit compilationUnit = (ICompilationUnit) method.getCompilationUnit();
            IImportDeclaration[] imports = getImports(compilationUnit);

            String fcqn = "org.jmolecules.event.annotation.DomainEventHandler";
            return test(fcqn, imports, annotations);
        }
    }

    static class DomainRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            String fcqn = "org.jmolecules.architecture.onion.simplified.DomainRing";
            return test(fcqn, imports, annotations);
        }
    }
}

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
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.CQRS_ARCHITECTURE;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.DDD;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.EVENTS;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.LAYERED_ARCHITECTURE;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Category.ONION_ARCHITECTURE;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getAnnotations;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getImports;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isAnnotation;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.supertypeHierarchy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
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
        concepts.add(new Association());
        concepts.add(new BoundedContext());
        concepts.add(new Entity());
        concepts.add(new Factory());
        concepts.add(new Identity());
        concepts.add(new Module());
        concepts.add(new Repository());
        concepts.add(new Service());
        concepts.add(new ValueObject());

        // Event based concepts
        concepts.add(new DomainEvent());
        concepts.add(new DomainEventHandler());
        concepts.add(new DomainEventPublisher());

        // CQRS Architecture based concepts
        concepts.add(new Command());
        concepts.add(new CommandDispatcher());
        concepts.add(new CommandHandler());
        concepts.add(new QueryModel());

        // Layered Architecture based concepts
        concepts.add(new ApplicationLayer());
        concepts.add(new DomainLayer());
        concepts.add(new InfrastructureLayer());
        concepts.add(new InterfaceLayer());

        // Onion Architecture based concepts
        concepts.add(new ApplicationServiceRing());
        concepts.add(new DomainModelRing());
        concepts.add(new DomainServiceRing());
        concepts.add(new ClassicalInfrastructureRing());
        concepts.add(new ApplicationRing());
        concepts.add(new DomainRing());
        concepts.add(new SimplifiedInfrastructureRing());

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

                DDD("DDD"), //
                EVENTS("Events"), //
                CQRS_ARCHITECTURE("CQRS-Architecture"), //
                LAYERED_ARCHITECTURE("Layered-Architecture"), //
                ONION_ARCHITECTURE("Onion-Architecture");

            private final String humanReadable;

            private Category(String humanReadable) {
                this.humanReadable = humanReadable;
            }

            String humanReadable() {
                return humanReadable;
            }
        }
    }

    static interface AnnotationBasedConcept extends Concept {

        default boolean isAnnotationAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            return isAnnotating(type.getCompilationUnit(), type, fqcn);
        }

        default boolean isFieldAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IField)) {
                return false;
            }

            IField field = (IField) source;
            return isAnnotating(field.getCompilationUnit(), field, fqcn);
        }

        default boolean isMethodAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            return isAnnotating(method.getCompilationUnit(), method, fqcn);
        }

        default boolean isPackageAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            return isAnnotating((ICompilationUnit) packageDeclaration.getParent(), packageDeclaration, fqcn);
        }

        default boolean isTypeAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            return isAnnotating(type.getCompilationUnit(), type, fqcn);
        }

        private boolean isAnnotating(ICompilationUnit compilationUnit, IAnnotatable annotatable, String fqcn) {
            IAnnotation[] annotations = getAnnotations(annotatable);
            if (stream(annotations).anyMatch(a -> a.getElementName().equals(fqcn))) {
                return true;
            }

            IImportDeclaration[] imports = getImports(compilationUnit);
            String pckg = substringBeforeLast(fqcn, ".").concat(".");
            String name = substringAfterLast(fqcn, ".");

            return stream(imports).anyMatch(i -> i.getElementName().startsWith(pckg))
                    && (stream(annotations).anyMatch(a -> a.getElementName().equals(name)));
        }
    }

    static interface TypeBasedConcept extends Concept {

        default boolean isTypeImplementing(IJavaElement source, String fqcn) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            ITypeHierarchy hierarchy = supertypeHierarchy(type);
            IType[] interfaces = hierarchy.getAllInterfaces();

            return stream(interfaces).anyMatch(i -> i.getFullyQualifiedName().equals(fqcn));
        }
    }

    static class AggregateRoot implements AnnotationBasedConcept, TypeBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.AggregateRoot")
                    || isTypeImplementing(source, "org.jmolecules.ddd.types.AggregateRoot");
        }
    }

    static class Association implements TypeBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeImplementing(source, "org.jmolecules.ddd.types.Association");
        }
    }

    static class BoundedContext implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.ddd.annotation.BoundedContext";
            return isPackageAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class Entity implements AnnotationBasedConcept, TypeBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.Entity")
                    || isTypeImplementing(source, "org.jmolecules.ddd.types.Entity");
        }
    }

    static class Factory implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.Factory");
        }
    }

    static class Identity implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.ddd.annotation.Identity";
            return isFieldAnnotating(source, fqcn) || isMethodAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class Module implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.ddd.annotation.Module";
            return isPackageAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class Repository implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.Repository");
        }
    }

    static class Service implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.Service");
        }
    }

    static class ValueObject implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.ValueObject");
        }
    }

    static class DomainEvent implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.event.annotation.DomainEvent");
        }
    }

    static class DomainEventHandler implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.event.annotation.DomainEventHandler";
            return isMethodAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class DomainEventPublisher implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.event.annotation.DomainEventPublisher";
            return isMethodAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class Command implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.architecture.cqrs.annotation.Command");
        }
    }

    static class CommandDispatcher implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.cqrs.annotation.CommandDispatcher";
            return isMethodAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class CommandHandler implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.cqrs.annotation.CommandHandler";
            return isMethodAnnotating(source, fqcn) || isAnnotationAnnotating(source, fqcn);
        }
    }

    static class QueryModel implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.architecture.cqrs.annotation.QueryModel");
        }
    }

    static class ApplicationLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.layered.ApplicationLayer";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class DomainLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.layered.DomainLayer";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class InfrastructureLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.layered.InfrastructureLayer";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class InterfaceLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.layered.InterfaceLayer";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class ApplicationServiceRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.classical.ApplicationServiceRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class DomainModelRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.classical.DomainModelRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class DomainServiceRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.classical.DomainServiceRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class ClassicalInfrastructureRing implements AnnotationBasedConcept {

        @Override
        public String getName() {
            return "InfrastructureRing";
        }

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.classical.InfrastructureRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class ApplicationRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.simplified.ApplicationRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class DomainRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.simplified.DomainRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }

    static class SimplifiedInfrastructureRing implements AnnotationBasedConcept {

        @Override
        public String getName() {
            return "InfrastructureRing";
        }

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fqcn = "org.jmolecules.architecture.onion.simplified.InfrastructureRing";
            return isPackageAnnotating(source, fqcn) || isTypeAnnotating(source, fqcn);
        }
    }
}

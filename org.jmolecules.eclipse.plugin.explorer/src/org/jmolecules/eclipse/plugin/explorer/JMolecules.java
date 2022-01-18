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

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fqcn, imports, annotations);
        }

        default boolean isPackageAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fqcn, imports, annotations);
        }

        default boolean isTypeAnnotating(IJavaElement source, String fqcn) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fqcn, imports, annotations);
        }

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

    static class Entity implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            return isTypeAnnotating(source, "org.jmolecules.ddd.annotation.Entity");
        }
    }

    static class Factory implements AnnotationBasedConcept {

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

            String fcqn = "org.jmolecules.ddd.annotation.Factory";
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
            String fcqn = "org.jmolecules.ddd.annotation.Identity";
            return testField(fcqn, source) || testMethod(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testField(String fcqn, IJavaElement source) {
            if (!(source instanceof IField)) {
                return false;
            }

            IField field = (IField) source;
            IAnnotation[] annotations = getAnnotations(field);
            IImportDeclaration[] imports = getImports(field.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testMethod(String fcqn, IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            IImportDeclaration[] imports = getImports(method.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class Module implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return DDD;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.ddd.annotation.Module";
            return testPackage(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class Repository implements AnnotationBasedConcept {

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

            String fcqn = "org.jmolecules.ddd.annotation.Repository";
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
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.ddd.annotation.Service";
            return test(fcqn, imports, annotations);
        }
    }

    static class ValueObject implements AnnotationBasedConcept {

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

            String fcqn = "org.jmolecules.ddd.annotation.ValueObject";
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
            String fcqn = "org.jmolecules.event.annotation.DomainEventHandler";
            return testMethod(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testMethod(String fcqn, IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            IImportDeclaration[] imports = getImports(method.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class DomainEventPublisher implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return EVENTS;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.event.annotation.DomainEventPublisher";
            return testMethod(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testMethod(String fcqn, IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            IImportDeclaration[] imports = getImports(method.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class Command implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.architecture.cqrs.annotation.Command";
            return test(fcqn, imports, annotations);
        }
    }

    static class CommandDispatcher implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.cqrs.annotation.CommandDispatcher";
            return testMethod(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testMethod(String fcqn, IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            IImportDeclaration[] imports = getImports(method.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class CommandHandler implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.cqrs.annotation.CommandHandler";
            return testMethod(fcqn, source) || testAnnotation(fcqn, source);
        }

        private boolean testMethod(String fcqn, IJavaElement source) {
            if (!(source instanceof IMethod)) {
                return false;
            }

            IMethod method = (IMethod) source;
            IAnnotation[] annotations = getAnnotations(method);
            IImportDeclaration[] imports = getImports(method.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

        private boolean testAnnotation(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            if (!isAnnotation(type)) {
                return false;
            }

            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class QueryModel implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return CQRS_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            String fcqn = "org.jmolecules.architecture.cqrs.annotation.QueryModel";
            return test(fcqn, imports, annotations);
        }
    }

    static class ApplicationLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.layered.ApplicationLayer";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }

    }

    static class DomainLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.layered.DomainLayer";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class InfrastructureLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.layered.InfrastructureLayer";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class InterfaceLayer implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return LAYERED_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.layered.InterfaceLayer";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class ApplicationServiceRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.onion.classical.ApplicationServiceRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class DomainModelRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.onion.classical.DomainModelRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class DomainServiceRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.onion.classical.DomainServiceRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
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
            String fcqn = "org.jmolecules.architecture.onion.classical.InfrastructureRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
        }
    }

    static class ApplicationRing implements AnnotationBasedConcept {

        @Override
        public Category getCategory() {
            return ONION_ARCHITECTURE;
        }

        @Override
        public boolean test(IJavaElement source) {
            String fcqn = "org.jmolecules.architecture.onion.simplified.ApplicationRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

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
            String fcqn = "org.jmolecules.architecture.onion.simplified.DomainRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IType)) {
                return false;
            }

            IType type = (IType) source;
            IAnnotation[] annotations = getAnnotations(type);
            IImportDeclaration[] imports = getImports(type.getCompilationUnit());

            return test(fcqn, imports, annotations);
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
            String fcqn = "org.jmolecules.architecture.onion.simplified.InfrastructureRing";
            return testPackage(fcqn, source) || testType(fcqn, source);
        }

        private boolean testPackage(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }

        private boolean testType(String fcqn, IJavaElement source) {
            if (!(source instanceof IPackageDeclaration)) {
                return false;
            }

            IPackageDeclaration packageDeclaration = (IPackageDeclaration) source;
            IAnnotation[] annotations = getAnnotations(packageDeclaration);
            ICompilationUnit compilationUnit = (ICompilationUnit) packageDeclaration.getParent();
            IImportDeclaration[] imports = getImports(compilationUnit);

            return test(fcqn, imports, annotations);
        }
    }
}

package org.jmolecules.eclipse.plugin.explorer;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Type.DDD;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Type.EVENTS;
import static org.jmolecules.eclipse.plugin.explorer.JMolecules.Concept.Type.ONION_ARCHITECTURE;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getAnnotations;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.getImports;
import static org.jmolecules.eclipse.plugin.explorer.JavaModelUtils.isAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;

class JMolecules {

    private final List<Concept> concepts;

    public JMolecules() {
        concepts = init();
    }

    <T extends IJavaElement> List<Concept> expresses(T source) {
        return concepts.stream().filter(c -> c.test(source)).collect(toList());
    }

    private static List<Concept> init() {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(new AggregateRoot());
        concepts.add(new BoundedContext());
        concepts.add(new Identity());
        concepts.add(new Service());
        concepts.add(new DomainEvent());
        concepts.add(new DomainEventHandler());
        concepts.add(new DomainRing());
        return concepts;
    }

    static interface Concept extends Predicate<IJavaElement>, Comparable<Concept> {

        @Override
        default int compareTo(Concept other) {
            int result = getType().compareTo(other.getType());
            return result == 0 ? getName().compareTo(other.getName()) : result;
        }

        default String getName() {
            return getClass().getSimpleName();
        }

        Type getType();

        enum Type {
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
        public Type getType() {
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
        public Type getType() {
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

    static class Identity implements AnnotationBasedConcept {

        @Override
        public Type getType() {
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
        public Type getType() {
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
        public Type getType() {
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
        public Type getType() {
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
        public Type getType() {
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

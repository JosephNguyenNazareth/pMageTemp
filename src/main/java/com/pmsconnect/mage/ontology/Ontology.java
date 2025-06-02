//package com.pmsconnect.mage.ontology;
//
//import org.semanticweb.HermiT.ReasonerFactory;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.*;
//import org.semanticweb.owlapi.reasoner.OWLReasoner;
//import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
//
//import javax.annotation.Nonnull;
//import java.io.File;
//import java.io.PrintStream;
//
//import static org.semanticweb.owlapi.search.EntitySearcher.getAnnotationObjects;
//
//public class Ontology {
//    @Nonnull
//    private final ReasonerFactory reasonerFactory;
//    @Nonnull
//    private final OWLOntology ontology;
//    private final PrintStream out;
//    private Ontology(@Nonnull OWLOntology inputOntology) {
//        this.reasonerFactory = new ReasonerFactory();
//        ontology = inputOntology;
//        out = System.out;
//    }
//
//    private void printHierarchy(@Nonnull OWLClass clazz) {
//        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
//        printHierarchy(reasoner, clazz, 0);
//        /* Now print out any unsatisfiable classes */
//        for (OWLClass cl : ontology.getClassesInSignature()) {
//            assert cl != null;
//            if (!reasoner.isSatisfiable(cl)) {
//                out.println("XXX: " + labelFor(cl));
//            }
//        }
//        reasoner.dispose();
//    }
//
//    private String labelFor(@Nonnull OWLClass clazz) {
//        /*
//         * Use a visitor to extract label annotations
//         */
//        LabelExtractor le = new LabelExtractor();
//        for (OWLAnnotation anno : getAnnotationObjects(clazz, ontology)) {
//            anno.accept(le);
//        }
//        /* Print out the label if there is one. If not, just use the class URI */
//        if (le.getResult() != null) {
//            return le.getResult();
//        } else {
//            return clazz.getIRI().toString();
//        }
//    }
//
//    private void printHierarchy(@Nonnull OWLReasoner reasoner, @Nonnull OWLClass clazz, int level) {
//        /*
//         * Only print satisfiable classes -- otherwise we end up with bottom everywhere
//         */
//        if (reasoner.isSatisfiable(clazz)) {
//            for (int i = 0; i < level * 4; i++) {
//                out.print(" ");
//            }
//            out.println(labelFor(clazz));
//            /* Find the children and recurse */
//            for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
//                if (!child.equals(clazz)) {
//                    printHierarchy(reasoner, child, level + 1);
//                }
//            }
//        }
//    }
//
//    public static void main(String[] args) throws OWLOntologyCreationException, ClassNotFoundException, InstantiationException, IllegalAccessException {
//        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//        File ontologyFile = new File("/home/nguyenminhkhoi/Documents/doctorat/etude/article/BPM2023/pmage.owx");
//        String reasonerFactoryClassName = null;
//        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
//        Ontology simpleHierarchy = new Ontology(ontology);
//        OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
//        System.out.println("Class       : " + clazz);
//        simpleHierarchy.printHierarchy(clazz);
//    }
//}

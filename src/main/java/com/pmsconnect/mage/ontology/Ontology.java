package com.pmsconnect.mage.ontology;

import static org.semanticweb.owlapi.search.Searcher.annotations;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_LABEL;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_SUBCLASS_OF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Ontology {
    protected final OWLOntology ontology;
    private OWLDataFactory df;

    public Ontology(OWLOntology ontology, OWLDataFactory df) {
        this.ontology = ontology;
        this.df = df;
    }

    public void parseOntology() {
        for (OWLClass cls : ontology.getClassesInSignature()) {
            String id = cls.getIRI().toString();
            String label = get(cls, RDFS_LABEL.toString()).get(0);
            System.out.println(label + " [" + id + "]");
        }
    }

    public void parseProperties() {
        for (OWLClass cls : ontology.getClassesInSignature()) {
            String id = cls.getIRI().toString();
            String label = get(cls, RDFS_SUBCLASS_OF.toString()).get(0);
            System.out.println(label + " [" + id + "]");
        }
    }

    private List<String> get(OWLClass clazz, String property) {
        List<String> ret = new ArrayList<>();

        final OWLAnnotationProperty owlProperty = df
                .getOWLAnnotationProperty(IRI.create(property));
        for (OWLOntology o : ontology.getImportsClosure()) {
            for (OWLAnnotation annotation : annotations(
                    o.getAnnotationAssertionAxioms(clazz.getIRI()), owlProperty)) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral val = (OWLLiteral) annotation.getValue();
                    ret.add(val.getLiteral());
                }
            }
        }
        return ret;
    }

    public static void main(String[] args) throws OWLException {
        File file = new File("/home/nguyenminhkhoi/Documents/doctorat/etude/article/BPM2023/pmage.owx");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        Ontology parser = new Ontology(ontology, manager.getOWLDataFactory());

        ontology.getAnnotationPropertiesInSignature();
        parser.parseProperties();
    }
}
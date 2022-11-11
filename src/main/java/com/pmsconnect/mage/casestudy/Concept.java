package com.pmsconnect.mage.casestudy;

import java.util.ArrayList;
import java.util.List;

public class Concept {
    String conceptName;
    List<Concept> childConcepts;
    List<String> objects;

    public Concept(String conceptName) {
        this.conceptName = conceptName;
        this.childConcepts = new ArrayList<>();
        this.objects = new ArrayList<>();
    }

    public Concept(String conceptName, List<Concept> childConcepts) {
        this.conceptName = conceptName;
        this.childConcepts = childConcepts;
        this.objects = new ArrayList<>();
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public List<Concept> getChildConcepts() {
        return childConcepts;
    }

    public void setChildConcepts(List<Concept> childConcepts) {
        this.childConcepts = childConcepts;
    }

    public void addChildConcept(Concept childConcept) {
        this.childConcepts.add(childConcept);
    }

    public void addObject(String object) {
        this.objects.add(object);
    }

    @Override
    public String toString() {
        return "Concept{" +
                "conceptName='" + conceptName + '\'' +
                ",\nchildConcepts=" + childConcepts +
                "}\n";
    }
}

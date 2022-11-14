package com.pmsconnect.mage.casestudy;

public interface CaseStudy {
    void createConcept();
    String checkRelevant(String relevantTerm, TermDetect termDetector);
}

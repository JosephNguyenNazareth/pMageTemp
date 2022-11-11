package com.pmsconnect.mage.casestudy;

import java.util.Map;
import java.util.Objects;

public class Shopping implements CaseStudy{
    Concept root;

    public Shopping(String name) {
        this.root = new Concept(name);
    }

    public Concept getRoot() {
        return root;
    }

    @Override
    public void createConcept() {
        root = new Concept("shopping");
        createRelatedTaskLevel1(root);
    }

    @Override
    public String checkRelevant(String relevantTerm, TermDetect termDetector) {
        try {
            Map<String, String> tokenReceived = termDetector.detectLemmaEnglish(relevantTerm);
            StringBuilder result = new StringBuilder();
            for (Concept child : root.getChildConcepts()) {
                String selfResult = selfCheckRelevant(tokenReceived, child);
                if (!selfResult.equals(""))
                    result.append(selfResult).append(" ");
            }
            if (result.toString().replaceAll("\\s+$", "").equals(""))
                return "other";
            return result.toString().replaceAll("\\s+$", "");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String selfCheckRelevant(Map<String, String> tokenReceived, Concept current) {
        String[] conceptList = current.getConceptName().split(",");
        for (Map.Entry<String, String> token : tokenReceived.entrySet()) {
            for (String conceptName : conceptList) {
                if (token.getKey().contains(conceptName)) {
                    return conceptName;
                }
            }
        }
        for (Concept child : current.getChildConcepts()) {
            String result =  selfCheckRelevant(tokenReceived, child);
            if (!Objects.equals(result, "")) {
                if (current.getConceptName().equals("verb")) {
                    return child.getConceptName();
                }
                return result;
            }
        }
        return "";
    }

    public void createRelatedTaskLevel1(Concept parent) {
        Concept noun = new Concept("noun");
        Concept backend = new Concept("backend");
        Concept frontend = new Concept("frontend");
        Concept project = new Concept("project");
        Concept documentation = new Concept("documentation");

        Concept verb = new Concept("verb");
        Concept start = new Concept("start");
        Concept modify = new Concept("modify");
        Concept complete = new Concept("complete");
        Concept merge = new Concept("merge");

        createRelatedTaskLevel2(backend);
        createRelatedTaskLevel2(frontend);
        createRelatedTaskLevel2(project);
        createRelatedTaskLevel2(documentation);
        createRelatedTaskLevel2(start);
        createRelatedTaskLevel2(modify);
        createRelatedTaskLevel2(complete);
        createRelatedTaskLevel2(merge);

        noun.addChildConcept(backend);
        noun.addChildConcept(frontend);
        noun.addChildConcept(project);
        noun.addChildConcept(documentation);
        verb.addChildConcept(start);
        verb.addChildConcept(modify);
        verb.addChildConcept(complete);
        verb.addChildConcept(merge);

        parent.addChildConcept(verb);
        parent.addChildConcept(noun);
    }

    public void createRelatedTaskLevel2(Concept parent) {
        if (parent.getConceptName().equals("start")) {
            parent.addChildConcept(new Concept("initial,start,begin,create,add,open"));
        } else if (parent.getConceptName().equals("modify")) {
            parent.addChildConcept(new Concept("modify,update,upgrade,fix,repair,delete,remove,change"));
        } else if (parent.getConceptName().equals("complete")) {
            parent.addChildConcept(new Concept("complete,finish,end,close"));
        } else if (parent.getConceptName().equals("merge")) {
            parent.addChildConcept(new Concept("merge,pull"));
        } else if (parent.getConceptName().equals("frontend")) {
            parent.addChildConcept(new Concept("css,margin,padding,color,font,size"));
            parent.addChildConcept(new Concept("view,ui,page,window"));
            parent.addChildConcept(new Concept("bar,tab,button,title,heading,form,image,logo,icon"));
        } else if (parent.getConceptName().equals("backend")) {
            parent.addChildConcept(new Concept("order,cart,basket,item,product,quantity,package,delivery,payment"));
            parent.addChildConcept(new Concept("database,db"));
            parent.addChildConcept(new Concept("sql,mongo,neo4j,postgre,oracle,hadoop,spark"));
            parent.addChildConcept(new Concept("query,extract,alter,checkpoint"));
        } else if (parent.getConceptName().equals("project")) {
            parent.addChildConcept(new Concept("project,git,class,module,pom"));
        } else if (parent.getConceptName().equals("documentation")) {
            parent.addChildConcept(new Concept("doc,comment,instruction"));
        }
    }
}

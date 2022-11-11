package com.pmsconnect.mage.connector;

import casestudy.CaseStudyBape;
import casestudy.CaseStudyBapeImpl;
import com.pmsconnect.mage.casestudy.CaseStudy;
import com.pmsconnect.mage.casestudy.Shopping;
import com.pmsconnect.mage.repo.UserRepo;
import type.ProcessInstance;

public class CoreBapeConnector implements PMSConnector{
    private CaseStudyBape caseStudyBape;
    private CaseStudy caseStudyMage;
    private UserRepo userRepo;

    public CoreBapeConnector() {
    }

    public CaseStudy getCaseStudyMage() {
        return caseStudyMage;
    }

    @Override
    public ProcessInstance createProcessInstance(String creatorRole, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir) {
        if (this.initConnectPMS(creatorRole, processType)){
            this.connectGitSystem(processType, user, userRealName, repoOrigin, token, repoLink, repoDir);
            return this.caseStudyBape.getProcessInstance();
        }
        return null;
    }

    @Override
    public ProcessInstance connectProcessInstance(String processId, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir) {
        if (this.connectPMS(processType, processId)) {
            this.connectGitSystem(this.caseStudyBape.getProcessInstance().getName(), user, userRealName, repoOrigin, token, repoLink, repoDir);
            return this.caseStudyBape.getProcessInstance();
        }
        return null;
    }

    public boolean connectPMS(String processType, String processInstanceId) {
        this.caseStudyBape = new CaseStudyBapeImpl(processType);
        return this.caseStudyBape.loadProcessInstanceInfo(processInstanceId);
    }

    public boolean initConnectPMS(String creatorRole, String processType) {
        this.caseStudyBape = new CaseStudyBapeImpl(processType);
        if (!this.caseStudyBape.checkProcessModelExistence()) {
            System.out.println("Process model is not existed. Please create the process model first or choose another process model.");
            return false;
        }
        this.caseStudyBape.loadProcess(creatorRole);

        return true;
    }

    public void connectGitSystem(String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir) {
        if (processType.equals("Shopping Website"))
            caseStudyMage = new Shopping(processType);
        caseStudyMage.createConcept();
    }
}

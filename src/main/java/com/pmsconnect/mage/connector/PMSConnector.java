package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.casestudy.CaseStudy;
import type.ProcessInstance;

public interface PMSConnector {
    ProcessInstance connectProcessInstance(String processType, String processId, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir);
    ProcessInstance createProcessInstance(String creatorRole, String processType, String user, String userRealName, String repoOrigin, String token, String repoLink, String repoDir);
    CaseStudy getCaseStudyMage();
}

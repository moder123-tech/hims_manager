package com.cmpay.lemon.monitor.service.jira;

import com.cmpay.lemon.monitor.bo.DemandBO;

public interface JiraDataCollationService {

    void  getEpicRelatedTasks(DemandBO demandBO);
    void getIssueModifiedWithinOneDay();
    void TestProgressDetailOneDay();
    void inquiriesAboutRemainingProblems(String reqNo);
    void getDefectAndProblem();
}

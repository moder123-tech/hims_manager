package com.cmpay.lemon.monitor.utils.jira;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.common.utils.StringUtils;
import com.cmpay.lemon.monitor.bo.jira.CreateIssueRequestBO;
import com.cmpay.lemon.monitor.bo.jira.JiraSubtasksBO;
import com.cmpay.lemon.monitor.bo.jira.JiraTaskBodyBO;
import com.cmpay.lemon.monitor.bo.jira.JiraWorklogBO;
import com.cmpay.lemon.monitor.entity.UserDO;
import com.cmpay.lemon.monitor.utils.DateUtil;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class JiraUtil {
    // header Authorization
    private final static String AUTHORIZATION = "Authorization";
    //Authorization value
    private final static String AUTHORIZATIONVALUE = "Basic YWRtaW46SGlzdW5wYXlAMjAxOQ==";
    // header ContentType
    private final static String CONTENTTYPE = "Content-Type";
    // header ContentTypeValue
    private final static String CONTENTTYPEVALUE = "application/json";
    // post 请求
    private final static String CREATEISSUEURL = "http://10.9.10.117:18080/rest/api/2/issue";

    // post 请求
    private final static String GETSEARCH = "http://10.9.10.117:18080/rest/api/2/search";
    //创建
    public static Response CreateIssue(CreateIssueRequestBO createIssueRequest) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .body(createIssueRequest.toString())
                .post(CREATEISSUEURL);
        return response;
    }

    //获取
    public static JiraTaskBodyBO GetIssue(String jirakey) {

        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .get(CREATEISSUEURL + "/" + jirakey+"?expand=changelog");
        ResponseBody body = response.getBody();
        String json = body.print();
        JSONObject object = JSONObject.parseObject(json);
        JiraTaskBodyBO jiraTaskBodyBO = getJiraTaskBodyBO(object);
        return jiraTaskBodyBO;
    }

    //解析jira  返回json
    private static JiraTaskBodyBO getJiraTaskBodyBO(JSONObject object) {
        JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
        //缺陷实际处理人
        boolean flag = false;
        if(object.getJSONObject("changelog").getJSONArray("histories").size() != 0){
            JSONArray jsonArray = JSONArray.parseArray(object.getJSONObject("changelog").getString("histories"));
            for(int i=jsonArray.size() -1;i>= 0 && !flag ;i--){
                JSONObject jsonObject1 =JSONObject.parseObject(jsonArray.get(i).toString());
                if(jsonObject1.getJSONArray("items").size() !=0){
                    for(int j =0;j<jsonObject1.getJSONArray("items").size();j++){
                        JSONObject jsonObject2 =JSONObject.parseObject(jsonObject1.getJSONArray("items").get(j).toString());
                        if(jsonObject2.getString("field").equals("status")){
                            if(jsonObject2.getString("fromString").equals("处理中")){
                                jiraTaskBodyBO.setProblemHandler(jsonObject1.getJSONObject("author").getString("displayName"));
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 解决结果
        if(object.getJSONObject("fields").getString("resolution")!=null) {
            String problemType = object.getJSONObject("fields").getJSONObject("resolution").getString("name");
            jiraTaskBodyBO.setSolution(problemType);

        }
        //获取jirakey
        jiraTaskBodyBO.setJiraKey(object.getString("key"));
        //获取获取jira任务类型
        String jiraType = object.getJSONObject("fields").getJSONObject("issuetype").getString("name");

        String subtasks = object.getJSONObject("fields").getString("subtasks");
        jiraTaskBodyBO.setSubtasks(subtasks);
        //缺陷重测次数
        if(object.getJSONObject("fields").getInteger("customfield_10217")!=null){
            int retestTimes = object.getJSONObject("fields").getInteger("customfield_10217");
            jiraTaskBodyBO.setRetestTimes(retestTimes);
        }

        if(object.getJSONObject("fields").getString("customfield_10221")!=null) {
            String problemType = object.getJSONObject("fields").getJSONObject("customfield_10221").getString("value");
            jiraTaskBodyBO.setProblemType(problemType);
        }
        //问题处理人
        if(object.getJSONObject("fields").getString("customfield_10222")!=null) {
            JSONArray jsonArray = JSONArray.parseArray(object.getJSONObject("fields").getString("customfield_10222"));
            String value = "";
            for(int i= 0;i<jsonArray.size();i++){
                JSONObject jsonObject1 =JSONObject.parseObject(jsonArray.get(i).toString());
                value += jsonObject1.getString("displayName")+";";
            }
            jiraTaskBodyBO.setProblemHandlers(value);
        }

        //问题归属部门
        if(object.getJSONObject("fields").getString("customfield_10208")!=null) {
            JSONArray jsonArray = JSONArray.parseArray(object.getJSONObject("fields").getString("customfield_10208"));
            String value = "";
            for(int i= 0;i<jsonArray.size();i++){
                JSONObject jsonObject1 =JSONObject.parseObject(jsonArray.get(i).toString());
                 value += jsonObject1.getString("value")+";";
            }
            jiraTaskBodyBO.setDefectsDepartment(value);
        }
        String defectName = object.getJSONObject("fields").getString("customfield_10203");
        jiraTaskBodyBO.setDefectName(defectName);
        //缺陷详情
        String defectDetails = object.getJSONObject("fields").getString("customfield_10201");
        jiraTaskBodyBO.setDefectDetails(defectDetails);
        //评审问题类型
        if(object.getJSONObject("fields").getString("customfield_10257")!=null) {
            String reviewQuestionType = object.getJSONObject("fields").getJSONObject("customfield_10257").getString("value");
            jiraTaskBodyBO.setReviewQuestionType(reviewQuestionType);
        }
        //测试案例总数
        if(object.getJSONObject("fields").getString("customfield_10300")!=null) {
            String testCaseNumber = object.getJSONObject("fields").getString("customfield_10300");
            jiraTaskBodyBO.setTestCaseNumber(testCaseNumber);
        }


        //需求状态
        String status = object.getJSONObject("fields").getJSONObject("status").getString("name");
        jiraTaskBodyBO.setStatus(status);
        JSONObject parent = object.getJSONObject("fields").getJSONObject("parent");
        String epicKey = object.getJSONObject("fields").getString("customfield_10102");
        String parentTaskKey = null;
        if(parent!=null){
            parentTaskKey=parent.getString("key");
        }
        //如果类型是epic
        if(jiraType.equals("Epic")){
            epicKey=jiraTaskBodyBO.getJiraKey();
        }

        jiraTaskBodyBO.setEpicKey(epicKey);
        jiraTaskBodyBO.setParentTaskKey(parentTaskKey);
        //获取获取jira任务名
        String issueName = object.getJSONObject("fields").getString("summary");
        jiraTaskBodyBO.setIssueName(issueName);
        jiraTaskBodyBO.setJiraType(jiraType);

        //获取获取总共已用时
        String timespent = object.getJSONObject("fields").getString("timespent");
        jiraTaskBodyBO.setTimespent(timespent);

        String aggregatetimespent = object.getJSONObject("fields").getString("aggregatetimespent");
        jiraTaskBodyBO.setAggregatetimespent(aggregatetimespent);
        if(object.getJSONObject("fields").getString("customfield_10225")!=null) {
            String department = object.getJSONObject("fields").getJSONObject("customfield_10225").getString("value");
            jiraTaskBodyBO.setDepartment(department);
        }
        //获取经办人
        if(object.getJSONObject("fields").getString("assignee")!=null) {
            String assignee = object.getJSONObject("fields").getJSONObject("assignee").getString("displayName");
            jiraTaskBodyBO.setAssignee(assignee);
        }
        //获取任务创建人
        String creator = object.getJSONObject("fields").getJSONObject("creator").getString("displayName");
        jiraTaskBodyBO.setCreator(creator);
        //获取起始时间
        String planStartTime = object.getJSONObject("fields").getString("customfield_10252");
        jiraTaskBodyBO.setPlanStartTime(planStartTime);
        //获取结束时间
        String planEndTime = object.getJSONObject("fields").getString("customfield_10253");
        jiraTaskBodyBO.setPlanEndTime(planEndTime);

        //缺陷安全级别
        if(object.getJSONObject("fields").getString("customfield_10212")!=null) {
            String securityLevel = object.getJSONObject("fields").getJSONObject("customfield_10212").getString("value");
            jiraTaskBodyBO.setSecurityLevel(securityLevel);
        }


        //日志流水
        String worklogs = object.getJSONObject("fields").getJSONObject("worklog").getString("worklogs");

        String total = object.getJSONObject("fields").getJSONObject("worklog").getString("total");
        if(Integer.parseInt(total)>20){
             worklogs = getAllWorklogs(jiraTaskBodyBO.getJiraKey());
        }
        jiraTaskBodyBO.setWorklogs(worklogs);
        //附属子任务
        String createTime = DateUtil.dealDateFormat(object.getJSONObject("fields").getString("created"));
        jiraTaskBodyBO.setCreateTime(createTime);
        // 投产问题提出人
        if(object.getJSONObject("fields").getString("customfield_10207")!=null) {
            String productionIssueRegistrant = object.getJSONObject("fields").getJSONObject("customfield_10207").getString("displayName");
            jiraTaskBodyBO.setProductionIssueRegistrant(productionIssueRegistrant);
        }
        // 投产问题编号
        if(object.getJSONObject("fields").getString("customfield_10400")!=null) {
            String proNumber = object.getJSONObject("fields").getString("customfield_10400");
            jiraTaskBodyBO.setProNumber(proNumber);
        }
        return jiraTaskBodyBO;
    }

    //获得子任务
    public static List<JiraSubtasksBO> getSubtasks(JiraTaskBodyBO jiraTaskBodyBO) {
        if(StringUtils.isBlank(jiraTaskBodyBO.getSubtasks())){
            return null;
        }
        List<JiraSubtasksBO> jiraSubtasksBOLinkedList = new LinkedList<>();
        //附属子任务
        JSONArray subtasksJsonArray = JSONArray.parseArray(jiraTaskBodyBO.getSubtasks());
        if (subtasksJsonArray != null) {
            for (int i = 0; i < subtasksJsonArray.size(); i++) {
                JSONObject jsonObject1 =JSONObject.parseObject(subtasksJsonArray.get(i).toString());
                String JiraKey = jsonObject1.getString("key");
                String summary = jsonObject1.getJSONObject("fields").getString("summary");

                JiraSubtasksBO jiraSubtasksBO = new JiraSubtasksBO();
                jiraSubtasksBO.setSubtaskkey(JiraKey);
                jiraSubtasksBO.setSubtaskname(summary);
                jiraSubtasksBO.setParenttaskkey(jiraTaskBodyBO.getJiraKey());
                jiraSubtasksBOLinkedList.add(jiraSubtasksBO);
            }
            return jiraSubtasksBOLinkedList;
        }
        return null;

    }

    public static  String getAllWorklogs(String jirakey) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .get(CREATEISSUEURL + "/" + jirakey+"/worklog");
        ResponseBody body = response.getBody();
        String json = body.print();
        JSONObject object = JSONObject.parseObject(json);
        String worklogs = object.getString("worklogs");
        return worklogs;
    }
    //获得工作流水
    public static  List<JiraWorklogBO> getWorklogs(JiraTaskBodyBO jiraTaskBodyBO) {
        JSONArray worklogsJsonArray = JSONArray.parseArray(jiraTaskBodyBO.getWorklogs());
        List<JiraWorklogBO> jiraWorklogBOLinkedList = new LinkedList<>();
        if (worklogsJsonArray != null) {
            for (int i = 0; i < worklogsJsonArray.size(); i++) {
                JiraWorklogBO jiraWorklogBO = new JiraWorklogBO();
                JSONObject jsonObject1 =JSONObject.parseObject(worklogsJsonArray.get(i).toString());
                String jiraWorklogKey = jsonObject1.getString("self");
                jiraWorklogBO.setJiraWorklogKey(jiraWorklogKey);

                String timeSpent = jsonObject1.getString("timeSpentSeconds");
                jiraWorklogBO.setTimespnet(timeSpent);
                String displayName = jsonObject1.getJSONObject("author").getString("displayName");
                jiraWorklogBO.setDisplayname(displayName);
                String name = jsonObject1.getJSONObject("author").getString("name");
                jiraWorklogBO.setName(name);
                //账号状态 flase 为不存在 true为正常
                boolean active = jsonObject1.getJSONObject("author").getBoolean("active");
                jiraWorklogBO.setActive(active);
                //备注
                String comment = jsonObject1.getString("comment");
                jiraWorklogBO.setComment(comment);
                // 原来jira版本获取日期时间加了8小时 ，新版时间不需要加
                //String created = DateUtil.addDateMinut(DateUtil.dealDateFormat(jsonObject1.getString("created")), 8);
                String created =   DateUtil.dealDateFormat(jsonObject1.getString("created"));
                jiraWorklogBO.setCreatedtime(created);

                //String started = DateUtil.addDateMinut(DateUtil.dealDateFormat(jsonObject1.getString("started")), 8);
                String started = DateUtil.dealDateFormat(jsonObject1.getString("started"));
                jiraWorklogBO.setStartedtime(started);

                //String updated = DateUtil.addDateMinut(DateUtil.dealDateFormat(jsonObject1.getString("updated")), 8);
                String updated = DateUtil.dealDateFormat(jsonObject1.getString("updated"));
                jiraWorklogBO.setUpdatedtime(updated);

                jiraWorklogBOLinkedList.add(jiraWorklogBO);
            }
            return jiraWorklogBOLinkedList;
        }
        return null;
    }

    //修改测试主任务
    public static void EditTheTestMainTask(JiraTaskBodyBO jiraTaskBodyBO) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .body(jiraTaskBodyBO.getEditTestMainTaskBody())
                .put(CREATEISSUEURL + "/" + jiraTaskBodyBO.getJiraKey());
    }


    //依据jql批量获取jira数据
    public static List<JiraTaskBodyBO>  batchQueryIssuesModifiedWithinOneDay(int page) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .get(GETSEARCH + "?" + "jql= updated >= -2d order by created ASC&startAt="+page+"&maxResults=50");
        ResponseBody body = response.getBody();
        String json = body.print();
        JSONObject object = JSONObject.parseObject(json);
        JSONArray issueJsonArray = JSONArray.parseArray( object.getString("issues"));
        List<JiraTaskBodyBO> jiraTaskBodyBOlist = new LinkedList<>();
        if (issueJsonArray != null) {
            for (int i = 0; i < issueJsonArray.size(); i++) {
                JSONObject jsonObject1 =JSONObject.parseObject(issueJsonArray.get(i).toString());
                JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
                jiraTaskBodyBO.setJiraKey(jsonObject1.getString("key"));
                jiraTaskBodyBOlist.add(jiraTaskBodyBO);
            }
        }
        return jiraTaskBodyBOlist;
    }

    //依据jql获取和包下项目指定epic下未完成的内部缺陷数据
    public static List<JiraTaskBodyBO>  batchQueryIssuesModifiedWithinEpic(String epic) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .get(GETSEARCH + "?" + "jql= project = CMPAY AND issuetype = 内部缺陷 AND status in (处理中, 待处理, 待更新, 待重测) AND 史诗链接 = "+ epic);
        ResponseBody body = response.getBody();
        String json = body.print();
        JSONObject object = JSONObject.parseObject(json);
        JSONArray issueJsonArray = JSONArray.parseArray( object.getString("issues"));
        List<JiraTaskBodyBO> jiraTaskBodyBOlist = new LinkedList<>();
        if (issueJsonArray != null) {
            for (int i = 0; i < issueJsonArray.size(); i++) {
                JSONObject jsonObject1 =JSONObject.parseObject(issueJsonArray.get(i).toString());
                JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
                jiraTaskBodyBO.setJiraKey(jsonObject1.getString("key"));
                jiraTaskBodyBOlist.add(jiraTaskBodyBO);
            }
        }
        return jiraTaskBodyBOlist;
    }

    //依据jql批量获取一天内的jira数据 内部缺陷, 评审问题
    public static List<JiraTaskBodyBO>  batchQueryIssuesModifiedWithinOneDay2(int page) {
        Response response = given()
                .header(AUTHORIZATION, AUTHORIZATIONVALUE)
                .header(CONTENTTYPE, CONTENTTYPEVALUE)
                .get(GETSEARCH + "?" + "jql= updated >= -1d AND issuetype in (内部缺陷, 评审问题) order by created ASC&startAt="+page+"&maxResults=50");
        ResponseBody body = response.getBody();
        String json = body.print();
        JSONObject object = JSONObject.parseObject(json);
        JSONArray issueJsonArray = JSONArray.parseArray( object.getString("issues"));
        List<JiraTaskBodyBO> jiraTaskBodyBOlist = new LinkedList<>();
        if (issueJsonArray != null) {
            for (int i = 0; i < issueJsonArray.size(); i++) {
                JSONObject jsonObject1 =JSONObject.parseObject(issueJsonArray.get(i).toString());
                JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
                jiraTaskBodyBO.setJiraKey(jsonObject1.getString("key"));
                jiraTaskBodyBOlist.add(jiraTaskBodyBO);
            }
        }
        return jiraTaskBodyBOlist;
    }

    /*
     *获取主任务，并解析相关信息
     */
    public static void main(String[] args) {

        String  a="2020-07-27";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse=new Date();
        try {
            parse= sdf.parse(a);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(parse);
        c.add(Calendar.DATE, + 7);
        Date time = c.getTime();
        String preDay = sdf.format(time);
    }


}







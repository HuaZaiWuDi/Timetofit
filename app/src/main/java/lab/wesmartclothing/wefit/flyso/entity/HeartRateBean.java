package lab.wesmartclothing.wefit.flyso.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Created icon_hide_password jk on 2018/6/11.
 */
public class HeartRateBean implements Serializable {


    /**
     * athlDate : 2018-11-07T06:56:21.290Z
     * athlDesc : string
     * athlScore : 1
     * heartList : [{"heartRate":120,"heartTime":"2018-11-07T06:56:21.290Z","stepTime":2}]
     * planFlag : 0
     * stepNumber : 1
     */

    private String athlDate;
    private String athlDesc;
    private double athlScore;
    private int planFlag;//定制课程：1
    private int stepNumber;
    private double complete;
    private double totalCalorie;
    private List<HeartRateItemBean> heartList;


    public HeartRateBean() {
    }

    public double getTotalCalorie() {
        return totalCalorie;
    }

    public void setTotalCalorie(double totalCalorie) {
        this.totalCalorie = totalCalorie;
    }

    public double getComplete() {
        return complete;
    }

    public void setComplete(double complete) {
        this.complete = complete;
    }


    public String getAthlDate() {
        return athlDate;
    }

    public void setAthlDate(String athlDate) {
        this.athlDate = athlDate;
    }

    public String getAthlDesc() {
        return athlDesc;
    }

    public void setAthlDesc(String athlDesc) {
        this.athlDesc = athlDesc;
    }

    public double getAthlScore() {
        return athlScore;
    }

    public void setAthlScore(double athlScore) {
        this.athlScore = athlScore;
    }

    public int getPlanFlag() {
        return planFlag;
    }

    public void setPlanFlag(int planFlag) {
        this.planFlag = planFlag;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public List<HeartRateItemBean> getHeartList() {
        return heartList;
    }

    public void setHeartList(List<HeartRateItemBean> heartList) {
        this.heartList = heartList;
    }


}

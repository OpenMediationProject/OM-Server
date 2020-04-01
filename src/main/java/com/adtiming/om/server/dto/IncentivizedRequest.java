// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;
import org.apache.http.StatusLine;

/**
 * IapRequest
 */
public class IncentivizedRequest extends CommonRequest {

    private int pid;       // PlacementId
    private int mid;       // AdNetworkId
    private int iid;       // InstanceID
    private int scene;     // sceneId
    private String content;// content

    // not from json
    // set by api controller
    private int status;
    private String msg;
    private int httpStatus;
    private String httpMsg;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getIid() {
        return iid;
    }

    public void setIid(int iid) {
        this.iid = iid;
    }

    public int getScene() {
        return scene;
    }

    public void setScene(int scene) {
        this.scene = scene;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public IncentivizedRequest setStatus(int status, String msg) {
        this.status = status;
        this.msg = msg;
        return this;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getHttpMsg() {
        return httpMsg;
    }

    public void setHttpMsg(String httpMsg) {
        this.httpMsg = httpMsg;
    }

    public void setHttpStatus(StatusLine sl) {
        this.httpStatus = sl.getStatusCode();
        this.httpMsg = sl.getReasonPhrase();
    }

    public void writeToLog(LogService logService) {
        logService.write("om.ic", this);
    }

}

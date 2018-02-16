package edu.asu.diging.gilesecosystem.cassiopeia.core.service.impl;

import edu.asu.diging.gilesecosystem.requests.RequestStatus;

public class RequestInfo {

    private String path;
    private long size;
    private String filename;
    private String imageFilename;
    private String uploadId;
    private String fileId;
    private RequestStatus status;
    private String errorMsg;
    
    public RequestInfo(String path, long size, String filename) {
        this.path = path;
        this.size = size;
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String filename) {
        this.imageFilename = filename;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    
}

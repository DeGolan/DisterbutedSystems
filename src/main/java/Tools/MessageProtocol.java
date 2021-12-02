package Tools;

import org.json.JSONObject;

public class MessageProtocol {
    JSONObject json=new JSONObject();
    private String task;
    private String bucketName;
    private String key;
    private int numOfPDFPerWorker;
    private String url;
    private String status;
    private String localApp;

    public MessageProtocol (String task, String bucketName, String key, int numOfPDFPerWorker,String url,String status, String localApp){
        this.task=task;
        this.bucketName=bucketName;
        this.key=key;
        this.numOfPDFPerWorker=numOfPDFPerWorker;
        this.url=url;
        this.status=status;
        this.localApp=localApp;
    }
    public MessageProtocol (JSONObject json){
        this.task = (String) json.get("task");
        this.bucketName = (String) json.get("bucketName");
        this.key = (String) json.get("key");
        this.numOfPDFPerWorker = (int) json.get("numOfPDFPerWorker");
        this.url=(String) json.get("url");
        this.status=(String)  json.get("status");
        this.localApp=(String) json.get("localApp");
    }

    public String getStatus(){
        return status;
    }
    public String getUrl() {
        return url;
    }

    public String getTask() {
        return task;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKey() {
        return key;
    }

    public int getNumOfPDFPerWorker() {
        return numOfPDFPerWorker;
    }

    public String getLocalApp() {
        return localApp;
    }

    public JSONObject getJson() {
        json.put("task",task);
        json.put("bucketName", bucketName);
        json.put("key", key);
        json.put("numOfPDFPerWorker", numOfPDFPerWorker);
        json.put("url", url);
        json.put("status", status);
        json.put("localApp",localApp);
        return json;
    }
}

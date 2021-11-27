package Tools;

import org.json.JSONObject;

public class MessageProtocol {
    JSONObject json=new JSONObject();
    private String task;
    private String bucketName;
    private String key;
    private int numOfPDFPerWorker;
    public MessageProtocol (String task, String bucketName, String key, int numOfPDFPerWorker){
        this.task=task;
        this.bucketName=bucketName;
        this.key=key;
        this.numOfPDFPerWorker=numOfPDFPerWorker;
    }
    public MessageProtocol (JSONObject json){
        this.task = (String) json.get("task");
        this.bucketName = (String) json.get("bucketName");
        this.key = (String) json.get("key");
        this.numOfPDFPerWorker = (int) json.get("numOfPDFPerWorker");
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

    public JSONObject getJson() {
        json.put("task",task);
        json.put("bucketName", bucketName);
        json.put("key", key);
        json.put("numOfPDFPerWorker", numOfPDFPerWorker);
        return json;
    }
}

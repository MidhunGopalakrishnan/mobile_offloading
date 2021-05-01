package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class ClientPayload implements Serializable {
    private String tag;
    private Object data;

    public String getTag() {
        return tag;
    }

    public ClientPayload setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public Object getData() {
        return data;
    }

    public ClientPayload setData(Object data) {
        this.data = data;
        return this;
    }
}

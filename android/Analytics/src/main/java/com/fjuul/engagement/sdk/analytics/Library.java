package com.fjuul.engagement.sdk.analytics;

import com.fjuul.engagement.sdk.FjuulSDK;

public class Library {

    private FjuulSDK coreSDK;

    Library() {
        coreSDK = new FjuulSDK();
    }

    public boolean someLibraryMethod() {
        return new FjuulSDK().someLibraryMethod();
    }

    public String getText() {
        return this.coreSDK.getText();
    }
}

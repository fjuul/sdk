package com.fjuul.sdk.analytics;

import com.fjuul.sdk.FjuulSDK;

public class Analytics {

    private FjuulSDK coreSDK;

    public Analytics() {
        coreSDK = new FjuulSDK();
    }

    public boolean someLibraryMethod() {
        return new FjuulSDK().someLibraryMethod();
    }

    public String getText() {
        return this.coreSDK.getText();
    }
}

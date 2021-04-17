/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxyz3d.samples;

import java.io.IOException;
import java.io.InputStream;
 
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ryzen
 */
public class DownloadTask {

    String downloadURL;
    Map<String, byte[]> sessionStore;
    String contentName;

    InputStream is;
    HashMap<String, String> params;

    public DownloadTask(String downloadURL, Map<String, byte[]> sessionStore, String contentName) {
        this.downloadURL = downloadURL;
        this.sessionStore = sessionStore;
        this.contentName = contentName;
    }

    public DownloadTask(String upLoadServerUri, String filename, InputStream is, HashMap<String, String> params) {
        this.downloadURL = upLoadServerUri;
        this.contentName = filename;
        this.is = is;
        this.params = params;
    }

    public String download() throws IOException {

        //String content = HTTPHelper.httpGetResponse(downloadURL);
        InputStream in = new URL(downloadURL).openStream();
        sessionStore.put(contentName, in.readAllBytes());

        return "success";
    }

    public String streamUpload() throws IOException {

        HTTPHelper.uploadFile(downloadURL, contentName, is, params);

        return "success";
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxyz3d.samples;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ryzen
 */
public class HTTPHelper {
    
    public static String httpGetResponse(String getURL) throws MalformedURLException, IOException {

        URL tgStoreServer = new URL(getURL);
        URLConnection yc = tgStoreServer.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        StringBuilder sb = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
            sb.append(inputLine);
        }
        in.close();

        return sb.toString();
    }
    
    public static int uploadFile(String upLoadServerUri, File sourceFile, HashMap<String, String> params) {

       // String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "------truegeometry";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        //File sourceFile = new File(fileName);
        int serverResponseCode = 0;
        //Log.e("joshtag", "Uploading: sourcefileURI, "+fileName);

        try {

            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(upLoadServerUri);
            //Log.v("joshtag",url.toString());

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy            s       
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("file", sourceFile.getName());
            //conn.setRequestProperty("user", user_id));

            dos = new DataOutputStream(conn.getOutputStream());
            //ADD Some -F Form parameters, helping method
            //... is declared down below
            for (Map.Entry<String, String> kv : params.entrySet()) {
                addFormField(dos, kv.getKey(), kv.getValue());
            }

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + sourceFile.getName() + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                //Log.i("joshtag","->");
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();
            //Log.i("joshtag", "HTTP Response is : "  + serverResponseMessage + ": " + serverResponseCode);  

            // ------------------ read the SERVER RESPONSE
            DataInputStream inStream;
            try {
                inStream = new DataInputStream(conn.getInputStream());
                String str;
                while ((str = inStream.readLine()) != null) {
                    //Log.e("joshtag", "SOF Server Response" + str);
                    System.out.println(str);
                }
                inStream.close();
            } catch (IOException ioex) {
                // Log.e("joshtag", "SOF error: " + ioex.getMessage(), ioex);
            }

            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();

            if (serverResponseCode == 200) {
                //Do something                       
            }//END IF Response code 200  

            //dialog.dismiss();
        }//END TRY - FILE READ      
        catch (MalformedURLException ex) {
            ex.printStackTrace();
            //Log.e("joshtag", "UL error: " + ex.getMessage(), ex);  
        } //CATCH - URL Exception
        catch (Exception e) {
            e.printStackTrace();
            //Log.e("Upload file to server Exception", "Exception : "+ e.getMessage(), e);
        } //

        return serverResponseCode; //after try       
        //END ELSE, if file exists.
    }
    
     public static void addFormField(DataOutputStream dos, String parameter, String value) {
        try {
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + parameter + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(value);
            dos.writeBytes(lineEnd);
        } catch (Exception e) {

        }
    }

    public static String lineEnd = "\r\n";
    public static String twoHyphens = "--";
    public static String boundary = "------------------------afb19f4aeefb356c";
    
}

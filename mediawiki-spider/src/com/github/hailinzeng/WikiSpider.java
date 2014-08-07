package com.github.hailinzeng;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Download resources in media-wiki
 *
 * Author : Hailin Zeng
 */
public abstract class WikiSpider {
    protected Map<String, WikiResourceMeta> alreadyDownloadMeta = new HashMap<String, WikiResourceMeta>();

    public WikiSpider(String outputFolder){
        File filedir = new File(outputFolder);
        if(!filedir.exists()){
            if(!filedir.mkdirs()){
                System.out.println("Failed to create output folder :" + outputFolder);
            }
        }

        // load already download meta
        loadAlreadyDownload(outputFolder + "mediawikispider.md");
    }

    private void loadAlreadyDownload(String metaFilePath){

        File metaFile = new File(metaFilePath);

        // init empty meta file if not exist
        if(!metaFile.exists()){
            try {
                if(!metaFile.createNewFile()){
                    System.out.println("Failed to create meta file " + metaFilePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(metaFile));

            String line;

            while((line = br.readLine()) != null){
                if(!line.isEmpty()) {
                    WikiResourceMeta meta = WikiResourceMeta.fromString(line);

                    System.out.println("old: " + meta);

                    alreadyDownloadMeta.put(meta.getNormalizeUrl(), meta);
                }
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if(br != null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void updateAlreadyDownload(String metaFilePath, Map<String, WikiResourceMeta> metaList){
        File metaFile = new File(metaFilePath);

        // init empty meta file if not exist
        if(!metaFile.exists()){
            try {
                if(!metaFile.createNewFile()){
                    System.out.println("Failed to create meta file " + metaFilePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream writer = null;

        try {
            writer = new FileOutputStream(metaFile);

            for(Map.Entry<String, WikiResourceMeta> en : metaList.entrySet()){
                writer.write(en.getValue().toString().getBytes());
                writer.write("\n".getBytes());
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if(writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public abstract void downloadAllFiles();
}

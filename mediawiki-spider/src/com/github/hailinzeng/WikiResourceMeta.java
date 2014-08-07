package com.github.hailinzeng;

import java.text.DateFormat;
import java.util.Date;

/**
 * Meta info of Wiki Resource File
 *
 * Format : Timestamp Url FileName
 *
 * Author : Hailin Zeng
 */
public class WikiResourceMeta {
    private String normalizeUrl;
    private String normalizeFileName;
    private String updateTime;

    public static WikiResourceMeta fromString(String line){
        WikiResourceMeta meta = new WikiResourceMeta();

        int firstSep = line.indexOf('\t');
        int nextSep  = line.substring(firstSep + 1).indexOf('\t');

        meta.setNormalizeUrl(line.substring(0, firstSep).trim());
        meta.setUpdateTime(line.substring(firstSep + 1).substring(0, nextSep).trim());
        meta.setNormalizeFileName(line.substring(nextSep + 1).substring(nextSep + 1).trim());

        return meta;
    }

    public String getNormalizeUrl() {
        return normalizeUrl;
    }

    public void setNormalizeUrl(String normalizeUrl) {
        this.normalizeUrl = normalizeUrl;
    }

    public String getNormalizeFileName() {
        return normalizeFileName;
    }

    public void setNormalizeFileName(String normalizeFileName) {
        this.normalizeFileName = normalizeFileName;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString(){
        return normalizeUrl + "\t" + updateTime + "\t" + normalizeFileName;
    }
}

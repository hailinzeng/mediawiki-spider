package com.github.hailinzeng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Download Pages
 *
 * As last update time is inside the page, so download them all.
 *
 * Author : Hailin Zeng
 */
public class WikiPageSpider extends WikiSpider{

    private static String outputFolder = WikiConf.OUTPUT_FOLDER + "page/";

    public WikiPageSpider(){
        super(outputFolder);
    }

    @Override
    public void downloadAllFiles() {
        Map<String, WikiResourceMeta> newFileMetaList = new HashMap<String, WikiResourceMeta>(alreadyDownloadMeta);

        try {
            Map<String, WikiResourceMeta> fileMetaList = getPageUrls();

            for (Map.Entry<String, WikiResourceMeta> fileMeta : fileMetaList.entrySet()) {
                WikiResourceMeta meta = fileMeta.getValue();

                HttpGet httpget = new HttpGet(WikiConf.HOST_PREFIX + meta.getNormalizeUrl());
                httpget.setHeader("Cookie", WikiConf.Cookie);

                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httpget);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                if(response == null){
                    continue;
                }

                HttpEntity entity = response.getEntity();

                String html = EntityUtils.toString(entity);

                // extract page content
                Document doc = Jsoup.parse(html);

                // check timestamp for update
/*
                Element footer = doc.getElementById("footer-info-Array");
                String footerText = footer.text();

                String date = footerText.substring(footerText.indexOf("最后修改于"));

                // check timestamp for update
                if(alreadyDownloadMeta.containsKey(meta.getNormalizeUrl())){

                    WikiResourceMeta oldmeta = newFileMetaList.get(meta.getNormalizeUrl());

                    if(date.equals(oldmeta.getUpdateTime())) {
                        continue;
                    }else{
                        oldmeta.setUpdateTime(meta.getUpdateTime());
                    }
                }
*/
                if(!alreadyDownloadMeta.containsKey(meta.getNormalizeUrl())){
                    newFileMetaList.put(fileMeta.getKey(), fileMeta.getValue());

                    System.out.println("new: " + meta.getNormalizeUrl() + "\t" + meta.getNormalizeFileName());
                }

                // save page content
                Elements contentE = doc.getElementsByClass("mw-body");

                byte[] content = contentE.text().getBytes();

                FileOutputStream fos = null;
                try{
                    fos = new FileOutputStream(outputFolder + makeNormalNameforPage(meta.getNormalizeFileName()));
                    fos.write(content, 0, content.length);
                }catch(Exception ex){
                    ex.printStackTrace();
                }finally{
                    try{
                        if(fos != null) {
                            fos.close();
                        }
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateAlreadyDownload(outputFolder + "mediawikispider.md", newFileMetaList);
    }

    /**
     * Get all pages: { URL -> FileName }
     */
    private static Map<String, WikiResourceMeta> getPageUrls()
            throws ClientProtocolException, IOException {
        Map<String, WikiResourceMeta> urls = new HashMap<String, WikiResourceMeta>();

        HttpClient httpclient = new DefaultHttpClient();

        String pageurl = WikiConf.PAGE_LIST_URL;

        // main
        HttpGet httpget = new HttpGet(pageurl);
        httpget.setHeader("Cookie", WikiConf.Cookie);

        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();

        String html = EntityUtils.toString(entity);

        Document doc = Jsoup.parse(html);

        Elements nameList = doc
                .getElementsByClass("mw-allpages-alphaindexline");

        for (Element e : nameList) {
            String urllistPage = e.childNode(0).attr("href");

            httpget = new HttpGet(WikiConf.HOST_PREFIX + urllistPage);
            httpget.setHeader("Cookie", WikiConf.Cookie);

            response = httpclient.execute(httpget);
            entity = response.getEntity();

            html = EntityUtils.toString(entity);

            Document doc2 = Jsoup.parse(html);

            // table
            Elements pageList = doc2
                    .getElementsByClass("mw-allpages-table-chunk");

            // table -> tbody -> tr
            for (Node page : pageList.first().childNode(0).childNodes()) {
                for (int i = 0; i < page.childNodeSize(); i++) {
                    String fileUrl = page.childNode(i).childNode(0).attr("href");
                    String fileName = page.childNode(i).childNode(0).attr("title");

                    WikiResourceMeta resource = new WikiResourceMeta();
                    resource.setNormalizeUrl(fileUrl);
                    resource.setNormalizeFileName(fileName.trim());
                    resource.setUpdateTime("1979-1-1"); // NOTE : left empty

                    urls.put(fileUrl, resource);
                }
            }
        }

        return urls;
    }

    private static String makeNormalNameforPage(String pageTitle){
        return pageTitle
                .replace("\\\\", "-bsl-")
                .replace(" ", "-sp-")
                .replace("/", "-sl-")
                .replace(":", "-se-")
                + ".txt";
    }

}

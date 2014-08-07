package com.github.hailinzeng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Download Uploaded Files
 *
 * Author : Hailin Zeng
 */
public class WikiFileSpider extends WikiSpider{

    private static String outputFolder = WikiConf.OUTPUT_FOLDER + "file/";

    public WikiFileSpider(){
        super(outputFolder);
    }

    @Override
    public void downloadAllFiles() {
        Map<String, WikiResourceMeta> newFileMetaList = new HashMap<String, WikiResourceMeta>(alreadyDownloadMeta);

        try {
            Map<String, WikiResourceMeta> fileMetaList = getFileUrls();

            for (Map.Entry<String, WikiResourceMeta> fileMeta : fileMetaList.entrySet()) {
                WikiResourceMeta meta = fileMeta.getValue();

                // check timestamp for update
                if(alreadyDownloadMeta.containsKey(meta.getNormalizeUrl())){
                    WikiResourceMeta oldmeta = newFileMetaList.get(meta.getNormalizeUrl());

                    if(meta.getUpdateTime().equals(oldmeta.getUpdateTime())) {
                        continue;
                    }else{
                        oldmeta.setUpdateTime(meta.getUpdateTime());
                    }
                }else{
                    newFileMetaList.put(fileMeta.getKey(), fileMeta.getValue());
                }

                System.out.println("new: " + meta.getNormalizeUrl() + "\t" + meta.getNormalizeFileName());

                URL fileurl = new URL(WikiConf.HOST_PREFIX + meta.getNormalizeUrl());
                ReadableByteChannel rbc = Channels.newChannel(fileurl.openStream());

                FileOutputStream fos = null;
                try{
                    fos = new FileOutputStream(outputFolder + meta.getNormalizeFileName());
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
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
     * Get all files uploaded: { URL -> FileName }
     */
    private static Map<String, WikiResourceMeta> getFileUrls() throws ParseException,
            IOException {
        Map<String, WikiResourceMeta> urls = new HashMap<String, WikiResourceMeta>();

        HttpClient httpclient = new DefaultHttpClient();

        String pageurl = WikiConf.FILE_LIST_URL;

        do {
            // get page size
            HttpGet httpget = new HttpGet(pageurl);
            httpget.setHeader("Cookie", WikiConf.Cookie);

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            String html = EntityUtils.toString(entity);

            Document doc = Jsoup.parse(html);

            Elements nameList = doc.getElementsByClass("TablePager_col_img_name");
            Elements dateList = doc.getElementsByClass("TablePager_col_img_timestamp");

            for (int i = 0, iend = nameList.size(); i < iend; i++) {
                String fileUrl = nameList.get(i).childNode(2).attr("href");
                String fileName = nameList.get(i).childNode(0).attr("title").substring(3);
                String date = dateList.get(i).text();

                WikiResourceMeta meta = new WikiResourceMeta();
                meta.setNormalizeUrl(fileUrl);
                meta.setNormalizeFileName(fileName.trim());
                meta.setUpdateTime(date.trim());

                urls.put(fileUrl, meta);
            }

            // has next page?
            Elements nextPages = doc.getElementsMatchingOwnText("下一页");
            Element nextPage = nextPages.first();

            if (nextPage.tagName().equals("a")) {
                pageurl = WikiConf.HOST_PREFIX + nextPage.attr("href");
            } else {
                break;
            }

        } while (true);

        return urls;
    }

}

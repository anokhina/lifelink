/*
 * Copyright 2020 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.lifelink;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Component
public class StoredLinkBuilder {
    
    @Autowired
    private LinkStorage linkStorage;
    
    public StoredLink build(final LinkContent lc) throws IOException {
        final StoredLink res = new StoredLink();
        
        res.setCreated(lc.getCreated());
        res.setCreatedDate(new Date().getTime());
        res.setGid(lc.getGid());
        res.setId(UUID.randomUUID().toString());
        res.setNote(lc.getNote());
        res.setTags(getTags(lc.getTags()));
        res.setTitle(lc.getTitle());
        res.setUrl(lc.getUrl());
        res.setWatchLater(lc.getWatchLater());
        
        saveUploadedFile(res, lc.getContent());
        saveUploadedImage(res, lc.getSshot());
        save(res);
        return res;
    }
    
    public StoredLink build(final JSONObject lc) throws IOException {
        final StoredLink res = new StoredLink();
        
        res.setCreated(getString(lc, "created"));
        final Long l = Long.valueOf(getString(lc, "createdDate"));
        res.setCreatedDate(l);
        res.setGid(getString(lc, "gid"));
        res.setId(getString(lc, "id"));
        res.setNote(getString(lc, "note"));
        res.setTags(getString(lc, "tags"));
        res.setTitle(getString(lc, "title"));
        res.setUrl(getString(lc, "url"));
        final Boolean b = Boolean.valueOf(getString(lc, "watchLater"));
        res.setWatchLater(b);
        
        res.setContentPath(getString(lc, "contentPath"));
        res.setSshotPath(getString(lc, "sshotPath"));
        return res;
    }
    
    private String getString(final JSONObject lc, final String k) {
        if (lc.has(k)) {
            return lc.getString(k);
        } else {
            return null;
        }
    }
    
    public File getStorageDir(final StoredLink res) {
        return getDir(linkStorage.getFileStorageDir(), res.getId());
    }
    
    private void save(final StoredLink res) throws IOException {
        JSONObject jo = new JSONObject(res);
        if (jo.has("content")) {
            jo.remove("content");
        }
        final byte[] bytes = jo.toString(2).getBytes(StandardCharsets.UTF_8);
        System.out.println(">>>>>LENGTH>>>>>>save>" + bytes.length);

        File f = new File(getStorageDir(res), res.getId() + ".json");
        f.getParentFile().mkdirs();
        System.out.println(">>>save>"+f.getAbsolutePath());
        Files.write(f.toPath(), bytes);
    }
    
    private void saveUploadedImage(final StoredLink res, final String img) throws IOException {
        if (img != null && img.length() > 0) {
            //byte[] bytes = img.getBytes(StandardCharsets.UTF_8);//Base64.getDecoder().decode(img.replace());
            byte[] bytes = Base64.getDecoder().decode(img.split(",")[1]);
            System.out.println(">>>>>LENGTH>>>>>>Image>" + bytes.length);
            
            File f = new File(getStorageDir(res), res.getId() + ".jpg");
            f.getParentFile().mkdirs();
            System.out.println(">>>Image>"+f.getAbsolutePath());
            Files.write(f.toPath(), bytes);
            res.setSshotPath(rel(linkStorage.getFileStorageDir(), f));
        }
    }
    
    private void saveUploadedFile(final StoredLink res, final MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            byte[] bytes = file.getBytes();
            
            System.out.println(">>>>>LENGTH>>>>>>>" + bytes.length + ":" + file.getContentType());
            final String str = new String(bytes, StandardCharsets.UTF_8);
            res.setContent(str);
            final String ext; 
            if ("text/html".equals(file.getContentType())) {
                ext = ".html";//text/html
            } else if ("application/octet-stream".equals(file.getContentType())) {
                ext = ".mhtml";//application/octet-stream
            }
            else {
                if (str.contains("-MultipartBoundary-")) {
                    ext = ".mhtml";//application/octet-stream
                } else {
                    ext = ".html";//text/html
                }
            }
            
            File f = new File(getStorageDir(res), res.getId() + ext);
            f.getParentFile().mkdirs();
            System.out.println(">>>>"+f.getAbsolutePath());
            Files.write(f.toPath(), bytes);
            res.setContentPath(rel(linkStorage.getFileStorageDir(), f));
        }
    }
    
    public File getStorageDir(final String id) {
        return getDir(linkStorage.getFileStorageDir(), id);
    }
    
    private File getDir(final File dir, final String s) {
        final String dirs = s.replace("-", "").substring(0, 6);
        return new File(new File(new File(new File(dir, dirs.substring(0, 2)), dirs.substring(2, 4)), dirs.substring(4)), s);
    }
    
    private String rel(final File dir, final File f) throws IOException {
        return dir.getCanonicalFile().toPath().relativize(f.getCanonicalFile().toPath()).toString();
    }

    private String getTags(String tags) {
        if (tags == null) {
            return null;
        }
        final String[] atags = tags.split(",");
        for (int i = 0; i < atags.length; i++) {
            atags[i] = atags[i].trim();
        }
        Arrays.sort(atags);
        return String.join(",", atags);
    }
}

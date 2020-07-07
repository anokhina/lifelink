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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AddLinkController {
    
    @Autowired
    private StoredLinkBuilder storedLinkBuilder;
    
    @Autowired
    private ObjectIndexer objectIndexer;
    
    @CrossOrigin
    @PostMapping("/addlnk")
    public ResponseEntity<?> greetingSubmit(@ModelAttribute LinkContent model) {
        
        System.out.println(">>>>>>>>>>" + new JSONObject(model).toString(2));
        try {
            final StoredLink storedLink = storedLinkBuilder.build(model);
            objectIndexer.processObject(storedLink);
        } catch (Exception ex) {
            Logger.getLogger(AddLinkController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(">>>>>ERROR>>>>>>>");
            return ResponseEntity.badRequest().body("Error");
        }
        System.out.println(">>>>>OK>>>>>>>");
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping(path = "/get", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody String getLink() throws Exception {
        return objectIndexer.findBy("tags", "*dashboard*");
    }
    
    @GetMapping(path = "/getlnk", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody String getLink(
            @RequestParam(value = "n", required = false) String n, 
            @RequestParam(value = "v", required = false) List<String> v) throws Exception {

        if (n == null || v == null || v.size() == 0) {
            return objectIndexer.findBy("tags", "*dashboard*");
        } else {
            return objectIndexer.findBy(n, v.get(0));
        }
    }
    
    //exo-open --launch FileManager . &
    @GetMapping(path = "/dir/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody String dir(
            @PathVariable("id") String id) throws Exception {
        
        final File dir = storedLinkBuilder.getStorageDir(id);
        ProcessBuilder builder = new ProcessBuilder();
        //builder.command("exo-open", "--launch", "FileManager");
        System.out.println(">>>>>>" + dir);
        builder.command("xdg-open", dir.getAbsolutePath());
        builder.directory(dir);
        builder.start();
        return "OK";
    }
    
    @GetMapping(path = "/link/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody String getDetails(
            @PathVariable("id") String id) throws Exception {
        
        return objectIndexer.findBy("id", id);
    }
    
    @GetMapping(path = "/link/{id}/del", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody String del(
            @PathVariable("id") String id) throws Exception {
        
        return objectIndexer.del("id", id);
    }
    
    private void saveUploadedFile(MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            byte[] bytes = file.getBytes();
            System.out.println(">>>>>LENGTH>>>>>>>" + bytes.length);
            final String str = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(str.substring(0, Math.min(str.length(), 500)));
            File f = File.createTempFile("zzz", ".mhtml");
            System.out.println(">>>>"+f.getAbsolutePath());
            Files.write(f.toPath(), bytes);
//            Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
//            Files.write(path, bytes);
        }
    }    
}

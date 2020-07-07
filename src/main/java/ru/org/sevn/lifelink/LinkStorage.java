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
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class LinkStorage {

    private final File userDir = new File(System.getProperty("user.home"));
    private final File storageDir = new File(userDir, "ru.org.sevn/lifelink/storage");
    private final File fileStorageDir = new File(userDir, "ru.org.sevn/lifelink/files");
    private final File trashDir = new File(userDir, "ru.org.sevn/lifelink/trash");
    
    @PostConstruct
    public void init() {
        storageDir.mkdirs();
        fileStorageDir.mkdirs();
        trashDir.mkdirs();
    }

    public File getStorageDir() {
        return storageDir;
    }

    public File getFileStorageDir() {
        return fileStorageDir;
    }

    public File getTrashDir() {
        return trashDir;
    }
    
}

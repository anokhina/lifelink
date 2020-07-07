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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.org.sevn.whereis.*;

@Component
public class ObjectIndexer {
    
    @Autowired
    private LinkStorage linkStorage;
    
    @Autowired
    private StoredLinkBuilder storedLinkBuilder;
    
    public static int PAGE_SIZE = 100;
    
    private final Indexer indexer = new QueueIndexer (20) {
        protected Field.Store isStoreField(final String n) {
            //System.out.println("+++" + n);
            if (n != null && n.equals(MetaParam.OBJ_ + "content")) {
                return Field.Store.NO;
            }
            return super.isStoreField(n);
        }
        protected IndexableField[] getField(final String n, final String c) {
            if (n != null && n.equals(MetaParam.OBJ_ + "content")) {
                final Field.Store isStoreField = isStoreField(n);
                return new IndexableField[] { new TextField(n, trim(n, isStoreField, c), isStoreField), new TextField(MetaParam.strName(n), toSearchable(c), Field.Store.NO) };
            } else {
                return super.getField(n, c);
            }
        }
        
    };
    
    private final ObjectMetadataExtractor metadataExtractor = new ObjectMetadataExtractor ();
    
    @PostConstruct
    public void init() {
        try {
            ru.org.sevn.whereis.Util.configDbPath(indexer, linkStorage.getStorageDir());
        } catch (IOException ex) {
            Logger.getLogger(ObjectIndexer.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Configuration error");
        }
        
    }

    public Indexer getIndexer () {
        return indexer;
    }

    public ObjectMetadataExtractor getMetadataExtractor () {
        return metadataExtractor;
    }
    
    public void processObject (final StoredLink obj) throws Exception {
        processObject(obj, "id");
    }

    private void processObject (final Object obj, final String... idFields) throws Exception {
        indexer.index (addIndexInfo (obj.getClass(), metadataExtractor.getMetadata (obj, idFields)));
    }

    ObjectMetadata addIndexInfo (final Class cl, final ObjectMetadata m) {
        m.add (MetaParam.INDEXED_AT, "" + indexAt ());
        m.set (MetaParam.CLS, getClName(cl));
        return m;
    }

    private long indexAt () {
        return new Date ().getTime ();
    }

    public List<StoredLink> listBy(final String fieldName, final String pattern) throws ParseException, IOException {
        final List<StoredLink> result = new ArrayList<>();
        
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        IndexFinder.findByFieldsQueryEq(builder, IndexFinder.toMap(MetaParam.CLS, getClName(StoredLink.class)));
        IndexFinder.findByFieldsQueryLike(builder, IndexFinder.toMap(MetaParam.OBJ_ +fieldName, pattern));
        
        //testPrint(baos, builder);
        indexer.setSort(new Sort(new SortField(MetaParam.OBJ_ + "created", SortField.Type.STRING, true)));
        indexer.find(PAGE_SIZE, builder.build()).forEach(d -> {
            final JSONObject jo = metadataExtractor.fromDocument (d);
            try {
                result.add(storedLinkBuilder.build(jo));
            } catch (IOException ex) {
                Logger.getLogger(ObjectIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return result;
    }
    
    public String findBy(final String fieldName, final String ... patterns) throws ParseException, IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        IndexFinder.findByFieldsQueryEq(builder, IndexFinder.toMap(MetaParam.CLS, getClName(StoredLink.class)));
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].contains("*")) {
                IndexFinder.findByFieldsQueryLike(builder, IndexFinder.toMap(MetaParam.OBJ_ +fieldName, patterns[i]));
            } else {
                IndexFinder.findByFieldsQueryEq(builder, IndexFinder.toMap(MetaParam.OBJ_ +fieldName, patterns[i]));
            }
        }
        
        //testPrint(baos, builder);
        indexer.setSort(new Sort(new SortField(MetaParam.OBJ_ + "created", SortField.Type.STRING, true)));
        final AtomicInteger idx = new AtomicInteger(1);
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            indexer.find(PAGE_SIZE, builder.build()).forEach(d -> {
                final JSONObject jo = metadataExtractor.fromDocument (d);
                ps.printf("<h3>%d. %s</h3>\n", idx.getAndIncrement(), get(jo, "title"));
                ps.printf("created=%s\n", get(jo, "created"));
                ps.printf("tags=%s\n", get(jo, "tags"));
                ps.printf("<a href=\"%s\">url</a>\n", get(jo, "url"));
                if (jo.has("contentPath")) {
                    final File f = new File(linkStorage.getFileStorageDir(), get(jo, "contentPath"));
                    ps.printf("<a href=\"%s\">saved</a>\n", f.toURI());
                }
                if (jo.has("sshotPath")) {
                    final File f = new File(linkStorage.getFileStorageDir(), get(jo, "sshotPath"));
                    ps.printf("<a href=\"%s\">img</a>\n", f.toURI());
                }
                {
                    final File f = storedLinkBuilder.getStorageDir(jo.getString("id"));
                    ps.printf("<a href=\"%s\">dir</a>\n", f.toURI());
                    ps.printf("<a href=\"%s\">open</a>\n", "/dir/" + jo.getString("id"));
                }
                ps.printf("<a href=\"%s\">details</a>\n", "/link/" + jo.getString("id"));
                ps.printf("<a href=\"%s\">edit</a>\n", "/Link?n=id&v=" + jo.getString("id"));
                ps.printf("<p>note:%s\n", get(jo, "note"));

            });
        }        
        return baos.toString(StandardCharsets.UTF_8.name());
    }
    
    private String getClName(final Class cl) {
        if (StoredLink.class.equals(cl)) {
            return "ru.org.sevn.StoredLink";
        }
        return cl.getName();
    }

    public String del(final String fieldName, final String pattern) throws ParseException, IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        IndexFinder.findByFieldsQueryEq(builder, IndexFinder.toMap(MetaParam.CLS, getClName(StoredLink.class)));
        IndexFinder.findByFieldsQueryLike(builder, IndexFinder.toMap(MetaParam.OBJ_ +fieldName, pattern));
        
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            indexer.find(PAGE_SIZE, builder.build()).forEach(d -> {
                final JSONObject jo = metadataExtractor.fromDocument (d);
                ps.printf("<h2>DELETED</h2>\n");
                ps.printf("<h3>%s</h3>\n", get(jo, "title"));
                ps.printf("created=%s\n", get(jo, "created"));
                ps.printf("tags=%s\n", get(jo, "tags"));
                ps.printf("<a href=\"%s\">url</a>\n", get(jo, "url"));
                if (jo.has("contentPath")) {
                    final File f = new File(linkStorage.getFileStorageDir(), get(jo, "contentPath"));
                    ps.printf("<a href=\"%s\">saved</a>\n", f.toURI());
                }
                if (jo.has("sshotPath")) {
                    final File f = new File(linkStorage.getFileStorageDir(), get(jo, "sshotPath"));
                    ps.printf("<a href=\"%s\">img</a>\n", f.toURI());
                }
                ps.printf("<a href=\"%s\">details</a>\n", "/link/" + jo.getString("id"));
                ps.printf("<p>note:%s\n", get(jo, "note"));
                {
                    final File f = storedLinkBuilder.getStorageDir(jo.getString("id"));
                    ps.printf("<p>rename %s\n", f.getAbsolutePath());
                    ps.printf("<p>to %s\n", linkStorage.getTrashDir().getAbsolutePath());
                    f.renameTo(linkStorage.getTrashDir());
                }
            });
        }        
        indexer.inWriter(w -> {
            w.deleteDocuments(builder.build());
        });
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    private String get(final JSONObject jo, final String n) {
        try {
            return jo.getString(n);
        } catch (Exception e) {
            return "";
        }
    }
    
    private void testPrint(final ByteArrayOutputStream baos, final BooleanQuery.Builder builder) throws UnsupportedEncodingException, IOException {
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            indexer.find(PAGE_SIZE, builder.build()).forEach(d -> {
                ps.println ("Document=>" + d);
                d.getFields ().forEach (f -> {
                    ps.println ("field=>" + f.name () + "=" + f.stringValue ());
                });
                ps.println ("json=>" + metadataExtractor.fromDocument (d).toString (2));
            });
        }
        
    }
}

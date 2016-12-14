package com.fragmentime.mdreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MdList extends AppCompatActivity {

    private Object lock = new Object();

    public static final int REQUEST_PERMISSION_CODE = '1';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_md_list);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS, Manifest.permission.INTERNET}, REQUEST_PERMISSION_CODE);
        }

//        try {
//            lock.wait();
//        } catch (InterruptedException e) {
//        }

        Uri data = getIntent().getData();
        String pathContext = data == null ? "" : data.getPath();
        File mdFile = new File(pathContext);
        if (mdFile.exists() && !mdFile.isDirectory()) {
            // start mdShow
            Intent mdReaderIntent = new Intent(this, MdReader.class);
            Uri uri = Uri.fromFile(mdFile);
            mdReaderIntent.setData(uri);
            this.startActivity(mdReaderIntent);
            this.finish();
            return;
        }

        ListView lv = (ListView) findViewById(R.id.listView);
        if (lv != null) {
            final SimpleAdapter adapter = getMdFilesAdaptor(pathContext);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView filePathView = (TextView) view.findViewById(R.id.file_path);
                    String path = filePathView.getText().toString();

                    Intent mdListIntent = new Intent(MdList.this, MdList.class);
                    Uri uri = Uri.fromFile(new File(path));
                    mdListIntent.setData(uri);
                    MdList.this.startActivity(mdListIntent);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                this.lock.notifyAll();
            } else {
                // Permission Denied
                this.finish();
            }
        }
    }

    private SimpleAdapter getMdFilesAdaptor(String path) {
        List<Map<String, Object>> mdList = new ArrayList<>();
        Node node = getMarkdownFileList(path);
        if (node != null) {
            node = node.right;
        }
        while (node != null) {
            Map<String, Object> file = new HashMap<>();
            int iconIdx = R.drawable.folder;
            if (node.isFile) {
                if (node.name.toLowerCase().endsWith(".md")) {
                    iconIdx = R.drawable.file_markdown;
                } else if (node.name.toLowerCase().endsWith("html")) {
                    iconIdx = R.drawable.file_html;
                }
            }
            file.put("fileIcon", iconIdx);
            file.put("fileName", node.name);
            file.put("filePath", node.path);
            mdList.add(file);
            node = node.left;
        }
        return new SimpleAdapter(this, mdList, R.layout.activity_md_list_item, new String[]{"fileIcon", "fileName", "filePath"}, new int[]{R.id.file_icon, R.id.file_name, R.id.file_path});
    }

    private Node getMarkdownFileList(String path) {
        File f = Environment.getExternalStorageDirectory();

        if (path != null && path.trim().startsWith(f.getAbsolutePath())) {
            f = new File(path);
        }
        return sortMarkdownFilesList(getMarkdownFiles(f));
    }

    /**
     * Sort the direct children of node
     *
     * @param node
     * @return
     */
    private Node sortMarkdownFilesList(Node node) {
        if (node == null) {
            return node;
        }
        List<Node> nodes = new ArrayList<>();
        Node current = node.right;
        while (current != null) {
            nodes.add(current);
            current = current.left;
        }
        if (nodes.size() > 1) {
            Collections.sort(nodes);
            Iterator<Node> it = nodes.iterator();
            Node firstNode = it.next(), currentSortItem = firstNode, sortItem = null;
            while (it.hasNext()) {
                sortItem = it.next();
                currentSortItem.left = sortItem;
                sortItem.parent = currentSortItem;
                currentSortItem = sortItem;
            }
            sortItem.left = null;

            node.right = firstNode;
            firstNode.parent = node;
        }
        return node;
    }

    private Node getMarkdownFiles(File folder) {
        if (!folder.exists()) {
            return null;
        }
        Node node = null;
        if (folder.isDirectory()) {
            File[] subFiles = folder.listFiles();
            if (subFiles == null || subFiles.length == 0) {
                return null;
            }
            Node current = null;
            for (File item : subFiles) {
                Node media = getMarkdownFiles(item);
                if (media != null) {
                    if (node == null) {
                        node = new Node();
                        node.isFile = false;
                        node.name = folder.getName();
                        node.path = folder.getAbsolutePath();
                        current = node;

                        current.right = media;
                        media.parent = current;
                        current = media;
                    } else {
                        current.left = media;
                        media.parent = current;
                        current = media;
                    }
                }
            }
        } else if (folder.getName().toLowerCase().endsWith(".md") || folder.getName().toLowerCase().endsWith(".html")) {
            node = new Node();
            node.name = folder.getName();
            node.path = folder.getAbsolutePath();
            node.isFile = true;
        }
        return node;
    }


    private static class Node implements Comparable<Node> {
        private String name;
        private String path;
        private boolean isFile;
        private Node parent;
        private Node left;
        private Node right;

        @Override
        public int compareTo(Node another) {
            if (this.isFile) {
                if (another.isFile) {
                    return this.name.compareTo(another.name);
                }
                return 1;
            } else {
                if (another.isFile) {
                    return -1;
                }
                return this.name.compareTo(another.name);
            }
        }

    }
}

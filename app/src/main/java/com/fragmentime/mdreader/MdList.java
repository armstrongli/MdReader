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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.fragmentime.mdreader.mdconst.Const;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MdList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_md_list);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS, Manifest.permission.INTERNET}, '1');
        }

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

    private SimpleAdapter getMdFilesAdaptor(String path) {
        List<Map<String, Object>> mdList = new ArrayList<>();
        Node node = getMardownFileList(path);
        if (node != null) {
            node = node.right;
        }
        while (node != null) {
            Map<String, Object> file = new HashMap<>();
            file.put("fileName", node.name);
            file.put("filePath", node.path);
            mdList.add(file);
            node = node.left;
        }
        return new SimpleAdapter(this, mdList, R.layout.activity_md_list_item, new String[]{"fileName", "filePath"}, new int[]{R.id.file_name, R.id.file_path});
    }

    private Node getMardownFileList(String path) {
        File f = Environment.getExternalStorageDirectory();

        if (path != null && path.trim().startsWith(f.getAbsolutePath())) {
            f = new File(path);
        }
        return getMarkdownFiles(f);
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
                        current = media;
                    } else {
                        current.left = media;
                        current = media;
                    }
                }
            }
        } else if (folder.getName().endsWith(".md")) {
            node = new Node();
            node.name = folder.getName();
            node.path = folder.getAbsolutePath();
            node.isFile = true;
        }
        return node;
    }


    private static class Node {
        private String name;
        private String path;
        private boolean isFile;
        private Node left;
        private Node right;
    }
}

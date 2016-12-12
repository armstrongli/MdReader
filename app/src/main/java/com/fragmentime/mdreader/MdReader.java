package com.fragmentime.mdreader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fragmentime.markdownj.MarkdownJ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MdReader extends AppCompatActivity {


    private Object lock = new Object();

    public static final int REQUEST_PERMISSION_CODE = '1';

    private static String mdPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_md_reader);

        WebView wvShow = (WebView) findViewById(R.id.wvShow);
        WebSettings webSettings = wvShow.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setAppCacheEnabled(true);
        wvShow.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS, Manifest.permission.INTERNET}, REQUEST_PERMISSION_CODE);
        }

//        try {
//            lock.wait();
//        } catch (InterruptedException e) {
//        }

        try {
            mdPath = getIntent().getData().getPath();
        } catch (Exception e) {
            // ignore
        }

        wvShow.addJavascriptInterface(new Md(), "md");
        wvShow.loadUrl("file:///android_asset/show.html");
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

    private static class Md {

        @JavascriptInterface
        public String getParsedData() {
            if (mdPath == null || mdPath.trim().length() == 0) {
                return "<h1>Ops! Markdown Reader cannot go there you just want to go :)</h1>";
            }
            try {
                return MarkdownJ.parseFile(mdPath);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("<h1>Ops! Something's wrong with MdReader!</h1>");
                sb.append("<br/>");
                sb.append("<pre><code class=\"language-\">").append("\n");
                sb.append(e.getMessage()).append("\n");
                for (Object obj : e.getStackTrace()) {
                    sb.append(obj.toString()).append("\n");
                }
                sb.append("</pre></code>");
                return sb.toString();
            }
        }
    }
}

package com.swimtimer.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manuale d'uso");
        }

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowFileAccess(true);

        try {
            // Copia il PDF dagli assets nella cache, poi lo apre
            File outFile = new File(getCacheDir(), "SwimTimer_Manuale.pdf");
            if (!outFile.exists()) {
                InputStream in = getAssets().open("SwimTimer_Manuale.pdf");
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
                out.close();
            }
            // Apre il PDF tramite Google Docs viewer (richiede internet)
            // oppure con viewer locale se disponibile
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.swimtimer.app.fileprovider", outFile);

            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish(); // chiude ManualActivity, il PDF apre nell'app esterna
            } else {
                // Fallback: WebView con Google Docs
                String url = "https://docs.google.com/gviewer?embedded=true&url="
                        + outFile.toURI().toString();
                webView.loadUrl(url);
            }

        } catch (Exception e) {
            WebView wv = findViewById(R.id.webView);
            wv.loadData("<h2>Errore caricamento manuale</h2><p>" 
                + e.getMessage() + "</p>", "text/html", "utf-8");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

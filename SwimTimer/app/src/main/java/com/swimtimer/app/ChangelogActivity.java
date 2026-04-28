package com.swimtimer.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ChangelogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Novità e aggiornamenti");
        }

        TextView tv = findViewById(R.id.tvChangelog);
        tv.setText(buildChangelog());
    }

    private String buildChangelog() {
        return
            "─────────────────────────────\n" +
            "  v1.1  –  Aprile 2025\n" +
            "─────────────────────────────\n" +
            "• Aggiunto rilevamento automatico sirena tramite microfono (FFT)\n" +
            "• Compensazione ritardo rilevamento: il cronometro parte dall'istante esatto della sirena\n" +
            "• Calibrazione sirena: distanza e sensibilità microfono configurabili\n" +
            "• Aggiunto contatore vasche durante la gara\n" +
            "• Badge record: 🥇 record assoluto, 📈 miglioramento rispetto all'ultima gara\n" +
            "• Importazione sessioni tramite deep link (swimtimer://import)\n" +
            "• Condivisione sessione via link\n" +
            "• Foto atleta allegabile alla sessione\n" +
            "• Tema Multicolor aggiunto\n" +
            "• WakeLock: lo schermo rimane acceso durante la cronometrazione\n" +
            "• Animazione nuotatore migliorata: tuffo, capriola e nuoto\n\n" +

            "─────────────────────────────\n" +
            "  v1.0  –  Febbraio 2025\n" +
            "─────────────────────────────\n" +
            "• Prima versione pubblica\n" +
            "• Cronometro con registrazione vasche\n" +
            "• Storico gare con dettaglio vasche\n" +
            "• Tema Chiaro e Scuro\n" +
            "• Salvataggio sessioni con nome atleta e specialità\n" +
            "• Vasca più veloce evidenziata in verde, più lenta in rosso\n";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

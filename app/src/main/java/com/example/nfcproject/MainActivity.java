package com.example.nfcproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    public static final String Error_Found = "No Tag Persent";

    public static final String Write_Succesful = "Write Sucessful!";

    public static final String Write_Error = "Error, Retry";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter tagWriteFilter[];
    boolean writeMode;
    Tag nfcTag;
    Context context;
    TextView edit_tag;
    TextView textViewContents;
    Button activbutton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        edit_tag = (TextView) findViewById(R.id.edit_tag);
        textViewContents = (TextView) findViewById(R.id.textViewContents);
        activbutton = findViewById(R.id.activbutton);
        context = this;

        activbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    if (nfcTag == null) {
                        Toast.makeText(context, Error_Found, Toast.LENGTH_LONG).show();
                    } else {
                        write("PlainText|" + edit_tag.getText().toString(), nfcTag);
                        Toast.makeText(context, Write_Succesful, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null){
            Toast.makeText(this, "This Device Does Not Support NFC :(", Toast.LENGTH_LONG).show();
            finish();
        }
            readfromIntent(getIntent());
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        tagWriteFilter = new IntentFilter[] {tagDetected};
    }

    private void readfromIntent(Intent intent) {

        String action = intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msg = null;
            if (rawMsg != null) {

                msg = new NdefMessage[rawMsg.length];
                for (int i = 0; i < rawMsg.length; i++) {

                    msg[i] = (NdefMessage) rawMsg[i];

                }

            }

            buildTagViews(msg);

        }

    }

    private void buildTagViews(NdefMessage[] msg){

        if (msg == null || msg.length == 0) return;
        String text = "";
        byte[] payload = msg[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] * 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        textViewContents.setText("NFC CONTENT:" + text);

    }

    private void write(String text, Tag nfcTag) throws IOException, FormatException {

        NdefRecord[] records = { createRecord(text)};
        NdefMessage message = new NdefMessage(records);

        Ndef ndef = Ndef.get(nfcTag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();

    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

        String lang = "en";

        byte[] textBytes = text.getBytes();
        int textlength = textBytes.length;
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langlength = langBytes.length;
        byte[] payload = new byte[1 + langlength + textlength];

        payload[0] = (byte) langlength;

        System.arraycopy(langBytes, 0, payload, 1, langlength);
        System.arraycopy(textBytes, 0, payload, 1 + langlength, textlength);

        NdefRecord recordNfc = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNfc;


    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        setIntent(intent);
        readfromIntent(intent);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        }

    }

    @Override
    public void onPause(){
        
        super.onPause();
        WriteModeisOFF();
        
    }
    @Override
    public void onResume(){

        super.onResume();
        WriteModeisON();

    }

    private void WriteModeisON(){

        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, tagWriteFilter, null);

    }

    private void WriteModeisOFF(){

        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);

    }

}
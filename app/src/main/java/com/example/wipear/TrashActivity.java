package com.example.wipear;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private final List<PhotoItem> items = new ArrayList<>();
    private TrashAdapter adapter;
    private TextView emptyView;
    private Button deleteButton;
    private TextView header;
    private final SoundFx sound = new SoundFx();

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            TrashStore.get().clear();
                            sound.deleted();
                            Toast.makeText(this, R.string.deleted_ok,
                                    Toast.LENGTH_SHORT).show();
                            refresh();
                        } else {
                            Toast.makeText(this, R.string.delete_cancelled,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);
        applySystemBarInsets(findViewById(R.id.root));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.trash_title);
        }

        emptyView = findViewById(R.id.trashEmpty);
        header = findViewById(R.id.trashHeader);
        deleteButton = findViewById(R.id.deleteButton);

        RecyclerView rv = findViewById(R.id.trashList);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new TrashAdapter(items, item -> {
            TrashStore.get().remove(item.id);
            refresh();
        });
        rv.setAdapter(adapter);

        deleteButton.setOnClickListener(v -> confirmDelete());
        refresh();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        sound.release();
        super.onDestroy();
    }

    private void applySystemBarInsets(final View root) {
        final int padH = root.getPaddingLeft();
        final int padTop = root.getPaddingTop();
        final int padBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(padH + bars.left, padTop + bars.top,
                    padH + bars.right, padBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void refresh() {
        items.clear();
        items.addAll(TrashStore.get().items());
        adapter.notifyDataSetChanged();

        boolean empty = items.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        deleteButton.setEnabled(!empty);
        header.setText(getString(R.string.trash_header, items.size()));
    }

    private void confirmDelete() {
        if (items.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_msg, items.size()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> performDelete())
                .show();
    }

    private void performDelete() {
        List<Uri> uris = new ArrayList<>();
        for (PhotoItem item : items) {
            uris.add(item.uri);
        }
        ContentResolver resolver = getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.app.PendingIntent pi =
                    android.provider.MediaStore.createDeleteRequest(resolver, uris);
            try {
                deleteLauncher.launch(
                        new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) {
                Toast.makeText(this, R.string.delete_failed,
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // API < 30: delete directly; on Android 10 handle the recoverable prompt.
        int deleted = 0;
        for (Uri uri : uris) {
            try {
                deleted += resolver.delete(uri, null, null);
            } catch (SecurityException se) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                        && se instanceof RecoverableSecurityException) {
                    IntentSender sender = ((RecoverableSecurityException) se)
                            .getUserAction().getActionIntent().getIntentSender();
                    try {
                        deleteLauncher.launch(
                                new IntentSenderRequest.Builder(sender).build());
                    } catch (Exception ignored) {
                        Toast.makeText(this, R.string.delete_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
        }
        if (deleted > 0) {
            TrashStore.get().clear();
            sound.deleted();
            Toast.makeText(this, R.string.deleted_ok, Toast.LENGTH_SHORT).show();
        }
        refresh();
    }
}

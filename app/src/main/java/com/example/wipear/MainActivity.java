package com.example.wipear;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View cardContainer;
    private ImageView cardImage;
    private TextView keepLabel;
    private TextView trashLabel;
    private TextView albumNameLabel;
    private TextView emptyView;
    private TextView filterInfo;
    private View filterBar;
    private Button trashButton;
    private View openGalleryButton;

    private final List<PhotoItem> deck = new ArrayList<>();
    private int index = 0;

    private long filterStartMs = Long.MIN_VALUE;
    private long filterEndMs = Long.MAX_VALUE;
    private String dateLabel = null;

    private final java.util.Set<String> selectedBuckets =
            new java.util.LinkedHashSet<>();
    private String albumLabel = null;

    private final SoundFx sound = new SoundFx();
    private static final int MENU_MUTE = 1;
    private SharedPreferences prefs;
    private boolean muted;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    reloadDeck();
                } else {
                    emptyView.setText(R.string.permission_needed);
                    emptyView.setVisibility(View.VISIBLE);
                    cardContainer.setVisibility(View.GONE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.root));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_title));
        }

        prefs = getSharedPreferences("wipear", MODE_PRIVATE);
        muted = prefs.getBoolean("muted", false);
        SoundFx.setMuted(muted);

        cardContainer = findViewById(R.id.cardContainer);
        cardImage = findViewById(R.id.cardImage);
        keepLabel = findViewById(R.id.keepLabel);
        trashLabel = findViewById(R.id.trashLabel);
        albumNameLabel = findViewById(R.id.albumNameLabel);
        emptyView = findViewById(R.id.emptyView);
        filterInfo = findViewById(R.id.filterInfo);
        filterBar = findViewById(R.id.filterBar);
        trashButton = findViewById(R.id.trashButton);
        openGalleryButton = findViewById(R.id.openGalleryButton);

        findViewById(R.id.dateFilterButton).setOnClickListener(v -> showDateFilter());
        findViewById(R.id.albumFilterButton).setOnClickListener(v -> showAlbumFilter());
        findViewById(R.id.clearFilterButton).setOnClickListener(v -> clearFilter());
        openGalleryButton.setOnClickListener(v -> openInGallery());
        trashButton.setOnClickListener(v ->
                startActivity(new Intent(this, TrashActivity.class)));
        findViewById(R.id.keepButton).setOnClickListener(v -> animateOff(true));
        findViewById(R.id.trashActionButton).setOnClickListener(v -> animateOff(false));

        setupSwipe();
        ensurePermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, MENU_MUTE, 0,
                muted ? R.string.unmute : R.string.mute);
        item.setIcon(muted
                ? android.R.drawable.ic_lock_silent_mode
                : android.R.drawable.ic_lock_silent_mode_off);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_MUTE) {
            muted = !muted;
            SoundFx.setMuted(muted);
            prefs.edit().putBoolean("muted", muted).apply();
            invalidateOptionsMenu();
            Toast.makeText(this,
                    muted ? R.string.muted_toast : R.string.unmuted_toast,
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTrashButton();
    }

    @Override
    protected void onDestroy() {
        sound.release();
        super.onDestroy();
    }

    private String requiredPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void ensurePermission() {
        String perm = requiredPermission();
        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
            reloadDeck();
        } else {
            permissionLauncher.launch(perm);
        }
    }

    private void reloadDeck() {
        List<PhotoItem> loaded = MediaRepository.loadImages(
                this, filterStartMs, filterEndMs, selectedBuckets);
        deck.clear();
        deck.addAll(loaded);
        index = 0;
        updateFilterBar();
        showCurrent();
    }

    private void showCurrent() {
        resetCardTransform();
        updateTrashButton();

        boolean has = index < deck.size();
        openGalleryButton.setVisibility(has ? View.VISIBLE : View.GONE);

        if (!has) {
            cardContainer.setVisibility(View.GONE);
            emptyView.setText(deck.isEmpty()
                    ? R.string.no_photos
                    : R.string.all_reviewed);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        cardContainer.setVisibility(View.VISIBLE);
        PhotoItem current = deck.get(index);
        String date = current.dateTakenMs > 0
                ? new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .format(new java.util.Date(current.dateTakenMs))
                : "";
        String caption;
        if (!current.albumName.isEmpty() && !date.isEmpty()) {
            caption = current.albumName + "  ·  " + date;
        } else {
            caption = current.albumName + date;
        }
        albumNameLabel.setText(caption);
        albumNameLabel.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
        Glide.with(this)
                .load(current.uri)
                .fitCenter()
                .into(cardImage);
    }

    private void updateTrashButton() {
        trashButton.setText(getString(R.string.trash_count, TrashStore.get().size()));
    }

    private void resetCardTransform() {
        cardContainer.setTranslationX(0f);
        cardContainer.setTranslationY(0f);
        cardContainer.setRotation(0f);
        keepLabel.setAlpha(0f);
        trashLabel.setAlpha(0f);
    }

    private void openInGallery() {
        if (index >= deck.size()) return;
        Uri uri = deck.get(index).uri;
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "image/*");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(view);
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_gallery_app, Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Swipe handling ----

    private float downX, downY, startTransX, startTransY;
    private boolean dragging;

    private void setupSwipe() {
        cardContainer.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startTransX = cardContainer.getTranslationX();
                    startTransY = cardContainer.getTranslationY();
                    dragging = true;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    if (!dragging) return false;
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    cardContainer.setTranslationX(startTransX + dx);
                    cardContainer.setTranslationY(startTransY + dy);
                    cardContainer.setRotation((dx / cardContainer.getWidth()) * 18f);
                    float progress = Math.min(1f, Math.abs(dx) / (cardContainer.getWidth() / 2f));
                    keepLabel.setAlpha(dx > 0 ? progress : 0f);
                    trashLabel.setAlpha(dx < 0 ? progress : 0f);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!dragging) return false;
                    dragging = false;
                    float dx = cardContainer.getTranslationX();
                    // shorter swipe distance: ~0.15 of card width triggers
                    float threshold = (cardContainer.getWidth() / 3f) * 0.45f;
                    if (dx > threshold) {
                        animateOff(true);
                    } else if (dx < -threshold) {
                        animateOff(false);
                    } else {
                        cardContainer.animate()
                                .translationX(0f).translationY(0f).rotation(0f)
                                .setDuration(200).start();
                        keepLabel.animate().alpha(0f).setDuration(200).start();
                        trashLabel.animate().alpha(0f).setDuration(200).start();
                    }
                    return true;
                }
            }
            return false;
        });
    }

    /** @param keep true = swipe right / keep; false = swipe left / mark for deletion */
    private void animateOff(boolean keep) {
        if (index >= deck.size()) return;
        final PhotoItem item = deck.get(index);
        if (keep) {
            sound.keep();
        } else {
            sound.trash();
        }
        float targetX = keep
                ? cardContainer.getWidth() * 1.5f
                : -cardContainer.getWidth() * 1.5f;
        keepLabel.animate().alpha(keep ? 1f : 0f).setDuration(150).start();
        trashLabel.animate().alpha(keep ? 0f : 1f).setDuration(150).start();
        cardContainer.animate()
                .translationX(targetX)
                .rotation(keep ? 22f : -22f)
                .setDuration(220)
                .withEndAction(() -> {
                    if (!keep) {
                        TrashStore.get().add(item);
                    }
                    index++;
                    showCurrent();
                })
                .start();
    }

    // ---- Filters ----

    private void showDateFilter() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.pick_date_range)
                        .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            filterStartMs = selection.first;
            // Material returns the start of the end day; extend to end of that day.
            filterEndMs = selection.second + (24L * 60L * 60L * 1000L) - 1L;
            dateLabel = picker.getHeaderText();
            reloadDeck();
            Toast.makeText(this, R.string.filter_applied, Toast.LENGTH_SHORT).show();
        });
        picker.show(getSupportFragmentManager(), "date_range");
    }

    private void showAlbumFilter() {
        final List<MediaRepository.Album> albums = MediaRepository.loadAlbums(this);
        final String[] names = new String[albums.size()];
        final boolean[] checked = new boolean[albums.size()];
        for (int i = 0; i < albums.size(); i++) {
            MediaRepository.Album a = albums.get(i);
            names[i] = a.name + " (" + a.count + ")";
            checked[i] = selectedBuckets.contains(a.id);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.pick_album)
                .setMultiChoiceItems(names, checked,
                        (d, which, isChecked) -> checked[which] = isChecked)
                .setNeutralButton(R.string.all_albums, (d, w) -> {
                    selectedBuckets.clear();
                    albumLabel = null;
                    reloadDeck();
                    Toast.makeText(this, R.string.filter_applied,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    selectedBuckets.clear();
                    int count = 0;
                    String lastName = null;
                    for (int i = 0; i < albums.size(); i++) {
                        if (checked[i]) {
                            selectedBuckets.add(albums.get(i).id);
                            lastName = albums.get(i).name;
                            count++;
                        }
                    }
                    if (count == 0) {
                        albumLabel = null;
                    } else if (count == 1) {
                        albumLabel = lastName;
                    } else {
                        albumLabel = getString(R.string.albums_count, count);
                    }
                    reloadDeck();
                    Toast.makeText(this, R.string.filter_applied,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateFilterBar() {
        StringBuilder sb = new StringBuilder();
        if (albumLabel != null) {
            sb.append("📁 ").append(albumLabel);
        }
        if (dateLabel != null) {
            if (sb.length() > 0) sb.append("   ");
            sb.append("📅 ").append(dateLabel);
        }
        if (sb.length() == 0) {
            filterBar.setVisibility(View.GONE);
        } else {
            filterInfo.setText(sb.toString());
            filterBar.setVisibility(View.VISIBLE);
        }
    }

    private void clearFilter() {
        filterStartMs = Long.MIN_VALUE;
        filterEndMs = Long.MAX_VALUE;
        selectedBuckets.clear();
        dateLabel = null;
        albumLabel = null;
        reloadDeck();
        Toast.makeText(this, R.string.filter_cleared, Toast.LENGTH_SHORT).show();
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
}

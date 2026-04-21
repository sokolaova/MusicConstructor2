package com.example.musicconstructor2.ui.home;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Playlist;
import com.example.musicconstructor2.data.model.RatingInfo;
import com.example.musicconstructor2.data.model.SortMode;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.example.musicconstructor2.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistViewActivity extends AppCompatActivity {

    private TextView tvTitle, tvEmpty;
    private RecyclerView rvTracks, rvPositions;
    private ProgressBar progressBar;
    private ImageView btnBack, btnMenu;
    private LinearLayout layoutDescription;
    private EditText etDescription;
    private PlaylistTrackAdapter trackAdapter;
    private PositionAdapter positionAdapter;

    private String playlistId;
    private String playlistTitle;
    private String playlistDescription;
    private String userId;
    private PlaylistRepository repository;

    // Сортировка
    private ImageView btnSort;
    private LinearLayout layoutSortIndicator;
    private TextView tvSortMode;
    private ImageView btnClearSort;

    private SortMode currentSortMode = SortMode.CUSTOM;
    private List<Track> originalTracks = new ArrayList<>();
    private Map<String, Float> trackRatings = new HashMap<>();
    private Map<String, Float> userTrackRatings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        userId = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0)
        );

        repository = new PlaylistRepository(userId);
        playlistId = getIntent().getStringExtra("playlist_id");
        playlistTitle = getIntent().getStringExtra("playlist_title");
        playlistDescription = getIntent().getStringExtra("playlist_description");

        android.util.Log.d("PlaylistView", "onCreate: playlistId=" + playlistId);

        if (playlistId == null || playlistId.isEmpty()) {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        if (!handleDeepLink()) {
            // Получаем ID плейлиста из Intent
            playlistId = getIntent().getStringExtra("playlist_id");

            if (playlistId != null && !playlistId.isEmpty()) {
                loadPlaylistInfo();
            } else {
                Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvTracks = findViewById(R.id.rvTracks);
        rvPositions = findViewById(R.id.rvPositions);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        layoutDescription = findViewById(R.id.layoutDescription);
        etDescription = findViewById(R.id.etDescription);
        btnSort = findViewById(R.id.btnSort);
        layoutSortIndicator = findViewById(R.id.layoutSortIndicator);
        tvSortMode = findViewById(R.id.tvSortMode);
        btnClearSort = findViewById(R.id.btnClearSort);

        tvTitle.setText(playlistTitle != null ? playlistTitle : "Плейлист");

        // Синхронизируем высоту списков
        rvPositions.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new PositionAdapter();
        rvPositions.setAdapter(positionAdapter);
        rvPositions.setNestedScrollingEnabled(false);

        rvTracks.setLayoutManager(new LinearLayoutManager(this));

        // Создаём адаптер для треков
        trackAdapter = new PlaylistTrackAdapter(userId, playlistId, repository,
                (track, position) -> {
                    List<Track> tracksToPlay = new ArrayList<>();
                    for (Track t : trackAdapter.getTracks()) {
                        if (t.getId() != null && !t.getId().isEmpty()) {
                            tracksToPlay.add(t);
                        }
                    }
                    MusicPlayerServiceHolder.tracks = new ArrayList<>(trackAdapter.getTracks());
                    MusicPlayerServiceHolder.startIndex = position;
                    startActivity(new Intent(this, PlayerActivity.class));
                });
        rvTracks.setAdapter(trackAdapter);
        rvTracks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Синхронизируем прокрутку rvPositions с rvTracks
                rvPositions.scrollBy(dx, dy);
            }
        });

        // Отключаем вложенную прокрутку у обоих списков
        rvTracks.setNestedScrollingEnabled(false);
        rvPositions.setNestedScrollingEnabled(false);
        // Drag and Drop для переупорядочивания треков
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        if (currentSortMode != SortMode.CUSTOM) {
                            return false;
                        }
                        int fromPosition = viewHolder.getAdapterPosition();
                        int toPosition = target.getAdapterPosition();
                        trackAdapter.moveTrack(fromPosition, toPosition);
                        return true;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return currentSortMode == SortMode.CUSTOM;
                    }
                });
        itemTouchHelper.attachToRecyclerView(rvTracks);

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> showPlaylistMenu());
        btnSort.setOnClickListener(v -> showSortOptionsDialog());
        btnClearSort.setOnClickListener(v -> {
            currentSortMode = SortMode.CUSTOM;
            updateSortIndicator();
            applySorting();
        });

        updateDescriptionUI();
    }

    private boolean handleDeepLink() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments != null && !pathSegments.isEmpty()) {
                playlistId = pathSegments.get(0);
                loadPlaylistInfo();
                return true;
            }
        }
        return false;
    }

    private void loadPlaylistInfo() {
        showLoading(true);

        repository.getPlaylistInfo(playlistId, new PlaylistRepository.Callback<Playlist>() {
            @Override
            public void onSuccess(Playlist playlist) {
                playlistTitle = playlist.getTitle();
                playlistDescription = playlist.getDescription();

                tvTitle.setText(playlistTitle != null ? playlistTitle : "Плейлист");
                updateDescriptionUI();
                loadPlaylistTracks();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(PlaylistViewActivity.this,
                        "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylistTracks() {
        if (playlistId == null || playlistId.isEmpty()) {
            showEmpty();
            return;
        }

        showLoading(true);

        repository.getPlaylistTracks(playlistId,
                new PlaylistRepository.Callback<List<Track>>() {
                    @Override
                    public void onSuccess(List<Track> tracks) {
                        showLoading(false);

                        if (tracks != null && !tracks.isEmpty()) {
                            repository.syncPlaylistTrackCount(playlistId, null);
                        }

                        if (tracks == null || tracks.isEmpty()) {
                            showEmpty();
                        } else {
                            showTracks(tracks);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showEmpty();
                        Toast.makeText(PlaylistViewActivity.this,
                                "Ошибка загрузки треков: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSortOptionsDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_sort_options);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        LinearLayout layoutCustom = dialog.findViewById(R.id.layoutSortCustom);
        LinearLayout layoutUserRating = dialog.findViewById(R.id.layoutSortUserRating);
        LinearLayout layoutAverageRating = dialog.findViewById(R.id.layoutSortAverageRating);

        ImageView ivCheckCustom = dialog.findViewById(R.id.ivCheckCustom);
        ImageView ivCheckUserRating = dialog.findViewById(R.id.ivCheckUserRating);
        ImageView ivCheckAverageRating = dialog.findViewById(R.id.ivCheckAverageRating);

        // Показываем галочку у текущего режима
        ivCheckCustom.setVisibility(currentSortMode == SortMode.CUSTOM ? View.VISIBLE : View.GONE);
        ivCheckUserRating.setVisibility(currentSortMode == SortMode.USER_RATING ? View.VISIBLE : View.GONE);
        ivCheckAverageRating.setVisibility(currentSortMode == SortMode.AVERAGE_RATING ? View.VISIBLE : View.GONE);

        layoutCustom.setOnClickListener(v -> {
            currentSortMode = SortMode.CUSTOM;
            dialog.dismiss();
            updateSortIndicator();
            applySorting();
        });

        layoutUserRating.setOnClickListener(v -> {
            currentSortMode = SortMode.USER_RATING;
            dialog.dismiss();
            updateSortIndicator();
            loadUserRatingsAndSort();
        });

        layoutAverageRating.setOnClickListener(v -> {
            currentSortMode = SortMode.AVERAGE_RATING;
            dialog.dismiss();
            updateSortIndicator();
            loadAverageRatingsAndSort();
        });

        dialog.show();
    }

    private void updateSortIndicator() {
        if (currentSortMode == SortMode.CUSTOM) {
            layoutSortIndicator.setVisibility(View.GONE);
        } else {
            layoutSortIndicator.setVisibility(View.VISIBLE);
            tvSortMode.setText(currentSortMode.getDisplayName());
        }
        // Обновляем адаптер для применения изменений drag-and-drop
        trackAdapter.notifyDataSetChanged();
    }

    private void applySorting() {
        List<Track> sortedTracks = new ArrayList<>(originalTracks);

        switch (currentSortMode) {
            case CUSTOM:
                Collections.sort(sortedTracks, (t1, t2) ->
                        Integer.compare(t1.getPosition(), t2.getPosition()));
                break;

            case USER_RATING:
                Collections.sort(sortedTracks, (t1, t2) -> {
                    Float r1 = userTrackRatings.getOrDefault(t1.getId(), 0f);
                    Float r2 = userTrackRatings.getOrDefault(t2.getId(), 0f);
                    return Float.compare(r2, r1);
                });
                break;

            case AVERAGE_RATING:
                Collections.sort(sortedTracks, (t1, t2) -> {
                    Float r1 = trackRatings.getOrDefault(t1.getId(), 0f);
                    Float r2 = trackRatings.getOrDefault(t2.getId(), 0f);
                    return Float.compare(r2, r1);
                });
                break;
        }
        for (int i = 0; i < sortedTracks.size(); i++) {
            sortedTracks.get(i).setPosition(i);
        }
        trackAdapter.setTracks(sortedTracks);
        positionAdapter.setTrackCount(sortedTracks.size());
    }

    private void loadUserRatingsAndSort() {
        if (originalTracks.isEmpty()) {
            applySorting();
            return;
        }

        showLoading(true);
        userTrackRatings.clear();

        for (Track track : originalTracks) {
            repository.getTrackRating(track.getId(), new PlaylistRepository.Callback<RatingInfo>() {
                @Override
                public void onSuccess(RatingInfo ratingInfo) {
                    userTrackRatings.put(track.getId(), ratingInfo.getUserRating());

                    if (userTrackRatings.size() == originalTracks.size()) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            applySorting();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    userTrackRatings.put(track.getId(), 0f);

                    if (userTrackRatings.size() == originalTracks.size()) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            applySorting();
                        });
                    }
                }
            });
        }
    }

    private void loadAverageRatingsAndSort() {
        if (originalTracks.isEmpty()) {
            applySorting();
            return;
        }

        showLoading(true);
        trackRatings.clear();

        for (Track track : originalTracks) {
            repository.getTrackRating(track.getId(), new PlaylistRepository.Callback<RatingInfo>() {
                @Override
                public void onSuccess(RatingInfo ratingInfo) {
                    trackRatings.put(track.getId(), ratingInfo.getAverageRating());

                    if (trackRatings.size() == originalTracks.size()) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            applySorting();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    trackRatings.put(track.getId(), 0f);

                    if (trackRatings.size() == originalTracks.size()) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            applySorting();
                        });
                    }
                }
            });
        }
    }

    private void showTracks(List<Track> tracks) {
        rvTracks.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        originalTracks = new ArrayList<>(tracks);
        applySorting();
    }

    private void showPlaylistMenu() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_playlist_menu);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.5),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
            dialog.getWindow().getAttributes().y = (int) (56 * getResources().getDisplayMetrics().density);
            dialog.getWindow().getAttributes().x = (int) (16 * getResources().getDisplayMetrics().density);
        }

        LinearLayout layoutAddDesc = dialog.findViewById(R.id.layoutAddDescription);
        LinearLayout layoutCopyLink = dialog.findViewById(R.id.layoutCopyLink);
        LinearLayout layoutDelete = dialog.findViewById(R.id.layoutDelete);

        layoutAddDesc.setOnClickListener(v -> {
            dialog.dismiss();
            showDescriptionEditor();
        });

        layoutCopyLink.setOnClickListener(v -> {
            dialog.dismiss();
            copyPlaylistLink();
        });

        layoutDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeletePlaylist();
        });

        dialog.show();
    }

    private void showDescriptionEditor() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_description);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        EditText etDesc = dialog.findViewById(R.id.etPlaylistDescription);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnSave = dialog.findViewById(R.id.btnSave);

        etDesc.setText(playlistDescription != null ? playlistDescription : "");
        etDesc.setSelection(etDesc.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String description = etDesc.getText().toString().trim();
            savePlaylistDescription(description);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void savePlaylistDescription(String description) {
        Toast.makeText(this, "Сохранение...", Toast.LENGTH_SHORT).show();

        repository.updatePlaylistDescription(playlistId, description,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        playlistDescription = description;
                        Toast.makeText(PlaylistViewActivity.this,
                                "Описание сохранено", Toast.LENGTH_SHORT).show();
                        updateDescriptionUI();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(PlaylistViewActivity.this,
                                "Ошибка сохранения: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDescriptionUI() {
        TextView tvDescription = findViewById(R.id.tvDescription);
        View dividerAfterDescription = findViewById(R.id.dividerAfterDescription);

        if (playlistDescription != null && !playlistDescription.isEmpty()) {
            tvDescription.setText(playlistDescription);
            tvDescription.setVisibility(View.VISIBLE);
            if (dividerAfterDescription != null) {
                dividerAfterDescription.setVisibility(View.VISIBLE);
            }
        } else {
            tvDescription.setText("");
            tvDescription.setVisibility(View.GONE);
            if (dividerAfterDescription != null) {
                dividerAfterDescription.setVisibility(View.GONE);
            }
        }
    }

    private void copyPlaylistLink() {
        String playlistLink = "https://musicconstructor.app/share/playlist?id=" + playlistId + "&userId=" + userId;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Ссылка на плейлист", playlistLink);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Ссылка скопирована", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeletePlaylist() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_delete);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnDelete = dialog.findViewById(R.id.btnDelete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deletePlaylist();
        });

        dialog.show();
    }

    private void deletePlaylist() {
        repository.deletePlaylist(playlistId,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(PlaylistViewActivity.this,
                                "Плейлист удалён", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(PlaylistViewActivity.this,
                                "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvTracks.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        rvTracks.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
}
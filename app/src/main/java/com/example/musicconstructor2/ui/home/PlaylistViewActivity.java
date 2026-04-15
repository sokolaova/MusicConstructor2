package com.example.musicconstructor2.ui.home;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Playlist;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.example.musicconstructor2.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        userId = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0)
        );

        repository = new PlaylistRepository(userId);

        initViews();

        // Получаем ID плейлиста
        playlistId = getIntent().getStringExtra("playlist_id");

        if (playlistId != null && !playlistId.isEmpty()) {
            loadPlaylistInfo();
        } else {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
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

        tvTitle.setText(playlistTitle != null ? playlistTitle : "Плейлист");
        TextView tvDescription = findViewById(R.id.tvDescription);
        View dividerAfterDescription = findViewById(R.id.dividerAfterDescription);
        // Синхронизируем высоту списков
        rvPositions.setLayoutManager(new LinearLayoutManager(this));
        positionAdapter = new PositionAdapter();
        rvPositions.setAdapter(positionAdapter);
        rvPositions.setNestedScrollingEnabled(false);

        rvTracks.setLayoutManager(new LinearLayoutManager(this));

        // Создаём адаптер для треков
        trackAdapter = new PlaylistTrackAdapter(userId, playlistId, repository,
                (track, position) -> {
                    // Переход в плеер при клике на трек
                    MusicPlayerServiceHolder.tracks = new ArrayList<>(trackAdapter.getTracks());
                    MusicPlayerServiceHolder.startIndex = position;
                    startActivity(new android.content.Intent(this, PlayerActivity.class));
                });
        rvTracks.setAdapter(trackAdapter);

        // Drag and Drop для переупорядочивания треков
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        int fromPosition = viewHolder.getAdapterPosition();
                        int toPosition = target.getAdapterPosition();
                        trackAdapter.moveTrack(fromPosition, toPosition);
                        return true;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    }
                });
        itemTouchHelper.attachToRecyclerView(rvTracks);

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> showPlaylistMenu());
        updateDescriptionUI();
    }

    private void loadPlaylistTracks() {
        if (playlistId == null || playlistId.isEmpty()) {
            showEmpty();
            return;
        }

        android.util.Log.d("PlaylistView", "Загрузка треков для: " + playlistId);

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
    private void showPlaylistMenu() {
        // Создаем кастомный диалог
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_playlist_menu);

        // Настраиваем размеры и позицию
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.5),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );

            // Позиционируем меню под кнопкой меню
            dialog.getWindow().setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
            dialog.getWindow().getAttributes().y = (int) (56 * getResources().getDisplayMetrics().density);
            dialog.getWindow().getAttributes().x = (int) (16 * getResources().getDisplayMetrics().density);
        }

        // Находим элементы
        LinearLayout layoutAddDesc = dialog.findViewById(R.id.layoutAddDescription);
        LinearLayout layoutCopyLink = dialog.findViewById(R.id.layoutCopyLink);
        LinearLayout layoutDelete = dialog.findViewById(R.id.layoutDelete);

        // Обработчики
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
        // Создаем кастомный диалог
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_description);

        // Настраиваем размеры и закругления
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        // Находим элементы
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        EditText etDescription = dialog.findViewById(R.id.etPlaylistDescription);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnSave = dialog.findViewById(R.id.btnSave);


        // Устанавливаем текущее описание
        etDescription.setText(playlistDescription != null ? playlistDescription : "");

        // Ставим курсор в конец
        etDescription.setSelection(etDescription.getText().length());

        // Обработчики
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String description = etDescription.getText().toString().trim();
            savePlaylistDescription(description);
            dialog.dismiss();
        });

        dialog.show();
    }
    private void savePlaylistDescription(String description) {
        // Показываем прогресс
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

        android.util.Log.d("PlaylistView", "updateDescriptionUI: description = '" + playlistDescription + "'");

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
    private void confirmDeletePlaylist() {
        // Создаем кастомный диалог
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_delete);

        // Настраиваем размеры и закругления
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        // Находим элементы
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnDelete = dialog.findViewById(R.id.btnDelete);

        // Обработчики
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
    private void copyPlaylistLink() {
        // Создаем правильную ссылку на плейлист
        String playlistLink;

        playlistLink = "https://musicconstructor.app/share/playlist?id=" + playlistId + "&userId=" + userId;

        // Копируем в буфер обмена
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Ссылка на плейлист", playlistLink);
        clipboard.setPrimaryClip(clip);

        // Показываем уведомление с ссылкой
        Toast.makeText(this,
                "Ссылка скопирована: " + playlistLink,
                Toast.LENGTH_LONG).show();
    }
    private void showTracks(List<Track> tracks) {
        rvTracks.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        trackAdapter.setTracks(tracks);
        positionAdapter.setTrackCount(tracks.size());
    }

    private void showEmpty() {
        rvTracks.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
    private boolean handleDeepLink() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            // Получаем ID плейлиста из ссылки
            // musicconstructor://playlist/PLAYLIST_ID
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

                // Логируем для отладки
                android.util.Log.d("PlaylistView", "=== ДАННЫЕ ПЛЕЙЛИСТА ===");
                android.util.Log.d("PlaylistView", "ID: " + playlistId);
                android.util.Log.d("PlaylistView", "Title: " + playlistTitle);
                android.util.Log.d("PlaylistView", "Description: '" + playlistDescription + "'");
                android.util.Log.d("PlaylistView", "Description empty: " + (playlistDescription == null || playlistDescription.isEmpty()));

                // Обновляем заголовок
                tvTitle.setText(playlistTitle != null ? playlistTitle : "Плейлист");

                updateDescriptionUI();
                loadPlaylistTracks();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                android.util.Log.e("PlaylistView", "Ошибка загрузки: " + error);
                Toast.makeText(PlaylistViewActivity.this,
                        "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

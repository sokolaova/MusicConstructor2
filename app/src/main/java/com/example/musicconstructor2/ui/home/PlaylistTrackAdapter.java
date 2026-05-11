package com.example.musicconstructor2.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistTrackAdapter extends RecyclerView.Adapter<PlaylistTrackAdapter.TrackViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private final String userId;
    private final String playlistId;
    private final PlaylistRepository repository;
    private final OnTrackClickListener listener;
    private final OnTrackRemoveListener removeListener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    public interface OnTrackRemoveListener {
        void onTrackRemoved(int position);
    }

    public PlaylistTrackAdapter(String userId, String playlistId,
                                PlaylistRepository repository,
                                OnTrackClickListener listener,
                                OnTrackRemoveListener removeListener) {
        this.userId = userId;
        this.playlistId = playlistId;
        this.repository = repository;
        this.listener = listener;
        this.removeListener = removeListener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void moveTrack(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= tracks.size() ||
                toPosition < 0 || toPosition >= tracks.size()) {
            return;
        }

        Collections.swap(tracks, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        if (playlistId != null && !playlistId.isEmpty()) {
            updateTracksPositions();
        }
    }

    public void updateTracksPositions() {
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).setPosition(i);
        }

        if (playlistId == null || playlistId.isEmpty()) {
            return;
        }

        for (Track track : tracks) {
            if (track.getId() == null || track.getId().isEmpty()) {
                return;
            }
        }

        repository.updateTracksOrder(playlistId, tracks, new PlaylistRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {}

            @Override
            public void onError(String error) {}
        });
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ✅ Правильное имя файла разметки
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track_in_playlist, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        holder.bind(tracks.get(position), position);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivCover;
        TextView tvTitle, tvArtist, tvDuration;
        ImageView btnRemove;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTrackTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnRemove = itemView.findViewById(R.id.btnRemoveTrack);
        }

        void bind(Track track, int position) {
            // ✅ Устанавливаем реальные данные трека
            if (tvTitle != null) {
                tvTitle.setText(track.getTitle() != null ? track.getTitle() : "Без названия");
            }
            if (tvArtist != null) {
                tvArtist.setText(track.getArtist() != null ? track.getArtist() : "Неизвестный исполнитель");
            }
            if (tvDuration != null) {
                tvDuration.setText(track.getFormattedDuration());
            }

            // ✅ Загружаем обложку трека
            if (ivCover != null) {
                if (track.getCoverUrl() != null && !track.getCoverUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(track.getCoverUrl())
                            .placeholder(R.drawable.ic_default_cover)
                            .error(R.drawable.ic_default_cover)
                            .into(ivCover);
                } else {
                    ivCover.setImageResource(R.drawable.ic_default_cover);
                }
            }

            // Клик по элементу
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onTrackClick(tracks.get(pos), pos);
                    }
                }
            });

            // Кнопка удаления
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        showDeleteDialog(pos);
                    }
                });
            }
        }

        private void showDeleteDialog(int position) {
            Track track = tracks.get(position);

            // Создаём кастомный диалог
            android.app.Dialog dialog = new android.app.Dialog(itemView.getContext());
            dialog.setContentView(R.layout.dialog_confirm_remove_track);

            // Настраиваем размеры и прозрачный фон
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        (int) (itemView.getContext().getResources().getDisplayMetrics().widthPixels * 0.85),
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }


            // Находим элементы
            TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
            TextView btnCancel = dialog.findViewById(R.id.btnCancel);
            TextView btnRemove = dialog.findViewById(R.id.btnRemove);


            // Обновляем сообщение с названием трека
            tvMessage.setText("Трек «" + track.getTitle() + "» будет удалён из этого плейлиста.");

            // Обработчики
            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnRemove.setOnClickListener(v -> {
                dialog.dismiss();

                // Удаляем из Firestore
                if (playlistId != null && track.getId() != null) {
                    repository.removeTrack(playlistId, track.getId(),
                            new PlaylistRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    android.util.Log.d("PlaylistTrackAdapter", "Трек удалён из Firestore");
                                }

                                @Override
                                public void onError(String error) {
                                    android.util.Log.e("PlaylistTrackAdapter", "Ошибка удаления: " + error);
                                }
                            });
                }

                // Удаляем из локального списка
                tracks.remove(position);
                notifyItemRemoved(position);

                // Обновляем позиции
                updateTracksPositions();

                // Уведомляем Activity
                if (removeListener != null) {
                    removeListener.onTrackRemoved(position);
                }
            });

            dialog.show();
        }
    }
}
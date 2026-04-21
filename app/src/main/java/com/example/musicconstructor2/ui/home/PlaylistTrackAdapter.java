package com.example.musicconstructor2.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistTrackAdapter extends RecyclerView.Adapter<PlaylistTrackAdapter.TrackViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private final String userId;
    private boolean dragEnabled = true;
    private final String playlistId;
    private final PlaylistRepository repository;
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onPlayClick(Track track, int position);
    }

    public PlaylistTrackAdapter(String userId, String playlistId, PlaylistRepository repository,
                                OnTrackClickListener listener) {
        this.userId = userId;
        this.playlistId = playlistId;
        this.repository = repository;
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
        notifyDataSetChanged();
    }
    public void setDragEnabled(boolean enabled) {
        this.dragEnabled = enabled;
    }
    public boolean isDragEnabled() {
        return dragEnabled;
    }
    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public void moveTrack(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(tracks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(tracks, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        updateTracksPositions();
    }

    private void updateTracksPositions() {
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).setPosition(i + 1);
        }
        repository.updateTracksOrder(playlistId, tracks,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Успешно обновлены позиции
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(null, "Ошибка сохранения порядка: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        ImageView btnMenu;
        CardView cardTrack;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover    = itemView.findViewById(R.id.ivCover);
            tvTitle    = itemView.findViewById(R.id.tvTrackTitle);
            tvArtist   = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnMenu    = itemView.findViewById(R.id.btnMenu);
            cardTrack  = itemView.findViewById(R.id.cardTrack);
        }

        void bind(Track track, int position) {
            tvTitle.setText(track.getTitle() != null ? track.getTitle() : "Без названия");
            tvArtist.setText(track.getArtist() != null ? track.getArtist() : "Неизвестный исполнитель");
            tvDuration.setText(track.getFormattedDuration());

            // Загрузка обложки
            if (track.getCoverUrl() != null && !track.getCoverUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(track.getCoverUrl())
                        .placeholder(R.drawable.ic_default_cover)
                        .error(R.drawable.ic_default_cover)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.ic_default_cover);
            }

            // Клик по карточке - воспроизведение
            cardTrack.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(track, position);
                }
            });

            // Клик на кнопку удаления
            btnMenu.setOnClickListener(v -> {
                // Подтверждение удаления
                new MaterialAlertDialogBuilder(itemView.getContext(), R.style.RoundedDialog)
                        .setTitle("Удалить трек?")
                        .setMessage("Трек будет удален из плейлиста")
                        .setPositiveButton("Удалить", (dialog, which) -> removeTrack(track.getId(), position))
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        private void removeTrack(String trackId, int position) {
            repository.removeTrack(playlistId, trackId,
                    new PlaylistRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            tracks.remove(position);
                            notifyItemRemoved(position);
                            updateTracksPositions();
                            Toast.makeText(itemView.getContext(),
                                    "Трек удалён из плейлиста", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(itemView.getContext(),
                                    "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}

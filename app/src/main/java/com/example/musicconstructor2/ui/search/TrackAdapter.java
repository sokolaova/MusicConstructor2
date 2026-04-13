package com.example.musicconstructor2.ui.search;

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
import com.example.musicconstructor2.ui.player.PlaylistPickerDialog;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private final String userId;

    public interface OnTrackClickListener {
        void onPlayClick(Track track, int position);
    }

    private OnTrackClickListener listener;

    public TrackAdapter(String userId, OnTrackClickListener listener) {
        this.userId = userId;
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return tracks;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        holder.bind(tracks.get(position));
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView ivCover;
        TextView tvTitle, tvArtist, tvDuration;
        ImageView btnAdd;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover    = itemView.findViewById(R.id.ivCover);
            tvTitle    = itemView.findViewById(R.id.tvTrackTitle);
            tvArtist   = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnAdd     = itemView.findViewById(R.id.btnAddTrack);
        }

        void bind(Track track) {
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

            btnAdd.setOnClickListener(v -> {
                // Создаем и показываем диалог выбора плейлиста
                PlaylistPickerDialog dialog = new PlaylistPickerDialog(
                        itemView.getContext(),
                        track,
                        userId
                );
                dialog.show();
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onPlayClick(track, position);
                    }
                }
            });
        }
    }
}
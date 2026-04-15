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
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onPlayClick(Track track, int position);
    }

    public TrackAdapter(String userId, OnTrackClickListener listener) {
        this.userId   = userId;
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return tracks != null ? tracks : new ArrayList<>();
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
        holder.bind(tracks.get(position), position);
    }

    @Override
    public int getItemCount() {
        return tracks != null ? tracks.size() : 0;
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

        void bind(Track track, int position) {
            tvTitle.setText(track.getTitle());
            tvArtist.setText(track.getArtist());
            tvDuration.setText(track.getFormattedDuration());

            if (track.getCoverUrl() != null && !track.getCoverUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(track.getCoverUrl())
                        .placeholder(R.drawable.ic_default_cover)
                        .error(R.drawable.ic_default_cover)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.ic_default_cover);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(track, position);
                }
            });

            btnAdd.setOnClickListener(v -> {
                PlaylistPickerDialog dialog = new PlaylistPickerDialog(
                        itemView.getContext(),
                        track,
                        userId
                );
                dialog.show();
            });
        }
    }
}

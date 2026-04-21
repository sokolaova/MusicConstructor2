package com.example.musicconstructor2.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Playlist;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists != null ? playlists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        holder.bind(playlists.get(position));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTrackCount;
        MaterialCardView cardPlaylist;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPlaylistTitle);
            tvTrackCount = itemView.findViewById(R.id.tvTrackCount);
            cardPlaylist = (MaterialCardView) itemView;
        }

        void bind(Playlist playlist) {
            tvTitle.setText(playlist.getTitle() != null ? playlist.getTitle() : "Без названия");
            tvTrackCount.setText(playlist.getTrackCount() + " треков");

            cardPlaylist.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }
    }
}

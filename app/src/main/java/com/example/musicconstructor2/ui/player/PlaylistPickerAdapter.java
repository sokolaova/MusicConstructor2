package com.example.musicconstructor2.ui.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Playlist;

import java.util.List;

public class PlaylistPickerAdapter
        extends RecyclerView.Adapter<PlaylistPickerAdapter.ViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    private final List<Playlist>          playlists;
    private final OnPlaylistClickListener listener;

    public PlaylistPickerAdapter(List<Playlist> playlists,
                                 OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTrackCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle      = itemView.findViewById(R.id.tvPlaylistTitle);
            tvTrackCount = itemView.findViewById(R.id.tvTrackCount);
        }

        void bind(Playlist playlist) {
            tvTitle.setText(playlist.getTitle() != null
                    ? playlist.getTitle() : "Без названия");
            tvTrackCount.setText(playlist.getTrackCount() + " треков");
        }
    }
}
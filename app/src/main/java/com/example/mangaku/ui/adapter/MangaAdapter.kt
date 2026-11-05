package com.example.mangaku.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaku.R
import com.example.mangaku.data.model.Manga
import com.example.mangaku.databinding.ItemMangaBinding

class MangaAdapter(
    private var mangaList: List<Manga>,
    private val onItemClick: (Manga) -> Unit,
    private val onEditClick: (Manga) -> Unit,
    private val onDeleteClick: (Manga) -> Unit
) : RecyclerView.Adapter<MangaAdapter.MangaViewHolder>() {

    inner class MangaViewHolder(val binding: ItemMangaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = ItemMangaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val manga = mangaList[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvMangaTitle.text = manga.title
            tvMangaAuthor.text = "Karya: ${manga.author} • ${manga.releaseYear}"
            tvMangaGenre.text = manga.genre
            tvCoverGenre.text = manga.genre.uppercase(java.util.Locale.ROOT)
            tvMangaRating.text = String.format(java.util.Locale.US, "%.1f", manga.rating)
            tvMangaSynopsis.text = manga.synopsis

            if (manga.status.equals("Tersedia", ignoreCase = true)) {
                tvMangaStatus.text = "Tersedia"
                tvMangaStatus.setTextColor(ContextCompat.getColor(context, R.color.status_available))
                tvMangaStatus.setBackgroundResource(R.drawable.bg_badge_available)
            } else {
                val borrower = if (!manga.borrowedBy.isNullOrBlank()) " (${manga.borrowedBy})" else ""
                tvMangaStatus.text = "Dipinjam$borrower"
                tvMangaStatus.setTextColor(ContextCompat.getColor(context, R.color.status_borrowed))
                tvMangaStatus.setBackgroundResource(R.drawable.bg_badge_borrowed)
            }

            root.setOnClickListener {
                onItemClick(manga)
            }

            btnEditManga.setOnClickListener {
                onEditClick(manga)
            }

            btnDeleteManga.setOnClickListener {
                onDeleteClick(manga)
            }
        }
    }

    override fun getItemCount(): Int = mangaList.size

    fun updateData(newList: List<Manga>) {
        mangaList = newList
        notifyDataSetChanged()
    }
}

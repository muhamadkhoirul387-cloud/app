package com.example.mangaku

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mangaku.data.db.MangaDatabaseHelper
import com.example.mangaku.data.model.Manga
import com.example.mangaku.databinding.ActivityMainBinding
import com.example.mangaku.databinding.DialogMangaDetailBinding
import com.example.mangaku.databinding.DialogMangaFormBinding
import com.example.mangaku.ui.adapter.MangaAdapter
import com.example.mangaku.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: MangaDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var mangaAdapter: MangaAdapter

    private var currentMangaList: List<Manga> = emptyList()
    private var selectedCategory: String = "Semua"
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = MangaDatabaseHelper(this)

        setupHeaderAndMenu()
        setupRecyclerView()
        setupSearchAndFilter()
        setupFab()

        loadMangaData()
    }

    /**
     * Tampilkan username dan avatar di menu/header perpustakaan
     */
    private fun setupHeaderAndMenu() {
        val username = sessionManager.getUsername()
        binding.tvWelcomeUser.text = "Halo, $username! 👋"
        val initial = if (username.isNotBlank()) username.first().uppercase() else "M"
        binding.tvUserInitial.text = initial

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar dari Akun")
                .setMessage("Apakah Anda ingin keluar dari akun perpustakaan?")
                .setPositiveButton("Keluar") { _, _ ->
                    sessionManager.logout()
                    Toast.makeText(this, "Anda telah keluar", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupRecyclerView() {
        mangaAdapter = MangaAdapter(
            mangaList = emptyList(),
            onItemClick = { manga -> showMangaDetailDialog(manga) },
            onEditClick = { manga -> showMangaFormDialog(manga) },
            onDeleteClick = { manga -> confirmDeleteManga(manga) }
        )

        binding.rvMangaList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mangaAdapter
        }
    }

    private fun setupSearchAndFilter() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.contains(R.id.chipShounen) -> "Shounen"
                checkedIds.contains(R.id.chipAction) -> "Action"
                checkedIds.contains(R.id.chipFantasy) -> "Fantasy"
                checkedIds.contains(R.id.chipRomance) -> "Romance"
                checkedIds.contains(R.id.chipMystery) -> "Mystery"
                checkedIds.contains(R.id.chipComedy) -> "Comedy"
                else -> "Semua"
            }
            applyFilters()
        }
    }

    private fun setupFab() {
        binding.fabAddManga.setOnClickListener {
            showMangaFormDialog(null)
        }
    }

    private fun loadMangaData() {
        applyFilters()
        updateStats()
    }

    private fun applyFilters() {
        currentMangaList = dbHelper.searchManga(currentSearchQuery, selectedCategory)
        mangaAdapter.updateData(currentMangaList)

        binding.layoutEmpty.visibility = if (currentMangaList.isEmpty()) View.VISIBLE else View.GONE
        binding.tvMangaCount.text = "${currentMangaList.size} Judul Ditemukan"
    }

    private fun updateStats() {
        val allManga = dbHelper.getAllManga()
        val total = allManga.size
        val available = allManga.count { it.status.equals("Tersedia", ignoreCase = true) }
        val borrowed = total - available

        binding.tvStatTotal.text = total.toString()
        binding.tvStatAvailable.text = available.toString()
        binding.tvStatBorrowed.text = borrowed.toString()
    }

    /**
     * CRUD: CREATE & UPDATE Dialog
     */
    private fun showMangaFormDialog(manga: Manga?) {
        val isEdit = manga != null
        val dialogBinding = DialogMangaFormBinding.inflate(LayoutInflater.from(this))

        if (manga != null) {
            dialogBinding.tvFormTitle.text = "Edit Koleksi Manga"
            dialogBinding.etTitle.setText(manga.title)
            dialogBinding.etAuthor.setText(manga.author)
            dialogBinding.etGenre.setText(manga.genre)
            dialogBinding.etYear.setText(manga.releaseYear)
            dialogBinding.etStatus.setText(manga.status)
            dialogBinding.etRating.setText(manga.rating.toString())
            dialogBinding.etSynopsis.setText(manga.synopsis)
        } else {
            dialogBinding.tvFormTitle.text = "Tambah Koleksi Manga"
            dialogBinding.etStatus.setText("Tersedia")
            dialogBinding.etRating.setText("4.5")
            dialogBinding.etYear.setText("2024")
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim()
            val author = dialogBinding.etAuthor.text.toString().trim()
            val genre = dialogBinding.etGenre.text.toString().trim()
            val year = dialogBinding.etYear.text.toString().trim()
            val status = dialogBinding.etStatus.text.toString().trim().ifEmpty { "Tersedia" }
            val rating = dialogBinding.etRating.text.toString().toDoubleOrNull() ?: 4.5
            val synopsis = dialogBinding.etSynopsis.text.toString().trim()

            if (title.isEmpty()) {
                dialogBinding.etTitle.error = "Judul manga wajib diisi"
                dialogBinding.etTitle.requestFocus()
                return@setOnClickListener
            }

            if (author.isEmpty()) {
                dialogBinding.etAuthor.error = "Nama mangaka/penulis wajib diisi"
                dialogBinding.etAuthor.requestFocus()
                return@setOnClickListener
            }

            if (genre.isEmpty()) {
                dialogBinding.etGenre.error = "Genre/Kategori wajib diisi"
                dialogBinding.etGenre.requestFocus()
                return@setOnClickListener
            }

            if (isEdit) {
                val updatedManga = manga!!.copy(
                    title = title,
                    author = author,
                    genre = genre,
                    releaseYear = year,
                    status = status,
                    rating = rating,
                    synopsis = synopsis
                )
                dbHelper.updateManga(updatedManga)
                Toast.makeText(this, "Manga '$title' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            } else {
                val newManga = Manga(
                    title = title,
                    author = author,
                    genre = genre,
                    releaseYear = year,
                    status = status,
                    rating = rating,
                    synopsis = synopsis
                )
                dbHelper.insertManga(newManga)
                Toast.makeText(this, "Manga '$title' berhasil ditambahkan ke katalog!", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            loadMangaData()
        }

        dialog.show()
    }

    /**
     * CRUD: READ Detail Dialog & Pinjam/Kembalikan action
     */
    private fun showMangaDetailDialog(manga: Manga) {
        val detailBinding = DialogMangaDetailBinding.inflate(LayoutInflater.from(this))

        detailBinding.tvDetailTitle.text = manga.title
        detailBinding.tvDetailAuthor.text = "Mangaka: ${manga.author}"
        detailBinding.tvDetailYear.text = "Tahun: ${manga.releaseYear}"
        detailBinding.tvDetailGenre.text = manga.genre
        detailBinding.tvDetailRating.text = "${String.format(java.util.Locale.US, "%.1f", manga.rating)} / 5.0"
        detailBinding.tvDetailSynopsis.text = manga.synopsis.ifBlank { "Tidak ada sinopsis tersedia." }

        val isAvailable = manga.status.equals("Tersedia", ignoreCase = true)
        if (isAvailable) {
            detailBinding.tvDetailStatus.text = "Tersedia"
            detailBinding.tvDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_available))
            detailBinding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_available)
            detailBinding.tvDetailBorrower.visibility = View.GONE
            detailBinding.btnBorrowToggle.text = "Pinjam Komik Ini"
            detailBinding.btnBorrowToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            val borrowerText = if (!manga.borrowedBy.isNullOrBlank()) "Sedang dipinjam oleh: ${manga.borrowedBy}" else "Status: Sedang Dipinjam"
            detailBinding.tvDetailStatus.text = "Dipinjam"
            detailBinding.tvDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_borrowed))
            detailBinding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_borrowed)
            detailBinding.tvDetailBorrower.visibility = View.VISIBLE
            detailBinding.tvDetailBorrower.text = borrowerText
            detailBinding.btnBorrowToggle.text = "Kembalikan Komik"
            detailBinding.btnBorrowToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.wood_brown))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(detailBinding.root)
            .create()

        detailBinding.btnBorrowToggle.setOnClickListener {
            val currentUsername = sessionManager.getUsername()
            if (isAvailable) {
                dbHelper.updateBorrowStatus(manga.id, "Dipinjam", currentUsername)
                Toast.makeText(this, "Berhasil meminjam '${manga.title}' atas nama $currentUsername", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.updateBorrowStatus(manga.id, "Tersedia", null)
                Toast.makeText(this, "Komik '${manga.title}' telah dikembalikan ke rak perpustakaan", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
            loadMangaData()
        }

        detailBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * CRUD: DELETE Confirmation
     */
    private fun confirmDeleteManga(manga: Manga) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Manga")
            .setMessage("Apakah Anda yakin ingin menghapus '${manga.title}' dari koleksi perpustakaan?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Hapus") { _, _ ->
                dbHelper.deleteManga(manga.id)
                Toast.makeText(this, "'${manga.title}' berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadMangaData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}

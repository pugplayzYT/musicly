package com.puggables.musically.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.puggables.musically.databinding.BottomsheetEditSongBinding

class EditSongBottomSheet(
    private val titleDefault: String,
    private val albumDefault: String,
    private val onPickImage: () -> Unit,
    private val onPickAudio: () -> Unit,
    private val onSave: (title: String?, album: String?) -> Unit,
    private val onDelete: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetEditSongBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomsheetEditSongBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.titleEdit.setText(titleDefault)
        b.albumEdit.setText(albumDefault)

        b.pickImageButton.setOnClickListener { onPickImage() }
        b.pickAudioButton.setOnClickListener { onPickAudio() }
        b.saveButton.setOnClickListener {
            onSave(b.titleEdit.text?.toString(), b.albumEdit.text?.toString())
            dismissAllowingStateLoss()
        }
        b.deleteButton.setOnClickListener {
            onDelete()
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

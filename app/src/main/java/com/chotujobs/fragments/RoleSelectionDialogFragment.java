package com.chotujobs.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chotujobs.R;

public class RoleSelectionDialogFragment extends DialogFragment {

    private OnRoleSelectedListener listener;

    public interface OnRoleSelectedListener {
        void onRoleSelected(String role);
    }

    public void setOnRoleSelectedListener(OnRoleSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Your Role");
        builder.setView(R.layout.dialog_role_selection);

        builder.setPositiveButton("OK", (dialog, which) -> {
            Spinner spinner = getDialog().findViewById(R.id.roleSpinner);
            String role = spinner.getSelectedItem().toString().toLowerCase();
            if (listener != null) {
                listener.onRoleSelected(role);
            }
        });

        return builder.create();
    }
}

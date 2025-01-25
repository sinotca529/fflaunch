package com.github.sinotca529.fflaunch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.List;

public class AliasDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE_NAME = "package_name";

    public static AliasDialogFragment newInstance(String packageName) {
        final var args = new Bundle();
        args.putSerializable(ARG_PACKAGE_NAME, packageName);
        final var fragment = new AliasDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final var packageName = getArguments().getString(ARG_PACKAGE_NAME);

        final var builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("べつめいをつける");

        final EditText aliasInput = new EditText(getActivity());
        builder.setView(aliasInput);

        builder.setPositiveButton("とうろく", (dialog, which) -> {
            final var alias = aliasInput.getText().toString().trim();
            if (alias.isEmpty()) return;
            addAlias(packageName, alias);
        });
        builder.setNegativeButton("やめる", (dialog, which) -> dismiss());
        builder.setNeutralButton("べつめいをけす", (dialog, which) -> showDeleteAliasDialog(packageName));

        return builder.create();
    }

    private void addAlias(String packageName, String alias) {
        AliasManager aliasManager = new AliasManager(getContext());
        aliasManager.addAlias(packageName, alias);
    }

    // 別名削除のダイアログ表示
    private void showDeleteAliasDialog(String packageName) {
        final var aliasManager = new AliasManager(getContext());
        final var aliases = aliasManager.getAliases(packageName);

        final var builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("どれをけす？");

        builder.setItems(aliases.toArray(new String[0]), (dialog, which) -> {
            final var aliasToDelete = aliases.get(which);
            aliasManager.removeAlias(packageName, aliasToDelete);
        });

        builder.setNegativeButton("やめる", (dialog, which) -> dismiss());
        builder.show();
    }
}

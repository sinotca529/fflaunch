package com.github.sinotca529.fflaunch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class AliasDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE_NAME = "package_name";
    private static final String ARG_APP_NAME = "app_name";

    public static AliasDialogFragment newInstance(String packageName, String appName) {
        final var args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, packageName);
        args.putString(ARG_APP_NAME, appName);
        final var fragment = new AliasDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final var packageName = getArguments().getString(ARG_PACKAGE_NAME);
        final var appName = getArguments().getString(ARG_APP_NAME);
        final var context = requireContext();

        final var builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("「" + appName + "」のべつめい");

        final var padding = (int) (20 * context.getResources().getDisplayMetrics().density);
        final var layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, 0, padding, 0);

        final var aliases = new AliasManager(context).getAliases(packageName);
        if (!aliases.isEmpty()) {
            final var currentAliases = new TextView(context);
            currentAliases.setText("いまのべつめい: " + String.join("、", aliases));
            layout.addView(currentAliases);
        }

        final EditText aliasInput = new EditText(context);
        aliasInput.setHint("あたらしいべつめい");
        layout.addView(aliasInput);
        builder.setView(layout);

        builder.setPositiveButton("とうろく", (dialog, which) -> {
            final var alias = aliasInput.getText().toString().trim();
            if (alias.isEmpty()) return;
            addAlias(packageName, alias);
        });
        builder.setNegativeButton("やめる", (dialog, which) -> dismiss());
        if (!aliases.isEmpty()) {
            builder.setNeutralButton("べつめいをけす", (dialog, which) -> showDeleteAliasDialog(packageName));
        }

        final var dialog = builder.create();

        // 開いたらすぐ入力できるようにする
        aliasInput.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    private void addAlias(String packageName, String alias) {
        AliasManager aliasManager = new AliasManager(getContext());
        aliasManager.addAlias(packageName, alias);
        notifyAliasesChanged();
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
            notifyAliasesChanged();
        });

        builder.setNegativeButton("やめる", (dialog, which) -> dismiss());
        builder.show();
    }

    // 検索結果と行の別名表示を最新化する
    private void notifyAliasesChanged() {
        final var activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).onAliasesChanged();
        }
    }
}

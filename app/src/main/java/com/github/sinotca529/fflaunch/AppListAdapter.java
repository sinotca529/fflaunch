package com.github.sinotca529.fflaunch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    private final List<AppInfo> appList;
    private final Context context;

    public AppListAdapter(List<AppInfo> appList, Context context) {
        this.appList = appList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final var appInfo = appList.get(position);
        holder.appName.setText(appInfo.getAppName());
        holder.appIcon.setImageDrawable(appInfo.getAppIcon());

        // 登録済みの別名を小さく併記する (何と登録したか思い出せるように)
        final var aliases = new AliasManager(context).getAliases(appInfo.getPackageName());
        if (aliases.isEmpty()) {
            holder.appAlias.setVisibility(View.GONE);
        } else {
            holder.appAlias.setVisibility(View.VISIBLE);
            holder.appAlias.setText(String.join("、", aliases));
        }

        holder.itemView.setOnLongClickListener(v -> {
            showAliasDialog(appInfo);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            launchApp(appInfo.getPackageName());
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    // 現在表示中の先頭候補 (Enter キーでの起動対象)
    public AppInfo getFirstApp() {
        return appList.isEmpty() ? null : appList.get(0);
    }

    public void updateAppList(List<AppInfo> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return appList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                final var oldAppName = appList.get(oldItemPosition).getPackageName();
                final var newAppName = newList.get(newItemPosition).getPackageName();
                return oldAppName.equals(newAppName);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                final var oldItem = appList.get(oldItemPosition);
                final var newItem = newList.get(newItemPosition);
                return oldItem.equals(newItem);
            }
        });

        appList.clear();
        appList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView appAlias;
        ImageView appIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appAlias = itemView.findViewById(R.id.app_alias);
            appIcon = itemView.findViewById(R.id.app_icon);
        }
    }

    private void showAliasDialog(AppInfo appInfo) {
        final var dialogFragment =
            AliasDialogFragment.newInstance(appInfo.getPackageName(), appInfo.getAppName());
        dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "aliasDialog");
    }

    public void launchApp(String packageName) {
         final var launchIntent = context
            .getPackageManager()
            .getLaunchIntentForPackage(packageName);

         if (launchIntent == null) {
             Toast.makeText(context, "アプリが起動できません", Toast.LENGTH_SHORT).show();
             return;
         }

         context.startActivity(launchIntent);
    }
}

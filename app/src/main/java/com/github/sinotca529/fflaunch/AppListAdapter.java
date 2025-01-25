package com.github.sinotca529.fflaunch;

import android.content.Context;
import android.content.Intent;
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

        holder.itemView.setOnLongClickListener(v -> {
            showAliasDialog(appInfo.getPackageName());
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
        ImageView appIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appIcon = itemView.findViewById(R.id.app_icon);
        }
    }

    private void showAliasDialog(String packageName) {
        final var dialogFragment = AliasDialogFragment.newInstance(packageName);
        dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "aliasDialog");
    }

    private void launchApp(String packageName) {
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
package com.scholix.app;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;

import java.util.Collections;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<Platform> accountList;
    private Context context;


    public AccountAdapter(Context context,List<Platform> accountList) {
        this.accountList = accountList;
        this.context = context;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Platform account = accountList.get(position);
        System.out.println(account);
        System.out.println(account);
        System.out.println(account);
        System.out.println(account);
        holder.username.setText(account.getUsername());
        holder.name.setText(account.getName());

        holder.password.setText(account.getPassword());
        holder.mainGlow.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        if (account.isEditing()) {
            // Edit Mode
            holder.username.setEnabled(true);
            holder.password.setEnabled(true);
            holder.name.setEnabled(true);
            holder.username.setVisibility(View.VISIBLE);
            holder.password.setVisibility(View.VISIBLE);
            holder.saveBtn.setVisibility(View.VISIBLE);
            holder.editBtn.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.password.setInputType(InputType.TYPE_CLASS_TEXT);

        } else {
            // View Mode
            holder.username.setEnabled(false);
            holder.password.setEnabled(false);
            holder.username.setVisibility(View.GONE);

            holder.password.setVisibility(View.GONE);
            holder.saveBtn.setVisibility(View.GONE);
            holder.editBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setVisibility(View.VISIBLE);
        }

        // Edit button clicked → switch to edit mode
        holder.editBtn.setOnClickListener(v -> {
            account.startEditing();
            notifyItemChanged(position);

            RecyclerView recyclerView = (RecyclerView) holder.itemView.getParent();
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));

            NestedScrollView scrollView = ((PlatformsActivity) holder.itemView.getContext()).findViewById(R.id.nested_scroll);
            scrollView.postDelayed(() -> {
                scrollView.smoothScrollTo(0, holder.itemView.getTop() + 1000);
            }, 100);
        });

        // Save button clicked
        holder.saveBtn.setOnClickListener(v -> {
            String updatedName = holder.name.getText().toString();

            String updatedUsername = holder.username.getText().toString();
            String updatedPassword = holder.password.getText().toString();

            if (updatedUsername.isEmpty() || updatedPassword.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate Webtop login
            new Thread(() -> {
                try {
                    LoginManager loginManager = new LoginManager();
                    boolean result = PlatformStorage.checkPlatform(context,updatedUsername, updatedPassword);

                    ((PlatformsActivity) holder.itemView.getContext()).runOnUiThread(() -> {
                        if (result) {
                            account.setName(updatedName);
                            account.setUsername(updatedUsername);
                            account.setPassword(updatedPassword);
                            account.stopEditing();
                            PlatformStorage.updatePlatform(context, position, account);
                            notifyItemChanged(position);
                            Toast.makeText(holder.itemView.getContext(), "Webtop Account Updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(holder.itemView.getContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((PlatformsActivity) holder.itemView.getContext()).runOnUiThread(() ->
                            Toast.makeText(holder.itemView.getContext(), "Login Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();

            // Close keyboard
            InputMethodManager imm = (InputMethodManager) holder.itemView.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
        });

        // Delete button
        holder.deleteBtn.setOnClickListener(v -> {
            PlatformStorage.removePlatform(context, position);
            accountList.remove(position); // remove from your adapter's data list
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, accountList.size()); // optional: keep indices in sync
        });

    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        EditText username, password, name;

        Button editBtn, deleteBtn, saveBtn;
        View mainGlow;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.account_username);
            password = itemView.findViewById(R.id.account_password);
            name = itemView.findViewById(R.id.account_name);

            editBtn = itemView.findViewById(R.id.edit_button);
            deleteBtn = itemView.findViewById(R.id.delete_button);
            saveBtn = itemView.findViewById(R.id.save_button);
            mainGlow = itemView.findViewById(R.id.main_glow); // 🔥 Add this

        }
    }
    public List<Platform> getPlatforms() {
        return accountList;
    }

    public void swapItems(int from, int to) {
        Collections.swap(accountList, from, to);
        notifyItemMoved(from, to);
        // rebind the affected range so your “position==0” glow logic runs
        notifyItemRangeChanged(Math.min(from, to), Math.abs(from - to) + 1);
    }

}

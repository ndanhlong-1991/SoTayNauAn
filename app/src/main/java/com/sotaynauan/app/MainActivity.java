package com.sotaynauan.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_PHOTO = 101;
    private static final int REQUEST_EXPORT = 102;
    private static final int REQUEST_IMPORT = 103;

    private static final int BROWN = Color.rgb(138, 60, 32);
    private static final int CREAM = Color.rgb(250, 247, 241);
    private static final int INK = Color.rgb(43, 39, 35);
    private static final int MUTED = Color.rgb(111, 105, 96);
    private static final String[] CATEGORIES =
        {"Món Việt", "Món Âu", "Món Á", "Món chay", "Khác"};
    private static final String[] FILTERS =
        {"Tất cả", "Món Việt", "Món Âu", "Món Á", "Món chay", "Khác"};

    private SharedPreferences store;
    private JSONArray recipes;
    private LinearLayout root, list, editor, photoArea;
    private ScrollView scrollView;
    private EditText search, nameInput, ingredientInput, instructionInput;
    private Spinner filterSpinner, categorySpinner;
    private TextView summary, editorTitle;
    private Button editToggle;
    private int editingIndex = -1;
    private String selectedPhoto = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BROWN);
        getWindow().setNavigationBarColor(Color.BLACK);
        store = getSharedPreferences("MonAn", MODE_PRIVATE);
        try {
            recipes = new JSONArray(store.getString("data", "[]"));
        } catch (Exception ignored) {
            recipes = new JSONArray();
        }
        buildScreen();
        refreshList();
    }

    private int dp(float size) {
        return (int) (size * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams margins(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(top), 0, dp(bottom));
        return lp;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setBackground(shape(color, 12));
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private void buildScreen() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(CREAM);
        root = column();
        root.setPadding(dp(18), dp(20), dp(18), dp(30));
        scrollView.addView(root);
        setContentView(scrollView);

        TextView title = text("🍲  SỔ TAY NẤU ĂN", 26, BROWN, true);
        root.addView(title, margins(0, 2));
        root.addView(text("Góc bếp nhỏ • Công thức của riêng bạn", 14, MUTED, false), margins(0, 20));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setHint("🔎  Tìm theo tên món hoặc nguyên liệu");
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        search.setBackground(shape(Color.WHITE, 12));
        root.addView(search, margins(0, 10));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { refreshList(); }
            public void afterTextChanged(Editable e) {}
        });

        filterSpinner = new Spinner(this);
        setSpinner(filterSpinner, FILTERS);
        root.addView(filterSpinner, margins(4, 8));
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshList();
            }
        });

        editToggle = button("＋ Thêm công thức mới", BROWN);
        root.addView(editToggle, margins(4, 14));
        editToggle.setOnClickListener(v -> {
            if (editor.getVisibility() == View.VISIBLE) {
                clearEditor();
                editor.setVisibility(View.GONE);
            } else {
                clearEditor();
                editor.setVisibility(View.VISIBLE);
            }
        });

        editor = column();
        editor.setPadding(dp(14), dp(14), dp(14), dp(14));
        editor.setBackground(shape(Color.WHITE, 14));
        root.addView(editor, margins(0, 16));
        editorTitle = text("Thêm công thức", 19, BROWN, true);
        editor.addView(editorTitle, margins(0, 8));
        nameInput = input("Tên món ăn", 1);
        editor.addView(nameInput, margins(0, 4));
        ingredientInput = input("Nguyên liệu và định lượng", 3);
        editor.addView(ingredientInput, margins(0, 4));
        instructionInput = input("Cách chế biến từng bước", 5);
        editor.addView(instructionInput, margins(0, 8));
        editor.addView(text("Phân loại món", 14, MUTED, true));
        categorySpinner = new Spinner(this);
        setSpinner(categorySpinner, CATEGORIES);
        editor.addView(categorySpinner, margins(0, 10));

        Button selectPhoto = button("📷  Chọn ảnh món ăn", Color.rgb(93, 104, 69));
        editor.addView(selectPhoto, margins(0, 8));
        selectPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PHOTO);
        });
        photoArea = column();
        editor.addView(photoArea, margins(0, 8));

        Button save = button("✓  Lưu công thức", BROWN);
        editor.addView(save, margins(0, 8));
        save.setOnClickListener(v -> saveRecipe());
        editor.setVisibility(View.GONE);

        summary = text("CÔNG THỨC ĐÃ LƯU", 20, INK, true);
        root.addView(summary, margins(8, 10));
        list = column();
        root.addView(list);

        root.addView(text("SAO LƯU VÀ KHÔI PHỤC", 17, BROWN, true), margins(24, 8));
        root.addView(text("Có thể chọn Google Drive khi lưu tệp sao lưu. Ảnh món ăn được đóng gói cùng công thức.", 13, MUTED, false), margins(0, 8));
        Button export = button("⬆  Sao lưu công thức", Color.rgb(76, 105, 75));
        Button restore = button("⬇  Khôi phục từ bản sao lưu", Color.rgb(86, 98, 112));
        root.addView(export, margins(0, 8));
        root.addView(restore, margins(0, 6));
        export.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "SoTayNauAn-SaoLuu.json");
            startActivityForResult(intent, REQUEST_EXPORT);
        });
        restore.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Khôi phục công thức")
            .setMessage("Bạn sẽ được chọn tệp JSON đã sao lưu. Công thức hiện tại chỉ thay đổi sau khi bạn xác nhận.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Chọn tệp", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_IMPORT);
            }).show());
    }

    private EditText input(String hint, int lines) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextColor(INK);
        field.setTextSize(16);
        if (lines == 1) field.setSingleLine(true);
        else { field.setMinLines(lines); field.setGravity(Gravity.TOP); }
        return field;
    }

    private void setSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
    }

    private void clearEditor() {
        editingIndex = -1;
        selectedPhoto = "";
        nameInput.setText("");
        ingredientInput.setText("");
        instructionInput.setText("");
        categorySpinner.setSelection(0);
        editorTitle.setText("Thêm công thức");
        refreshPhoto();
    }

    private void refreshPhoto() {
        photoArea.removeAllViews();
        if (selectedPhoto.isEmpty()) return;
        File photo = photoFile(selectedPhoto);
        if (!photo.exists()) return;
        ImageView image = new ImageView(this);
        image.setImageBitmap(BitmapFactory.decodeFile(photo.getAbsolutePath()));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoArea.addView(image, new LinearLayout.LayoutParams(-1, dp(160)));
        Button remove = button("Bỏ ảnh", Color.rgb(125, 125, 125));
        photoArea.addView(remove, margins(6, 0));
        remove.setOnClickListener(v -> { selectedPhoto = ""; refreshPhoto(); });
    }

    private void saveRecipe() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError("Hãy nhập tên món ăn");
            nameInput.requestFocus();
            return;
        }
        try {
            JSONObject item = new JSONObject();
            item.put("ten", name);
            item.put("nguyenlieu", ingredientInput.getText().toString().trim());
            item.put("cachlam", instructionInput.getText().toString().trim());
            item.put("loai", CATEGORIES[categorySpinner.getSelectedItemPosition()]);
            item.put("anh", selectedPhoto);
            if (editingIndex >= 0) recipes.put(editingIndex, item);
            else recipes.put(item);
            persist();
            clearEditor();
            editor.setVisibility(View.GONE);
            refreshList();
            Toast.makeText(this, "Đã lưu công thức", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Không thể lưu công thức", Toast.LENGTH_LONG).show();
        }
    }

    private void editRecipe(int index) {
        JSONObject item = recipes.optJSONObject(index);
        if (item == null) return;
        editingIndex = index;
        nameInput.setText(item.optString("ten"));
        ingredientInput.setText(item.optString("nguyenlieu"));
        instructionInput.setText(item.optString("cachlam"));
        String category = item.optString("loai", "Món Việt");
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) categorySpinner.setSelection(i);
        }
        selectedPhoto = item.optString("anh", "");
        editorTitle.setText("Sửa công thức");
        refreshPhoto();
        editor.setVisibility(View.VISIBLE);
        scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
    }

    private void deleteRecipe(int index) {
        JSONObject item = recipes.optJSONObject(index);
        if (item == null) return;
        new AlertDialog.Builder(this)
            .setTitle("Xóa công thức?")
            .setMessage("Xóa món “" + item.optString("ten") + "”? Không thể hoàn tác nếu chưa sao lưu.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa", (dialog, which) -> {
                JSONArray next = new JSONArray();
                for (int j = 0; j < recipes.length(); j++) {
                    if (j != index) next.put(recipes.opt(j));
                }
                recipes = next;
                persist();
                clearEditor();
                editor.setVisibility(View.GONE);
                refreshList();
            }).show();
    }

    private void refreshList() {
        if (list == null) return;
        list.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
        String filter = filterSpinner.getSelectedItem() == null ? "Tất cả" :
            filterSpinner.getSelectedItem().toString();
        int count = 0;
        for (int i = recipes.length() - 1; i >= 0; i--) {
            JSONObject item = recipes.optJSONObject(i);
            if (item == null) continue;
            String title = item.optString("ten");
            String category = item.optString("loai", "Món Việt");
            if (!filter.equals("Tất cả") && !filter.equals(category)) continue;
            String searchable = title + " " + item.optString("nguyenlieu");
            if (!searchable.toLowerCase(java.util.Locale.ROOT).contains(query)) continue;
            count++;
            final int position = i;
            LinearLayout card = column();
            card.setBackground(shape(Color.WHITE, 16));
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            root.setClipChildren(false);
            list.addView(card, margins(0, 12));

            File photo = photoFile(item.optString("anh", ""));
            if (photo.isFile()) {
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath());
                if (bitmap != null) {
                    ImageView pic = new ImageView(this);
                    pic.setImageBitmap(bitmap);
                    pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    card.addView(pic, new LinearLayout.LayoutParams(-1, dp(175)));
                }
            }
            card.addView(text("🍽  " + title, 20, BROWN, true), margins(10, 5));
            card.addView(text(category, 13, MUTED, false), margins(0, 7));
            card.addView(text("NGUYÊN LIỆU", 13, MUTED, true), margins(0, 5));
            card.addView(text(item.optString("nguyenlieu", "Chưa nhập"), 16, INK, false), margins(0, 9));
            card.addView(text("CÁCH CHẾ BIẾN", 13, MUTED, true), margins(0, 5));
            card.addView(text(item.optString("cachlam", "Chưa nhập"), 16, INK, false), margins(0, 14));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(actions);
            Button edit = button("✎ Sửa", Color.rgb(89, 112, 81));
            Button delete = button("Xóa", Color.rgb(169, 70, 56));
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(44), 1);
            left.setMargins(0, 0, dp(8), 0);
            actions.addView(edit, left);
            actions.addView(delete, new LinearLayout.LayoutParams(0, dp(44), 1));
            edit.setOnClickListener(v -> editRecipe(position));
            delete.setOnClickListener(v -> deleteRecipe(position));
        }
        summary.setText("CÔNG THỨC ĐÃ LƯU (" + count + ")");
        if (count == 0) {
            TextView empty = text("Chưa có món ăn phù hợp. Hãy thêm công thức đầu tiên!", 15, MUTED, false);
            empty.setPadding(dp(8), dp(18), dp(8), dp(20));
            list.addView(empty);
        }
    }

    private void persist() {
        store.edit().putString("data", recipes.toString()).apply();
    }

    private File photoFile(String filename) {
        if (filename == null || !filename.startsWith("photo_") || !filename.endsWith(".jpg")
                || filename.contains("/") || filename.contains("\\")) {
            return new File(getFilesDir(), "__missing_photo__");
        }
        return new File(getFilesDir(), filename);
    }

    private String writePhoto(byte[] bytes) throws Exception {
        String filename = "photo_" + UUID.randomUUID() + ".jpg";
        FileOutputStream output = new FileOutputStream(photoFile(filename));
        try { output.write(bytes); } finally { output.close(); }
        return filename;
    }

    private String importPhoto(Uri uri) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream probe = getContentResolver().openInputStream(uri);
        try { BitmapFactory.decodeStream(probe, null, options); } finally { if (probe != null) probe.close(); }
        int scale = 1;
        while (options.outWidth / scale > 1400 || options.outHeight / scale > 1400) scale *= 2;
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;
        InputStream source = getContentResolver().openInputStream(uri);
        Bitmap image;
        try { image = BitmapFactory.decodeStream(source, null, options); }
        finally { if (source != null) source.close(); }
        if (image == null) throw new Exception("Ảnh không hợp lệ");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 78, output);
        image.recycle();
        return writePhoto(output.toByteArray());
    }

    private byte[] readBytes(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        try { while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n); }
        finally { input.close(); }
        return output.toByteArray();
    }

    private void exportBackup(Uri uri) {
        try {
            JSONArray exported = new JSONArray();
            for (int i = 0; i < recipes.length(); i++) {
                JSONObject original = recipes.optJSONObject(i);
                if (original == null) continue;
                JSONObject item = new JSONObject(original.toString());
                File photo = photoFile(item.optString("anh", ""));
                if (photo.isFile()) {
                    item.put("imageBase64", Base64.encodeToString(readBytes(photo), Base64.NO_WRAP));
                }
                exported.put(item);
            }
            JSONObject backup = new JSONObject();
            backup.put("format", "SoTayNauAn");
            backup.put("version", 2);
            backup.put("recipes", exported);
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) throw new Exception("Không thể mở tệp");
            try { out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8)); }
            finally { out.close(); }
            Toast.makeText(this, "Đã xuất bản sao lưu", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Sao lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importBackup(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("Không mở được tệp");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            try {
                while ((n = in.read(buffer)) != -1) {
                    if (output.size() + n > 80 * 1024 * 1024) throw new Exception("Tệp sao lưu quá lớn");
                    output.write(buffer, 0, n);
                }
            } finally { in.close(); }
            JSONObject backup = new JSONObject(output.toString("UTF-8"));
            if (!"SoTayNauAn".equals(backup.optString("format"))) {
                throw new Exception("Không đúng định dạng sao lưu Sổ Tay Nấu Ăn");
            }
            JSONArray restored = backup.getJSONArray("recipes");
            if (restored.length() > 2000) throw new Exception("Quá nhiều công thức");
            new AlertDialog.Builder(this)
                .setTitle("Nhập " + restored.length() + " công thức")
                .setMessage("Chọn Gộp để giữ món đang có, hoặc Thay thế để dùng danh sách từ bản sao lưu.")
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Thay thế", (d, w) -> applyBackup(restored, false))
                .setPositiveButton("Gộp", (d, w) -> applyBackup(restored, true))
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được bản sao lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyBackup(JSONArray source, boolean merge) {
        try {
            JSONArray next = new JSONArray();
            if (merge) {
                for (int i = 0; i < recipes.length(); i++) next.put(recipes.opt(i));
            }
            for (int i = 0; i < source.length(); i++) {
                JSONObject original = source.optJSONObject(i);
                if (original == null) continue;
                JSONObject item = new JSONObject();
                item.put("ten", original.optString("ten"));
                item.put("nguyenlieu", original.optString("nguyenlieu"));
                item.put("cachlam", original.optString("cachlam"));
                item.put("loai", original.optString("loai", "Món Việt"));
                String encoded = original.optString("imageBase64", "");
                if (!encoded.isEmpty()) {
                    byte[] photo = Base64.decode(encoded, Base64.DEFAULT);
                    if (photo.length > 12 * 1024 * 1024) throw new Exception("Có ảnh quá lớn");
                    item.put("anh", writePhoto(photo));
                } else {
                    item.put("anh", "");
                }
                next.put(item);
            }
            recipes = next;
            persist();
            clearEditor();
            editor.setVisibility(View.GONE);
            refreshList();
            Toast.makeText(this, "Khôi phục thành công", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Khôi phục thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_PHOTO) {
            try {
                selectedPhoto = importPhoto(uri);
                refreshPhoto();
            } catch (Exception e) {
                Toast.makeText(this, "Không thể thêm ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_EXPORT) {
            exportBackup(uri);
        } else if (requestCode == REQUEST_IMPORT) {
            importBackup(uri);
        }
    }
}

package com.sotaynauan.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    LinearLayout danhSach;
    JSONArray congThuc;
    SharedPreferences luuTru;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        luuTru = getSharedPreferences("MonAn", MODE_PRIVATE);

        try {
            congThuc = new JSONArray(
                luuTru.getString("data", "[]")
            );
        } catch (Exception e) {
            congThuc = new JSONArray();
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 30);
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SỔ TAY NẤU ĂN");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(180, 65, 20));
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        EditText ten = new EditText(this);
        ten.setHint("Tên món ăn");
        root.addView(ten);

        EditText nguyenLieu = new EditText(this);
        nguyenLieu.setHint("Nguyên liệu");
        nguyenLieu.setMinLines(2);
        root.addView(nguyenLieu);

        EditText cachLam = new EditText(this);
        cachLam.setHint("Các bước chế biến");
        cachLam.setMinLines(3);
        root.addView(cachLam);

        Button them = new Button(this);
        them.setText("LƯU CÔNG THỨC");
        root.addView(them);

        TextView label = new TextView(this);
        label.setText("CÔNG THỨC ĐÃ LƯU");
        label.setTextSize(20);
        label.setTextColor(Color.BLACK);
        label.setPadding(0, 28, 0, 15);
        root.addView(label);

        danhSach = new LinearLayout(this);
        danhSach.setOrientation(LinearLayout.VERTICAL);
        root.addView(danhSach);

        them.setOnClickListener(v -> {
            String mon = ten.getText().toString().trim();

            if (mon.isEmpty()) {
                Toast.makeText(this,
                    "Hãy nhập tên món ăn",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject item = new JSONObject();
                item.put("ten", mon);
                item.put("nguyenlieu",
                    nguyenLieu.getText().toString());
                item.put("cachlam",
                    cachLam.getText().toString());

                congThuc.put(item);

                luuTru.edit()
                    .putString("data", congThuc.toString())
                    .apply();

                ten.setText("");
                nguyenLieu.setText("");
                cachLam.setText("");

                hienThi();

                Toast.makeText(this,
                    "Đã lưu công thức!",
                    Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this,
                    "Không thể lưu công thức",
                    Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(scroll);
        hienThi();
    }

    void hienThi() {
        danhSach.removeAllViews();

        for (int i = congThuc.length() - 1; i >= 0; i--) {
            JSONObject item = congThuc.optJSONObject(i);
            if (item == null) continue;

            TextView text = new TextView(this);
            text.setText(
                "🍲 " + item.optString("ten") +
                "\n\nNguyên liệu:\n" +
                item.optString("nguyenlieu") +
                "\n\nCách làm:\n" +
                item.optString("cachlam")
            );

            text.setTextSize(17);
            text.setTextColor(Color.BLACK);
            text.setTextIsSelectable(true);
            text.setPadding(16, 20, 16, 25);
            danhSach.addView(text);

            View line = new View(this);
            line.setBackgroundColor(Color.LTGRAY);
            danhSach.addView(line,
                new LinearLayout.LayoutParams(-1, 2));
        }
    }
}

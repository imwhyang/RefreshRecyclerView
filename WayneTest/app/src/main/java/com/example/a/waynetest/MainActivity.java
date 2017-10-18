package com.example.a.waynetest;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a.waynetest.adapter.BaseRecAdapter;
import com.example.a.waynetest.adapter.RecyHeaderAdapter;
import com.example.a.waynetest.adapter.ViewHolder;
import com.example.a.waynetest.weight.DividerGridItemDecoration;
import com.example.a.waynetest.weight.DividerItemDecoration;
import com.example.a.waynetest.weight.RefreshRecyclerView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Bind(R.id.recyclerview)
    RefreshRecyclerView recyclerview;
    private String MOSPACKAGE = "com.zhonghe.shiangou";
    private String MPOSCLASSNAME = MOSPACKAGE + ".ui.activity.SplashActivity";

    //如下是本应用的包名
    private static String PACKAGE = "com.example.a.waynetest";
    private ArrayList<String> data;
    private BaseRecAdapter<String> baseRecAdapter;
    private RecyHeaderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return false;
            }

        });

        data = new ArrayList<>();
        String str = "...textstextstextstextstextstextstextstextstextstextstextstexts";
        for (int i = 0; i < 20; i++) {
            data.add("item" +i+ str.substring(0, i % 3 * 15));
        }
        baseRecAdapter = new BaseRecAdapter<String>(this, R.layout.item_recyc, data) {
            @Override
            public void convert(ViewHolder holder, String o) {
                holder.setText(R.id.textview, o + o);
            }
        };
       adapter = new RecyHeaderAdapter(baseRecAdapter);

        //        不要忘记设置布局管理器
//        recyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
//        recyclerview.setLayoutManager(new GridLayoutManager(this,2));
        recyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

//        给Recycler设置分割线
//        recyclerview.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recyclerview.addItemDecoration(new DividerGridItemDecoration(this));
//        recyclerview.setPullLoadEnable(false);
//        recyclerview.setPullRefreshEnable(false);
//        recyclerview.addHeaderView( recyclerview.getHeaderView(), adapter);
//        recyclerview. addFooterView( recyclerview.getFooterView(), adapter);
        TextView t1 = new TextView(this);
        t1.setBackgroundColor(Color.BLACK);
        t1.setTextColor(Color.WHITE);
        t1.setText("Header 1");
        adapter.addHeaderView(t1);
//        recyclerview.setAdapter(adapter);
        recyclerview.setWrapperAdapter(adapter);

        recyclerview.setOnRefreshListener(new OnRecyclerRefreshListener());
        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
//        tv.setOnClickListener(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recycler_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_linear:
                recyclerview.setLayoutManager(new LinearLayoutManager(this));
                recyclerview.setPullLoadEnable(false);
                recyclerview.setPullRefreshEnable(false);
                break;
            case R.id.action_grid:
                recyclerview.setLayoutManager(new GridLayoutManager(this, 2));
                recyclerview.setPullLoadEnable(false);
                recyclerview.setPullRefreshEnable(false);
                break;
            case R.id.action_staggered:
                recyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                recyclerview.setPullLoadEnable(false);
                recyclerview.setPullRefreshEnable(false);
                break;
        }
        recyclerview.setAdapter(adapter);

        return super.onOptionsItemSelected(item);
    }

    private static final int REFRESH = 0;
    private static final int LOADMORE = 1;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    recyclerview.onFinishRefresh(true);
                    break;
                case LOADMORE:
                    recyclerview.onFinishRefresh(false);
                    break;
            }
        }
    };

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public void onClick(View v) {
        //首先判断调用的apk是否安装
        boolean isAlive = Utils.isInstall(MainActivity.this, MOSPACKAGE);
        if (isAlive) {
            Intent intent = new Intent(Intent.ACTION_MAIN);//设置action
            intent.addCategory(Intent.CATEGORY_LAUNCHER);//设置category
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//设置singleTask启动模式
            ComponentName cn = new ComponentName(MOSPACKAGE, MPOSCLASSNAME);//封装了包名 + 类名
            //设置数据
            intent.putExtra("package", PACKAGE);
            intent.putExtra("className", MPOSCLASSNAME);
            intent.putExtra("isOnLineSign", true);
            intent.setComponent(cn);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "没有找到对应的应用程序", Toast.LENGTH_SHORT).show();
        }
    }

    private class OnRecyclerRefreshListener implements RefreshRecyclerView.OnRefreshListener {
        @Override
        public void onPullDownRefresh() {
            Message message = new Message();
            message.what = REFRESH;
            handler.sendMessageDelayed(message, 2000);

        }

        @Override
        public void onLoadingMore() {
            Log.d("LOADMORE","LOADMORE");
            Message message = new Message();
            message.what = LOADMORE;
            handler.sendMessageDelayed(message, 2000);
        }
    }
}

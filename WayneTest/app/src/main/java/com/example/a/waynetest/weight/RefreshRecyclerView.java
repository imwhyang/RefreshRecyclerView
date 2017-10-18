package com.example.a.waynetest.weight;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.a.waynetest.R;
import com.example.a.waynetest.adapter.RecyHeaderAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * auther: whyang
 * date: 2017/10/13
 * desc:
 */

public class RefreshRecyclerView extends RecyclerView {
    private Context mContext;
    //    顶部视图，下拉刷新控件
    private LinearLayout headerView;
    //    正在刷新状态的进度条
    private ProgressBar pb_header_refresh;
    //    刷新箭头
    private ImageView iv_header_refresh;
    //    显示刷新状态
    private TextView tv_status;
    //    显示最近一次的刷新时间
    private TextView tv_time;
    //    转到下拉刷新状态时的动画
    private RotateAnimation downAnima;
    //     转到释放刷新状态时的动画
    private RotateAnimation upAnima;
    //    尾部视图
    private View footerView;
    //    尾部（上拉加载控件）的高度
    private int footerViewHeight;
    //触摸事件中按下的Y坐标，初始值为-1，为防止ACTION_DOWN事件被抢占
    private float startY = -1;
    //    下拉刷新控件的高度
    private int pulldownHeight;
    //    刷新状态：下拉刷新
    private final int PULL_DOWN_REFRESH = 0;
    //    刷新状态：释放刷新
    private final int RELEASE_REFRESH = 1;
    //    刷新状态：正常刷新
    private final int REFRESHING = 2;
    //    当前头布局的状态-默认为下拉刷新
    private int currState = PULL_DOWN_REFRESH;
    //    刷新事件的回调
    private RefreshRecyclerView.OnRefreshListener mOnRefreshListener;
    //    判断是否是加载更多
    private boolean isLoadingMore;
    // StaggeredGridLayoutManager
    private int[] lastPositions;
    //是否需要刷新
    private boolean isNeedLoadingMore = true;
    private boolean isNeedRefresh = true;

    public RefreshRecyclerView(Context context) {
        this(context, null);
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initHeaderView();
        initFooterView();
    }

    /**
     * 是否加载更多
     *
     * @param enable
     */
    public void setPullLoadEnable(boolean enable) {
        isNeedLoadingMore = enable;
    }

    /**
     * 是否刷新
     *
     * @param enable
     */
    public void setPullRefreshEnable(boolean enable) {
        isNeedRefresh = enable;
    }

    /**
     * 返回尾部布局，供外部调用
     *
     * @return
     */
    public View getFooterView() {
        return footerView;
    }

    /**
     * 返回头部布局，供外部调用
     *
     * @return
     */
    public View getHeaderView() {
        return headerView;
    }

    /**
     * 通过HeaderAndFooterWrapper对象给RecyclerView添加尾部
     *
     * @param footerView             尾部视图
     * @param headerAndFooterWrapper RecyclerView.Adapter的包装类对象，通过它给RecyclerView添加尾部视图
     */
    public void addFooterView(View footerView, RecyHeaderAdapter headerAndFooterWrapper) {
        headerAndFooterWrapper.addFooterView(footerView);
    }

    /**
     * 通过HeaderAndFooterWrapper对象给RecyclerView添加头部部
     *
     * @param headerView             尾部视图
     * @param headerAndFooterWrapper RecyclerView.Adapter的包装类对象，通过它给RecyclerView添加头部视图
     */
    public void addHeaderView(View headerView, RecyHeaderAdapter headerAndFooterWrapper) {
        headerAndFooterWrapper.addHeaderView(headerView);
    }

    private void initHeaderView() {
        headerView = (LinearLayout) View.inflate(mContext, R.layout.layout_recyclerview_header, null);
        tv_time = (TextView) headerView.findViewById(R.id.tv_time);
        tv_status = (TextView) headerView.findViewById(R.id.tv_status);
        iv_header_refresh = (ImageView) headerView.findViewById(R.id.iv_header_refresh);
        pb_header_refresh = (ProgressBar) headerView.findViewById(R.id.pb_header_refresh);
        headerView.measure(0, 0);
        pulldownHeight = headerView.getMeasuredHeight();
        headerView.setPadding(0, -pulldownHeight, 0, 0);
        //初始化头部布局的动画
        initAnimation();
    }

    /**
     * 刷新状态改变时的动画
     */
    private void initAnimation() {
//        从下拉刷新状态转换为释放刷新状态
        upAnima = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        upAnima.setFillAfter(true);
        upAnima.setDuration(500);
//         转到下拉刷新的动画
        downAnima = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        downAnima.setFillAfter(true);
        downAnima.setDuration(500);
    }

    private void initFooterView() {
        footerView = View.inflate(mContext, R.layout.layout_recyclerview_footer, null);
        footerView.measure(0, 0);
        //得到控件的高
        footerViewHeight = footerView.getMeasuredHeight();
        //默认隐藏下拉刷新控件
        // View.setPadding(0,-控件高，0,0);//完全隐藏
        //View.setPadding(0, 0，0,0);//完全显示
        footerView.setPadding(0, -footerViewHeight, 0, 0);
//        addFooterView(footerView);
//        自己监听自己
        this.addOnScrollListener(new MyOnScrollListener());
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isNeedRefresh) {//不用刷新直接return
            return super.onTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN://按下
                startY = ev.getY();
//                Log.d("ACTION_DOWN.........Y", startY + "");
                break;
            case MotionEvent.ACTION_MOVE:
                //防止ACTION_DOWN事件被抢占，没有执行
                if (startY == -1) {
                    startY = ev.getY();
                }
                float endY = ev.getY();
                float dY = endY - startY;
//                Log.d("ACTION_MOVE.......Y", endY + "");
                //判断当前是否正在刷新中
                if (currState == REFRESHING) {
                    //如果当前是正在刷新，不执行下拉刷新了，直接break;
                    break;
                }
                LayoutManager layoutManager = getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                    int firstCompletelyVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                    int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    Log.d("firstposition.Complete", firstCompletelyVisibleItemPosition + "....firstVisibleItemPosition..." + firstVisibleItemPosition);
                    if (firstVisibleItemPosition == 0) {
                        //                如果是下拉
                        if (dY > 0) {
                            int paddingTop = (int) (dY / 3 - pulldownHeight);
                            if (paddingTop > 0 && currState != RELEASE_REFRESH) {
                                //完全显示下拉刷新控件，进入松开刷新状态
                                currState = RELEASE_REFRESH;
                                refreshViewState();
                            } else if (paddingTop < 0 && currState != PULL_DOWN_REFRESH) {
                                //没有完全显示下拉刷新控件，进入下拉刷新状态
                                currState = PULL_DOWN_REFRESH;
                                refreshViewState();
                            }
                            headerView.setPadding(0, paddingTop, 0, 0);
                        }
                    }
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    if (lastPositions == null) {
                        lastPositions = new int[staggeredGridLayoutManager.getSpanCount()];
                    }

                    int[] firstVisibleItemPositions = staggeredGridLayoutManager.findFirstVisibleItemPositions(lastPositions);
                    Log.d("firstposition.Complete", firstVisibleItemPositions[0] + "");
                    if (firstVisibleItemPositions[0] == 0) {
                        //                如果是下拉
                        if (dY > 0) {
                            int paddingTop = (int) (dY / 3 - pulldownHeight);
                            if (paddingTop > 0 && currState != RELEASE_REFRESH) {
                                //完全显示下拉刷新控件，进入松开刷新状态
                                currState = RELEASE_REFRESH;
                                refreshViewState();
                            } else if (paddingTop < 0 && currState != PULL_DOWN_REFRESH) {
                                //没有完全显示下拉刷新控件，进入下拉刷新状态
                                currState = PULL_DOWN_REFRESH;
                                refreshViewState();
                            }
                            headerView.setPadding(0, paddingTop, 0, 0);
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                //5.重新记录值
                startY = -1;
                if (currState == PULL_DOWN_REFRESH) {
                    //设置默认隐藏
                    headerView.setPadding(0, -pulldownHeight, 0, 0);
                } else if (currState == RELEASE_REFRESH) {
                    //当前是释放刷新，进入到正在刷新状态，完全显示
                    currState = REFRESHING;
                    refreshViewState();
                    headerView.setPadding(0, 0, 0, 0);
                    //调用用户的回调事件，刷新页面数据
                    if (mOnRefreshListener != null) {
                        mOnRefreshListener.onPullDownRefresh();
                    }
                }
                break;
        }

        return super.onTouchEvent(ev);
    }

    /**
     * 跳转刷新状态
     */
    private void refreshViewState() {
        switch (currState) {
//            跳转到下拉刷新
            case PULL_DOWN_REFRESH:
                iv_header_refresh.startAnimation(downAnima);
                tv_status.setText("下拉刷新");
                break;
//            跳转到释放刷新
            case RELEASE_REFRESH:
                iv_header_refresh.startAnimation(upAnima);
                tv_status.setText("释放刷新");
                break;
//            跳转到正在刷新
            case REFRESHING:
                iv_header_refresh.clearAnimation();
                iv_header_refresh.setVisibility(GONE);
                pb_header_refresh.setVisibility(VISIBLE);
                tv_status.setText("正在刷新中.....");
                break;
        }
    }

    public void setWrapperAdapter(RecyHeaderAdapter wrapperAdapter) {
        addHeaderView(getHeaderView(), wrapperAdapter);
        addFooterView(getFooterView(), wrapperAdapter);
        super.setAdapter(wrapperAdapter);
    }

    /**
     * 定义下拉刷新和上拉加载的接口
     */
    public interface OnRefreshListener {
        /**
         * 当下拉刷新时触发此方法
         */
        void onPullDownRefresh();

        /**
         * 当加载更多的时候回调这个方法
         */
        void onLoadingMore();

    }

    public void setOnRefreshListener(RefreshRecyclerView.OnRefreshListener listener) {
        this.mOnRefreshListener = listener;
    }

    /**
     * 当刷新完数据之后，调用此方法，把头文件隐藏，并且状态设置为初始状态
     *
     * @param isSuccess
     */
    public void onFinishRefresh(boolean isSuccess) {
        if (isLoadingMore) {
            footerView.setPadding(0, -footerViewHeight, 0, 0);
            isLoadingMore = false;
        } else {
            headerView.setPadding(0, -pulldownHeight, 0, 0);
            currState = PULL_DOWN_REFRESH;
            iv_header_refresh.setVisibility(VISIBLE);
            pb_header_refresh.setVisibility(GONE);
            tv_status.setText("下拉刷新");
            if (isSuccess) {
                //设置更新时间
                tv_time.setText(getSystemTime());
            }
        }
    }

    private String getSystemTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    /**
     * recyclerView 滑动监听
     * 滑动到最后一个时启动加载更多
     */
    private class MyOnScrollListener extends OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (currState == REFRESHING || isLoadingMore == true || !isNeedLoadingMore) {
                //如果当前是正在刷新，或者加载更多，或者不用加载更多，不执行;
                return;
            }
            if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_SETTLING) {
                //判断是当前layoutManager是否为LinearLayoutManager
                // 只有LinearLayoutManager才有查找第一个和最后一个可见view位置的方法
                LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                    //当停止滚动时或者惯性滚动时，RecyclerView的最后一个显示的条目：getCount()-1
///                    注意是findLastVisibleItemPosition()而不是getLastVisiblePosition
                    if (linearLayoutManager.findLastVisibleItemPosition() >= getChildCount() - 1) {
                        isLoadingMore = true;
                        //把底部加载显示
                        footerView.setPadding(0, 0, 0, 0);
                        if (mOnRefreshListener != null) {
                            mOnRefreshListener.onLoadingMore();
                        }
                    }
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    if (lastPositions == null) {
                        lastPositions = new int[staggeredGridLayoutManager.getSpanCount()];
                    }
                    int[] firstVisibleItemPositions = staggeredGridLayoutManager.findLastVisibleItemPositions(lastPositions);
                    Log.d("firstposition.Complete", firstVisibleItemPositions[0] + "");
                    //滑动到最后一个
                    if (firstVisibleItemPositions[0] >= getChildCount() - 1) {
                        isLoadingMore = true;
                        //把底部加载显示
                        footerView.setPadding(0, 0, 0, 0);
                        if (mOnRefreshListener != null) {
                            mOnRefreshListener.onLoadingMore();
                        }
                    }
                }
            }
        }
    }


}

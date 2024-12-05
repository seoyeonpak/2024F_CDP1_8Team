package com.dashboard.obd.driving;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.dashboard.obd.R;

public class NaverView extends Fragment {

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 초기화 작업이 필요한 경우 작성
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 레이아웃 인플레이션
        return inflater.inflate(R.layout.naverview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // WebView 초기화
        mWebView = view.findViewById(R.id.naverView); // WebView ID

        if (mWebView != null) {
            // WebView 설정
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);  // 자바스크립트 허용
            webSettings.setDomStorageEnabled(true);  // 로컬 저장소 허용
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 캐시 사용 안함

            // WebViewClient 설정: 링크 클릭 시 새 창을 띄우지 않도록
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    view.loadUrl(request.getUrl().toString());
                    return true;
                }
            });

            // WebChromeClient 설정: 페이지 진행 상태 등을 처리
            mWebView.setWebChromeClient(new WebChromeClient());

            // 네이버 웹 페이지 로드
            mWebView.loadUrl("http://itabus.duckdns.org:50001/");
        }
    }

    // 뒤로가기 처리
    public boolean onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();  // WebView에서 뒤로 가기
            return true;  // 뒤로 가기 처리 완료
        }
        return false;  // WebView에서 뒤로 갈 수 없으면 기본 뒤로 가기 동작 수행
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // WebView 리소스 해제
        if (mWebView != null) {
            mWebView.clearCache(true);
            mWebView.clearHistory();
            mWebView.destroy();
        }
    }
}

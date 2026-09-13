import SwiftUI
import WebKit

public struct WebViewContainer: UIViewRepresentable {
    @ObservedObject var state: BrowserState
    let tabId: UUID

    public func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    public func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()

        // 1. Azokle Shield Privacy Scriptlet
        let shieldScriptSource = """
        (function() {
            'use strict';
            if (window.__azokle_shield_active) return;
            window.__azokle_shield_active = true;

            // Hardware Uniformity
            try {
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: function() { return 4; }, configurable: true });
                Object.defineProperty(navigator, 'deviceMemory', { get: function() { return 8; }, configurable: true });
            } catch(e) {}

            // Battery API Shield
            try {
                if (navigator.getBattery) {
                    navigator.getBattery = function() {
                        return Promise.resolve({
                            charging: true,
                            chargingTime: 0,
                            dischargingTime: Infinity,
                            level: 1.0,
                            addEventListener: function() {},
                            removeEventListener: function() {}
                        });
                    };
                }
            } catch(e) {}

            // WebRTC Local IP Obfuscation
            try {
                var OrigRTC = window.RTCPeerConnection || window.webkitRTCPeerConnection;
                if (OrigRTC && OrigRTC.prototype) {
                    var origCreateOffer = OrigRTC.prototype.createOffer;
                    if (origCreateOffer) {
                        OrigRTC.prototype.createOffer = function() {
                            return origCreateOffer.apply(this, arguments).then(function(offer) {
                                if (offer && offer.sdp) {
                                    offer.sdp = offer.sdp.split('\\r\\n').filter(function(line) {
                                        return line.indexOf('a=candidate:') !== 0 || line.indexOf(' typ host') === -1;
                                    }).join('\\r\\n');
                                }
                                return offer;
                            });
                        };
                    }
                }
            } catch(e) {}
        })();
        """

        let shieldScript = WKUserScript(
            source: shieldScriptSource,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: false
        )
        configuration.userContentController.addUserScript(shieldScript)

        // 2. Preferences & Media Playback
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.allowsInlineMediaPlayback = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = UIColor(Color(hex: 0x101010))

        context.coordinator.setupObservers(for: webView)

        if let tab = state.tabs.first(where: { $0.id == tabId }),
           let url = URL(string: tab.url) {
            let request = URLRequest(url: url)
            webView.load(request)
        }

        return webView
    }

    public func updateUIView(_ webView: WKWebView, context: Context) {
        guard let tab = state.tabs.first(where: { $0.id == tabId }) else { return }

        // Update Desktop / Mobile Mode User-Agent
        if state.desktopMode {
            webView.customUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        } else {
            webView.customUserAgent = nil
        }

        // Check if URL changed externally (e.g. from omnibox navigation)
        if let currentUrl = webView.url?.absoluteString, currentUrl != tab.url {
            if let newUrl = URL(string: tab.url) {
                let request = URLRequest(url: newUrl)
                webView.load(request)
            }
        }
    }

    public class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var parent: WebViewContainer
        private var progressObservation: NSKeyValueObservation?
        private var titleObservation: NSKeyValueObservation?
        private var urlObservation: NSKeyValueObservation?
        private var canGoBackObservation: NSKeyValueObservation?
        private var canGoForwardObservation: NSKeyValueObservation?

        init(_ parent: WebViewContainer) {
            self.parent = parent
        }

        func setupObservers(for webView: WKWebView) {
            progressObservation = webView.observe(\.estimatedProgress, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].progress = webView.estimatedProgress
                    }
                }
            }

            titleObservation = webView.observe(\.title, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let title = webView.title, !title.isEmpty,
                       let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].title = title
                    }
                }
            }

            urlObservation = webView.observe(\.url, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let url = webView.url?.absoluteString, !url.isEmpty,
                       let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].url = url
                    }
                }
            }

            canGoBackObservation = webView.observe(\.canGoBack, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].canGoBack = webView.canGoBack
                    }
                }
            }

            canGoForwardObservation = webView.observe(\.canGoForward, options: [.new]) { [weak self] webView, _ in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                        self.parent.state.tabs[index].canGoForward = webView.canGoForward
                    }
                }
            }
        }

        public func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = true
                }
            }
        }

        public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = false
                    self.parent.state.tabs[index].progress = 1.0
                    // Simulate tracker blocking tally
                    if self.parent.state.isShieldActive {
                        self.parent.state.tabs[index].blockedTrackers = Int.random(in: 4...18)
                    }
                }
            }
        }

        public func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            DispatchQueue.main.async {
                if let index = self.parent.state.tabs.firstIndex(where: { $0.id == self.parent.tabId }) {
                    self.parent.state.tabs[index].isLoading = false
                }
            }
        }

        // Handle target="_blank" or window.open in the same tab or new tab safely
        public func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            if navigationAction.targetFrame == nil {
                webView.load(navigationAction.request)
            }
            return nil
        }
    }
}
